package com.casm.acled.crawler.springrunners;

import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.scraper.locale.LocaleService;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.source.Source;
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


@EnableAutoConfiguration(exclude={HibernateJpaAutoConfiguration.class,CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class, ValidationAutoConfiguration.class})
// We need the special object mapper, though.
//@Import({ObjectMapperConfiguration.class, CLIRunner.ShutdownConfig.class})
@Import({ObjectMapperConfiguration.class})
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao", "com.casm.acled.crawler"})
public class LocaleServiceRunner implements CommandLineRunner {

    protected static final Logger logger = LoggerFactory.getLogger(LocaleServiceRunner.class);

    @Autowired
    private LocaleService localeService;

    @Autowired
    private SourceListDAO sourceListDAO;

    @Autowired
    private SourceDAO sourceDAO;

    @Autowired
    private Reporter reporter;


    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(LocaleServiceRunner.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(args);
        logger.info("Spring Boot application started");
        ctx.close();
    }



    @Override
    public void run(String[] args) throws Exception {
        reporter.randomRunId();

//        localeHelper.allS                return Optional.empty();
//        localeHelper.determineSourceLocalesAndListTimeZones("balkans");

//        Source source = sourceDAO.getById(1262).get();
//        Source source = sourceDAO.getById(2977).get();
//        Source source = sourceDAO.getById(1659).get();
//        Source source = sourceDAO.getById(17749).get();
//        Source source = sourceDAO.getById(38).get();
//        Source source = sourceDAO.getById(3795).get();
//        Source source = sourceDAO.getById(1658).get();
//        Source source = sourceDAO.getById(3591).get();
//        Source source = sourceDAO.getById(4421).get();
//        Source source = sourceDAO.getById(16778).get();
//        Source source = sourceDAO.getById(2122).get();
//        Source source = sourceDAO.getById(3264).get();
//        Source source = sourceDAO.getById(4345).get();
//        Source source = sourceDAO.getById(1891).get();
//        Source source = sourceDAO.getById(2139).get();
//        Source source = sourceDAO.getById(4642).get();
//        Source source = sourceDAO.getById(3596).get();
//        Source source = sourceDAO.getById(17335).get();
//        Source source = sourceDAO.getById(1265).get();
//        Source source = sourceDAO.getById(1263).get();
        Source source = sourceDAO.getById(2254).get();

//        System.out.println(localeService.determineLocale(source));
//        System.out.println(localeService.determineTimeZone(source));

        localeService.autoAssignLocalesAndTimeZones(source);

        reporter.getRunReports().stream().forEach(r -> logger.info(r.toString()));
    }
}
