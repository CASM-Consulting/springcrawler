package com.casm.acled.crawler.scraper;

import com.casm.acled.AppSpringOnly;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.springrunners.SpringOnlyRunner;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.source.Source;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.file.Paths;

import static org.junit.Assert.*;
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { SpringOnlyRunner.class })
public class ScraperServiceTest {

    @Autowired
    private ScraperService scraperService;

    @Autowired
    private Reporter reporter;

    @Test
    public void testScraper() {
        Source source = EntityVersions.get(Source.class).current()
                .put(Source.EXAMPLE_URLS, ImmutableList.of("http://www.0.com:5000", "http://www.1.com:5000"))
                .id(0);

        ACLEDScraper scraper = ACLEDScraper.load(Paths.get("testscrapers/generic"), source, reporter);

        scraperService.testScraper(scraper, source);

    }
}