package com.casm.acled.crawler.scraper;

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
import org.apache.lucene.search.Query;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class AcceptFilter extends AbstractDocumentFilter {


    public AcceptFilter() {
        setOnMatch(OnMatch.INCLUDE);
    }
    @Override
    protected boolean isDocumentMatched(String reference, InputStream input, ImporterMetadata metadata, boolean parsed) {
        return true;
    }

    @Override
    protected void loadFilterFromXML(XMLConfiguration xml) throws IOException {
    }

    @Override
    protected void saveFilterToXML(EnhancedXMLStreamWriter writer) throws XMLStreamException {
    }

}
