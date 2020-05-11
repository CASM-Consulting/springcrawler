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
import com.norconex.collector.http.fetch.impl.GenericDocumentFetcher;
import com.norconex.collector.http.pipeline.importer.HttpImporterPipelineUtilProxy;
import com.norconex.commons.lang.io.CachedInputStream;
import com.norconex.commons.lang.io.CachedStreamFactory;
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

    public void checkScraperCoverage(Path scraperDir, SourceList sourceList) {

        List<Source> sources = sourceDAO.byList(sourceList);

        for(Source source: sources) {
            if(source.get(Source.CRAWL_ACTIVE)) {
                if(scraperExists(scraperDir, source)) {
                    reporter.report(Report.of(Event.SCRAPER_FOUND)
                            .id(source.id())
                            .message(source.get(Source.NAME))
                    );
                } else {
                    reporter.report(Report.of(Event.SCRAPER_NOT_FOUND)
                            .message(source.get(Source.LINK))
                            .id(source.id())
                    );
                }
            }
        }
    }

    public boolean isScrapable(Path scraperDir, Source source) {
        return (Boolean)source.get(Source.CRAWL_ACTIVE) && scraperExists(scraperDir, source);
    }

    public void checkExampleURLs(Path scraperDir, SourceList sourceList) {

        List<Source> sources = sourceDAO.byList(sourceList);
        for(Source source : sources) {
            if(isScrapable(scraperDir, source)) {

                String id = Util.getID(source);

                ACLEDScraper scraper = ACLEDScraper.load(scraperDir.resolve(id), source, reporter);

                checkExampleURLs(scraper, source);
            }
        }
    }

    public void checkExampleURLs(ACLEDScraper scraper, Source source) {

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
