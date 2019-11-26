package com.casm.acled.crawler.utils;

// google imports
import com.google.common.io.Files;

// json imports
import org.json.JSONObject;

// java imports
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

import com.casm.acled.crawler.IncorrectScraperJSONException;

public class Utils {

    public static String KEYWORDS = ".+(?:kill|massacre|death|died|dead|bomb|bombed|bombing|rebel|attack|attacked|riot|battle|protest|clash|demonstration|strike|wound|injure|casualty|displace|unrest|casualties|vigilante|torture|march|rape).+";

    public static String getDomain(String urlString) throws MalformedURLException {
        URL url = new URL(urlString);
        return (url.getHost().startsWith("www")) ? url.getHost().substring(4) : url.getHost();
    }

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

    public static String processJSON(File scraperLocation) throws IOException, IncorrectScraperJSONException {
        String json = Files.toString(scraperLocation, Charset.defaultCharset());
        return (scraperLocation.getName().equals("last_scrape.json")) ? processScraperJSON(json) : processJobJSON(json);
    }

}



