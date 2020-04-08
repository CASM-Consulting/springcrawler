package uk.ac.susx.tag.norconex;

import com.casm.acled.crawler.scraper.ACLEDScraper;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TestScraperInit {

    @Test
    public void TestScraperInit() {
        String path = "/Users/jp242/Documents/Projects/ACLED/ManualScrapers/demo-scrapers";
        Path loc = Paths.get(path);
        ACLEDScraper acp = new ACLEDScraper(loc);
//        for(Map.Entry scraper : ACLEDScraperPreProcessor.scraperJson.entrySet()){
//
//            System.out.println((GeneralSplitterFactory) scraper.getValue().create());
//        }
    }

}
