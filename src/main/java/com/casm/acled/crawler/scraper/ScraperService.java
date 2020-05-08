package com.casm.acled.crawler.scraper;


import com.casm.acled.crawler.Util;
import com.casm.acled.crawler.reporting.Event;
import com.casm.acled.crawler.reporting.Report;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.norconex.collector.http.client.impl.GenericHttpClientFactory;
import com.norconex.collector.http.doc.HttpDocument;
import com.norconex.collector.http.doc.HttpMetadata;
import com.norconex.collector.http.fetch.HttpFetchResponse;
import com.norconex.collector.http.fetch.impl.GenericDocumentFetcher;
import com.norconex.collector.http.pipeline.importer.HttpImporterPipelineUtilProxy;
import com.norconex.commons.lang.file.ContentType;
import com.norconex.commons.lang.io.CachedInputStream;
import com.norconex.commons.lang.io.CachedStreamFactory;
import com.norconex.importer.doc.ImporterDocument;
import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;



import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class ScraperService {


    @Autowired
    private SourceDAO sourceDAO;

    @Autowired
    private Reporter reporter;

    public void reportScraperCoverage(Path scraperDir, SourceList sourceList) {

        List<Source> sources = sourceDAO.byList(sourceList);

        for(Source source: sources) {

            if(scraperExists(scraperDir, source)) {
                reporter.report(Report.of(Event.SCRAPER_FOUND).id(source.id()));
            } else {
                reporter.report(Report.of(Event.SCRAPER_NOT_FOUND).id(source.id()));
            }
        }
    }


    public void testScraper(ACLEDScraper scraper, Source source) {

        GenericDocumentFetcher fetcher = new GenericDocumentFetcher();

        HttpClient client = new GenericHttpClientFactory().createHTTPClient("www.acleddata.com");
        CachedInputStream inputStream = new CachedStreamFactory(1024, 1024).newInputStream("");
        List<String> exampleURLs = source.get(Source.EXAMPLE_URLS);

        for(String exampleURL : exampleURLs) {
            HttpDocument document = new HttpDocument(exampleURL, inputStream);
            fetcher.fetchDocument(client, document);
            HttpImporterPipelineUtilProxy.enhanceHTTPHeaders(document.getMetadata());
            HttpImporterPipelineUtilProxy.applyMetadataToDocument(document);
            scraper.processDocument(client, document);
        }
    }

    public boolean scraperExists(Path path, Source source) {
        String id = Util.getID(source);
        return Files.exists(path.resolve(id).resolve(ACLEDScraper.JOB_JSON));
    }
}
