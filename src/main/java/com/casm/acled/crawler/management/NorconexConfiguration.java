package com.casm.acled.crawler.management;

import com.casm.acled.crawler.scraper.ACLEDMetadataPreProcessor;
import com.casm.acled.crawler.scraper.ACLEDScraper;
import com.norconex.collector.core.crawler.ICrawlerConfig;
import com.norconex.collector.core.data.store.impl.mvstore.MVStoreCrawlDataStoreFactory;
import com.norconex.collector.http.HttpCollectorConfig;
import com.norconex.collector.http.crawler.HttpCrawlerConfig;
import com.norconex.collector.http.crawler.URLCrawlScopeStrategy;
import com.norconex.collector.http.delay.impl.GenericDelayResolver;
import com.norconex.collector.http.url.impl.GenericLinkExtractor;
import com.norconex.importer.ImporterConfig;
import com.norconex.importer.handler.IImporterHandler;
import com.norconex.importer.handler.filter.AbstractDocumentFilter;
import com.norconex.importer.parser.GenericDocumentParserFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class NorconexConfiguration {


    private final HttpCollectorConfig collector;
    private final HttpCrawlerConfig crawler;
    private final ImporterConfig importer;

    private Path crawlStore;
    private String userAgent = "CASM Consulting LLP";
    private int numThreads = 3;
    private boolean ignoreRobots = false;
    private boolean ignoreSiteMap = true;
    private int depth = 5;
    private String urlRegex ;
    private long politeness = 100;
    private List<String> regexFilterPatterns;
//    private ZonedDateTime from;
//    private ZonedDateTime to;

    private static String PROGRESS = "progress";
    private static String LOGS = "logs";

    private final List<AbstractDocumentFilter> filters;

    public NorconexConfiguration(Path workDir) {
//        this.from = from;
//        this.to = to;

        filters = new ArrayList<>();
        importer = new ImporterConfig();
        collector = new HttpCollectorConfig();
        crawler = new HttpCrawlerConfig();

        crawlStore = workDir;

        configureImporter();
        configureCrawler();
        configureCollector();
    }

    public HttpCollectorConfig collector () {
        return collector;
    }
    public HttpCrawlerConfig crawler() {
        return crawler;
    };
    public ImporterConfig importer() {
        return importer;
    };

    public void setId(String id) {
        collector.setId(id);
        crawler.setId(id);
    }

    private void configureCollector() {

        collector.setCrawlerConfigs(crawler);
        collector.setProgressDir(crawlStore.resolve(PROGRESS).toString());
        collector.setLogsDir(crawlStore.resolve(LOGS).toString());
        //collector.setLogsUnmanaged(true);
    }

    private void configureCrawler() {

//        MD5DocumentChecksummer checksummer = new MD5DocumentChecksummer();
//        checksummer.setSourceFields(CrawlerArguments.SCRAPEDARTICLE);
//        checksummer.setTargetField(CrawlerArguments.CONTENTHASH);
//        crawler.setDocumentChecksummer(checksummer);
//        crawler.setDocumentChecksummer(new ArticleExtractorChecksum());




//        crawler.setMaxDocuments(10);
        crawler.setLinkExtractorQuitAtDepth(true);
        crawler.setImporterConfig(importer);
        // Basic crawler config
        crawler.setUserAgent(userAgent);
        crawler.setMaxDepth(depth); // -1 for inf
        crawler.setIgnoreRobotsMeta(ignoreRobots);
        crawler.setIgnoreRobotsTxt(ignoreRobots);
        crawler.setIgnoreCanonicalLinks(false);
        crawler.setIgnoreSitemap(ignoreSiteMap);
        crawler.setNumThreads(numThreads);
        // only store a crawl cache, not content
        crawler.setKeepDownloads(false);
        // Page found but record of its parent lost - process the content and links anyway
        crawler.setOrphansStrategy(ICrawlerConfig.OrphansStrategy.PROCESS);
        // Keeps the crawler within the same domain
        URLCrawlScopeStrategy ucs = new URLCrawlScopeStrategy();
        ucs.setStayOnDomain(true);
        ucs.setIncludeSubdomains(true);
        ucs.setStayOnPort(false);
        ucs.setStayOnProtocol(false);
        crawler.setUrlCrawlScopeStrategy(ucs);
        // set to false so crawl cache is only those of interest
        crawler.setKeepOutOfScopeLinks(false);

        crawler.setWorkDir(crawlStore.toFile());

        crawler.setCrawlDataStoreFactory(new MVStoreCrawlDataStoreFactory());

//        crawler.setId(id);
//            crawler.setStartSitemapURLs(seeds);
//        crawler.setStartURLs(seeds);


        // Used to set the politeness delay for consecutive post calls to the site (helps prevent being blocked)
        GenericDelayResolver gdr = new GenericDelayResolver();
        gdr.setDefaultDelay((politeness <= 50) ? 50 : politeness); // safety check to avoid to to small a delay
        gdr.setIgnoreRobotsCrawlDelay(ignoreRobots);
        gdr.setScope(GenericDelayResolver.SCOPE_SITE);
        crawler.setDelayResolver(gdr);

        GenericLinkExtractor gle = new GenericLinkExtractor();
        gle.setIgnoreNofollow(ignoreRobots);
        gle.setCharset(StandardCharsets.UTF_8.toString());
        crawler.setLinkExtractors(gle);

        // create the url filters - e.g. regex filters
        // url regex match
        // parent link prevention
//        if(regexFilterPatterns!=null) {
//            crawler.setReferenceFilters(
//                    regexFilterPatterns.stream()
//                            .map(regex -> new RegexReferenceFilter(regex))
//                            .collect(Collectors.toList())
//                            .toArray(new RegexReferenceFilter[regexFilterPatterns.size()])
//            );
//        }
    }

    public NorconexConfiguration addFilter(AbstractDocumentFilter filter) {
        filters.add(filter);
        return this;
    }

    public NorconexConfiguration finalise() {

        importer.setPostParseHandlers(filters.toArray(new IImporterHandler[filters.size()]));
        return this;
    }


    public void setScraper(ACLEDScraper scraper, ACLEDMetadataPreProcessor metadata) {

        crawler.setPreImportProcessors(scraper, metadata);
    }

    private void configureImporter() {

        // set this to correctly manage file sizes etc... (sw ???)
//        importer.setMaxFileCacheSize(10);
//        importer.setMaxFilePoolCacheSize(200);

        //effectively disables default importer framework
        GenericDocumentParserFactory gdpf = new GenericDocumentParserFactory();
        gdpf.setIgnoredContentTypesRegex(".*");
        importer.setParserFactory(gdpf);
        importer.setTempDir(crawlStore.toFile());

    }

}
