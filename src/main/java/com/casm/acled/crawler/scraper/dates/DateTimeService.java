package com.casm.acled.crawler.scraper.dates;

import com.casm.acled.crawler.reporting.Event;
import com.casm.acled.crawler.reporting.Report;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.scraper.keywords.KeywordsService;
import com.casm.acled.crawler.scraper.locale.LocaleService;
import com.casm.acled.crawler.util.Util;
import com.casm.acled.dao.entities.DeskDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.dao.entities.SourceSourceListDAO;
import com.casm.acled.entities.desk.Desk;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
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

    public void attemptAllDateTimeParsers(List<DateParser> dateParsers, Function<Source, List<String>> exampleGetter) {
        for(Desk desk : deskDAO.getAll()) {
            attemptDeskDateTimeParsers(desk, dateParsers, exampleGetter);
        }
    }

    public void attemptDeskDateTimeParsers(Desk desk, List<DateParser> dateParsers, Function<Source, List<String>> exampleGetter) {
        List<SourceList> lists = sourceListDAO.byDesk(desk.id());

        for(SourceList list : lists) {
            attemptSourceListDateTimeParsers(list, dateParsers, exampleGetter);
        }
    }

    public void attemptSourceListDateTimeParsers(SourceList list, List<DateParser> dateParsers, Function<Source, List<String>> exampleGetter) {
        List<Source> sources = sourceDAO.byList(list);

        for(Source source : sources) {
            try {
                attemptDateTimeParse(source, dateParsers, exampleGetter);
            } catch (IllegalArgumentException e) {
                reporter.report(Report.of(Event.ERROR).message(e.getMessage()));
            }
        }
    }

    public static Function<Source, List<String>> lastScrapeExampleGetter(Path scrapersDir) {
        return source -> {
            String url = source.get(Source.LINK);

            String id = Util.getID(url);

            Path path = scrapersDir.resolve(id);
            Optional<String> maybeDate = DateUtil.extractDateFromLastScrapeJson(path);

            if(maybeDate.isPresent()) {
                return ImmutableList.of(maybeDate.get());
            } else {
                return ImmutableList.of();
            }
        };
    }


    public void attemptDateTimeParse(Source source, List<DateParser> dateParsers, Function<Source, List<String>> exampleGetter) {

        String url = source.get(Source.LINK);

        if(url == null) {
            reporter.report(Report.of(Event.MISSING_URL, source.id(), Source.class.getName()));
            return;
        }

        String id = Util.getID(url);

        List<String> exampleDates = exampleGetter.apply(source);

        boolean allPassed = true;

        if(exampleDates.isEmpty()) {
            reporter.report(Report.of(Event.DATE_NOT_FOUND, source.id()));
            allPassed = false;
        }

        Set<DateParser> passingParsers = new HashSet<>();

        for(String date : exampleDates) {

            Optional<DateParser> passingParser = Optional.empty();

            for(DateParser dateParser : dateParsers) {

                List<String> formatSpecs = source.get(Source.DATE_FORMAT);

                if(formatSpecs == null ) {
                    Optional<LocalDateTime> maybeParsed = dateParser.parse(date);
                    if(maybeParsed.isPresent()){

                        reporter.report(Report.of(Event.DATE_PARSE_SUCCESS, source.id()).message(date + StringUtils.join(formatSpecs, "<|#|>")));
                        passingParser = Optional.of(dateParser);
                        continue;
                    } else {
//                        reporter.report(Report.of(Event.DATE_PARSE_FAILED, source.id()).message(date));
                    }
                } else {
                    DateParser existing = CompositeDateParser.of(formatSpecs);
                    Optional<LocalDateTime> maybeParsed = existing.parse(date);
                    //check parser
                }
            }

            if(passingParser.isPresent()) {
                passingParsers.add(passingParser.get());
            } else {
                reporter.report(Report.of(Event.DATE_PARSE_FAILED, source.id()).message(date));
                allPassed = false;
            }
        }

        if(allPassed) {
            reporter.report(Report.of(Event.DATE_ALL_PARSE_SUCCESS, source.id()).message(source.get(Source.NAME)));
            CompositeDateParser passingParser = new CompositeDateParser(new ArrayList<>(passingParsers));
            List<String> spec = passingParser.getFormatSpec();
            source = source.put(Source.DATE_FORMAT, spec);
            sourceDAO.update(source);
        }
    }
}
