package com.casm.acled.crawler.management;

import com.casm.acled.crawler.ACLEDImporter;
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

public class Crawl {

    private static final Path ALL_SCRAPERS = Paths.get("allscrapers");
    private static final Path CACHE_DIR = Paths.get("scraper_cache");

    private final Source source;

    public static final String SOURCE_ID = "SOURCE_ID";
    public static final String SOURCE_LIST_ID = "SOURCE_LIST_ID";
    public static final String FROM = "FROM";
    public static final String TO = "TO";

    private final LocalDate from;
    private final LocalDate to;

    private final NorconexConfiguration config;

    public Crawl(SourceList sourceList, Source source, LocalDate from, LocalDate to, ACLEDImporter importer) {
        this.source = source;
        this.from = from;
        this.to = to;

        ZoneId zoneId = ZoneId.of(source.get(Source.TIMEZONE));

        List<String> query = resolveQuery(sourceList, source);

        String id = id();

        Path cachePath = Paths.get(id);

        config = new NorconexConfiguration(CACHE_DIR.resolve(cachePath));

        List<String> dateFormatSpecs = source.get(Source.DATE_FORMAT);

        DateParser dateParser = CompositeDateParser.of(dateFormatSpecs);

        config.setFilters(
                ZonedDateTime.of(from.atTime(0,0,0), zoneId),
                ZonedDateTime.of(to.atTime(0,0,0), zoneId),
                dateParser,
                query
        );

        String startURL = source.get(Source.LINK);

        String scraperName = Util.getID(startURL);

        ACLEDScraper scraper = ACLEDScraper.load(ALL_SCRAPERS.resolve(scraperName));

        config.setScraper(scraper);
        config.crawler().setStartURLs(startURL);
        config.setId(id);
        config.crawler().setPostImportProcessors(importer);
    }

    public String id() {
        StringBuilder sb = new StringBuilder();
        sb.append((String)source.get(Source.NAME))
                .append("-")
                .append(from.toString())
                .append("-")
                .append(to.toString());
        return sb.toString();
    }

    private List<String> resolveQuery(SourceList sourceList, Source list) {
        return Util.KEYWORDS_LUCENE;
    }

    public void run() {
        HttpCollector collector = new HttpCollector(config.collector());
        collector.start(true);
    }
}
