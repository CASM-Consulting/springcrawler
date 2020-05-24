package com.casm.acled.crawler;

import com.casm.acled.crawler.management.NorconexConfiguration;
import com.casm.acled.crawler.scraper.*;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.scraper.dates.CompositeDateParser;
import com.casm.acled.crawler.scraper.dates.ExcludingCustomDateMetadataFilter;
import com.casm.acled.crawler.scraper.dates.DateParser;
import com.casm.acled.crawler.scraper.keywords.ExcludingKeywordFilter;
import com.casm.acled.crawler.util.Util;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.ibm.icu.util.ULocale;
import com.norconex.collector.http.HttpCollector;
import com.norconex.importer.handler.filter.AbstractDocumentFilter;
import com.norconex.importer.handler.filter.OnMatch;
import com.norconex.importer.handler.filter.impl.DateMetadataFilter;
import com.norconex.importer.handler.filter.impl.EmptyMetadataFilter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Crawl {

    private static final Path ALL_SCRAPERS = Paths.get("/home/sw206/git/acled-scrapers");
    private static final Path CACHE_DIR = Paths.get("/home/sw206/git/springcrawler/balkans_scrapers");

    private final Source source;

    public static final String SOURCE_ID = "SOURCE_ID";
    public static final String SOURCE_LIST_ID = "SOURCE_LIST_ID";
    public static final String SKIP_KEYWORD_FILTER = "SKIP_KEYWORD_FILTER";
    public static final String FROM = "FROM";
    public static final String TO = "TO";
    public static final String ARTICLE_LIMIT = "ARTICLE_LIMIT";
    public static final String DEPTH_LIMIT = "DEPTH_LIMIT";

    private final LocalDate from;
    private final LocalDate to;

    private final NorconexConfiguration config;

    private HttpCollector collector;

    private Supplier<HttpCollector> collectorSupplier = () -> collector;

    private final Reporter reporter;

    public Crawl(SourceList sourceList, Source source, LocalDate from, LocalDate to, boolean skipKeywords,
                 ACLEDImporter importer, Reporter reporter) {
        this.source = source;
        this.from = from;
        this.to = to;
        this.reporter = reporter;

        String id = id();
        Path cachePath = Paths.get(id);

        importer.setCollectorSupplier(collectorSupplier);

        //LOOK AT THIS !!!
//        importer.setMaxArticles(10);

        config = new NorconexConfiguration(CACHE_DIR.resolve(cachePath));

        List<AbstractDocumentFilter> filters = new ArrayList<>();
        filters.add(new AcceptFilter());

        EmptyMetadataFilter emptyArticle = new EmptyMetadataFilter(OnMatch.EXCLUDE,
                ScraperFields.SCRAPED_ARTICLE,
                ScraperFields.SCRAPED_DATE);

        filters.add(emptyArticle);

        if(from != null && to != null ) {

            ZoneId zoneId = ZoneId.of(source.get(Source.TIMEZONE));

            DateMetadataFilter dateFilter = dateFilter(source,
                    ZonedDateTime.of(from.atTime(0,0,0), zoneId),
                    ZonedDateTime.of(to.atTime(0,0,0), zoneId)
            );

            filters.add(dateFilter);
        }

        if(!skipKeywords) {

            ExcludingKeywordFilter keywordFilter = keywordFilter(sourceList, source);
            filters.add(keywordFilter);
        }

        filters.forEach(config::addFilter);

        String[] startURL =((String) source.get(Source.LINK)).split(",");

        String scraperName = Util.getID(startURL[0]);

        ACLEDScraper scraper = ACLEDScraper.load(ALL_SCRAPERS.resolve(scraperName), source, reporter);
        ACLEDMetadataPreProcessor metadata = new ACLEDMetadataPreProcessor(startURL[0]);

        applySourceIdiosyncrasies(source, config);

        config.setScraper(scraper, metadata);
        config.crawler().setStartURLs(startURL);
//        config.collector();
        config.setId(id);
        config.crawler().setPostImportProcessors(importer);
    }

    public NorconexConfiguration getConfig() {
        return config;
    }

    private ExcludingKeywordFilter keywordFilter(SourceList sourceList, Source source) {

        String query = resolveQuery(sourceList, source);

        ExcludingKeywordFilter keywordFilter = new ExcludingKeywordFilter(ScraperFields.SCRAPED_ARTICLE, query);

        return keywordFilter;
    }

    private DateMetadataFilter dateFilter(Source source, ZonedDateTime from, ZonedDateTime to) {

        List<String> dateFormatSpecs = source.get(Source.DATE_FORMAT);

        DateParser dateParser = CompositeDateParser.of(dateFormatSpecs);

        DateMetadataFilter dateMetadataFilter = new ExcludingCustomDateMetadataFilter(source, ScraperFields.SCRAPED_DATE, dateParser, reporter);

        dateMetadataFilter.addCondition(DateMetadataFilter.Operator.GREATER_EQUAL, Date.from(from.toInstant()));
        dateMetadataFilter.addCondition(DateMetadataFilter.Operator.LOWER_EQUAL, Date.from(to.toInstant()));

        return dateMetadataFilter;
    }

//    private ULocale getLocale(Source source) {
//        ULocale locale = new ULocale(source.get(Source.LOCALE));
//        return locale;
//    }

    private void applySourceIdiosyncrasies(Source source, NorconexConfiguration config){
//        if(source.get(Source.))
    }

    public String id() {
        StringBuilder sb = new StringBuilder();
        String standardName = source.get(Source.STANDARD_NAME);
        standardName = standardName.toLowerCase().replaceAll(" ", "-");
        sb.append(standardName)
                .append("-")
                .append(from == null ? "" : from.toString())
                .append("-")
                .append(to == null ? "" : to.toString());
        return sb.toString();
    }

    private String resolveQuery(SourceList sourceList, Source list) {
        
        String query = sourceList.get(SourceList.KEYWORDS);

        return query;
    }

    public void run() {
        config.finalise();
        collector = new HttpCollector(config.collector());
        collector.start(true);
    }
}
