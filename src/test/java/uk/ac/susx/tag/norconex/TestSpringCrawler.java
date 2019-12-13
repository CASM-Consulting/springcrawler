package uk.ac.susx.tag.norconex;

import com.casm.acled.crawler.spring.SpringCrawler;
import com.google.common.collect.Lists;
import org.apache.commons.math3.analysis.function.Sin;
import org.hibernate.tool.hbm2ddl.SingleLineSqlCommandExtractor;
import org.junit.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import uk.ac.susx.tag.norconex.jobqueuemanager.CrawlerArguments;
import uk.ac.susx.tag.norconex.jobqueuemanager.SingleSeedCollector;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestSpringCrawler {


    @Test
    public void TestSpringCrawler() {
        SpringCrawler  sc = new SpringCrawler();
        String seed = "https://www.gulftoday.ae";
        int depth = 3;
        CrawlerArguments ca = new CrawlerArguments();
        ca.seeds = Arrays.asList(seed);
        ca.crawldb= "/Users/jp242/Desktop";
        ca.depth= 0;
        ca.ignoreRobots = true;
        ca.ignoreSitemap = true;
        ca.polite = 300;
        ca.scrapers = "/Users/jp242/Documents/Projects/ACLED/ManualScrapers/demo-scrapers";
        ca.threadsPerSeed = 2;
        ca.urlFilter = ".*";
        ca.userAgent = "taglab";
        ca.id = "testspring";
        List<String> params = new ArrayList<>();
        params.add(SingleSeedCollector.SEED);
        params.add(seed);
        params.add(SingleSeedCollector.CRAWLB);
        params.add(ca.crawldb);
        params.add(SingleSeedCollector.DEPTH);
        params.add("0");
        params.add(SingleSeedCollector.ROBOTS);
        params.add("true");
        params.add(SingleSeedCollector.SITEMAP);
        params.add("true");
        params.add(SingleSeedCollector.POLITENESS);
        params.add("300");
        params.add("casm.jqm.scraping.scrapers");
        params.add(ca.scrapers);
        params.add(SingleSeedCollector.THREADS);
        params.add("2");
        params.add(SingleSeedCollector.FILTER);
        params.add(".*");
        params.add(SingleSeedCollector.USERAGENT);
        params.add("taglab");
        params.add(SingleSeedCollector.ID);
        params.add("springtest");

        String[] args = params.toArray(new String[params.size()]);

        SpringApplication app = new SpringApplication(SpringCrawler.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);

    }

}
