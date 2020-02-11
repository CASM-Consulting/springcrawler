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
        String seed = "http://www.newagebd.net/article/98651/articlelist/323/articlelist/323/Cartoon";
        CrawlerArguments ca = new CrawlerArguments();
        ca.seeds = Arrays.asList(seed);
        ca.crawldb= "/Users/jp242/Desktop";
        ca.depth=0;
        ca.ignoreRobots = false;
        ca.ignoreSitemap = false;
        ca.polite = 400;
        ca.scrapers = "/Users/jp242/Documents/Projects/ACLED/ManualScrapers/demoscrapers";
        ca.scraper = "newagebdnet";
        ca.threadsPerSeed = 2;
        ca.urlFilter = ".*";
        ca.userAgent = "taglab";
        ca.id = "testspring";
        ca.index = true;

        List<String> params = new ArrayList<>();
        params.add(SingleSeedCollector.SEED);
        params.add(seed);
        params.add(SingleSeedCollector.CRAWLB);
        params.add(ca.crawldb);
        params.add(SingleSeedCollector.DEPTH);
        params.add(String.valueOf(ca.depth));
        params.add(SingleSeedCollector.ROBOTS);
        params.add(String.valueOf(ca.ignoreRobots));
        params.add(SingleSeedCollector.SITEMAP);
        params.add(String.valueOf(ca.ignoreSitemap));
        params.add(SingleSeedCollector.POLITENESS);
        params.add(String.valueOf(ca.polite));
        params.add("casm.jqm.scraping.scrapers.dir");
        params.add(ca.scrapers);
        params.add(SingleSeedCollector.THREADS);
        params.add(String.valueOf(ca.threadsPerSeed));
        params.add(SingleSeedCollector.FILTER);
        params.add(".*");
        params.add(SingleSeedCollector.USERAGENT);
        params.add("taglab");
        params.add(SingleSeedCollector.ID);
        params.add("springtest");
        params.add(CrawlerArguments.SCRAPER);
        params.add(ca.scraper);
//        params.add(SingleSeedCollector.INDEXONLY);

        String[] args = params.toArray(new String[params.size()]);

        SpringApplication app = new SpringApplication(SpringCrawler.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);

    }

}
