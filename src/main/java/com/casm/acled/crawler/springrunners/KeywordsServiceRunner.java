package com.casm.acled.crawler.springrunners;


import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.reporting.Event;
import com.casm.acled.crawler.reporting.Report;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.reporting.ReportingException;
import com.casm.acled.crawler.scraper.keywords.KeywordsService;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
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

@EnableAutoConfiguration(exclude={HibernateJpaAutoConfiguration.class, CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class, ValidationAutoConfiguration.class})
// We need the special object mapper, though.
//@Import({ObjectMapperConfiguration.class, CLIRunner.ShutdownConfig.class})
@Import({ObjectMapperConfiguration.class})
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao", "com.casm.acled.crawler"})
public class KeywordsServiceRunner implements CommandLineRunner {

    protected static final Logger logger = LoggerFactory.getLogger(KeywordsServiceRunner.class);

    @Autowired
    private KeywordsService keywordsService;

    @Autowired
    private SourceListDAO sourceListDAO;

    @Autowired
    private SourceDAO sourceDAO;

    @Autowired
    private Reporter reporter;

    public void testURL(String sourceListName, String sourceName, String url) {
        SourceList sourceList = sourceListDAO.getByUnique(SourceList.LIST_NAME, sourceListName).get();
        Source source = sourceDAO.getByUnique(Source.STANDARD_NAME, sourceName).get();

        try {

            boolean matched = keywordsService.checkURL(sourceList, source, url);

            if(matched) {
                reporter.report(Report.of(Event.QUERY_MATCH).id(source.id()).message(url));
            } else {
                reporter.report(Report.of(Event.QUERY_NO_MATCH).id(source.id()).message(url));
            }
        } catch (ReportingException e) {

            reporter.report(e.get());
        }

    }


    @Override
    public void run(String... args) throws Exception {
        reporter.randomRunId();

        testURL("mexico-1", "MiMorelia", "https://www.mimorelia.com/localizan-cuerpo-de-una-mujer-en-la-carretera-zamora-morelia/");
//        testURL("balkans", "Politika", "http://www.politika.rs/sr/clanak/453708/Protest-ispred-Predsednistva");
//        testURL("Balkans", "24sata.hr", "https://www.24sata.hr/news/mirni-prosvjed-s-300-autobusa-problemi-su-lizing-i-krediti-691882");
//        testURL("Balkans", "24sata.hr", "https://www.24sata.hr/news/otkrivamo-misterij-stepinceva-dnevnika-koji-je-uzela-udba-690916");
//        testURL("Balkans", "Glas Slavonije", "http://www.glas-slavonije.hr/431867/1/Turisticki-prijevoznici-traze-odgadjanje-placanja-leasinga");


//        keywordsHelper.determineKeywordsList();
//        String query = keywordsService.importFromCSV(Paths.get("/home/sw206/Dropbox/acled/spec/Balkans Keyword List_0522.csv"));
//        keywordsService.test(query, "bomb");
//        SourceList sourceList = sourceListDAO.getByUnique(SourceList.LIST_NAME, "fake-net").get();
//        keywordsService.assignKeywords(sourceList, "(attack bomb explosion)");
//
//        reporter.getRunReports().stream().forEach(r -> logger.info(r.toString()));
    }

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(KeywordsServiceRunner.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(args);
        logger.info("Spring Boot application started");
        ctx.close();
    }
}