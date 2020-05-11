package com.casm.acled.crawler.springrunners;

import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.scraper.ScraperService;
import com.casm.acled.dao.entities.SourceListDAO;
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

import java.nio.file.Path;
import java.nio.file.Paths;

@EnableAutoConfiguration(exclude={HibernateJpaAutoConfiguration.class, CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class, ValidationAutoConfiguration.class})
// We need the special object mapper, though.
//@Import({ObjectMapperConfiguration.class, CLIRunner.ShutdownConfig.class})
@Import({ObjectMapperConfiguration.class})
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao", "com.casm.acled.crawler"})
public class ScraperRunner implements CommandLineRunner {

    protected static final Logger logger = LoggerFactory.getLogger(ScraperRunner.class);

    private static Path scraperDir = Paths.get("allscrapers");

    @Autowired
    private SourceListDAO sourceListDAO;

    @Autowired
    private ScraperService scraperService;

    @Autowired
    private Reporter reporter;

    @Override
    public void run(String... args) {

        reporter.randomRunId();

        SourceList sourceList = sourceListDAO.getByUnique(SourceList.LIST_NAME, "balkans").get();

        scraperService.checkExampleURLs(scraperDir, sourceList);

        reporter.getRunReports().stream().forEach(r -> logger.info(r.toString()));
    }

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(ScraperRunner.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(args);
        logger.info("Spring Boot application started");
        ctx.close();
    }
}