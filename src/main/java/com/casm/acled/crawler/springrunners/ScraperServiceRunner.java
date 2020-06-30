package com.casm.acled.crawler.springrunners;

import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.reporting.Event;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.scraper.ScraperService;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.dao.util.ExportCSV;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.crawlreport.CrawlReport;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import java.time.LocalDateTime;
import java.util.*;

@EnableAutoConfiguration(exclude={HibernateJpaAutoConfiguration.class, CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class, ValidationAutoConfiguration.class})
// We need the special object mapper, though.
//@Import({ObjectMapperConfiguration.class, CLIRunner.ShutdownConfig.class})
@Import({ObjectMapperConfiguration.class})
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao", "com.casm.acled.crawler"})
public class ScraperServiceRunner implements CommandLineRunner {

    protected static final Logger logger = LoggerFactory.getLogger(ScraperServiceRunner.class);

    private static Path scraperDir = Paths.get("/home/sw206/git/acled-scrapers");

    @Autowired
    private SourceListDAO sourceListDAO;

    @Autowired
    private SourceDAO sourceDAO;

    @Autowired
    private ArticleDAO articleDAO;

    @Autowired
    private ScraperService scraperService;

    @Autowired
    private Reporter reporter;


    @Override
    public void run(String... args) {

        String runId = reporter.randomRunId();

//        Map<String, Event> playThrough = new LinkedHashMap<>();

//        SourceList sourceList = sourceListDAO.getByUnique(SourceList.LIST_NAME, "balkans").get();
//        Source source = sourceDAO.getById(3281).get();
//        Source source = sourceDAO.getById(3795).get();
//        Source source = sourceDAO.getById(1657).get();
//        Source source = sourceDAO.getById(2230).get();
//        Source source = sourceDAO.getById(1265).get();

//        scraperService.checkExampleURLs(scraperDir, source);
//        scraperService.checkExampleURLs(scraperDir,  sourceListDAO.byName("Balkans").get());
//        scraperService.checkExampleURLs(scraperDir,  sourceDAO.byName("Insajderi").get());

//        scraperService.checkScraperCoverage(scraperDir, sourceListDAO.byName("Balkans").get());

//        scraperService.checkScrapersFromFile(Paths.get("allscrapers2"), reporter);
//        List<Article> articles = scraperService.evalCsv2Articles( Paths.get("mexico-bc~GT.csv"));

//        List<Article> previous = articleDAO.getByBusinessKey("mexico-2018-gt");
//        articleDAO.delete(previous);
        List<Article> remaining;
//        scraperService.importEvalCsv( Paths.get("mexico-bc~GT.csv"), "mexico-2018-gt");
//        List<Article> articles = articleDAO.getByBusinessKey("mexico-2018-gt");
//        scraperService.matchArticlesByUrl(articles); //992bff4f-1c92-444f-821c-f1fa2fb87e93
//        playThrough.put(runId, Event.ARTICLE_NO_MATCH);
//
//        remaining = scraperService.getRemaining(runId, Event.ARTICLE_NO_MATCH);
//        reporter.runId(null);
//        runId = reporter.randomRunId();
//        scraperService.checkArticlesSource(remaining);// 6926d924-bfc2-45aa-8a86-7dd1db921c0d
//        playThrough.put(runId, Event.SOURCE_FOUND);
//
//
//        remaining = scraperService.getRemaining(runId, Event.SOURCE_FOUND);
//        reporter.runId(null);
//        runId = reporter.randomRunId();
//        scraperService.checkArticlesScraperExists(Paths.get("allscrapers2"), remaining);
//        playThrough.put(runId, Event.SCRAPER_FOUND);




//        runId = "ab20797b-1f0b-4b16-a2b6-d90986e9456f";
//        remaining = scraperService.getRemaining(runId, Event.SCRAPER_FOUND);
//
//        reporter.runId(null);
//        runId = reporter.randomRunId();
//        scraperService.checkArticlesScraperFunction(Paths.get("allscrapers2"), remaining); //a3cdebfa-456b-49dc-befc-e78380d0a4d7

//        runId = "257f826e-3396-4e56-966e-6e5a5de804ed";
//        remaining = scraperService.getRemaining(runId, Event.SCRAPE_PASS);
//        scraperService.checkArticlesDateParse(remaining, ImmutableMap.of(
//                LocalDateTime.of(2018, 6, 10, 0,0,0),LocalDateTime.of(2018, 6, 12, 0,0,0),
//                LocalDateTime.of(2018, 9, 23, 0,0,0),LocalDateTime.of(2018, 9, 25, 0,0,0)
//        ));
//
//        playThrough.put(runId, Event.SCRAPE_PASS);


        Map<Article, CrawlReport> explained = compileReport(playThrough, "mexico-2018-gt");


        scraperService.outputResults(Paths.get("mexico-bc-2018-gt-analysis.csv"), explained);

//        scraperService.eval(Paths.get("allscrapers2"), Paths.get("mexico-bc-GT.csv"),
//                sourceListDAO.getByUnique(SourceList.LIST_NAME, "mexico-back-code-2018").get());

        reporter.getRunReports().stream().forEach(r -> logger.info(r.toString()));

    }
    private static Map<String, Event> playThrough = new LinkedHashMap<>();
    static {
        playThrough.put("f24a9cd6-6c63-4b46-9576-7a1761a42416", Event.ARTICLE_NO_MATCH);
        playThrough.put("e2529c8f-9192-43b4-9a03-a668a3b07707", Event.SOURCE_FOUND);
        playThrough.put("ab20797b-1f0b-4b16-a2b6-d90986e9456f", Event.SCRAPER_FOUND);
        playThrough.put("257f826e-3396-4e56-966e-6e5a5de804ed", Event.SCRAPE_PASS);
        playThrough.put("8374f25b-ee0b-4ef4-9f68-856e5259ffed", Event.DATE_PARSE_SUCCESS);

    }

    public Map<Article, CrawlReport>  compileReport(Map<String, Event> playThrough, String businessKey) {
        Set<Article> articles = new HashSet<>(articleDAO.getByBusinessKey(businessKey));

        Map<Article, CrawlReport> explained = new HashMap<>();

        for(Map.Entry<String, Event> entry : playThrough.entrySet()) {
            String runId = entry.getKey();

            Map<Integer, CrawlReport> article2reportIds = scraperService.getReportIds(runId);

            Event event = entry.getValue();
            Set<Article> explananda = new HashSet<>(scraperService.getRemaining(runId, event));
            articles.removeAll(explananda);

            for(Article article : articles) {
                int id = article.id();
                explained.put(article, article2reportIds.get(id));
            }

            articles = explananda;
        }

        for(Article article : articles) {

            explained.put(article, EntityVersions.get(CrawlReport.class).current());
        }

        return explained;
    }

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(ScraperServiceRunner.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(args);
        logger.info("Spring Boot application started");
        ctx.close();
    }
}