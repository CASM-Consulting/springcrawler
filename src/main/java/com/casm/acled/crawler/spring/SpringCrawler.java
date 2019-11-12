package com.casm.acled.crawler.spring;

import com.beust.jcommander.JCommander;
import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.ACLEDPostProcessor;
import com.casm.acled.crawler.ACLEDScraperPreProcessor;
import com.casm.acled.crawler.utils.Utils;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.util.ImportJSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.norconex.collector.http.crawler.HttpCrawlerConfig;
import com.norconex.importer.ImporterConfig;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.IImporterHandler;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.IDocumentFilter;
import com.norconex.importer.handler.filter.OnMatch;
import com.norconex.importer.handler.filter.impl.EmptyMetadataFilter;
import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
import org.camunda.bpm.spring.boot.starter.rest.CamundaBpmRestJerseyAutoConfiguration;
import org.hibernate.validator.HibernateValidatorConfiguration;
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
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@EnableAutoConfiguration(exclude={HibernateJpaAutoConfiguration.class,CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class, ValidationAutoConfiguration.class})
// We need the special object mapper, though.
@Import(ObjectMapperConfiguration.class)
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao"})

public class SpringCrawler implements CommandLineRunner {

    protected static final Logger logger = LoggerFactory.getLogger(ACLEDScraperPreProcessor.class);

    @Autowired
    private ArticleDAO articleDAO;


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
        config.setPostImportProcessors(new ACLEDPostProcessor(articleDAO));
        config.setPreImportProcessors(new ACLEDScraperPreProcessor(Paths.get(ca.scrapers)));

        ImporterConfig ic = new ImporterConfig();

        EmptyMetadataFilter emptyArticle = new EmptyMetadataFilter(OnMatch.EXCLUDE,ACLEDScraperPreProcessor.SCRAPEDJSON);
        ic.setPostParseHandlers(emptyArticle);

        config.setImporterConfig(ic);

        cc.setConfiguration(config);

        try {
            cc.start();
        } catch (URISyntaxException e) {
            throw new RuntimeException("The provided URL was invalid");
        }
    }
}
