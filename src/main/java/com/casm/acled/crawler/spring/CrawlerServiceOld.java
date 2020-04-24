package com.casm.acled.crawler.spring;

import com.casm.acled.crawler.*;
import com.casm.acled.crawler.scraper.ACLEDScraper;
import com.casm.acled.crawler.utils.Util;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.norconex.collector.core.checksum.impl.MD5DocumentChecksummer;
import com.norconex.collector.core.data.store.impl.mvstore.MVStoreCrawlDataStoreFactory;
import com.norconex.collector.http.crawler.HttpCrawlerConfig;
import com.norconex.collector.http.doc.HttpMetadata;
import com.norconex.importer.ImporterConfig;
import com.norconex.importer.handler.IImporterHandler;
import com.norconex.importer.handler.filter.OnMatch;
import com.norconex.importer.handler.filter.impl.EmptyMetadataFilter;
import com.norconex.importer.handler.filter.impl.RegexMetadataFilter;
import com.norconex.importer.handler.tagger.impl.KeepOnlyTagger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.susx.tag.norconex.database.ConcurrentContentHashStore;
import uk.ac.susx.tag.norconex.jobqueuemanager.CrawlerArguments;
import uk.ac.susx.tag.norconex.jobqueuemanager.SingleSeedCollector;
import uk.ac.susx.tag.norconex.utils.WebsiteReport;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;


@Component
public class CrawlerServiceOld {

    protected static final Logger logger = LoggerFactory.getLogger(CrawlerServiceOld.class);

    @Autowired
    private ArticleDAO articleDAO;
    @Autowired
    private SourceDAO sourceDAO;
    @Autowired
    private SourceListDAO sourceListDAO;


    private String ensureURL(String url) {
        HttpURLConnection con = null;
        try {
            con = WebsiteReport.ensureConnection(url);
            url = con.getURL().toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if(con != null) {
                con.disconnect();
            }
        }
        return url;
    }

    public void run(CrawlerArguments crawlerArguments) throws Exception {

        String seed = crawlerArguments.seeds.get(0);

//        seed = ensureURL(seed);

        SingleSeedCollector collector = new SingleSeedCollector(
                crawlerArguments.userAgent,
                new File(crawlerArguments.crawldb),
                Util.getDomain(crawlerArguments.seeds.get(0)),
                crawlerArguments.depth,
                crawlerArguments.urlFilter,
                crawlerArguments.threadsPerSeed,
                crawlerArguments.ignoreRobots,
                crawlerArguments.ignoreSitemap,
                crawlerArguments.polite,
                seed
        );

        HttpCrawlerConfig config = collector.getConfiguration();

//        config.setIgnoreCanonicalLinks(true);

        logger.info("Starting config");
        ImporterConfig importerConfig = config.getImporterConfig();
        List<IImporterHandler> handlers = new ArrayList<>();

        Path contentHashStorePath = Paths.get(crawlerArguments.crawldb, Util.getDomain(crawlerArguments.seeds.get(0)),"crawlstore");
        ConcurrentContentHashStore contentHashStore = new ConcurrentContentHashStore(contentHashStorePath);
//        config.setDocumentChecksummer(new WebScraperMetadataChecksum(contentHashStore));

        MD5DocumentChecksummer checksummer = new MD5DocumentChecksummer();
        checksummer.setSourceFields(CrawlerArguments.SCRAPEDARTICLE);
        checksummer.setTargetField(CrawlerArguments.CONTENTHASH);
        config.setDocumentChecksummer(checksummer);

        logger.info("index only: {}", crawlerArguments.index);
        // Only performs this step when we are wanting to produce to a table
        if(!crawlerArguments.index){

            // Add the source information to the metadata
            Map<String,List<String>> metadata = buildACLEDMetadata(crawlerArguments.sourcedomain, crawlerArguments.seeds.get(0));
            metadata.put(CrawlerArguments.SOURCENAME, Arrays.asList(crawlerArguments.source));
//            metadata.put(CrawlerArguments.COUNTRIES, Arrays.asList(crawlerArguments.countries));

            buildACLEDArticleFilters(handlers);

            Path scrapersPath = Paths.get(crawlerArguments.scrapers);

            String explicitScraper = crawlerArguments.scraper;

            ACLEDScraper scraper = resolveScraper(scrapersPath, explicitScraper, seed);

//            config.setPreImportProcessors(scraper,new ACLEDMetadataPreProcessor(metadata));

            // Add the crawler-to-spring-magic post-processor
            config.setPostImportProcessors(new ACLEDImporter(articleDAO, sourceDAO, sourceListDAO,false));
            
        }
//        importerConfig.setParserFactory();
        importerConfig.setPostParseHandlers(handlers.toArray(new IImporterHandler[handlers.size()]));
        config.setImporterConfig(importerConfig);

        collector.setConfiguration(config);

        collector.start();
    }

