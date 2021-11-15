package com.casm.acled.crawler.springrunners;

import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.reporting.Event;
import com.casm.acled.entities.sourcelist.SourceList;
//import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
//import org.camunda.bpm.spring.boot.starter.rest.CamundaBpmRestJerseyAutoConfiguration;
//import org.camunda.bpm.spring.boot.starter.webapp.CamundaBpmWebappAutoConfiguration;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

//@EnableAutoConfiguration(
//        exclude = {CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class, CamundaBpmWebappAutoConfiguration.class}
//)
@Import({ObjectMapperConfiguration.class})
@ComponentScan(
        basePackages = {"com.casm.acled.dao", "com.casm.acled.crawler"}
)
public class SpringOnlyRunner {

    public static Object none = new Object();

    public SpringOnlyRunner() {
    }

    public void run(String... args) {



    }

    public static void main(String... args) {
        System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(SpringOnlyRunner.class, args);
    }
}
