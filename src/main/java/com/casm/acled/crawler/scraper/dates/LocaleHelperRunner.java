package com.casm.acled.crawler.scraper.dates;

import com.casm.acled.configuration.ObjectMapperConfiguration;
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

import java.util.*;


@EnableAutoConfiguration(exclude={HibernateJpaAutoConfiguration.class,CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class, ValidationAutoConfiguration.class})
// We need the special object mapper, though.
//@Import({ObjectMapperConfiguration.class, CLIRunner.ShutdownConfig.class})
@Import({ObjectMapperConfiguration.class})
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao", "com.casm.acled.crawler.spring"})
public class LocaleHelperRunner implements CommandLineRunner {

    protected static final Logger logger = LoggerFactory.getLogger(LocaleHelperRunner.class);

    @Autowired
    private SourceListDAO sourceListDAO;
    @Autowired
    private SourceDAO sourceDAO;

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(LocaleHelperRunner.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(args);
        logger.info("Spring Boot application started");
        ctx.close();
    }

    private void determineSourceLocalesAndListTimeZones(String listName) {

        SourceList sourceList = sourceListDAO.getBy(SourceList.LIST_NAME, listName).get(0);
        List<Source> sources = sourceDAO.byList(sourceList);
        LocaleHelper dph = new LocaleHelper();

        dph.determineTimeZones(sources);
    }

    @Override
    public void run(String[] args) throws Exception {



        determineSourceLocalesAndListTimeZones("Levant");
    }
}
