package com.casm.acled.crawler.springrunners;

import com.beust.jcommander.JCommander;
import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.management.CheckListService;
import com.casm.acled.crawler.management.CrawlArgs;
import com.casm.acled.crawler.reporting.Reporter;
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
    private Reporter reporter;

    @Autowired
    private CrawlArgs crawlArgs;


    @Override
    public void run(String... args) throws Exception {
        reporter.randomRunId();

        JCommander.newBuilder()
                .addObject(crawlArgs.raw)
                .build()
                .parse(args);

        crawlArgs.init();

        switch(crawlArgs.program) {
            case "import":
                checkListService.importCrawlerSourceList(crawlArgs);
                break;
            case "export":
                checkListService.exportCrawlerSourceList(crawlArgs);
                break;
            case "check":
                checkListService.checkSourceList(crawlArgs);
                break;
            case "example-urls":
                checkListService.outputExampleURLCheck(crawlArgs);
                break;
            default:
                logger.error("program {} not recognised", crawlArgs.program);
                break;
        }

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