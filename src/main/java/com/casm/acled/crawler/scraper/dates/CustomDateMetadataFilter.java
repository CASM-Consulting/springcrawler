package com.casm.acled.crawler.scraper.dates;

import com.casm.acled.crawler.reporting.Event;
import com.casm.acled.crawler.reporting.Report;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.scraper.ScraperFields;
import com.casm.acled.entities.source.Source;
import com.google.common.collect.ImmutableList;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.impl.DateMetadataFilter;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class CustomDateMetadataFilter extends DateMetadataFilter {


    private final String field;
    private final DateParser dateParser;

    private static String STANDARD_FORMAT = "yyyy-MM-dd HH:mm:ss3";

    private final DateTimeFormatter dtf;

    private final Reporter reporter;

    private final Source source;

    public CustomDateMetadataFilter(Source source, String field, DateParser dateParser, Reporter reporter) {
        super();
        setFormat(STANDARD_FORMAT);
        setField(ScraperFields.STANDARD_DATE);

        this.dateParser = dateParser;
        this.field = field;

        dtf = DateTimeFormatter.ofPattern(STANDARD_FORMAT);

        this.reporter = reporter;
        this.source = source;
    }

    @Override
    protected boolean isDocumentMatched(String reference, InputStream input,
                                        ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {

        Optional<LocalDateTime> maybeDateTime = dateParser.parse(metadata.get(field).get(0));

        if (maybeDateTime.isPresent()) {
            LocalDateTime ldt = maybeDateTime.get();
            String standardDateString = ldt.format(dtf);
            metadata.put(ScraperFields.STANDARD_DATE, ImmutableList.of(standardDateString));

            return super.isDocumentMatched(reference, input, metadata, parsed);
        } else {
            reporter.report(Report.of(Event.DATE_PARSE_FAILED, source.id(), metadata.getReference()));
            return true;
        }

    }

    public static LocalDateTime toDate(String date) {
        return LocalDateTime.parse(date, DateTimeFormatter.ofPattern(STANDARD_FORMAT));
    }

}
