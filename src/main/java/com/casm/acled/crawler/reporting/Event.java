package com.casm.acled.crawler.reporting;

public enum Event {
    ERROR,
    LOCALE_NOT_FOUND,
    TIMEZONE_NOT_FOUND,
    COUNTRY_NOT_FOUND,
    LANGUAGE_NOT_FOUND,
    DATE_PARSE_FAILED,
    DATE_PARSE_SUCCESS,
    DATE_ALL_PARSE_SUCCESS,
    DATE_NOT_FOUND,
    NO_EXAMPLES,
    SCRAPER_NOT_FOUND,
    SCRAPER_FOUND,
    MISSING_URL,
    SCRAPE_NO_ARTICLE,
    SCRAPE_NO_DATE,
    SCRAPE_NO_TITLE,
    SCRAPE_NO_RESULT,
    QUERY_MATCH,
    QUERY_NO_MATCH,
    ;
}
