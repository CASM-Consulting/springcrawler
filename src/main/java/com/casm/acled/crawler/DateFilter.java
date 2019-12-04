package com.casm.acled.crawler;

// faster xml
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

// apache commons
import org.apache.commons.configuration.XMLConfiguration;

// norconex
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.AbstractDocumentFilter;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import org.joda.time.DateTime;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// java
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class DateFilter extends AbstractDocumentFilter {

    protected static final Logger logger = LoggerFactory.getLogger(DateFilter.class);

    public static final int DEFAULT = 24;
    public Date threshold;
    private PrettyTimeParser parser;

    public DateFilter() {
        this(new DateTime().minusHours(DEFAULT).toDate());
    }

    public DateFilter(Date threshold) {
        this.threshold = threshold;
        parser = new PrettyTimeParser();
    }

    @Override
    protected boolean isDocumentMatched(String reference, InputStream input, ImporterMetadata metadata, boolean parsed) throws ImporterHandlerException {

        List<String> meta = metadata.get(ACLEDScraperPreProcessor.SCRAPEDJSON);
        if(meta == null || meta.size() <= 0) {
            logger.error("INFO: No metadata found for url: " + reference);
            return false;
        }

        ObjectMapper om = new ObjectMapper();
        try{
            Map<String, String> data = om.readValue(meta.get(0), Map.class);
            if(data.containsKey(ACLEDScraperPreProcessor.metaDATE)){
                String date = data.get(ACLEDScraperPreProcessor.metaDATE);
                List<Date> dates = parseDate(date);
                logger.error("INFO: filtering article by date: " + reference + " date: " + threshold.toString()
                        + " article date: " + dates.get(0).toString() + "after?: " + dates.get(0).after(threshold));
                if(dates.size() > 0) {
                    return dates.get(0).after(threshold);
                }
                logger.error("ERROR: article did not pass date filter: threshold - " + threshold.toString()
                        + " article date: " + dates.get(0).toString());
            }
            return false;
        } catch (JsonParseException e) {
            logger.error("Error parsing date: " + reference);
        } catch (JsonMappingException e) {
            logger.error("Error parsing date: " + reference);
        } catch (IOException e) {
            logger.error("Error parsing date: " + reference);
        }
        return true;

    }

    public List<Date> parseDate(String date) {
        return parser.parse(date);
    }

    @Override
    protected void saveFilterToXML(EnhancedXMLStreamWriter writer) throws XMLStreamException {
        // NAH MATE
    }

    @Override
    protected void loadFilterFromXML(XMLConfiguration xml) throws IOException {
        // NAH MATE
    }
}
