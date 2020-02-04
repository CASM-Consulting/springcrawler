package uk.ac.susx.tag.norconex;

import com.casm.acled.crawler.spring.SpringCrawler;
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
        String seed = "https://www.presstv.com/Detail/2020/02/01/617641/UK-Scotland-Nicola-Sturgeon-New-Speech-";
        CrawlerArguments ca = new CrawlerArguments();
        ca.seeds = Arrays.asList(seed);
        ca.crawldb= "/Users/jp242/Desktop";
        ca.depth=1;
        ca.ignoreRobots = false;
        ca.ignoreSitemap = false;
        ca.polite = 300;
        ca.scrapers = "/Users/jp242/Documents/Projects/ACLED/ManualScrapers/demo-scrapers-2";
        ca.scraper = "presstvcom";
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
        params.add(String.valueOf(ca.depth));
        params.add(SingleSeedCollector.ROBOTS);
        params.add("false");
        params.add(SingleSeedCollector.SITEMAP);
        params.add("false");
        params.add(SingleSeedCollector.POLITENESS);
        params.add("500");
        params.add("casm.jqm.scraping.scrapers.dir");
        params.add(ca.scrapers);
        params.add(SingleSeedCollector.THREADS);
        params.add("2");
        params.add(SingleSeedCollector.FILTER);
        params.add(".*");
        params.add(SingleSeedCollector.USERAGENT);
        params.add("taglab");
        params.add(SingleSeedCollector.ID);
        params.add("springtest");
        params.add(CrawlerArguments.SCRAPER);
        params.add(ca.scraper);

        String[] args = params.toArray(new String[params.size()]);

        SpringApplication app = new SpringApplication(SpringCrawler.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);

    }

}
