package com.casm.acled.crawler.spring;

import com.beust.jcommander.JCommander;
import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.ACLEDMetadataPreProcessor;
import com.casm.acled.crawler.ACLEDPostProcessor;
import com.casm.acled.crawler.ACLEDScraperPreProcessor;
import com.casm.acled.crawler.DateFilter;
import com.casm.acled.crawler.utils.Utils;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.norconex.collector.http.crawler.HttpCrawlerConfig;
import com.norconex.importer.ImporterConfig;
import com.norconex.importer.handler.filter.OnMatch;
import com.norconex.importer.handler.filter.impl.DateMetadataFilter;
import com.norconex.importer.handler.filter.impl.EmptyMetadataFilter;
import com.norconex.importer.handler.filter.impl.RegexMetadataFilter;
import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
import org.camunda.bpm.spring.boot.starter.rest.CamundaBpmRestJerseyAutoConfiguration;
import org.ocpsoft.prettytime.shade.net.fortuna.ical4j.model.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.*;

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

        CrawlerArguments ca = new CrawlerArguments();
        JCommander.newBuilder()
                .addObject(ca)
                .build()
                .parse(corrArgs);

        SingleSeedCollector cc = new SingleSeedCollector(ca.userAgent,new File(ca.crawldb), Utils.getDomain(ca.seeds.get(0)),
                ca.depth, ca.urlFilter,ca.threadsPerSeed,ca.ignoreRobots,
                ca.ignoreSitemap, ca.polite,
                ca.seeds.get(0));

        HttpCrawlerConfig config = cc.getConfiguration();

        // Only performs this step when we are wanted to produce to a table
        if(!ca.index){
            logger.info("INFO: The web content with be scraped and produced to the database.");
            Map<String,List<String>> map = new HashMap<>();
            map.put(ACLEDMetadataPreProcessor.LINK, Arrays.asList(ca.seeds.get(0)));
            map.put(CrawlerArguments.SOURCENAME, Arrays.asList(ca.source));
            map.put(CrawlerArguments.COUNTRIES, Arrays.asList(ca.countries));
            config.setPreImportProcessors(new ACLEDScraperPreProcessor(Paths.get(ca.scrapers)),new ACLEDMetadataPreProcessor(map));
            //,new ACLEDMetadataPreProcessor(map)
            config.setPostImportProcessors(new ACLEDPostProcessor(articleDAO, sourceDAO, sourceListDAO));
            ImporterConfig ic = new ImporterConfig();

            // Set the various document filters
            EmptyMetadataFilter emptyArticle = new EmptyMetadataFilter(OnMatch.EXCLUDE,ACLEDScraperPreProcessor.SCRAPEDJSON);
            RegexMetadataFilter regexFilter = new RegexMetadataFilter(ACLEDScraperPreProcessor.SCRAPEDJSON, Utils.KEYWORDS);
            DateFilter df = new DateFilter();
            ic.setPostParseHandlers(emptyArticle, df,regexFilter);
            config.setImporterConfig(ic);
        }



        cc.setConfiguration(config);


//        KeepOnlyTagger keeper = new KeepOnlyTagger();
//        keeper.addField("reference");
//        keeper.addField("crawldate");
//        keeper.addField(ACLEDMetadataPreProcessor.CRAWLDATE);
//        keeper.addField(ACLEDMetadataPreProcessor.DEPTH);
//        keeper.addField(ACLEDMetadataPreProcessor.LINK);
//        keeper.addField(ACLEDMetadataPreProcessor.UPDATED);
        //Need to add hash field as well.

        try {
            cc.start();
        } catch (URISyntaxException e) {
            throw new RuntimeException("The provided URL was invalid");
        }
    }
}
