package com.casm.acled.crawler.scraper.keywords;
import com.casm.acled.crawler.scraper.ScraperFields;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.*;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.util.Objects;


public class KeywordTagger implements IDocumentTagger{

    private String queryConfig;
    private String field;

    private final LuceneMatcher matcher;

    public KeywordTagger(String field, String queryConfig) {

        this.queryConfig = queryConfig;
        this.field = field;

        matcher = new LuceneMatcher(queryConfig);

    }
    @Override
    public void tagDocument(String s, InputStream inputStream, ImporterMetadata importerMetadata, boolean b) throws ImporterHandlerException {

        if (queryConfig.isEmpty()) {
            importerMetadata.setBoolean(ScraperFields.KEYWORD_PASSED, true);
        }
        String value =  importerMetadata.getString(field);
        String text = Objects.toString(value, StringUtils.EMPTY);

        if(matcher.isMatched(text)) {

            importerMetadata.setString(ScraperFields.KEYWORD_HIGHLIGHT, matcher.getHighlights(text));
            importerMetadata.setBoolean(ScraperFields.KEYWORD_PASSED, true);

        }
        else {
            importerMetadata.setBoolean(ScraperFields.KEYWORD_PASSED, false);
        }

    }

    private void setField(String field) {
        this.field = field;
    }

    private void setQueryConfig(String queryConfig) {
        this.queryConfig = queryConfig;
    }

}
