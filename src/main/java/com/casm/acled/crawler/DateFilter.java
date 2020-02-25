package com.casm.acled.crawler;

// apache commons
import com.casm.acled.crawler.utils.Util;
import com.norconex.importer.handler.filter.OnMatch;
import org.apache.commons.configuration.XMLConfiguration;

// norconex
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.AbstractDocumentFilter;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// java
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;


public class DateFilter extends AbstractDocumentFilter {

    protected static final Logger logger = LoggerFactory.getLogger(DateFilter.class);

    public static final int DEFAULT = 1;
    public LocalDate threshold;

    public DateFilter() {
        this(LocalDate.now().minusDays(DEFAULT));
    }

    public DateFilter(LocalDate threshold) {
        this.threshold = threshold;
        this.setOnMatch(OnMatch.EXCLUDE);

    }

    @Override
    protected boolean isDocumentMatched(String reference, InputStream input, ImporterMetadata metadata, boolean parsed) throws ImporterHandlerException {

        String dateStr = metadata.get(ACLEDScraperPreProcessor.SCRAPEDATE).get(0);
        if(dateStr == null || dateStr.length() <= 0) {
            logger.debug("INFO: No date found for url: " + reference);
            return false;
        }
        try{
            LocalDate date = parseDate(dateStr);
//            logger.info("INFO: filtering article by date: " + reference + " date: " + date + " " + threshold.toString()
//                    + " article date: " + dateStr + "after?: " + date.isAfter(threshold));
            if(date != null) {
                if(date.isBefore(threshold)) {
                    logger.error("DATE-PRIOR-TO-THRESH " + date.toString() + " | " + dateStr);
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing date: " + reference);
        }

        return false;

    }



    public LocalDate parseDate(String date) { return Util.getDate(date); }

    @Override
    protected void saveFilterToXML(EnhancedXMLStreamWriter writer) throws XMLStreamException {
        // NAH MATE
    }

    @Override
    protected void loadFilterFromXML(XMLConfiguration xml) throws IOException {
        // NAH MATE
    }
}
