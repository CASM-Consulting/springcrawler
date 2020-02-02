package com.casm.acled.crawler.spring;

import com.beust.jcommander.JCommander;

// acled
import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.ACLEDMetadataPreProcessor;
import com.casm.acled.crawler.ACLEDPostProcessor;
import com.casm.acled.crawler.ACLEDScraperPreProcessor;
import com.casm.acled.crawler.DateFilter;
import com.casm.acled.crawler.utils.Utils;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;

// norconex
import com.norconex.collector.http.crawler.HttpCrawlerConfig;
import com.norconex.collector.http.data.store.impl.jdbc.JDBCCrawlDataStoreFactory;
import com.norconex.collector.http.doc.HttpMetadata;
import com.norconex.collector.http.recrawl.impl.GenericRecrawlableResolver;
import com.norconex.importer.ImporterConfig;
import com.norconex.importer.handler.IImporterHandler;
import com.norconex.importer.handler.filter.OnMatch;
import com.norconex.importer.handler.filter.impl.EmptyMetadataFilter;
import com.norconex.importer.handler.filter.impl.RegexMetadataFilter;

//camunda
import com.norconex.importer.handler.tagger.AbstractDocumentTagger;
import com.norconex.importer.handler.tagger.impl.CurrentDateTagger;
import com.norconex.importer.handler.tagger.impl.DebugTagger;
import com.norconex.importer.handler.tagger.impl.KeepOnlyTagger;
//import org.apache.tools.ant.taskdefs.condition.Http;
import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
import org.camunda.bpm.spring.boot.starter.rest.CamundaBpmRestJerseyAutoConfiguration;

// logging
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// java
import java.io.File;
import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.*;

// spring
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import uk.ac.susx.tag.norconex.jobqueuemanager.CrawlerArguments;
import uk.ac.susx.tag.norconex.jobqueuemanager.SingleSeedCollector;

@EnableAutoConfiguration(exclude={HibernateJpaAutoConfiguration.class,CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class, ValidationAutoConfiguration.class})
// We need the special object mapper, though.
@Import(ObjectMapperConfiguration.class)
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao"})
public class SpringCrawler implements CommandLineRunner {

    protected static final Logger logger = LoggerFactory.getLogger(ACLEDScraperPreProcessor.class);

    @Autowired
    private ArticleDAO articleDAO;
    @Autowired
    private SourceDAO sourceDAO;
    @Autowired
    private SourceListDAO sourceListDAO;


    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SpringCrawler.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }

    @Override
    public void run(String[] args) throws Exception {

        List<String> splitArgs = new ArrayList<>();
        for(String arg : args){
            splitArgs.addAll(Arrays.asList(arg.split("\\s+")));
        }

        String[] corrArgs = splitArgs.toArray(new String[splitArgs.size()]);

        CrawlerArguments crawlerArguments = new CrawlerArguments();
        JCommander.newBuilder()
                .addObject(crawlerArguments)
                .build()
                .parse(corrArgs);



        // add the protocol
//        String seed = (ca.seeds.get(0).startsWith("http")) ? ca.seeds.get(0) : ("http://" + ca.seeds.get(0));

        SingleSeedCollector collector = new SingleSeedCollector(crawlerArguments.userAgent,new File(crawlerArguments.crawldb), Utils.getDomain(crawlerArguments.seeds.get(0)),
                crawlerArguments.depth, crawlerArguments.urlFilter,crawlerArguments.threadsPerSeed,crawlerArguments.ignoreRobots,
                crawlerArguments.ignoreSitemap, crawlerArguments.polite,
                crawlerArguments.seeds.get(0));

        HttpCrawlerConfig config = collector.getConfiguration();

        logger.error("Starting config");
        ImporterConfig ic = new ImporterConfig();
        List<IImporterHandler> handlers = new ArrayList<>();

        // Only performs this step when we are wanting to produce to a table
        if(!crawlerArguments.index){


            // Add the source information to the metadata
            Map<String,List<String>> metadata = buildACLEDMetadata(crawlerArguments.sourcedomain, crawlerArguments.seeds.get(0));
            metadata.put(CrawlerArguments.SOURCENAME, Arrays.asList(crawlerArguments.source));
            metadata.put(CrawlerArguments.COUNTRIES, Arrays.asList(crawlerArguments.countries));

            buildACLEDArticleFilters(handlers);

            // Add the scraper definition(s)
            // single scraper definition overrides scraper directory
            if(crawlerArguments.scraper != null) {
                logger.info("INFO: Scraper " + crawlerArguments.scraper + " found for " + crawlerArguments.seeds.get(0));
                config.setPreImportProcessors(new ACLEDScraperPreProcessor(Paths.get(crawlerArguments.scrapers,crawlerArguments.scraper)), new ACLEDMetadataPreProcessor(metadata));
            }
            else {
                config.setPreImportProcessors(new ACLEDScraperPreProcessor(Paths.get(crawlerArguments.scrapers)), new ACLEDMetadataPreProcessor(metadata));
            }

            // Add the crawler-to-spring-magic post-processor
            config.setPostImportProcessors(new ACLEDPostProcessor(articleDAO, sourceDAO, sourceListDAO,false));
            
        }

        // appended to list last to avoid errors
//        KeepOnlyTagger kop = buildKeepOnly();
//        handlers.add(kop);

        ic.setPostParseHandlers(handlers.toArray(new IImporterHandler[handlers.size()]));
        config.setImporterConfig(ic);

//        GenericRecrawlableResolver grr = new GenericRecrawlableResolver();
//        GenericRecrawlableResolver.MinFrequency minFreq = new GenericRecrawlableResolver.MinFrequency();
//        minFreq.setCaseSensitive(false);
//        minFreq.setApplyTo();
//        grr.setMinFrequencies();
//        config.setRecrawlableResolver();

//        public class CleanUP implements CrawlerC
//        config.setCrawlDataStoreFactory();

        try {
            collector.start();
        } catch (URISyntaxException e) {
            throw new RuntimeException("The provided URL was invalid");
        }
    }

    private static void buildACLEDArticleFilters(List<IImporterHandler> handlers) {

        // Set the various document filters
        EmptyMetadataFilter emptyArticle = new EmptyMetadataFilter(OnMatch.EXCLUDE,ACLEDScraperPreProcessor.SCRAPEDJSON);
        RegexMetadataFilter regexFilter = new RegexMetadataFilter(ACLEDScraperPreProcessor.SCRAPEDJSON, Utils.KEYWORDS);

        int week = 168;
        DateFilter df = new DateFilter(new DateTime().minusHours(week).toDate());
//        CurrentDateTagger date = new CurrentDateTagger();

        handlers.add(emptyArticle);
        handlers.add(regexFilter);
        handlers.add(df);
//        handlers.add(date);

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
