package com.casm.acled.crawler;

import com.casm.acled.crawler.management.CrawlArgs;
import com.casm.acled.crawler.management.NorconexConfiguration;
import com.casm.acled.crawler.scraper.*;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.scraper.dates.*;
import com.casm.acled.crawler.scraper.keywords.ExcludingKeywordFilter;
import com.casm.acled.crawler.scraper.keywords.KeywordTagger;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.norconex.collector.core.filter.impl.RegexReferenceFilter;
import com.norconex.collector.http.HttpCollector;
import com.norconex.collector.http.url.IURLNormalizer;
import com.norconex.collector.http.url.impl.GenericURLNormalizer;
import com.norconex.importer.handler.IImporterHandler;
import com.norconex.importer.handler.filter.OnMatch;
import com.norconex.importer.handler.filter.impl.DateMetadataFilter;
import com.norconex.importer.handler.transformer.impl.ReplaceTransformer;

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

import com.norconex.importer.handler.tagger.impl.*;

import com.norconex.collector.http.robot.RobotsTxt;
import com.norconex.collector.http.robot.impl.StandardRobotsTxtProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import com.norconex.collector.http.delay.impl.GenericDelayResolver;


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

    public Crawl(CrawlArgs args, ACLEDCommitter committer, Reporter reporter, List<String> sitemaps) {
        this.source = args.source;
        this.from = args.from;
        this.to = args.to;
        this.reporter = reporter;

        String id = id(false);
        Path scraperCachePath = Paths.get(id);

        committer.setCollectorSupplier(collectorSupplier);

        committer.setMaxArticles(args.maxArticle);

        Path workingDir = args.workingDir;

        //force always true as this only switches off norconex sitemap discovery and we're doing this ourselves
        args.ignoreSiteMap = true;

        config = new NorconexConfiguration(workingDir.resolve(scraperCachePath), args);
        config.crawler().setUrlNormalizer(new RootLogAppenderClearingURLNormaliser());

        // added for checking the crawlDelay in robots.txt
        String url = source.get(Source.LINK);

        StandardRobotsTxtProvider srtp  = new StandardRobotsTxtProvider();

        HttpClient httpClient = HttpClientBuilder.create().build();

        RobotsTxt robotsTxt = srtp.getRobotsTxt(httpClient, url, "CASM Tech");

        float robotsDelay = robotsTxt.getCrawlDelay();

        if (robotsDelay > 100) { // certain threshold
            GenericDelayResolver gdr = new GenericDelayResolver();
            gdr.setDefaultDelay((config.getPoliteness() <= 50) ? 50 : config.getPoliteness()); // safety check to avoid to to small a delay
            gdr.setIgnoreRobotsCrawlDelay(true); // disable the crawlDelay in robots.txt file
            gdr.setScope(GenericDelayResolver.SCOPE_SITE);
            config.crawler().setDelayResolver(gdr);
        }


        config.crawler().setStartSitemapURLs(sitemaps.toArray(new String[]{}));

//        List<String> sitemaps = new ArrayList<>();
//        if(source.isFalse(Source.CRAWL_DISABLE_SITEMAPS)) {
//            if(source.isFalse(Source.CRAWL_DISABLE_SITEMAP_DISCOVERY)) {
//                sitemaps.addAll(discoveredSitemaps);
//            }
//            if(source.hasValue(Source.CRAWL_SITEMAP_LOCATIONS)) {
//                sitemaps.addAll(source.get(Source.CRAWL_SITEMAP_LOCATIONS));
//            }
//        }


        List<IImporterHandler> preParsers = new ArrayList<>();
        List<IImporterHandler> postParsers = new ArrayList<>();

        if(from != null && to != null ) {

            String zid = source.get(Source.TIMEZONE);
            ZoneId zoneId;
            if(zid==null) {
                zoneId = ZoneId.systemDefault();
            } else {
                zoneId = ZoneId.of(zid);
            }

            DateTagger dateTagger = dateTagger(source,
                    ZonedDateTime.of(from.atTime(0,0,0), zoneId),
                    ZonedDateTime.of(to.atTime(0,0,0), zoneId));
            postParsers.add(dateTagger);

        }

        if(!args.skipKeywords) {
            for (SourceList sourceList : args.sourceLists) {
                KeywordTagger keywordTagger = keywordTagger(sourceList, source);
                postParsers.add(keywordTagger);
            }

        }

        List<String> seedUrls = source.get(Source.SEED_URLS);

        String[] startURLs;

        if(seedUrls != null && !seedUrls.isEmpty()) {
            startURLs = seedUrls.toArray(new String[]{});
        } else {
            startURLs = ((String) source.get(Source.LINK)).split(",");
        }


        DOMTagger documentTagger = new ACLEDTaggerFactory(args.scrapersDir, source).get();
        ReplaceTransformer documentTransfomer = new ReplaceTransformer();
        documentTransfomer.addReplacement("<script.*?>.*?<\\/script>", "");

        preParsers.add(documentTransfomer);
        preParsers.add(documentTagger);

        config.importer().setPreParseHandlers(preParsers.toArray(new IImporterHandler[]{}));
        config.importer().setPostParseHandlers(postParsers.toArray(new IImporterHandler[]{}));

        if(source.hasValue(Source.CRAWL_EXCLUDE_PATTERN)) {
            String pattern = source.get(Source.CRAWL_EXCLUDE_PATTERN);
            RegexReferenceFilter filter = new RegexReferenceFilter(pattern, OnMatch.EXCLUDE);
            config.crawler().setReferenceFilters(filter);
        }

        if(!args.ignoreSiteMap && from != null) {
            config.crawler().setMetadataFilters(new SiteMapLastModifiedMetadataFilter(from));
        }

        applySourceIdiosyncrasies(source, config);

        config.crawler().setRecrawlableResolver(new DontRecrawlResolver(startURLs, source.hasValue(Source.CRAWL_RECRAWL_PATTERN)? Pattern.compile(source.get(Source.CRAWL_RECRAWL_PATTERN)) : null));

        config.crawler().setMaxDepth(args.depth);

        config.crawler().setStartURLs(startURLs);
        if(args.crawlId != null && !args.crawlId.isEmpty()) {
            config.setId(args.crawlId);
        } else {
            config.setId(id);
        }
        // use committer for testing
        config.crawler().setCommitter(committer);
    }

    public NorconexConfiguration getConfig() {
        return config;
    }

    private ExcludingKeywordFilter keywordFilter(SourceList sourceList, Source source) {

        String query = resolveQuery(sourceList, source);

        ExcludingKeywordFilter keywordFilter = new ExcludingKeywordFilter(ScraperFields.SCRAPED_ARTICLE, query);

        return keywordFilter;
    }

    private KeywordTagger keywordTagger(SourceList sourceList, Source source) {

//        String query = resolveQuery(sourceList, source);
        KeywordTagger keywordTagger = new KeywordTagger(ScraperFields.SCRAPED_ARTICLE, sourceList);

        return keywordTagger;
    }

    private DateMetadataFilter dateFilter(Source source, ZonedDateTime from, ZonedDateTime to) {

        List<String> dateFormatSpecs = source.get(Source.DATE_FORMAT);

        String timezone = source.get(Source.TIMEZONE);
        DateParser dateParser = CompositeDateParser.of(dateFormatSpecs, timezone);
//        DateParser dateParser = CompositeDateParser.of(dateFormatSpecs);

        DateMetadataFilter dateMetadataFilter = new ExcludingCustomDateMetadataFilter(source, ScraperFields.SCRAPED_DATE, dateParser, reporter);

        dateMetadataFilter.addCondition(DateMetadataFilter.Operator.GREATER_EQUAL, Date.from(from.toInstant()));
        dateMetadataFilter.addCondition(DateMetadataFilter.Operator.LOWER_EQUAL, Date.from(to.toInstant()));

        return dateMetadataFilter;
    }

    private DateTagger dateTagger(Source source, ZonedDateTime from, ZonedDateTime to) {
        List<String> dateFormatSpecs = source.get(Source.DATE_FORMAT);

        String timezone = source.get(Source.TIMEZONE);
        DateParser dateParser = CompositeDateParser.of(dateFormatSpecs, timezone);

        DateTagger dateMetadataTagger = new DateTagger(source, ScraperFields.SCRAPED_DATE, dateParser, reporter);
        dateMetadataTagger.setFromTime(from);
        dateMetadataTagger.setToTime(to);

        return dateMetadataTagger;

    }
//    private ULocale getLocale(Source source) {
//        ULocale locale = new ULocale(source.get(Source.LOCALE));
//        return locale;
//    }

    private void applySourceIdiosyncrasies(Source source, NorconexConfiguration config){
//        if(source.get(Source.))
    }

    public String id(boolean withDates){
        return id(source, from, to, withDates);
    }

    public static String id(Source source){
        return id(source, null, null, false);
    }

    public static String id(Source source, LocalDate from, LocalDate to, boolean withDates) {
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

        collector = new HttpCollector(config.collector());
//        ErrorMailNotifier errorMailNotifier = new ErrorMailNotifier();
//        collector.getCollectorConfig().setJobErrorListeners(errorMailNotifier);
        collector.getCollectorConfig().setSuiteLifeCycleListeners();
        collector.start(true);
    }
}
