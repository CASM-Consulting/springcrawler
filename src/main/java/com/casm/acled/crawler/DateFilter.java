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

// java
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class DateFilter extends AbstractDocumentFilter {

    public static final int DEFAULT = 48;
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
            return true;
        }

        ObjectMapper om = new ObjectMapper();
        try{
            Map<String, String> data = om.readValue(meta.get(0), Map.class);
            if(data.containsKey(ACLEDScraperPreProcessor.metaDATE)){
                String date = data.get(ACLEDScraperPreProcessor.metaDATE);
                List<Date> dates = parseDate(date);
                return !dates.get(0).before(threshold);
            }
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
