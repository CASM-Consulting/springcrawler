package com.casm.acled.crawler.management;

import com.casm.acled.crawler.ACLEDImporter;
import com.casm.acled.crawler.ACLEDMetadataPreProcessor;
import com.casm.acled.crawler.scraper.ACLEDScraper;
import com.casm.acled.crawler.scraper.dates.CompositeDateParser;
import com.casm.acled.crawler.scraper.dates.DateParser;
import com.casm.acled.crawler.utils.Util;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.norconex.collector.http.HttpCollector;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

    private final LocalDate from;
    private final LocalDate to;

    private final NorconexConfiguration config;

    private HttpCollector collector;

    private Supplier<HttpCollector> collectorSupplier = ()->collector;

    public Crawl(SourceList sourceList, Source source, LocalDate from, LocalDate to, boolean skipKeywords, ACLEDImporter importer) {
        this.source = source;
        this.from = from;
        this.to = to;

        String id = id();
        Path cachePath = Paths.get(id);

        importer.setCollectorSupplier(collectorSupplier);
        importer.setMaxArticles(10);

        config = new NorconexConfiguration(CACHE_DIR.resolve(cachePath));

        List<String> query = resolveQuery(sourceList, source);

        if(from != null && to != null && !skipKeywords) {

            ZoneId zoneId = ZoneId.of(source.get(Source.TIMEZONE));

            List<String> dateFormatSpecs = source.get(Source.DATE_FORMAT);

            DateParser dateParser = CompositeDateParser.of(dateFormatSpecs);

            config.setFilters(
                    ZonedDateTime.of(from.atTime(0,0,0), zoneId),
                    ZonedDateTime.of(to.atTime(0,0,0), zoneId),
                    dateParser,
                    query
            );
        } else if (from != null && to != null) {

            ZoneId zoneId = ZoneId.of(source.get(Source.TIMEZONE));

            List<String> dateFormatSpecs = source.get(Source.DATE_FORMAT);

            DateParser dateParser = CompositeDateParser.of(dateFormatSpecs);

            config.setFilters(
                    ZonedDateTime.of(from.atTime(0,0,0), zoneId),
                    ZonedDateTime.of(to.atTime(0,0,0), zoneId),
                    dateParser
            );
        } else if (!skipKeywords) {
            config.setFilters(
                    query
            );
        }

        String startURL = source.get(Source.LINK);

        String scraperName = Util.getID(startURL);

        ACLEDScraper scraper = ACLEDScraper.load(ALL_SCRAPERS.resolve(scraperName));
        ACLEDMetadataPreProcessor metadata = new ACLEDMetadataPreProcessor(startURL);

        config.setScraper(scraper, metadata);
        config.crawler().setStartURLs(startURL);
//        config.collector();
        config.setId(id);
        config.crawler().setPostImportProcessors(importer);
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

    private List<String> resolveQuery(SourceList sourceList, Source list) {
        return Util.KEYWORDS_LUCENE;
    }

    public void run() {
        collector = new HttpCollector(config.collector());
        collector.start(true);
    }
}
