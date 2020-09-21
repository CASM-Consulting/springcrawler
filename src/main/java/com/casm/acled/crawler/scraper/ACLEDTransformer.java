package com.casm.acled.crawler.scraper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.norconex.commons.lang.config.XMLConfigurationUtil;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;

import com.norconex.importer.handler.transformer.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class ACLEDTransformer {
    // haven't finished yet; still coding and testing;
    // not sure how to test the pipeline.. directly take the output of the transformer and feed it to tagger??
    // let me try that.

    protected static final Logger logger = LoggerFactory.getLogger(ACLEDTransformer.class);

    // have a replacement string, convert xxx to xxx;
    public static ReplaceTransformer transformer;

    private final String restrictionTestConfig = "<transformer>"
            + "<replace><fromValue><script>.*?<\\/script></fromValue>"
            + "<toValue></toValue></replace>"
            + "<restrictTo caseSensitive=\"false\" "
            + "field=\"document.reference\">.*test.*</restrictTo>"
            + "</transformer>";

    public ACLEDTransformer () {}

    public void load(Map<String, String> replaceMap) {
        for (Map.Entry<String, String> strMap: replaceMap.entrySet()) {
            String fromStr = strMap.getKey();
            String toStr = strMap.getValue();
            transformer.addReplacement(fromStr, toStr);
        }
    }

    // test
    public void run() {
        transformer.addReplacement("<script>.*?<\\/script>", "");
    }
}
