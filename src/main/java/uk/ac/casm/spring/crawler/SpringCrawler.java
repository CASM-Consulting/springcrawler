package uk.ac.casm.spring.crawler;

import com.beust.jcommander.JCommander;
import com.norconex.collector.http.crawler.HttpCrawlerConfig;
import uk.ac.susx.tag.norconex.jobqueuemanager.CrawlerArguments;
import uk.ac.susx.tag.norconex.jobqueuemanager.SingleSeedCollector;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;

public class SpringCrawler {

    //Include JCommander interface for parsing the inputs!!!
    public static void main(String[] args) {

        CrawlerArguments ca = new CrawlerArguments();
        new JCommander().newBuilder()
                .addObject(ca)
                .build()
                .parse(args);

        SingleSeedCollector cc = new SingleSeedCollector(ca.userAgent,new File(ca.crawldb), ca.id,
                ca.depth, ca.urlFilter,ca.threadsPerSeed,ca.ignoreRobots,
                ca.ignoreSitemap, ca.polite,
                ca.seeds.get(0));

        HttpCrawlerConfig config = cc.getConfiguration();
        config.setPostImportProcessors(new ACLEDPostProcessor());
        config.setPreImportProcessors(new ACLEDScraperPreProcessor(Paths.get(ca.scrapers)));
        cc.setConfiguration(config);

        try {
            cc.start();
        } catch (URISyntaxException e) {
            throw new RuntimeException("The provided URL was invalid");
        }

    }

}
