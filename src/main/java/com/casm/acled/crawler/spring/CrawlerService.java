package com.casm.acled.crawler.spring;

import com.casm.acled.crawler.ACLEDMetadataPreProcessor;
import com.casm.acled.crawler.ACLEDPostProcessor;
import com.casm.acled.crawler.ACLEDScraperPreProcessor;
import com.casm.acled.crawler.DateFilter;
import com.casm.acled.crawler.utils.Util;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
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
import uk.ac.susx.tag.norconex.document.WebScraperMetadataChecksum;
import uk.ac.susx.tag.norconex.jobqueuemanager.CrawlerArguments;
import uk.ac.susx.tag.norconex.jobqueuemanager.SingleSeedCollector;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;


@Component
public class CrawlerService {

    protected static final Logger logger = LoggerFactory.getLogger(CrawlerService.class);

    @Autowired
    private ArticleDAO articleDAO;
    @Autowired
    private SourceDAO sourceDAO;
    @Autowired
    private SourceListDAO sourceListDAO;



//    public static void main(String[] args) {
//
//        SpringApplication app = new SpringApplication(CrawlerService.class);
//        app.setBannerMode(Banner.Mode.OFF);
//        app.setWebApplicationType(WebApplicationType.NONE);
//        ConfigurableApplicationContext ctx = app.run(args);
//        logger.info("Spring Boot application started");
//
//        // Close when complete
////        ctx.getBean(TerminateBean.class);
//        ctx.close();
//    }

//    public class TerminateBean {
//
//        @PreDestroy
//        public void onDestroy() throws Exception {
//            contentHashStore.close();
//            logger.info("Spring Container is destroyed!");
//        }
//    }

//    @Configuration
//    public class ShutdownConfig {
//
//        @Bean
//        public TerminateBean getTerminateBean() {
//            return new TerminateBean();
//        }
//    }

    public void run(CrawlerArguments crawlerArguments)  {

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
                crawlerArguments.seeds.get(0)
        );

        HttpCrawlerConfig config = collector.getConfiguration();

        logger.info("Starting config");
        ImporterConfig importerConfig = config.getImporterConfig();
        List<IImporterHandler> handlers = new ArrayList<>();

        Path contentHashStorePath = Paths.get(crawlerArguments.crawldb, Util.getDomain(crawlerArguments.seeds.get(0)),"crawlstore");
        ConcurrentContentHashStore contentHashStore = new ConcurrentContentHashStore(contentHashStorePath);

        logger.info("index only: {}", crawlerArguments.index);
        // Only performs this step when we are wanting to produce to a table
        if(!crawlerArguments.index){

            // Add the source information to the metadata
            Map<String,List<String>> metadata = buildACLEDMetadata(crawlerArguments.sourcedomain, crawlerArguments.seeds.get(0));
            metadata.put(CrawlerArguments.SOURCENAME, Arrays.asList(crawlerArguments.source));
            metadata.put(CrawlerArguments.COUNTRIES, Arrays.asList(crawlerArguments.countries));

            buildACLEDArticleFilters(handlers);

            // Add the scraper definition(s)
            // single scraper definition overrides scraper directory
            if(crawlerArguments.scraper != null && !crawlerArguments.scraper.equals("null")) {
                logger.info("Scraper " + crawlerArguments.scraper + " found for " + crawlerArguments.seeds.get(0));
                config.setPreImportProcessors(new ACLEDScraperPreProcessor(Paths.get(crawlerArguments.scrapers,crawlerArguments.scraper)),new ACLEDMetadataPreProcessor(metadata));
            }
            else {
                config.setPreImportProcessors(new ACLEDScraperPreProcessor(Paths.get(crawlerArguments.scrapers)),new ACLEDMetadataPreProcessor(metadata));
            }

            config.setDocumentChecksummer(new WebScraperMetadataChecksum(contentHashStore));
            // Add the crawler-to-spring-magic post-processor
            config.setPostImportProcessors(new ACLEDPostProcessor(articleDAO, sourceDAO, sourceListDAO,false));
            
        }

        importerConfig.setPostParseHandlers(handlers.toArray(new IImporterHandler[handlers.size()]));
        config.setImporterConfig(importerConfig);

        collector.setConfiguration(config);

        try {
            collector.start();
        } catch (URISyntaxException e) {
            throw new RuntimeException("The provided URL was invalid");
        }
    }

    private static void buildACLEDArticleFilters(List<IImporterHandler> handlers) {

        // Set the various document filters
        RegexMetadataFilter regexFilter = new RegexMetadataFilter(CrawlerArguments.SCRAPEDARTICLE, Util.KEYWORDS);
        EmptyMetadataFilter emptyArticle = new EmptyMetadataFilter(OnMatch.EXCLUDE,CrawlerArguments.SCRAPEDARTICLE);
        int week = 7;
        DateFilter df = new DateFilter(LocalDate.now().minusDays(week));

        handlers.add(emptyArticle);
        handlers.add(regexFilter);
        handlers.add(df);

    }

    private static Map<String,List<String>> buildACLEDMetadata(String sourcedomain, String seed) {
        logger.info("INFO: The web content with be scraped and produced to the database.");
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
