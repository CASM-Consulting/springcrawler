package com.casm.acled.crawler.reporting;

public enum Event {
    ERROR,
    LOCALE_NOT_FOUND,
    TIMEZONE_NOT_FOUND,
    COUNTRY_NOT_FOUND,
    LANGUAGE_NOT_FOUND,
    DATE_PARSER_NOT_FOUND,
    DATE_PARSE_SUCCESS,
    DATE_PARSE_INCORRECT,
    // When a date text was found, but was not able to be parsed into ScraperFields.STANDARD_DATE
    DATE_PARSE_FAILED,
    DATE_ALL_PARSE_SUCCESS,
    DATE_NOT_FOUND,
    // A successfully parsed date falls within the required period
    DATE_MATCH,
    // A successfully parsed date does not fall within the required period
    DATE_NO_MATCH,
    NO_EXAMPLES,
    NO_SITE_MAPS,
    HAS_SITE_MAPS,
    SCRAPER_NOT_FOUND,
    SCRAPER_FOUND,
    MISSING_URL,
    // A reference reaches the stage of being considered for addition by the Committer.
    REFERENCE_ACCEPTED,
    // When ScraperFields.SCRAPED_ARTICLE is empty
    SCRAPE_NO_ARTICLE,
    // When ScraperFields.SCRAPED_DATE is empty
    SCRAPE_NO_DATE,
    // when ScraperFields.SCRAPED_TITLE is empty
    SCRAPE_NO_TITLE,
    SCRAPE_NO_RESULT,
    // When an article with text and correctly parsed date falls within the correct from/to parameters
    SCRAPE_PASS,
    // When article does not meet criteria for SCRAPE_PASS
    SCRAPE_FAIL,
    SCRAPE_TIMEOUT,
    SCRAPE_ERROR,
    // A successfully scraped article text contains the necessary keywords to be considered a match
    QUERY_MATCH,
    // A successfully scraped article text does not contain the necessary keywords to be considered a match
    QUERY_NO_MATCH,
    SOURCE_NOT_FOUND,
    SOURCE_FOUND,
    SOURCE_LINK_REDIRECT,
    SOURCE_LINK_INVALID,
    SCRAPE_TEST_TP,
    SCRAPE_TEST_FP,
    SCRAPE_TEST_TN,
    SCRAPE_TEST_FN,
    ARTICLE_NO_MATCH,
    ARTICLE_URL_NO_MATCH,
    ARTICLE_URL_MATCH,
    ARTICLE_URL_TOO_MANY_MATCHES,
    ARTICLE_CONTENT_NO_MATCH,
    ARTICLE_CONTENT_MATCH,
    ARTICLE_CONTENT_TOO_MANY_MATCHES,
    // new class used for scheduler
    JOB_STARTED,
    JOB_CANCELLED,
    JOB_STILL_STARTING,
    JOB_STILL_RUNNING,
    JOB_CRASHED
    ;
}
