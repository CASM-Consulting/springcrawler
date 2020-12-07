package com.casm.acled.crawler.scraper.keywords;
import com.casm.acled.crawler.scraper.ScraperFields;
import com.casm.acled.entities.sourcelist.SourceList;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.*;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.util.Objects;


public class KeywordTagger implements IDocumentTagger{

    private String name;
    private String queryConfig;
    private String field;

    private final LuceneMatcher matcher;

    public KeywordTagger(String field, SourceList sourceList) {
        this(field, sourceList.get(SourceList.KEYWORDS), sourceList.get(SourceList.LIST_NAME));
    }

    public KeywordTagger(String field, String query, String name){
        this.queryConfig = query;
        this.name = name;
        this.field = field;

        if(query.isEmpty()) {
            matcher = null;
        } else {
            matcher = new LuceneMatcher(queryConfig);
        }
    }

    @Override
    public void tagDocument(String s, InputStream inputStream, ImporterMetadata importerMetadata, boolean b) throws ImporterHandlerException {

        String value =  importerMetadata.getString(field);
        String text = Objects.toString(value, StringUtils.EMPTY);

        // If no query specified, then all articles match
        if (queryConfig.isEmpty()) {

            importerMetadata.addString(ScraperFields.KEYWORD_PASSED, name);
            importerMetadata.addString(ScraperFields.KEYWORD_HIGHLIGHT, "");

        }
        // Otherwise the matcher defined by the query decides whether article matches
        else if(matcher.isMatched(text)) {

            importerMetadata.addString(ScraperFields.KEYWORD_PASSED, name);
            importerMetadata.addString(ScraperFields.KEYWORD_HIGHLIGHT, matcher.getHighlights(text));

        }
    }

    private void setField(String field) {
        this.field = field;
    }

    private void setQueryConfig(String queryConfig) {
        this.queryConfig = queryConfig;
    }

    private void setName(String name){
        this.name = name;
    }

}
