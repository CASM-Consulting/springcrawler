package com.casm.acled.crawler.springrunners;

import com.beust.jcommander.JCommander;
import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.management.CheckListService;
import com.casm.acled.crawler.management.CrawlArgs;
import com.casm.acled.crawler.reporting.Event;
import com.casm.acled.crawler.reporting.Report;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.scraper.ACLEDScraper;
import com.casm.acled.crawler.scraper.ScraperFields;
import com.casm.acled.crawler.scraper.ScraperService;
import com.casm.acled.crawler.scraper.dates.DateParser;
import com.casm.acled.crawler.scraper.dates.DateParsers;
import com.casm.acled.crawler.scraper.dates.DateTimeService;
import com.casm.acled.crawler.spring.CrawlService;
import com.casm.acled.crawler.util.Util;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.google.common.collect.ImmutableList;
import com.norconex.collector.http.doc.HttpDocument;
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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@EnableAutoConfiguration(exclude={HibernateJpaAutoConfiguration.class, CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class, ValidationAutoConfiguration.class})
// We need the special object mapper, though.
//@Import({ObjectMapperConfiguration.class, CLIRunner.ShutdownConfig.class})
@Import({ObjectMapperConfiguration.class})
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao", "com.casm.acled.crawler"})
public class CheckListRunner implements CommandLineRunner {

    protected static final Logger logger = LoggerFactory.getLogger(CheckListRunner.class);

    @Autowired
    private CheckListService checkListService;

    @Autowired
    private DateTimeService dateTimeService;

    @Autowired
    private ScraperService scraperService;

    @Autowired
    private CrawlService crawlService;

    @Autowired
    private Reporter reporter;

    @Autowired
    private SourceDAO sourceDAO;

    @Autowired
    private SourceListDAO sourceListDAO;

    @Autowired
    private ArticleDAO articleDAO;

    @Autowired
    private CrawlArgs crawlArgs;

    private void attemptAllScrapers() {
        dateTimeService.setScrapersPath(Paths.get("allscrapers"));

        dateTimeService.attemptAllDateTimeParsers(DateParsers.ALL, DateTimeService.lastScrapeExampleGetter(Paths.get("/home/sw206/git/acled-scrapers")));

//        System.out.println(reporter.reports(r->r.event().equals(Event.DATE_PARSE_SUCCESS.name())));
//        System.out.println(reporter.reports(r->r.event().equals(Event.DATE_PARSE_FAILED.name())));
    }

    private Function<Source, List<String>> getFromArticles = s -> {
        List<Article> articles = articleDAO.bySource(s);
        return articles.stream()
                .filter(a -> a.hasValue(Article.SCRAPE_DATE))
                .map(a -> (String)a.get(Article.SCRAPE_DATE))
                .collect(Collectors.toList());
    };


    private void attemptSourceListExistingArticles(String name) {
        SourceList sourceList = sourceListDAO.getByUnique(SourceList.LIST_NAME, name).get();
        dateTimeService.attemptSourceListDateTimeParsers(sourceList, DateParsers.ALL, getFromArticles);

        for(Report report : reporter.reports()) {
            logger.info(report.toString());
        }

//        System.out.println(reporter.reports(r->r.event().equals(Event.DATE_PARSE_SUCCESS.name())));
//        System.out.println(reporter.reports(r->r.event().equals(Event.DATE_PARSE_FAILED.name())));
    }


    public void attemptSource(int id, DateParser dateParser) {

        Source source = sourceDAO.getById(id).get();
        dateTimeService.attemptDateTimeParse(source, ImmutableList.of(dateParser), getFromArticles);
    }


    public boolean scraperExists(CrawlArgs args, Source source) {
        if(Util.scraperExists(args.scrapersDir, source)) {
            return true;
        } else {
            reporter.report(Report.of(Event.SCRAPER_NOT_FOUND).id(source.id()).message(source.get(Source.STANDARD_NAME)));
            return false;
        }
    }

