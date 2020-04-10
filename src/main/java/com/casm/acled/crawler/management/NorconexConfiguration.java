package com.casm.acled.crawler.management;

import com.casm.acled.crawler.DateFilter;
import com.casm.acled.crawler.utils.Util;
import com.norconex.collector.core.checksum.impl.MD5DocumentChecksummer;
import com.norconex.collector.core.crawler.ICrawlerConfig;
import com.norconex.collector.core.data.store.impl.mvstore.MVStoreCrawlDataStoreFactory;
import com.norconex.collector.core.filter.impl.RegexReferenceFilter;
import com.norconex.collector.http.HttpCollectorConfig;
import com.norconex.collector.http.crawler.HttpCrawlerConfig;
import com.norconex.collector.http.crawler.URLCrawlScopeStrategy;
import com.norconex.collector.http.delay.impl.GenericDelayResolver;
import com.norconex.collector.http.url.impl.GenericLinkExtractor;
import com.norconex.importer.ImporterConfig;
import com.norconex.importer.handler.IImporterHandler;
import com.norconex.importer.handler.filter.OnMatch;
import com.norconex.importer.handler.filter.impl.EmptyMetadataFilter;
import com.norconex.importer.handler.filter.impl.RegexMetadataFilter;
import com.norconex.importer.parser.GenericDocumentParserFactory;
import uk.ac.susx.tag.norconex.document.ArticleExtractorChecksum;
import uk.ac.susx.tag.norconex.jobqueuemanager.CrawlerArguments;
import uk.ac.susx.tag.norconex.jobqueuemanager.SingleSeedCollector;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NorconexConfiguration {


    private final HttpCollectorConfig collector;
    private final HttpCrawlerConfig crawler;
    private final ImporterConfig importer;

    private Path crawlStore;
    private String userAgent;
    private int threadsPerSeed;
    private boolean ignoreRobots;
    private boolean ignoreSiteMap;
    private int depth;
    private String urlRegex;
    private String seed;
    private long politeness;
    private int numThreads;
    private String regxFiltPatterns;
    private LocalDateTime from;
    private LocalDateTime to;

    private static String PROGRESS = "progress";
    private static String LOGS = "logs";

    public NorconexConfiguration(LocalDateTime from, LocalDateTime to) {
        this.from = from;
        this.to = to;
        importer = new ImporterConfig();
        collector = new HttpCollectorConfig();
        crawler = new HttpCrawlerConfig();

    }

    public HttpCollectorConfig collector (){
        return collector;
    }
    public HttpCrawlerConfig crawler() {
        return crawler;
    };
    public ImporterConfig importer() {
        return importer;
    };

    private void configureCollector() {

        collector.setProgressDir(crawlStore.resolve(PROGRESS).toString());
        collector.setLogsDir(crawlStore.resolve(LOGS).toString());
    }

    private void configureCrawler() {


        MD5DocumentChecksummer checksummer = new MD5DocumentChecksummer();
        checksummer.setSourceFields(CrawlerArguments.SCRAPEDARTICLE);
        checksummer.setTargetField(CrawlerArguments.CONTENTHASH);
        crawler.setDocumentChecksummer(checksummer);

        // Basic crawler config
        crawler.setUserAgent(userAgent);
        crawler.setMaxDepth(depth); // -1 for inf
        crawler.setIgnoreRobotsMeta(ignoreRobots);
        crawler.setIgnoreRobotsTxt(ignoreRobots);
        crawler.setIgnoreCanonicalLinks(false);
        crawler.setDocumentChecksummer(new ArticleExtractorChecksum());
        crawler.setIgnoreSitemap(ignoreSiteMap);

        crawler.setCrawlDataStoreFactory(new MVStoreCrawlDataStoreFactory());

        // Control the threadpool size for each crawler
        crawler.setNumThreads(numThreads);

        // Location of crawl output, db etc...
        crawler.setWorkDir(crawlStore.toFile());

        // only store a crawl cache M52 deals with content
        crawler.setKeepDownloads(false);
//        crawler.setId(id);

        // Page found but record of its parent lost - process the content and links anyway
        crawler.setOrphansStrategy(ICrawlerConfig.OrphansStrategy.PROCESS);

        // Keeps the crawler within the same domain
        URLCrawlScopeStrategy ucs = new URLCrawlScopeStrategy();
        ucs.setStayOnDomain(true);
        ucs.setIncludeSubdomains(true);
        ucs.setStayOnPort(false);
        ucs.setStayOnProtocol(false);
        crawler.setUrlCrawlScopeStrategy(ucs);

//		GenericRecrawlableResolver grr = new GenericRecrawlableResolver();
//		grr.setMinFrequencies();
//		this.setRecrawlableResolver(grr);

//		GenericURLNormalizer urlNormaliser = new GenericURLNormalizer();
//		urlNormaliser.setNormalizations(GenericURLNormalizer.Normalization);

        // set to false so crawl cache is only those of interest
        crawler.setKeepOutOfScopeLinks(false);

//        crawler.setStartURLs(seeds);
//
//        // use this if you want to adhere to sitemap.
//        if(!ignoreSiteMap) {
//            crawler.setStartSitemapURLs(seeds);
//        }


        crawler.setImporterConfig(importer);

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
        RegexReferenceFilter[] referenceFilters = new RegexReferenceFilter[]{new RegexReferenceFilter(regxFiltPatterns)};
//			.map(regex -> new RegexReferenceFilter(regex))
//			.collect(Collectors.toList()).toArray(new RegexReferenceFilter[regxFiltPatterns.size()]);
        crawler.setReferenceFilters(referenceFilters);

    }

    private void configureImporter() {

        RegexMetadataFilter regexFilter = new RegexMetadataFilter(CrawlerArguments.SCRAPEDARTICLE, Util.KEYWORDS);
        EmptyMetadataFilter emptyArticle = new EmptyMetadataFilter(OnMatch.EXCLUDE,CrawlerArguments.SCRAPEDARTICLE);
        int week = 7;
        DateFilter df = new DateFilter(LocalDate.now().minusDays(week));


        List<IImporterHandler> handlers = new ArrayList<>();

        handlers.add(emptyArticle);
        handlers.add(regexFilter);
        importer.setPostParseHandlers(handlers.toArray(new IImporterHandler[handlers.size()]));


        // set this to correctly manage file sizes etc...
        importer.setMaxFileCacheSize(10);
        importer.setMaxFilePoolCacheSize(200);
        GenericDocumentParserFactory gdpf = new GenericDocumentParserFactory();
        gdpf.setIgnoredContentTypesRegex(".*");
        importer.setParserFactory(gdpf);
        importer.setTempDir(crawlStore.toFile());

    }

}
