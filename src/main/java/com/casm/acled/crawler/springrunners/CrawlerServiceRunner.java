package com.casm.acled.crawler.springrunners;

import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.spring.CrawlService;
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
public class CrawlerServiceRunner implements CommandLineRunner {

    protected static final Logger logger = LoggerFactory.getLogger(CrawlerServiceRunner.class);


    @Autowired
    private CrawlService crawlService;


    @Override
    public void run(String... args) throws Exception {

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

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(CrawlerServiceRunner.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(args);
        logger.info("Spring Boot application started");
        ctx.close();
    }
}