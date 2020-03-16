package com.casm.acled.crawler.springcli;

import com.beust.jcommander.JCommander;

// acled
import com.casm.acled.configuration.ObjectMapperConfiguration;

// norconex

//camunda
import com.casm.acled.crawler.spring.CrawlerService;
import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
import org.camunda.bpm.spring.boot.starter.rest.CamundaBpmRestJerseyAutoConfiguration;

// logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// java
import java.util.*;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import uk.ac.susx.tag.norconex.database.ConcurrentContentHashStore;
import uk.ac.susx.tag.norconex.jobqueuemanager.CrawlerArguments;

import javax.annotation.PreDestroy;

@EnableAutoConfiguration(exclude={HibernateJpaAutoConfiguration.class,CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class, ValidationAutoConfiguration.class})
// We need the special object mapper, though.
//@Import({ObjectMapperConfiguration.class, CLIRunner.ShutdownConfig.class})
@Import({ObjectMapperConfiguration.class})
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao", "com.casm.acled.crawler.spring"})
public class CLIRunner implements CommandLineRunner {

    protected static final Logger logger = LoggerFactory.getLogger(CLIRunner.class);

    @Autowired
    private CrawlerService crawlerService;

    private ConcurrentContentHashStore contentHashStore;


    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(CLIRunner.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(args);
        logger.info("Spring Boot application started");

        // Close when complete
//        ctx.getBean(TerminateBean.class);
        ctx.close();
    }

//    public class TerminateBean {
//
//        @PreDestroy
//        public void onDestroy() throws Exception {
//            contentHashStore.close();
//            logger.info("Spring Container is destroyed!");
//        }
//    }
//
//    @Configuration
//    public class ShutdownConfig {
//
//        @Bean
//        public TerminateBean getTerminateBean() {
//            return new TerminateBean();
//        }
//    }

    @Override
    public void run(String[] args)  {

        List<String> splitArgs = new ArrayList<>();
        for(String arg : args){
            splitArgs.addAll(Arrays.asList(arg.split("\\s+")));
        }

        String[] corrArgs = splitArgs.toArray(new String[splitArgs.size()]);

        CrawlerArguments crawlerArguments = new CrawlerArguments();
        JCommander.newBuilder()
                .addObject(crawlerArguments)
                .build()
                .parse(corrArgs);
        crawlerService.run(crawlerArguments);
    }
}
