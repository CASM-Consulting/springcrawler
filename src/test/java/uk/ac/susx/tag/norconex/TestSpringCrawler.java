package uk.ac.susx.tag.norconex;

import com.casm.acled.crawler.springcli.CLIRunner;
import org.junit.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import uk.ac.susx.tag.norconex.jobqueuemanager.CrawlerArguments;
import uk.ac.susx.tag.norconex.jobqueuemanager.SingleSeedCollector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestSpringCrawler {



    @Test
    public void TestSpringCrawler() {
//        CLIRunner sc = new CLIRunner();
        String seed = "http://www.0.com:5000";
//        String seed = "http://english.pnn.ps/";
//        String seed = "https://english.pnn.ps/";
//        String seed = "http://www.israelnationalnews.com/";
//        String seed = "https://www.jpost.com/";
//        String seed = "https://www.timesofisrael.com/";
//        String seed = "http://en.annahar.com/section/184-lebanon";

//        String seed = "http://nna-leb.gov.lb/en";
//        String seed = "https://reliefweb.int/";
        CrawlerArguments ca = new CrawlerArguments();
        ca.seeds = Arrays.asList(seed);
        ca.crawldb= "/home/sw206/git/springcrawler/testcrawldb";
        ca.depth=5;
        ca.ignoreRobots = false;
        ca.ignoreSitemap = true;
        ca.polite = 100;
//        ca.scrapers = "/home/sw206/git/springcrawler/allscrapers";
        ca.scrapers = "/home/sw206/git/springcrawler/testscrapers";
        ca.scraper = "generic";
        ca.threadsPerSeed = 2;
        ca.urlFilter = ".*";
        ca.userAgent = "casmconsulting.co.uk";
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
//        params.add("");
//        params.add(SingleSeedCollector.INDEXONLY);

        String[] args = params.toArray(new String[params.size()]);

        SpringApplication app = new SpringApplication(CLIRunner.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);

    }

}
