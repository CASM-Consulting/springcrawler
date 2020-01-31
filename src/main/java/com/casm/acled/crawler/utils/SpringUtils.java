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

public class SpringUtils {

    // keyword query specific to potential articles of interest to ACLED
    public static String KEYWORDS = ".+(?:kill|massacre|death|died|dead|bomb|bombed|bombing|rebel|attack|attacked|riot|battle|protest|clash|demonstration|strike|wound|injure|casualty|displace|unrest|casualties|vigilante|torture|march|rape).+";

    // Returns the originating domain of a given url - minus any trailing 'www'
    public static String getDomain(String urlString) throws MalformedURLException {
        URL url = new URL(urlString);
        return (url.getHost().startsWith("www")) ? url.getHost().substring(4) : url.getHost();
    }


    // NEED TO RESOLVE THESE BETTER

}



