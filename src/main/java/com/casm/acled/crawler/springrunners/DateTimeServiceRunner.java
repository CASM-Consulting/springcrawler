package com.casm.acled.crawler.springrunners;

import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.reporting.Event;
import com.casm.acled.crawler.reporting.Report;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.scraper.dates.CompositeDateParser;
import com.casm.acled.crawler.scraper.dates.DateParser;
import com.casm.acled.crawler.scraper.dates.DateParsers;
import com.casm.acled.crawler.scraper.dates.DateTimeService;
import com.casm.acled.crawler.scraper.locale.LocaleService;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.article.Article;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@EnableAutoConfiguration(exclude={HibernateJpaAutoConfiguration.class, CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class, ValidationAutoConfiguration.class})
// We need the special object mapper, though.
//@Import({ObjectMapperConfiguration.class, CLIRunner.ShutdownConfig.class})
@Import({ObjectMapperConfiguration.class})
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao", "com.casm.acled.crawler"})
public class DateTimeServiceRunner implements CommandLineRunner {

    protected static final Logger logger = LoggerFactory.getLogger(DateTimeServiceRunner.class);

    @Autowired
    private DateTimeService dateTimeService;

    @Autowired
    private Reporter reporter;

    @Autowired
    private SourceDAO sourceDAO;

    @Autowired
    private SourceListDAO sourceListDAO;

    @Autowired
    private ArticleDAO articleDAO;

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


    public void assignParser(String sourceName, DateParser parser, boolean overwrite) {
        Optional<Source> maybeSource = sourceDAO.getByUnique(Source.STANDARD_NAME, sourceName);
        if(maybeSource.isPresent()) {

            Source source = maybeSource.get();
            if(source.hasValue(Source.DATE_FORMAT) && !overwrite) {

                List<String> spec = parser.getFormatSpec();
                List<String> existing = source.get(Source.DATE_FORMAT);

                spec.addAll(existing);
                source = source.put(Source.DATE_FORMAT, spec);
                sourceDAO.update(source);

                logger.error("updating date format {}", sourceName);
            } else {

                List<String> spec = parser.getFormatSpec();
                source = source.put(Source.DATE_FORMAT, spec);
                logger.error("adding date format {}", sourceName);
                sourceDAO.update(source);
            }
        } else {
            logger.error("source not found {}", sourceName);
        }
    }

    public void assignParsers(Map<String, DateParser> parsres) {

        for(Map.Entry<String, DateParser> entry : parsres.entrySet()) {

            String sourceName = entry.getKey();
            DateParser parser = entry.getValue();

            assignParser(sourceName, parser, false);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        reporter.randomRunId();

//        attemptSourceListExistingArticles("balkans");
//        dateTimeService.checkExistingPasses(sourceListDAO.getByUnique(SourceList.LIST_NAME, "balkans").get(), getFromArticles);
//        dateTimeService.attemptSourceListDateTimeParsers(sourceListDAO.getByUnique(SourceList.LIST_NAME, "Balkans").get(), DateParsers.ALL, getFromArticles);

//        assignParsers(DateParsers.SOURCE_SPECIFIC);

//        assignParser("Hoy Tamaulipas", CompositeDateParser.of(ImmutableList.of("ISO:/d 'de' MMM 'del' yyyy 'a las' HH:mm/es/RE.*?(\\d{1,2} de.+)/")), true);
//        assignParser("8 Columnas", CompositeDateParser.of(ImmutableList.of("ISO:/MMM dd, yyyy/es/")), true);
        assignParser("La Jornada", CompositeDateParser.of(ImmutableList.of( "ISO:/d 'de' MMM 'de' yyyy/es/RE.*?(\\d{1,2} de.+),.+/", "ISO:/dd MMM yyyy HH:mm/es/RE.*,(.*)")), true);

//        dateTimeService.attemptDateTimeParse(sourceDAO.getById(2254).get(), DateParsers.ALL, getFromArticles);

//        dateTimeService.attemptDateTimeParse(sourceDAO.getById(3281).get(), DateParsers.ALL, getFromArticles);
//        dateTimeService.attemptDateTimeParse(sourceDAO.getById(1262).get(), DateParsers.ALL, getFromArticles);
//        dateTimeService.attemptDateTimeParse(sourceDAO.getById(2977).get(), DateParsers.ALL, getFromArticles);
//        dateTimeService.attemptDateTimeParse(sourceDAO.getById(17749).get(), DateParsers.ALL, getFromArticles);
//        dateTimeService.attemptDateTimeParse(sourceDAO.getById(38).get(), DateParsers.ALL, getFromArticles);
//        dateTimeService.attemptDateTimeParse(sourceDAO.getById(3795).get(), DateParsers.ALL, getFromArticles);
//        dateTimeService.attemptDateTimeParse(sourceDAO.getById(1658).get(), DateParsers.ALL, getFromArticles);
//        dateTimeService.attemptDateTimeParse(sourceDAO.getById(3591).get(), DateParsers.ALL, getFromArticles);
//        dateTimeService.attemptDateTimeParse(sourceDAO.getById(4421).get(), DateParsers.ALL, getFromArticles);
//        dateTimeService.attemptDateTimeParse(sourceDAO.getById(16778).get(), DateParsers.ALL, getFromArticles);
//        dateTimeService.attemptDateTimeParse(sourceDAO.getById(2122).get(), DateParsers.ALL, getFromArticles);
//        dateTimeService.attemptDateTimeParse(sourceDAO.getById(3264).get(), DateParsers.ALL, getFromArticles);
//        dateTimeService.attemptDateTimeParse(sourceDAO.getById(3264).get(), DateParsers.ALL, getFromArticles);
//        dateTimeService.attemptDateTimeParse(sourceDAO.getById(4345).get(), DateParsers.ALL, getFromArticles);
//        dateTimeService.attemptDateTimeParse(sourceDAO.getById(1891).get(), DateParsers.ALL, getFromArticles);
//        dateTimeService.attemptDateTimeParse(sourceDAO.getById(2139).get(), DateParsers.ALL, getFromArticles);
//        dateTimeService.attemptDateTimeParse(sourceDAO.getById(4642).get(), DateParsers.ALL, getFromArticles);
//        dateTimeService.attemptDateTimeParse(sourceDAO.getById(3596).get(), DateParsers.ALL, getFromArticles);
//        dateTimeService.attemptDateTimeParse(sourceDAO.getById(17335).get(), DateParsers.ALL, getFromArticles);
//        dateTimeService.attemptDateTimeParse(sourceDAO.getById(1265).get(), DateParsers.ALL, getFromArticles);
//        dateTimeService.attemptDateTimeParse(sourceDAO.getById(1263).get(), DateParsers.ALL, getFromArticles);



        reporter.getRunReports().stream().forEach(r -> logger.info(r.toString()));

    }

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(DateTimeServiceRunner.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(args);
        logger.info("Spring Boot application started");
        ctx.close();
    }
}