package com.casm.acled.crawler.scraper;

import com.google.common.collect.ImmutableMap;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.impl.DOMTagger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * Simple wrapper for the DOMTagger class to provide convenient public access to the
 * underlying protected tagApplicableDocument() method of the DOMTagger.
 * Returned by the ACLEDTagger.get() method.
 */
public class ACLEDTagger extends DOMTagger {

    /**
     * Convenience method for directly tagging a String of html.
     * Permits public availability of the functionality of the protected tagApplicableDocument() method.
     */
    public Map<String, String> tag(String html) throws ImporterHandlerException {

        return tag(new ByteArrayInputStream(html.getBytes()));
    }

    public Map<String, String> tag(InputStream html) throws ImporterHandlerException {

        ImporterMetadata metadata = new ImporterMetadata();

        super.tagApplicableDocument("", html, metadata, false);

        return new ImmutableMap.Builder<String, String>()
                .put(ScraperFields.SCRAPED_ARTICLE, metadata.getString(ScraperFields.SCRAPED_ARTICLE))
                .put(ScraperFields.SCRAPED_DATE, metadata.getString(ScraperFields.SCRAPED_DATE))
                .put(ScraperFields.SCRAPED_TITLE, metadata.getString(ScraperFields.SCRAPED_TITLE))
                .build();
    }
}
