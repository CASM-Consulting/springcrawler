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
    DATE_PARSE_FAILED,
    DATE_ALL_PARSE_SUCCESS,
    DATE_NOT_FOUND,
    NO_EXAMPLES,
    NO_SITE_MAPS,
    HAS_SITE_MAPS,
    SCRAPER_NOT_FOUND,
    SCRAPER_FOUND,
    MISSING_URL,
    SCRAPE_NO_ARTICLE,
    SCRAPE_NO_DATE,
    SCRAPE_NO_TITLE,
    SCRAPE_NO_RESULT,
    SCRAPE_PASS,
    SCRAPE_FAIL,
    SCRAPE_TIMEOUT,
    SCRAPE_ERROR,
    QUERY_MATCH,
    QUERY_NO_MATCH,
    SOURCE_NOT_FOUND,
    SOURCE_FOUND,
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
    JOB_CANCELLED,
    JOB_STILL_ATTRIBUTED,
    JOB_STILL_RUNNING,
    JOB_CRASHED
    ;
}
