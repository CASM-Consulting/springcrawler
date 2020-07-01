package com.casm.acled.crawler.springrunners;

import org.junit.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;

import java.time.LocalDate;

import static org.junit.Assert.*;

public class CrawlerSweepRunnerTest {

    @Test
    public void singleSource() {

        String sourceListId = "balkans";
//        String sourceId = "2158";
        //http://www.0.com:5000
        String sourceId = "RFE/RL";

        String[] args  = new String[]{sourceListId, sourceId };

        SpringApplication app = new SpringApplication(CrawlerSweepRunner.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);


    }
}