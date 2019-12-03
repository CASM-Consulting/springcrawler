package uk.ac.susx.tag.norconex;

import com.casm.acled.crawler.ACLEDScraperPreProcessor;
import org.apache.nutch.parse.forum.splitter.GeneralSplitterFactory;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class TestScraperInit {

    @Test
    public void TestScraperInit() {
        String path = "/Users/jp242/Documents/Projects/ACLED/ManualScrapers/demo-scrapers";
        Path loc = Paths.get(path);
        ACLEDScraperPreProcessor acp = new ACLEDScraperPreProcessor(loc);
//        for(Map.Entry scraper : ACLEDScraperPreProcessor.scraperJson.entrySet()){
//
//            System.out.println((GeneralSplitterFactory) scraper.getValue().create());
//        }
    }

}