    private ACLEDScraper resolveScraper(Path scrapersPath, String explicitScraper, String seed) throws IOException {
        ACLEDScraper scraper;
        Path path;
        String id = Util.getDomain(seed).replaceAll("\\.","");

        if(explicitScraper != null && ACLEDScraper.validPath(scrapersPath.resolve(explicitScraper))) {
            path = scrapersPath.resolve(explicitScraper);
        } else if(ACLEDScraper.validPath(scrapersPath.resolve(id))) {
            path = scrapersPath.resolve(id);
        } else {
            throw new ScraperNotFoundException(id);
        }

        scraper = ACLEDScraper.load(path);
        return scraper;
    }

    private static void buildACLEDArticleFilters(List<IImporterHandler> handlers) {

        // Set the various document filters
        RegexMetadataFilter regexFilter = new RegexMetadataFilter(CrawlerArguments.SCRAPEDARTICLE, Util.KEYWORDS);
        EmptyMetadataFilter emptyArticle = new EmptyMetadataFilter(OnMatch.EXCLUDE,CrawlerArguments.SCRAPEDARTICLE);
        int week = 7;
        DateFilter df = new DateFilter(LocalDate.now().minusDays(week));

        handlers.add(emptyArticle);
        handlers.add(regexFilter);
//        handlers.add(df);

    }

    private static Map<String,List<String>> buildACLEDMetadata(String sourcedomain, String seed) {
        Map<String,List<String>> map = new HashMap<>();
        List<String> source;
        if(sourcedomain != null) {
            logger.info("INFO: Adding source LINK as " + sourcedomain);
            source = Arrays.asList(sourcedomain);
        }
        else {
            logger.info("INFO: Could not find source LINK. Defaulting to seed - " + seed);
            source = Arrays.asList(seed);
        }
        map.put(ACLEDMetadataPreProcessor.LINK, source);

        return map;
    }

    private MVStoreCrawlDataStoreFactory createDBStore() {
        MVStoreCrawlDataStoreFactory factory = new MVStoreCrawlDataStoreFactory();
        return factory;
    }

    /**
     * Removes all non-essential metadata fields to reduce index size
     * @return
     */
    private static KeepOnlyTagger buildKeepOnly() {
        KeepOnlyTagger kop = new KeepOnlyTagger();
        //  date, depth and seen before info
        kop.addField(HttpMetadata.COLLECTOR_DEPTH);
        kop.addField(HttpMetadata.COLLECTOR_IS_CRAWL_NEW);
        kop.addField(HttpMetadata.DOC_IMPORTED_DATE);
        // change and recrawl info
        kop.addField(HttpMetadata.COLLECTOR_SM_CHANGE_FREQ);
        kop.addField(HttpMetadata.COLLECTOR_SM_PRORITY);
        kop.addField(HttpMetadata.COLLECTOR_SM_LASTMOD);
        // checksum info
        kop.addField(HttpMetadata.COLLECTOR_CHECKSUM_METADATA);
        kop.addField(HttpMetadata.COLLECTOR_CHECKSUM_DOC);
        // references
        kop.addField(HttpMetadata.DOC_REFERENCE);
        kop.addField(HttpMetadata.DOC_EMBEDDED_PARENT_REFERENCE);
        // acled meta
        kop.addField(ACLEDMetadataPreProcessor.LINK);
        return kop;
    }
}
