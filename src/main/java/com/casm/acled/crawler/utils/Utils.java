package com.casm.acled.crawler.utils;

import java.net.MalformedURLException;
import java.net.URL;

public class Utils {


    public static String KEYWORDS = ".+(?:kill|massacre|death|died|dead|bomb|bombed|bombing|rebel|attack|attacked|riot|battle|protest|clash|demonstration|strike|wound|injure|casualty|displace|unrest|casualties|vigilante|torture|march|rape).+";


    public static String getDomain(String urlString) throws MalformedURLException {
        URL url = new URL(urlString);
        return (url.getHost().startsWith("www")) ? url.getHost().substring(4) : url.getHost();
    }
}
