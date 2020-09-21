package com.casm.acled.crawler.scraper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

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

    public ACLEDTransformer (Map<String, String> replaceMap) {
//        Map<String, String> params = new HashMap<String, String>();
//        params.put("<script.*?>.*?<\\/script>", "");
        load(replaceMap);

    }

    public void load(Map<String, String> replaceMap) {
        ReplaceTransformer t = new ReplaceTransformer();
        for (Map.Entry<String, String> strMap: replaceMap.entrySet()) {
            String fromStr = strMap.getKey();
            String toStr = strMap.getValue();
            t.addReplacement(fromStr, toStr);
        }
        transformer = t;
    }

    // used for testing
    public String transform(String content) throws ImporterHandlerException{
        InputStream is = IOUtils.toInputStream(content, StandardCharsets.UTF_8);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImporterMetadata metadata = new ImporterMetadata();
        metadata.setString("document.reference", "N/A");
        transformer.transformDocument("N/A", is, os, metadata, true);
        String response = os.toString();
        return response;
    }

    // used for testing;
    public void test() throws ImporterHandlerException {
        String content = "<p>waterloon</p><script>a=25+2</script><p>hello world<a>String (a)</a></p>";
        InputStream is = IOUtils.toInputStream(content, StandardCharsets.UTF_8);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImporterMetadata metadata = new ImporterMetadata();
        metadata.setString("document.reference", "N/A");
        transformer.transformDocument("N/A", is, os, metadata, true);
        String response = os.toString();

    }

    public static void main(String[] args) throws ImporterHandlerException, IOException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("<script.*?>.*?<\\/script>", "");

        ACLEDTransformer a = new ACLEDTransformer(params);
        a.test();

    }

    }
