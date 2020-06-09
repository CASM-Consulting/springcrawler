package com.casm.acled.crawler.scraper;


import com.casm.acled.crawler.util.Util;
import com.casm.acled.crawler.reporting.Event;
import com.casm.acled.crawler.reporting.Report;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.norconex.collector.core.CollectorException;
import com.norconex.collector.http.client.impl.GenericHttpClientFactory;
import com.norconex.collector.http.doc.HttpDocument;
import com.norconex.collector.http.fetch.impl.GenericDocumentFetcher;
import com.norconex.collector.http.pipeline.importer.HttpImporterPipelineUtilProxy;
import com.norconex.commons.lang.io.CachedInputStream;
import com.norconex.commons.lang.io.CachedStreamFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ConnectTimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class ScraperService {

    private static Path scraperDir = Paths.get("/home/sw206/git/acled-scrapers");

    @Autowired
    private SourceDAO sourceDAO;

    @Autowired
    private Reporter reporter;

    public void checkScraperCoverage(Path scraperDir, SourceList sourceList) {

        List<Source> sources = sourceDAO.byList(sourceList);

        for(Source source: sources) {
//            if(!Util.isDisabled(source)) {
                if(Util.scraperExists(scraperDir, source)) {
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
//            }
        }
    }

    public void checkExampleURLs(Path scraperDir, SourceList sourceList) {

        List<Source> sources = sourceDAO.byList(sourceList);
        for(Source source : sources) {
            if(Util.scraperExists(scraperDir, source)) {

               checkExampleURLs(scraperDir, source);
            }
        }
    }


    public String getText(Source source, String url) {
        String id = Util.getID(source);
        ACLEDScraper scraper = ACLEDScraper.load(scraperDir.resolve(id), source, reporter);

        HttpDocument document = scrapeURL(scraper, url);

        String article = document.getMetadata().getString(ScraperFields.SCRAPED_ARTICLE);

        return article;
    }

    public void checkExampleURLs(Path scraperDir, Source source) {
        String id = Util.getID(source);

        ACLEDScraper scraper = ACLEDScraper.load(scraperDir.resolve(id), source, reporter);
        checkExampleURLs(scraper, source);
    }

    public HttpDocument scrapeURL(ACLEDScraper scraper, String url) {
        GenericDocumentFetcher fetcher = new GenericDocumentFetcher();

        HttpClient client = new GenericHttpClientFactory().createHTTPClient("www.acleddata.com");
        CachedInputStream inputStream = new CachedStreamFactory(1024, 1024).newInputStream("");

        HttpDocument document = new HttpDocument(url, inputStream);
        fetcher.fetchDocument(client, document);
        HttpImporterPipelineUtilProxy.enhanceHTTPHeaders(document.getMetadata());
        HttpImporterPipelineUtilProxy.applyMetadataToDocument(document);
        scraper.processDocument(client, document);


        return document;
    }

    public void checkExampleURLs(ACLEDScraper scraper, Source source) {

        GenericDocumentFetcher fetcher = new GenericDocumentFetcher();

        HttpClient client = new GenericHttpClientFactory().createHTTPClient("www.acleddata.com");
        CachedInputStream inputStream = new CachedStreamFactory(1024, 1024).newInputStream("");
        List<String> exampleURLs = source.get(Source.EXAMPLE_URLS);

        for(String exampleURL : exampleURLs) {
            try {

                HttpDocument document = new HttpDocument(exampleURL, inputStream);
                fetcher.fetchDocument(client, document);
                HttpImporterPipelineUtilProxy.enhanceHTTPHeaders(document.getMetadata());
                HttpImporterPipelineUtilProxy.applyMetadataToDocument(document);
                scraper.processDocument(client, document);
            } catch (IllegalStateException | CollectorException e){
                reporter.report(Report.of(Event.ERROR)
                        .type(Source.class.getName())
                        .id(source.id())
                        .message(e.getMessage())
                );
            }
        }
    }

}
