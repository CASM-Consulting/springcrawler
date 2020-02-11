package com.casm.acled.crawler.utils;

// google imports
import com.google.common.io.Files;

// json imports
import com.mdimension.jchronic.Chronic;
import com.mdimension.jchronic.utils.Span;
import org.json.JSONObject;

// java imports
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

import com.casm.acled.crawler.IncorrectScraperJSONException;

public class Utils {

    // keyword query specific to potential articles of interest to ACLED
    public static String KEYWORDS = ".+(?:kill|killed|massacre|death|\\bdied\\b|\\bdead\\b|\\bbomb\\b|\\bbombed\\b|\\bbombing\\b|\\brebel\\b|\\battack\\b|\\battacked\\b|\\briot\\b|\\bbattle\\b|\\bprotest\\b|\\bclash\\b|\\bdemonstration\\b|\\bstrike\\b|\\bwound\\b|\\binjure\\b|\\bcasualty\\b|\\bdisplace\\b|\\bunrest\\b|\\bcasualties\\b|\\bvigilante\\b|\\btorture\\b|\\bmarch\\b|\\brape\\b).+";
//    public static String KEYWORDS2 = ".+(?:kill|massacre|\\bdeath\\b|died|dead|bomb|bombed|bombing|rebel|attack|attacked|riot|battle|protest|clash|demonstration|strike|wound|injure|casualty|displace|unrest|casualties|vigilante|torture|march|rape).+";

    // Returns the originating domain of a given url - minus any trailing 'www'
    public static String getDomain(String urlString) throws MalformedURLException {
        URL url = new URL(urlString);
        return (url.getHost().startsWith("www")) ? url.getHost().substring(4) : url.getHost();
    }


    // NEED TO RESOLVE THESE BETTER

    // Processes a M52 job json to scraper rules
    public static String processJobJSON(String json) throws IncorrectScraperJSONException {
        JSONObject jobj = new JSONObject(json);
        try {
            return jobj.getJSONArray("components").getJSONObject(0).getJSONObject("opts").getJSONArray("fields").toString();
        } catch (Exception e) {
            throw new IncorrectScraperJSONException();
        }
    }


    public static String processScraperJSON(String json){
        // BUG FOUND - NEED TO USE job.json!!
        return null;
    }

    // returns a web scraper based on a job spect of last_scrape file
    public static String processJSON(File scraperLocation) throws IOException, IncorrectScraperJSONException {
        String json = Files.toString(scraperLocation, Charset.defaultCharset());
        return (scraperLocation.getName().equals("last_scrape.json")) ? processScraperJSON(json) : processJobJSON(json);
    }

}



