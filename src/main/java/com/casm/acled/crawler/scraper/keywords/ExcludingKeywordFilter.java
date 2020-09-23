package com.casm.acled.crawler.scraper.keywords;

import com.casm.acled.crawler.scraper.ScraperFields;
import com.casm.acled.crawler.util.Util;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.AbstractDocumentFilter;
import com.norconex.importer.handler.filter.OnMatch;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ExcludingKeywordFilter extends AbstractDocumentFilter {

    private String queryConfig;
    private String field;

    private final LuceneMatcher matcher;

    private static final boolean PASSING = false;

    public ExcludingKeywordFilter(String field, String queryConfig) {
        setOnMatch(OnMatch.EXCLUDE);

        this.queryConfig = queryConfig;
        this.field = field;

        matcher = new LuceneMatcher(queryConfig);

    }

    @Override
    protected boolean isDocumentMatched(String reference, InputStream input, ImporterMetadata metadata, boolean parsed) throws ImporterHandlerException {
        if (queryConfig.isEmpty()) {
            return true;
        }
        String value =  metadata.getString(field);

        String text = Objects.toString(value, StringUtils.EMPTY);

        if(matcher.isMatched(text)) {
            metadata.setString(ScraperFields.KEYWORD_HIGHLIGHT, matcher.getHighlights(text));

            metadata.setBoolean(ScraperFields.KEYWORD_PASSED, true);
            return PASSING;
        } else {
            metadata.setBoolean(ScraperFields.KEYWORD_PASSED, false);
            return PASSING;
        }
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
