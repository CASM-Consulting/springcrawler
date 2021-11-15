package com.casm.acled.dao.util;

import com.casm.acled.configuration.ObjectMapperConfiguration;
//import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
//import org.camunda.bpm.spring.boot.starter.rest.CamundaBpmRestJerseyAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import java.nio.file.Paths;

// We have to exclude these classes, because they only work in a web context.
//@EnableAutoConfiguration(exclude={CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class})
// We need the special object mapper, though.
@org.springframework.context.annotation.Import(ObjectMapperConfiguration.class)
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao"})
public class HistoricalImportRunner implements CommandLineRunner {

    @Autowired
    private Import importService;


    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(HistoricalImportRunner.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }

    public void run(String... args) throws Exception {
        importService.importHistoricalEvents(Paths.get(args[0]));
    }
}