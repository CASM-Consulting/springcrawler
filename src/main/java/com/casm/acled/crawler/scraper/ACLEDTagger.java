package com.casm.acled.crawler.scraper;

import com.google.common.collect.ImmutableMap;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.impl.DOMTagger;

import java.io.ByteArrayInputStream;
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

        ImporterMetadata metadata = new ImporterMetadata();

        // URL param left blank and parsed set to true - so assuming already UTF-8
        super.tagApplicableDocument("", new ByteArrayInputStream(html.getBytes()), metadata, true);

        return new ImmutableMap.Builder<String, String>()
                .put(ScraperFields.SCRAPED_ARTICLE, metadata.getString(ScraperFields.SCRAPED_ARTICLE))
                .put(ScraperFields.SCRAPED_DATE, metadata.getString(ScraperFields.SCRAPED_DATE))
                .put(ScraperFields.SCRAPED_TITLE, metadata.getString(ScraperFields.SCRAPED_TITLE))
                .build();
    }
}
