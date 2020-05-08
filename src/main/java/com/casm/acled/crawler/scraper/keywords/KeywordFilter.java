package com.casm.acled.crawler.scraper.keywords;

import com.casm.acled.crawler.Util;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.AbstractDocumentFilter;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class KeywordFilter extends AbstractDocumentFilter {

    private String queryConfig;
    private String field;

    private final Analyzer analyzer;
    private final Query query;


    public KeywordFilter(String field, List<String> queryConfig) {
        if(queryConfig == null) {
            queryConfig = Util.KEYWORDS_LUCENE;
        }
        this.queryConfig = StringUtils.join(queryConfig," ");
        this.field = field;

        analyzer = new SimpleAnalyzer();

        QueryParser parser = new QueryParser(field, analyzer);

        try {
            query = parser.parse(this.queryConfig);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isMatched(String text) {

        MemoryIndex index = new MemoryIndex();
        index.addField(field, text, analyzer);
        float score = index.search(query);

        return score > 0.0f;
    }

    @Override
    protected boolean isDocumentMatched(String reference, InputStream input, ImporterMetadata metadata, boolean parsed) throws ImporterHandlerException {
        if (queryConfig.isEmpty()) {
            return true;
        }
        Collection<String> values =  metadata.getStrings(field);

        for (Object value : values) {

            String strVal = Objects.toString(value, StringUtils.EMPTY);

            if(isMatched(strVal)) {
                return true;
            }
        }
        return false;
    }

    private void setField(String field) {
        this.field = field;
    }

    private void setQueryConfig(String queryConfig) {
        this.queryConfig = queryConfig;
    }

    @Override
    protected void loadFilterFromXML(XMLConfiguration xml) throws IOException {
        setField(xml.getString("[@field]"));
        String query = xml.getString("query");
        setQueryConfig(query);
    }

    @Override
    protected void saveFilterToXML(EnhancedXMLStreamWriter writer) throws XMLStreamException {
        writer.writeAttributeString("field", field);
        writer.writeElementString("query", queryConfig);
    }

}
