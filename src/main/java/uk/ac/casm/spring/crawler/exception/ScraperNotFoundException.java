package uk.ac.casm.spring.crawler.exception;

public class ScraperNotFoundException extends Exception {


    public ScraperNotFoundException(String domain) {
        super("No scraper found for the domain: " + domain);
    }
}
