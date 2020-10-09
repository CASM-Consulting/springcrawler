package com.casm.acled.crawler.springrunners;

import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.management.CrawlArgs;
import com.casm.acled.crawler.management.CrawlArgsService;
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

//import org.springframework.shell.standard.ShellMethod;
//import org.springframework.shell.standard.ShellComponent;


@EnableAutoConfiguration(exclude={HibernateJpaAutoConfiguration.class, CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class, ValidationAutoConfiguration.class})
// We need the special object mapper, though.
//@Import({ObjectMapperConfiguration.class, CLIRunner.ShutdownConfig.class})
@Import({ObjectMapperConfiguration.class})
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao", "com.casm.acled.crawler"})

//@ShellComponent
public class CrawlerJQMRunner implements CommandLineRunner {

    protected static final Logger logger = LoggerFactory.getLogger(CrawlerJQMRunner.class);

    @Autowired
    private CrawlerSweep crawlerSweep;

    @Autowired
    private Reporter reporter;

    @Autowired
    private SourceListDAO sourceListDAO;

    @Autowired
    private SourceDAO sourceDAO;


    @Autowired
    private CrawlArgsService argsService;

    private CrawlArgs crawlArgs;


    public static String JQMSpringCollectorV1 = "JQMSpringCollectorV1";
    public static String JQMSpringExampleCollectorV1 = "JQMSpringExampleCollectorV1";

    public void setCrawlArgs(String app, String listName, LocalDate from, LocalDate to, Path workingDir, Boolean skipKeywords) {
        Path scraperDir = Paths.get("/home/sw206/git/acled-scrapers");

        crawlArgs = argsService.get();

//        crawlArgs.raw.sourceLists = listName;

        if (from!=null) {
            crawlArgs.raw.from = from.toString();
        }
        if (to!=null) {
            crawlArgs.raw.to = to.toString();
        }
        crawlArgs.raw.skipKeywords = skipKeywords;
        crawlArgs.raw.program = app;

        crawlArgs.depth = 0;

        crawlArgs.init();

        crawlArgs.scrapersDir = scraperDir;

        if (workingDir == null) {
            crawlArgs.workingDir = Paths.get(".");
        }
        else {
            crawlArgs.workingDir = workingDir;
        }

//        List<JobRequest> requests = crawlerArgs.toJobRequests();
    }

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

//    @Override
//    @ShellMethod("Translate text from one language to another.")
    public void run(String... args) throws Exception {

//        crawlerSweep.sweepAvailableScrapers(Paths.get("allscrapers"));
        Path scraperDir = Paths.get("/home/sw206/git/acled-scrapers");

        crawlArgs = argsService.get();

        crawlArgs.raw.sourceLists = ImmutableList.of("mexico-1");
        crawlArgs.raw.source = "8 Columnas";

//        sweepSourceList("balkans", LocalDate.of(2020, 5,3), LocalDate.of(2020, 5,9), Boolean.FALSE);
//        sweepSourceList(JQMSpringCollectorV1, "Balkans", LocalDate.of(2020, 5,3), LocalDate.of(2020, 5,16), Boolean.FALSE);
        //sweepSourceListCollectExamples(JQMSpringExampleCollectorV1, "Balkans");

//        sweepSourceList(JQMSpringCollectorV1, "mexico-back-code-2018", LocalDate.of(2018, 1,1), LocalDate.of(2018, 12,31), Boolean.FALSE);
//        sweepSourceList(JQMSpringCollectorV1, "fake-net", LocalDate.now().minusDays(10), LocalDate.now(), Boolean.TRUE);
//        sweepSourceList(JQMSpringCollectorV1, "fake-net", LocalDate.of(2020, 8,21), LocalDate.of(2020, 8,28), Boolean.TRUE);

        setCrawlArgs(JQMSpringCollectorV1, "mexico-1", LocalDate.of(2020, 10,1), LocalDate.of(2020, 10,17), Paths.get("/home/sw206/jqm/jqm-2.2.5/test"), Boolean.FALSE);

        crawlerSweep.sweep(ImmutableList.of(crawlArgs));

//        sweepSourceList(JQMSpringCollectorV1, "fake-net", LocalDate.of(2020, 8,21), LocalDate.of(2020, 8,28), Boolean.TRUE);

        //        sweepSourceCollectExamples("HRW", "Balkans");

//        singleSource(args[0], args[1]);
    }

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(CrawlerJQMRunner.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(args);
        logger.info("Spring Boot application started");
        ctx.close();
    }
}