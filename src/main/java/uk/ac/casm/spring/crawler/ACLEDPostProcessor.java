package uk.ac.casm.spring.crawler;

import com.norconex.collector.http.doc.HttpDocument;
import com.norconex.collector.http.processor.IHttpDocumentProcessor;
import org.apache.http.client.HttpClient;

public class ACLEDPostProcessor implements IHttpDocumentProcessor {


    @Override
    public void processDocument(HttpClient httpClient, HttpDocument doc) {

        String scrapedJson = doc.getMetadata().get(ACLEDScraperPreProcessor.SCRAPEDJSON).get(0);

        //TODO: Spring gubbins

    }



}
