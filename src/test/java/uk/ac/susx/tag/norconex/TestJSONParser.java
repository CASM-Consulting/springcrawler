package uk.ac.susx.tag.norconex;

import com.casm.acled.crawler.IncorrectScraperJSONException;
import com.casm.acled.crawler.utils.SpringUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;


public class TestJSONParser {


    @Test
    public void testJsonParser() {

        String f = "/Users/jp242/Documents/Projects/ACLED/ManualScrapers/rinascrapers/azatliqorg/job.json";
        try {
            String json = FileUtils.readFileToString(new File(f), Charset.defaultCharset());
            String processed = SpringUtils.processJSON(new File(f));
            System.out.println(processed);
//            String parsed = "[{\"field\":\"root/root\",\"tags\":[{\"custom\":\"#content\"}]},{\"field\":\"field.name/title\",\"tags\":[{\"tag\":\"h1\",\"class\":\"pg-title\"}]},{\"field\":\"field.name/date\",\"tags\":[{\"tag\":\"div\",\"class\":\"published\"}]},{\"field\":\"field.name/article\",\"tags\":[{\"tag\":\"div\",\"class\":\"wsw\"}]}]\n";
//            Assert.assertEquals(processed,parsed);
        } catch (IOException e) {
            Assert.fail();
        } catch (IncorrectScraperJSONException e) {
            e.printStackTrace();
        }


    }
}
