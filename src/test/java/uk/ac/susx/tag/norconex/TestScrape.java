package uk.ac.susx.tag.norconex;

import com.casm.acled.crawler.ACLEDScraperPreProcessor;
import org.apache.nutch.parse.filter.Post;
import org.apache.nutch.parse.forum.splitter.GeneralSplitterFactory;
import org.apache.nutch.splitter.utils.POJOHTMLMatcherDefinition;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TestScrape {

    @Test
    public void TestScraped(){
        String url = "https://www.malaymail.com/news/malaysia/2019/11/12/why-blame-us-when-police-responsible-for-permit-ruling-for-walkabouts-ec-as/1809302";
        Path scraper = Paths.get("/Users/jp242/Documents/Projects/ACLED/extracted-scrapers/scrapers/malaymail.json");

        try {
            List<POJOHTMLMatcherDefinition> tagset = GeneralSplitterFactory.getTagSetFromJson(scraper);
            for(POJOHTMLMatcherDefinition matcher : tagset) {
                System.out.println(matcher.field);
                System.out.println(matcher.getTagDefinitions());
            }
            Map<String, List<Map<String, String>>> tags = ACLEDScraperPreProcessor.buildScraperDefinition(tagset);
            GeneralSplitterFactory splitterFact = new GeneralSplitterFactory(tags);
            Document doc = Jsoup.connect(url).userAgent("Mozilla").get();
            LinkedList<Post> splitDoc = splitterFact.create().split(doc);
            System.out.println(splitDoc.getFirst().get(ACLEDScraperPreProcessor.article));
            System.out.println(splitDoc.size());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
