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
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.desk.Desk;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.google.common.collect.ImmutableList;
import com.ibm.icu.util.ULocale;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /**
     * Parse the scraped date of an article according to how we generate the
     * Article.DATE field from ScrapedFields.SCRAPED_DATE field.
     */
    public Optional<LocalDate> parseDate(String scrapedDate, Source src){
        // Ready parser on Source-defined format
        DateParser parser = CompositeDateParser.of(src.get(Source.DATE_FORMAT));
        // Pay attention to Source's locale definition
        List<String> locales = src.get(Source.LOCALES);
        parser.locale(locales.stream().map(ULocale::new).collect(Collectors.toList()));
        // Perform parse
        Optional<LocalDateTime> date = parser.parse(scrapedDate);
        return date.map(LocalDateTime::toLocalDate);
    }

    /**
     * Check date is in range of inclusive lower (from) and inclusive upper (to) bounds.
     * If date is null, then return false.
     * If from is null then it's always passing the lower bound.
     * If to is null then it's always passing the upper bound.
     */
    public boolean isInRange(LocalDate date, LocalDate from, LocalDate to){
        if (date != null){
            boolean validForLowerBound = from == null || date.isAfter(from) || date.isEqual(from);
            boolean validForUpperBound = to == null || date.isBefore(to) || date.isEqual(to);
            return validForLowerBound && validForUpperBound;
        } else return false;
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

    public void checkExistingPasses(SourceList sourceList, Function<Source, List<String>> exampleGetter) {

        List<Source> sources = sourceDAO.byList(sourceList);

        for(Source source : sources) {
            boolean passed = checkExistingPasses(source, exampleGetter);
            if(passed) {
                reporter.report(Report.of(Event.DATE_PARSE_SUCCESS).id(source.id()).message(source.get(Source.STANDARD_NAME)));
            } else {
                reporter.report(Report.of(Event.DATE_PARSE_FAILED).id(source.id()).message(source.get(Source.STANDARD_NAME)));
            }
        }
    }

    public boolean checkExistingPasses(Source source, Function<Source, List<String>> exampleGetter) {
        List<String> formatSpecs = source.get(Source.DATE_FORMAT);

        if(formatSpecs != null && ! formatSpecs.isEmpty()) {

            DateParser existing = CompositeDateParser.of(formatSpecs);

            List<String> exampleDates = exampleGetter.apply(source);

            boolean passed = exampleDates.size() > 0;
            for(String date : exampleDates) {
                Optional<LocalDateTime> maybeParsed = existing.parse(date);
                if(!maybeParsed.isPresent()) {
                    passed = false;
                    break;
                }
            }

            return passed;
        } else {
            return false;
        }
    }

    public void attemptDateTimeParse(Source source, List<DateParser> dateParsers, Function<Source, List<String>> exampleGetter) {

        List<String> exampleDates = exampleGetter.apply(source);

        boolean allPassed = true;

        if(exampleDates.isEmpty()) {
            reporter.report(Report.of(Event.NO_EXAMPLES, source.id()));
            allPassed = false;
        }

        Set<DateParser> passingParsers = new HashSet<>();

        if(true || !checkExistingPasses(source, exampleGetter)) {

            for(String date : exampleDates) {

                Optional<DateParser> passingParser = Optional.empty();

                for(DateParser dateParser : dateParsers) {

                    if(source.hasValue(Source.LOCALES)) {
                        List<String> locales = source.get(Source.LOCALES);

                        dateParser = dateParser.locale(locales.stream().map(ULocale::new).collect(Collectors.toList()));
                    }

                    Optional<LocalDateTime> maybeParsed = dateParser.parse(date);
                    if(maybeParsed.isPresent()){
//                        reporter.report(Report.of(Event.DATE_PARSE_SUCCESS, source.id()).message(date + StringUtils.join(dateParser.getFormatSpec(), "<|#|>")));
                        passingParser = Optional.of(dateParser);
                        break;
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
                sourceDAO.upsert(source);
            }
        }
    }
}
