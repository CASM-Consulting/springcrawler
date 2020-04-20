package com.casm.acled.crawler.scraper.dates;

import com.casm.acled.crawler.reporting.Event;
import com.casm.acled.crawler.reporting.Report;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.scraper.ACLEDScraper;
import com.casm.acled.crawler.scraper.keywords.KeywordsService;
import com.casm.acled.crawler.scraper.locale.LocaleService;
import com.casm.acled.crawler.utils.Util;
import com.casm.acled.dao.entities.DeskDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.dao.entities.SourceSourceListDAO;
import com.casm.acled.entities.region.Desk;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.google.common.collect.ImmutableList;
import com.ibm.icu.util.ULocale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Service
public class DateTimeService {

    protected static final Logger logger = LoggerFactory.getLogger(KeywordsService.class);


    @Autowired
    private Reporter reporter;

    @Autowired
    private DeskDAO deskDAO;
    @Autowired
    private SourceSourceListDAO sourceSourceListDAO;
    @Autowired
    private SourceListDAO sourceListDAO;
    @Autowired
    private SourceDAO sourceDAO;

    @Autowired
    private LocaleService localeService;

    private Path scrapersPath;

    public DateTimeService() {
    }

    public void setScrapersPath(Path scrapersPath ) {
        this.scrapersPath = scrapersPath;
    }

    public void attemptAllDateTimeParsers(List<DateParser> dateParsers) {
        for(Desk desk : deskDAO.getAll()) {
            attemptDeskDateTimeParsers(desk, dateParsers);
        }
    }

    public void attemptDeskDateTimeParsers(Desk desk, List<DateParser> dateParsers) {
        List<SourceList> lists = sourceListDAO.byDesk(desk.id());

        for(SourceList list : lists) {
            attemptSourceListDateTimeParsers(list, dateParsers);
        }
    }

    public void attemptSourceListDateTimeParsers(SourceList list, List<DateParser> dateParsers) {
        List<Source> sources = sourceDAO.byList(list);

        for(Source source : sources) {
            try {
                attemptDateTimeParse(source, dateParsers, lastScrapeExampleGetter);
            } catch (IOException e) {
                reporter.report(Report.of(Event.SCRAPER_NOT_FOUND).message(e.getMessage()));
            }
        }
    }

    private final  Function<Source, List<String>> lastScrapeExampleGetter = source -> {
        String url = source.get(Source.LINK);

        String id = Util.getID(url);

        Path path = scrapersPath.resolve(id);
        Optional<String> maybeDate = DateUtil.extractDateFromLastScrapeJson(path);

        if(maybeDate.isPresent()) {
            return ImmutableList.of(maybeDate.get());
        } else {
            return ImmutableList.of();
        }
    };

    public void attemptDateTimeParse(Source source, List<DateParser> dateParsers, Function<Source, List<String>> exampleGetter) throws IOException {

        String url = source.get(Source.LINK);

        if(url == null) {
            reporter.report(Report.of(Event.MISSING_URL, source.id(), Source.class.getName()));
            return;
        }

        String id = Util.getID(url);

        Path path = scrapersPath.resolve(id);

        if(ACLEDScraper.validPath(path)) {

            List<String> exampleDates = exampleGetter.apply(source);

            if(exampleDates.isEmpty()) {
                reporter.report(Report.of(Event.DATE_NOT_FOUND, id));
            }

            Optional<DateParser> passingParser = Optional.empty();

            for(DateParser dateParser : dateParsers) {

                boolean passed = true;

                for(String date : exampleDates) {
                    List<String> formatSpecs = source.get(Source.DATE_FORMAT);

                    if(formatSpecs == null ) {
                        Optional<LocalDateTime> maybeParsed = dateParser.parse(date);
                        if(maybeParsed.isPresent()){

                            reporter.report(Report.of(Event.DATE_PARSE_SUCCESS, id).message(date));
                        } else {
                            reporter.report(Report.of(Event.DATE_PARSE_FAILED, id).message(date));
                            passed = false;
                            break;
                        }
                    } else {
                        DateParser existing = CompositeDateParser.of(formatSpecs);
                        Optional<LocalDateTime> maybeParsed = existing.parse(date);
                        //check parser
                    }
                }

                if(passed) {
                    passingParser = Optional.of(dateParser);

                    //TODO: assign parser to source
                }
            }

        } else {
            reporter.report(Report.of(Event.SCRAPER_NOT_FOUND,  id));
        }
    }

}
