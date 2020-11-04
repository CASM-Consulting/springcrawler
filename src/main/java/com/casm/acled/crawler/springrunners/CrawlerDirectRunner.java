package com.casm.acled.crawler.springrunners;

import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.management.CrawlArgs;
import com.casm.acled.crawler.management.CrawlArgsService;
import com.casm.acled.crawler.management.CrawlerSweep;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.spring.CrawlService;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.source.Source;
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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@EnableAutoConfiguration(exclude={HibernateJpaAutoConfiguration.class, CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class, ValidationAutoConfiguration.class})
// We need the special object mapper, though.
//@Import({ObjectMapperConfiguration.class, CLIRunner.ShutdownConfig.class})
@Import({ObjectMapperConfiguration.class})
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao", "com.casm.acled.crawler"})
public class CrawlerDirectRunner implements CommandLineRunner {

//    static {
//        Object guard = new Object();
//        LoggerRepository rs = new CustomLoggerRepository(new RootLogger((Level) Level.DEBUG));
//        LogManager.setRepositorySelector(new DefaultRepositorySelector(rs), guard);
//    }
    protected static final Logger logger = LoggerFactory.getLogger(CrawlerDirectRunner.class);

    @Autowired
    private SourceListDAO sourceListDAO;

    @Autowired
    private CrawlService crawlService;

    @Autowired
    private Reporter reporter;

    @Autowired
    private CrawlArgsService argsService;

    private CrawlArgs crawlArgs;

    public void crawl(String[] args) {
        int sourceListId = Integer.parseInt(args[0]);
        int sourceId = Integer.parseInt(args[1]);

        LocalDate from;
        try {
            from = LocalDate.parse(args[2]);
        } catch (DateTimeParseException e) {
            from = null;
        }
        LocalDate to;
        try {
            to = LocalDate.parse(args[3]);
        } catch (DateTimeParseException e) {
            to = null;
        }

        boolean skipKeywords = Boolean.parseBoolean(args[4]);

        if(from != null && to != null) {
            crawlService.run(sourceListId, sourceId, from, to, skipKeywords);
        } else {
            crawlService.run(sourceListId, sourceId, skipKeywords);
        }
    }


    private void collectExamples(int sourceId, int sourceListId) {

        crawlService.collectExamples(sourceListId, sourceId);
    }



    @Override
    public void run(String... args) throws Exception {

        reporter.randomRunId();

        crawlArgs = argsService.get();

        crawlArgs.raw.skipKeywords = false;
        crawlArgs.raw.program = "crawl";

//        crawlArgs.raw.source = "1";
//        crawlArgs.raw.source = "Riodoce";
//        crawlArgs.raw.source = "La Verdad";
//        crawlArgs.raw.source = "Animal Politico";
        crawlArgs.raw.source = "El Proceso";
//        crawlArgs.raw.source = "La Silla Rota";
//        crawlArgs.raw.source = "Imagen del Golfo";
//        crawlArgs.raw.source = "Sintesis";
//        crawlArgs.raw.source = "8 Columnas";
//        crawlArgs.raw.source = "El Sol de Acapulco";
//        crawlArgs.raw.source = "Siglo de Durango";
//        crawlArgs.raw.source = "Imagen del Golfo";

//        crawlArgs.raw.source = "Milenio";
//        crawlArgs.raw.source = "La Prensa de Monclova";
//        crawlArgs.raw.source = "MiMorelia";
//        crawlArgs.raw.source = "Sintesis";
//        crawlArgs.raw.source = "Noroeste";
//        crawlArgs.raw.source = "24 Horas (Mexico)";
//        crawlArgs.raw.source = "nncMX";
//        crawlArgs.raw.source = "Tribuna Campeche";
        crawlArgs.raw.sourceLists = ImmutableList.of("mexico-1");
//        crawlArgs.raw.sourceLists = ImmutableList.of("fake-net");
        crawlArgs.raw.from = "2020-10-29";
        crawlArgs.raw.to =  "2021-01-01";
        crawlArgs.raw.workingDir = "test";
//        crawlArgs.raw.workingDir = "/Users/pengqiwei/Downloads/My/PhDs/acled_thing/JQM_ROOT/test";
//        crawlArgs.raw.scrapersDir = "/home/sw206/git/acled-scrapers/";
        crawlArgs.raw.scrapersDir = "/Users/adr27/Documents/git/acled-scrapers/";
//        crawlArgs.raw.scrapersDir = "/Users/pengqiwei/Downloads/My/PhDs/acled_thing/springcrawler/testscrapers/generic/";
//        crawlArgs.raw.scrapersDir = "/Users/pengqiwei/Downloads/My/PhDs/acled_thing/acled-scrapers/";

        crawlArgs.raw.depth = 0;
//        crawlArgs.raw.ignoreSiteMap = true;

        crawlArgs.init();

        // added here for testing; need to remove afterwards, probably should not assign it like this;
//        crawlArgs.source = crawlArgs.source.put(Source.SCRAPER_RULE_ARTICLE, "div.siete60 div#contenido");
//        crawlArgs.source = crawlArgs.source.put(Source.SCRAPER_RULE_TITLE, "div.siete60 div.SlaBLK22, div.siete60 div#contenido"); // multiple selectors;
//        crawlArgs.source = crawlArgs.source.put(Source.SCRAPER_RULE_DATE, "div.siete60 div.RobBLK12");

        crawlService.run(crawlArgs);
//        collectExamples(1657,1);

//        Map<String, List<String>> sitemaps = crawlService.getSitemaps(sourceListDAO.getByUnique(SourceList.LIST_NAME, "mexico-back-code-2018").get());
//        System.out.println(sitemaps);

//        crawlService.getRobots("https://www.elsoldesinaloa.com.mx");

        reporter.getRunReports().stream().forEach(r -> logger.info(r.toString()));

    }

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(CrawlerDirectRunner.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(args);
        logger.info("Spring Boot application started");
        ctx.close();
    }
}
