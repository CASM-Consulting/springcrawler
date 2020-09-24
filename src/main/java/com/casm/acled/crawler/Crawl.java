package com.casm.acled.crawler;

import com.casm.acled.crawler.management.CrawlArgs;
import com.casm.acled.crawler.management.NorconexConfiguration;
import com.casm.acled.crawler.scraper.*;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.scraper.dates.CompositeDateParser;
import com.casm.acled.crawler.scraper.dates.ExcludingCustomDateMetadataFilter;
import com.casm.acled.crawler.scraper.dates.DateParser;
import com.casm.acled.crawler.scraper.dates.SiteMapLastModifiedMetadataFilter;
import com.casm.acled.crawler.scraper.keywords.ExcludingKeywordFilter;
import com.casm.acled.crawler.util.CustomLoggerRepository;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.norconex.collector.core.filter.impl.RegexReferenceFilter;
import com.norconex.collector.http.HttpCollector;
import com.norconex.collector.http.url.IURLNormalizer;
import com.norconex.collector.http.url.impl.GenericURLNormalizer;
import com.norconex.importer.handler.filter.AbstractDocumentFilter;
import com.norconex.importer.handler.filter.OnMatch;
import com.norconex.importer.handler.filter.impl.DateMetadataFilter;
import com.norconex.importer.handler.filter.impl.EmptyMetadataFilter;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.spi.DefaultRepositorySelector;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RootLogger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class Crawl {

//    private static final Path ALL_SCRAPERS = Paths.get("/home/sw206/git/acled-scrapers");
//    private static final Path CACHE_DIR = Paths.get("/home/sw206/git/springcrawler/balkans_scrapers");

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

    private class RootLogAppenderClearingURLNormaliser implements IURLNormalizer {
        private final GenericURLNormalizer genericURLNormalizer;
        public RootLogAppenderClearingURLNormaliser() {
            this.genericURLNormalizer = new GenericURLNormalizer();
        }

        @Override
        public String normalizeURL(String url) {

//            LogManager.getRootLogger().removeAllAppenders();

            return genericURLNormalizer.normalizeURL(url);
        }
    }

    public Crawl(CrawlArgs args, ACLEDImporter importer, Reporter reporter, List<String> sitemaps) {
        this.source = args.source.get(0);
        this.from = args.from;
        this.to = args.to;
        this.reporter = reporter;

        String id = id(false);
        Path scraperCachePath = Paths.get(id);

        importer.setCollectorSupplier(collectorSupplier);

        importer.setMaxArticles(args.maxArticle);

        Path workingDir = args.workingDir;

        config = new NorconexConfiguration(workingDir.resolve(scraperCachePath), args);
        config.crawler().setUrlNormalizer(new RootLogAppenderClearingURLNormaliser());

        if(!args.ignoreSiteMap) {
            config.crawler().setStartSitemapURLs(sitemaps.toArray(new String[]{}));
        }

        configureLogging(workingDir);

        List<AbstractDocumentFilter> filters = new ArrayList<>();
        filters.add(new AcceptFilter());

        EmptyMetadataFilter emptyArticle = new EmptyMetadataFilter(OnMatch.EXCLUDE,
                ScraperFields.SCRAPED_ARTICLE,
                ScraperFields.SCRAPED_DATE);

        filters.add(emptyArticle);

        if(from != null && to != null ) {

            String zid = source.get(Source.TIMEZONE);
            ZoneId zoneId;
            if(zid==null) {
                zoneId = ZoneId.systemDefault();
            } else {
                zoneId = ZoneId.of(zid);
            }

            DateMetadataFilter dateFilter = dateFilter(source,
                    ZonedDateTime.of(from.atTime(0,0,0), zoneId),
                    ZonedDateTime.of(to.atTime(0,0,0), zoneId)
            );

            filters.add(dateFilter);
        }

        if(!args.skipKeywords) {

            ExcludingKeywordFilter keywordFilter = keywordFilter(args.sourceLists, source);
            filters.add(keywordFilter);
        }

        filters.forEach(config::addFilter);

        List<String> seedUrls = source.get(Source.SEED_URLS);

        String[] startURLs;

        if(seedUrls != null && !seedUrls.isEmpty()) {
            startURLs = seedUrls.toArray(new String[]{});
        } else {
            startURLs = ((String) source.get(Source.LINK)).split(",");
        }

//        String scraperName = Util.getID(startURLs[0]);

        ACLEDScraper scraper = ACLEDScraper.load(args.scrapersDir, source, reporter);
        ACLEDMetadataPreProcessor metadata = new ACLEDMetadataPreProcessor(startURLs[0]);

        if(source.hasValue(Source.CRAWL_EXCLUDE_PATTERN)) {
            String pattern = source.get(Source.CRAWL_EXCLUDE_PATTERN);
            RegexReferenceFilter filter = new RegexReferenceFilter(pattern, OnMatch.EXCLUDE);
            config.crawler().setReferenceFilters(filter);
        }

        if(!sitemaps.isEmpty() && from != null) {
            config.crawler().setMetadataFilters(new SiteMapLastModifiedMetadataFilter(from));
        }

        applySourceIdiosyncrasies(source, config);

        config.crawler().setRecrawlableResolver(new DontRecrawlResolver(startURLs, source.hasValue(Source.CRAWL_RECRAWL_PATTERN)? Pattern.compile(source.get(Source.CRAWL_RECRAWL_PATTERN)) : null));

        config.crawler().setMaxDepth(args.depth);

        config.setScraper(scraper, metadata);
        config.crawler().setStartURLs(startURLs);
//        config.collector();
        if(args.crawlId != null && !args.crawlId.isEmpty()) {
            config.setId(args.crawlId);
        } else {
            config.setId(id);
        }
        config.crawler().setPostImportProcessors(importer);
    }

    public NorconexConfiguration getConfig() {
        return config;
    }

    private void configureLogging(Path workingDir){

        try {
            Object guard = new Object();

            LoggerRepository rs = new CustomLoggerRepository(new RootLogger((Level) Level.DEBUG), workingDir);
            LogManager.setRepositorySelector(new DefaultRepositorySelector(rs), guard);
        } catch (IllegalArgumentException e) {
            //pass already installed
            int x = 0;
        }
        ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
        String name = threadGroup.getName();

        CustomLoggerRepository.register(name, id(false));

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

    public String id(boolean withDates) {
        StringBuilder sb = new StringBuilder();
        String standardName = source.get(Source.STANDARD_NAME);
        standardName = standardName.toLowerCase().replaceAll(" ", "-");
        sb.append(standardName);
        if(withDates) {
            sb.append("-")
                .append(from == null ? "" : from.toString())
                .append("-")
                .append(to == null ? "" : to.toString());
        }
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
