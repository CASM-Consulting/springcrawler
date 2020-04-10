package com.casm.acled.crawler.scraper.dates;

import com.casm.acled.crawler.scraper.ScraperFields;
import com.google.common.collect.ImmutableList;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.impl.DateMetadataFilter;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class CustomDateMetadataFilter extends DateMetadataFilter {


    private final String field;
    private final DateParser dateParser;

    private static String STANDARD_FORMAT = "YYYY-MM-DD HH:MM:SS ZZZ";

    private final DateTimeFormatter dtf;

    public CustomDateMetadataFilter(String field, DateParser dateParser) {
        super();
        setFormat(STANDARD_FORMAT);
        setField(ScraperFields.STANDARD_DATE);

        this.dateParser = dateParser;
        this.field = field;

        dtf = DateTimeFormatter.ofPattern(STANDARD_FORMAT);
    }

    @Override
    protected boolean isDocumentMatched(String reference, InputStream input,
                                        ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {

        Optional<ZonedDateTime> maybeDateTime = dateParser.parse(metadata.get(field).get(0));

        if (maybeDateTime.isPresent()) {
            ZonedDateTime zdt = maybeDateTime.get();
            String standardDateString = zdt.format(dtf);
            metadata.put(ScraperFields.STANDARD_DATE, ImmutableList.of(standardDateString));

            return super.isDocumentMatched(reference, input, metadata, parsed);
        } else {
            //report failed parse!
            return true;
        }

    }


}
