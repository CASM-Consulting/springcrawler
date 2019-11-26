package com.casm.acled.crawler;

public class IncorrectScraperJSONException extends Exception {

    public IncorrectScraperJSONException() {
        super("Poorly formed json for scraper found");
    }

}