    public boolean hasExamples(Source source) {
        List<String> exampleURLs = source.get(Source.EXAMPLE_URLS);

        if(exampleURLs == null || exampleURLs.isEmpty()) {
            reporter.report(Report.of(Event.NO_EXAMPLES).id(source.id()).message(source.get(Source.STANDARD_NAME)));
            return false;
        } else {
            return true;
        }
    }

    public boolean hasDateFormat(Source source) {
        List<String> formats = source.get(Source.DATE_FORMAT);

        if(formats == null || formats.isEmpty()) {
            reporter.report(Report.of(Event.DATE_PARSER_NOT_FOUND).id(source.id()).message(source.get(Source.STANDARD_NAME)));
            return false;
        } else {
            return true;
        }
    }

    public boolean datesParse(CrawlArgs args, Source source) {
        ACLEDScraper scraper = ACLEDScraper.load(args.scrapersDir, source, reporter);

        return dateTimeService.checkExistingPasses(source, (s) -> {

            List<HttpDocument> docs = scraperService.checkExampleURLs(scraper, s);
            List<String> dateExamples = docs.stream()
                    .filter(doc -> doc.getMetadata().containsKey(ScraperFields.SCRAPED_DATE) &&
                            !doc.getMetadata().getString(ScraperFields.SCRAPED_DATE).isEmpty())
                    .map(doc -> doc.getMetadata().getString(ScraperFields.SCRAPED_DATE))
                    .collect(Collectors.toList());

            return dateExamples;
        });
    }

    public boolean hasSiteMaps(Source source) {
        List<String> siteMaps = crawlService.getSitemaps(source);

        if(siteMaps.isEmpty()) {
            reporter.report(Report.of(Event.NO_SITE_MAPS).id(source.id()).message(source.get(Source.STANDARD_NAME)));
            return false;
        } else {
            return true;
        }
    }

    public void checkSource(CrawlArgs args, Source source, SourceList sourceList)  {

        boolean scraperExists = scraperExists(args, source);
        boolean hasExamples = hasExamples(source);
        boolean hasDateFormat = hasDateFormat(source);
        boolean hasSiteMaps = hasSiteMaps(source);

        if(hasSiteMaps) {
            reporter.report(Report.of(Event.HAS_SITE_MAPS).id(source.id()).message(source.get(Source.STANDARD_NAME)));
        }

        if(hasDateFormat && hasExamples && scraperExists) {

            boolean datesParsed = datesParse(args, source);


            if(datesParsed) {
                reporter.report(Report.of(Event.SCRAPE_PASS).id(source.id()).message(source.get(Source.STANDARD_NAME)));
            }


        }

    }

    public void outputCrawlerSourceList(CrawlArgs args) throws IOException {

        SourceList sourceList = args.sourceList;
        String name = sourceList.get(SourceList.LIST_NAME);

        checkListService.exportCrawlerSourcesToCSV(args.workingDir, name+".csv", sourceList);
    }

    public void importCrawlerSourceList(CrawlArgs args) throws IOException {

        SourceList sourceList = args.sourceList;
        String name = sourceList.get(SourceList.LIST_NAME);

        checkListService.importCrawlerSourcesFromCSV(args.workingDir.resolve(name+".csv"), EntityVersions.get(Source.class).current());
    }


    public void checkSourceList(CrawlArgs args) {

        SourceList sourceList = args.sourceList;
        List<Source> sources = sourceDAO.byList(sourceList);

        for(Source source : sources) {

            checkSource(args, source, sourceList);
        }


    }

    @Override
    public void run(String... args) throws Exception {
        reporter.randomRunId();


        JCommander.newBuilder()
                .addObject(crawlArgs.raw)
                .build()
                .parse(args);

        crawlArgs.init();

//        checkSourceList(crawlArgs);
//        outputCrawlerSourceList(crawlArgs);
        importCrawlerSourceList(crawlArgs);

        reporter.getRunReports().stream().forEach(r -> logger.info(r.toString()));

    }

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(CheckListRunner.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(args);
        logger.info("Spring Boot application started");
        ctx.close();
    }
}