package com.casm.acled.crawler.springrunners;

import com.beust.jcommander.JCommander;

// acled
import com.casm.acled.configuration.ObjectMapperConfiguration;

// norconex

//camunda
import com.casm.acled.crawler.management.CrawlArgs;
import com.casm.acled.crawler.management.CrawlArgsService;
import com.casm.acled.crawler.management.CrawlerSweep;
import com.google.common.collect.ImmutableList;
//import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
//import org.camunda.bpm.spring.boot.starter.rest.CamundaBpmRestJerseyAutoConfiguration;

// logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// java

// spring
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

@EnableAutoConfiguration(exclude={HibernateJpaAutoConfiguration.class, ValidationAutoConfiguration.class})
// We need the special object mapper, though.
//@Import({ObjectMapperConfiguration.class, CLIRunner.ShutdownConfig.class})
@Import({ObjectMapperConfiguration.class})
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao", "com.casm.acled.crawler"})
public class CLIRunner implements CommandLineRunner {

    protected static final Logger logger = LoggerFactory.getLogger(CLIRunner.class);

    @Autowired
    private CrawlerSweep crawlerSweep;

    @Autowired
    private CrawlArgsService argsService;

    private CrawlArgs crawlArgs;

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(CLIRunner.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(args);

        ctx.close();
    }

    @Override
    public void run(String[] args) throws Exception {

        crawlArgs = argsService.get();

        JCommander.newBuilder()
                .addObject(crawlArgs.raw)
                .build()
                .parse(args);

        crawlArgs.init();

        crawlerSweep.sweep(ImmutableList.of(crawlArgs));
    }


}
