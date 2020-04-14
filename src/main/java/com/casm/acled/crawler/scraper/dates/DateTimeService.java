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




    public void attemptAllDateTimeParsers(DateParser defaultDateParser) {
        for(Desk desk : deskDAO.getAll()) {
            attemptDeskDateTimeParsers(desk, defaultDateParser);
        }
    }

    public void attemptDeskDateTimeParsers(Desk desk, DateParser defaultDateParser) {
        List<SourceList> lists = sourceListDAO.byDesk(desk.id());

        for(SourceList list : lists) {
            attemptSourceListDateTimeParsers(list, defaultDateParser);
        }
    }

    public void attemptSourceListDateTimeParsers(SourceList list, DateParser defaultDateParser) {
        List<Source> sources = sourceDAO.byList(list);

        for(Source source : sources) {
            try {
                attemptDateTimeParse(source, defaultDateParser);
            } catch (IOException e) {
                reporter.report(Report.of(Event.SCRAPER_NOT_FOUND).message(e.getMessage()));
            }
        }
    }

    public void attemptDateTimeParse(Source source, DateParser defaultDateParser) throws IOException {

        String url = source.get(Source.LINK);

        if(url == null) {
            reporter.report(Report.of(Event.MISSING_URL, source.id(), Source.class.getName()));
            return;
        }

        String id = Util.getDomain(url).replaceAll("\\.","");

        Path path = scrapersPath.resolve(id);

        if(ACLEDScraper.validPath(path)) {

            Optional<String> maybeDate = DateUtil.extractDateFromLastScrapeJson(path);

            if(maybeDate.isPresent()) {
                String date = maybeDate.get();
                List<String> formatSpecs = source.get(Source.DATE_FORMAT);

                if(formatSpecs == null ) {
                    Optional<LocalDateTime> maybeParsed = defaultDateParser.parse(date);
                    if(maybeParsed.isPresent()){

                        reporter.report(Report.of(Event.DATE_PARSE_SUCCESS, id));
                    } else {
                        reporter.report(Report.of(Event.DATE_PARSE_FAILED, id).message(date));
                    }
                } else {
                    CompositeDateParser.of(formatSpecs);
                }

            } else {

                reporter.report(Report.of(Event.DATE_NOT_FOUND, id));
            }

        } else {
            reporter.report(Report.of(Event.SCRAPER_NOT_FOUND));
        }
    }

}
