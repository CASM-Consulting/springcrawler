package com.casm.acled.crawler.springrunners;

import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.management.CrawlerSweep;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.google.common.collect.ImmutableList;
import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
import org.camunda.bpm.spring.boot.starter.rest.CamundaBpmRestJerseyAutoConfiguration;
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
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.nio.file.Paths;
import java.time.LocalDate;

@EnableAutoConfiguration(exclude={HibernateJpaAutoConfiguration.class, CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class, ValidationAutoConfiguration.class})
// We need the special object mapper, though.
//@Import({ObjectMapperConfiguration.class, CLIRunner.ShutdownConfig.class})
@Import({ObjectMapperConfiguration.class})
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao", "com.casm.acled.crawler"})
public class CrawlerSweepRunner implements CommandLineRunner {

    protected static final Logger logger = LoggerFactory.getLogger(CrawlerSweepRunner.class);

    @Autowired
    private CrawlerSweep crawlerSweep;

    @Autowired
    private Reporter reporter;

    @Autowired
    private SourceListDAO sourceListDAO;

    @Autowired
    private SourceDAO sourceDAO;

    public static String JQMSpringCollectorV1 = "JQMSpringCollectorV1";
    public static String JQMSpringExampleCollectorV1 = "JQMSpringExampleCollectorV1";

    public void sweepSourceList(String app, String name, LocalDate from, LocalDate to, Boolean skipKeywords) {
        SourceList sourceList = sourceListDAO.getByUnique(SourceList.LIST_NAME, name).get();
        crawlerSweep.sweepSourceList(app, sourceList, Paths.get("/home/sw206/git/acled-scrapers"), from, to, skipKeywords);
    }

    public void singleSource(String app, String listName, String sourceName, LocalDate from, LocalDate to, Boolean skipKeywords) {
        Source source = sourceDAO.getByUnique(Source.STANDARD_NAME, sourceName).get();
        SourceList sourceList = sourceListDAO.getByUnique(SourceList.LIST_NAME, listName).get();
        crawlerSweep.submitJobs(app, ImmutableList.of(source), sourceList.id(), from, to, skipKeywords);
    }

    public void sweepSourceListCollectExamples(String app, String name) {
        SourceList sourceList = sourceListDAO.getByUnique(SourceList.LIST_NAME, name).get();
        crawlerSweep.sweepSourceList(app, sourceList, Paths.get("/home/sw206/git/acled-scrapers"), null, null, Boolean.TRUE);
    }

    public void sweepSourceCollectExamples(String name, String list) {
        Source source = sourceDAO.getByUnique(Source.STANDARD_NAME, name).get();
        SourceList sourceList = sourceListDAO.getByUnique(SourceList.LIST_NAME, list).get();
        crawlerSweep.submitJob(JQMSpringExampleCollectorV1, source, sourceList.id(), null,null, Boolean.TRUE);
    }

    @Override
    public void run(String... args) throws Exception {

//        crawlerSweep.sweepAvailableScrapers(Paths.get("allscrapers"));

//        sweepSourceList("balkans", LocalDate.of(2020, 5,3), LocalDate.of(2020, 5,9), Boolean.FALSE);
        sweepSourceList(JQMSpringCollectorV1, "Balkans", LocalDate.of(2020, 5,3), LocalDate.of(2020, 5,16), Boolean.FALSE);
//        sweepSourceListCollectExamples(JQMSpringExampleCollectorV1, "Balkans");

        //        sweepSourceCollectExamples("HRW", "Balkans");

//        singleSource(args[0], args[1]);
    }

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(CrawlerSweepRunner.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(args);
        logger.info("Spring Boot application started");
        ctx.close();
    }
}