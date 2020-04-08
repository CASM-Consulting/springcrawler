package com.casm.acled.crawler;

import com.casm.acled.crawler.dates.DateUtil;
import com.casm.acled.crawler.utils.Util;
import com.norconex.collector.http.doc.HttpDocument;
import com.norconex.collector.http.processor.IHttpDocumentProcessor;
import org.apache.http.client.HttpClient;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Adds a number of meta-data to a url
public class ACLEDMetadataPreProcessor implements IHttpDocumentProcessor {

    public static final String CRAWLDATE = "acled.jqm.date";
    public static final String DEPTH = "acled.jqm.depth";
    public static final String UPDATED = "acled.jqm.update";
    public static final String LINK = "casm.jqm.link";


    public final Map<String, List<String>> metadata;

    public ACLEDMetadataPreProcessor(){
        this(new HashMap<>());
    }

    public ACLEDMetadataPreProcessor(Map<String, List<String>> stringStringHashMap) {
        this.metadata = stringStringHashMap;
    }

    @Override
    public void processDocument(HttpClient httpClient, HttpDocument doc) {
//        metadata.put(UPDATED,Arrays.asList(String.valsueOf(doc.getMetadata().getBoolean(HttpMetadata.COLLECTOR_IS_CRAWL_NEW))));
//        metadata.put(DEPTH, Arrays.asList(String.valueOf(doc.getMetadata().getInt(HttpMetadata.COLLECTOR_DEPTH))));
//        doc.getMetadata().getDocumentUrls();

        String crawlDate = DateUtil.toDateString(LocalDate.now());

        Util.metadataPut(metadata, CRAWLDATE, crawlDate);

        doc.getMetadata().putAll(metadata);
    }


//    public static void main(String[] args ){
//        System.out.println(LocalDate.now().toString());
//    }
}
