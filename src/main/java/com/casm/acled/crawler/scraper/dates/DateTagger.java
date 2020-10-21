package com.casm.acled.crawler.scraper.dates;
import com.casm.acled.crawler.reporting.Event;
import com.casm.acled.crawler.reporting.Report;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.scraper.ScraperFields;
import com.casm.acled.entities.source.Source;
import com.ibm.icu.util.ULocale;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.OnMatch;
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;
import com.norconex.importer.handler.tagger.*;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public class DateTagger implements IDocumentTagger{

    private final String field;
    private final DateParser dateParser;

    private static String STANDARD_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private final DateTimeFormatter dtf;

    private final Source source;

    private final Reporter reporter;


    private LocalDateTime from;
    private LocalDateTime to;


    public DateTagger(Source source, String field, DateParser dateParser, Reporter report) {

        this.dateParser = dateParser.locale(getLocales(source));
        this.field = field;

        this.reporter = report;

        dtf = DateTimeFormatter.ofPattern(STANDARD_FORMAT);

        this.source = source;
    }

    private List<ULocale> getLocales(Source source) {
        return ((List<String>)source.get(Source.LOCALES)).stream().map(ULocale::new).collect(Collectors.toList());
    }

    public void setFromTime(ZonedDateTime from) {
        this.from = from.toLocalDateTime();
    }

    public void setToTime(ZonedDateTime to) {
        this.to = to.toLocalDateTime();
    }

    @Override
    public void tagDocument(String s, InputStream inputStream, ImporterMetadata importerMetadata, boolean b) throws ImporterHandlerException {

        String date = importerMetadata.get(field).get(0);
        Optional<LocalDateTime> maybeDateTime = dateParser.parse(date);

        if (maybeDateTime.isPresent()) {
            LocalDateTime ldt = maybeDateTime.get();
            String standardDateString = ldt.format(dtf);
            importerMetadata.addString(ScraperFields.STANDARD_DATE, standardDateString);

            if((ldt.isBefore(this.to) && ldt.isAfter(this.from)) || (ldt.isEqual(this.to) || ldt.isEqual(this.from))) {
                importerMetadata.addBoolean(ScraperFields.DATE_PASSED, true);
            } else {
                importerMetadata.addBoolean(ScraperFields.DATE_PASSED, false);
            }
        } else {
//            reporter.report(Report.of(Event.DATE_PARSE_FAILED, source.id(), importerMetadata.getReference()));
            importerMetadata.addBoolean(ScraperFields.DATE_PASSED, false);
//            return true;
        }
    }
}
