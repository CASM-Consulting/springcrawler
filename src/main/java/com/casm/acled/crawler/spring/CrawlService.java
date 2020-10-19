package com.casm.acled.crawler.spring;

import bithazard.sitemap.parser.SitemapParser;
import bithazard.sitemap.parser.model.InvalidSitemapUrlException;
import bithazard.sitemap.parser.model.UrlConnectionException;
import com.casm.acled.crawler.Crawl;
import com.casm.acled.crawler.management.CheckListService;
import com.casm.acled.crawler.management.CrawlArgs;
import com.casm.acled.crawler.management.CrawlArgsService;
import com.casm.acled.crawler.scraper.ACLEDCommitter;
import com.casm.acled.crawler.scraper.ACLEDImporter;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.util.CustomLoggerRepository;
import com.casm.acled.crawler.util.Util;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.google.common.collect.ImmutableList;
import com.norconex.collector.http.crawler.HttpCrawlerConfig;
import com.norconex.collector.http.data.HttpCrawlData;
import com.norconex.collector.http.robot.RobotsTxt;
import com.norconex.collector.http.robot.impl.StandardRobotsTxtProvider;
import com.norconex.collector.http.sitemap.ISitemapResolver;
import com.norconex.collector.http.sitemap.SitemapURLAdder;
import com.norconex.collector.http.sitemap.impl.StandardSitemapResolverFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.spi.DefaultRepositorySelector;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RootLogger;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


@Component
public class CrawlService {

    protected static final Logger logger = LoggerFactory.getLogger(CrawlService.class);

    @Autowired
    private SourceListDAO sourceListDAO;

    @Autowired
    private SourceDAO sourceDAO;

    @Autowired
    private ArticleDAO articleDAO;

    @Autowired
    private Reporter reporter;

//    private CrawlArgs args;
    @Autowired
    private CrawlArgsService argsService;

    @Autowired
    private CheckListService checkListService;

    public CrawlService() {
//        args = argsService.get();
    }

    public void run(int sourceListId, int sourceId, boolean skipKeywords) {
        run(sourceListId, sourceId, null, null, skipKeywords);
    }


    public void collectExamples(int sourceListId, int sourceId) {

        Optional<SourceList> maybesSourceList = sourceListDAO.getById(sourceListId);
        Optional<Source> maybeSource = sourceDAO.getById(sourceId);

        System.out.println(sourceListId);
        System.out.println(sourceId);

        if(maybesSourceList.isPresent() && maybeSource.isPresent()) {

            ACLEDCommitter committer = new ACLEDCommitter(articleDAO, maybeSource.get(), sourceListDAO, true, true);
            committer.setMaxArticles(10);

            CrawlArgs args = argsService.get();

            args.source = maybeSource.get();
            args.sourceLists = ImmutableList.of(maybesSourceList.get());
            args.depth = 3;

            Crawl crawl = new Crawl(args, committer, reporter, ImmutableList.of());
            crawl.run();
        } else {

            throw new RuntimeException("source or source list not found!");
        }
    }

    public void run(int sourceListId, int sourceId, LocalDate from, LocalDate to, boolean skipKeywords) {

        Optional<SourceList> maybesSourceList = sourceListDAO.getById(sourceListId);
        Optional<Source> maybeSource = sourceDAO.getById(sourceId);

        CrawlArgs args = argsService.get();

        args.source = maybeSource.get();
        args.sourceLists = ImmutableList.of(maybesSourceList.get());
        args.from = from;
        args.to = to;
        args.skipKeywords = skipKeywords;

        run(args);
    }

    public void run(CrawlArgs args) {

        Source source = args.source;

        int sourceId = source.id();

        //ThreadGroup required for logger context, see CustomLoggerRepository
        ThreadGroup tg = new ThreadGroup(Integer.toString(sourceId));

//            ExecutorService executor = Executors.newSingleThreadExecutor();
//            Future<Void> future = executor.submit()

        Thread thread = new Thread(tg, () -> {

            configureLogging(args.workingDir, Crawl.id(args.source));

//            ACLEDImporter importer = new ACLEDImporter(articleDAO, source, sourceListDAO, true);
            ACLEDCommitter committer = new ACLEDCommitter(articleDAO, source, sourceListDAO, true, true);

            List<String> discoveredSitemaps = getSitemaps(source);

            Crawl crawl = new Crawl(args, committer, reporter, discoveredSitemaps);

            crawl.run();
        });

        AtomicReference<Throwable> thrown = new AtomicReference<>();

        thread.setUncaughtExceptionHandler((Thread th, Throwable ex)->{
            thrown.set(ex);
        });

        thread.start();

        try {
            thread.join();
        } catch (InterruptedException e){
            throw new RuntimeException(e);
        }
        if(thrown.get()!=null) {
            throw new RuntimeException(thrown.get());
        }

    }

    private void configureLogging(Path workingDir, String id){

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

        CustomLoggerRepository.register(name, id);

    }

    public Set<String> recentSitemapURLs(String urlRoot, List<String> sitemaps) {
        StandardSitemapResolverFactory ssrf = new StandardSitemapResolverFactory();
        ssrf.setLenient(true);

        long recently = LocalDateTime.now().minus(3, ChronoUnit.DAYS)
                .toInstant(ZoneOffset.UTC).toEpochMilli();

        ssrf.setFrom(recently);

        HttpCrawlerConfig hcc = new HttpCrawlerConfig();

        hcc.setId(Util.getID(urlRoot));
        hcc.setWorkDir(Paths.get("sitemap-check").toFile());

        ISitemapResolver resolver = ssrf.createSitemapResolver(hcc, false);
        HttpClient httpClient = HttpClientBuilder.create().build();

        final Set<String> urls = new HashSet<>();

        SitemapURLAdder adder = new SitemapURLAdder() {
            @Override
            public void add(HttpCrawlData baseURL) {
                String url = baseURL.getReference( );
                if(!url.isEmpty() ) {
                    urls.add( baseURL.getReference( ) );
                }
                if(urls.size() > 10) {

                    resolver.stop();
                }
            }
        };

        resolver.resolveSitemaps(httpClient, urlRoot, sitemaps.toArray(new String[]{}), adder,true);

        resolver.stop();

        return urls;
    }

    public Map<String,String> getRobots(String url) {
        Client client = ClientBuilder.newClient();

        WebTarget target = client.target(url).path("robots.txt");
        target.property(ClientProperties.FOLLOW_REDIRECTS, Boolean.TRUE);

        logger.info(url+"/robots.txt");

        try {

            Invocation.Builder invocationBuilder = target.request(MediaType.TEXT_PLAIN);
            String response = invocationBuilder.get(String.class);
            return parseRobots(response);
        } catch (ProcessingException | WebApplicationException e) {
            logger.warn(e.getMessage());
            return new HashMap<>();
        }
    }

    public Map<String,String> parseRobots(String raw) {
        Map<String,String> robots = new HashMap<>();
        String[] lines = raw.split("\n");
        for(String line : lines) {
            int i = line.indexOf(":");
            String key = "_default";
            if( i > 0) {
                key = line.substring(0, i);
            }
            String value = line.substring(i+1).trim();
            if(robots.containsKey(key)) {
                value = robots.get(key) + "," + value;
            }
            robots.put(key, value);
        }

        return robots;
    }

    public Map<String, List<String>> getSitemaps(SourceList sourceList) {

        Map<String, List<String>> sourceListSitemaps = new HashMap<>();

        List<Source> sources = sourceDAO.byList(sourceList);
        for(Source source : sources) {
            String name = source.get(Source.STANDARD_NAME);
            List<String> sitemaps = getSitemaps(source);
            sourceListSitemaps.put(name, sitemaps);
        }

        return sourceListSitemaps;
    }


    public static List<String> STANDARD_SITEMAP_LOCS = ImmutableList.of(
            "sitemap.xml",
            "sitemap_index.xml"
    );
    private static String SITEMAP = "Sitemap";

    private List<String> checkStandardLocs(String url) {

        Client client = ClientBuilder.newClient();

        List<String> sitemaps = new ArrayList<>();

        for(String loc : STANDARD_SITEMAP_LOCS) {

            WebTarget target = client.target(url).path(loc);
            target.property(ClientProperties.FOLLOW_REDIRECTS, Boolean.TRUE);
            try {
                Invocation.Builder invocationBuilder = target.request(MediaType.APPLICATION_XML);

                invocationBuilder.get(String.class);

                logger.info(url+"/"+loc);

                sitemaps.add(url+"/"+loc);

            } catch (ProcessingException | WebApplicationException e) {

                logger.warn(url+"/"+loc + " : " + e.getMessage());
            }
        }

        return sitemaps;
    }

    public List<String> getSitemaps3(Source source) {

        String url = source.get(Source.LINK);

        StandardRobotsTxtProvider srtp  = new StandardRobotsTxtProvider();

        HttpClient httpClient = HttpClientBuilder.create().build();

        RobotsTxt robotsTxt = srtp.getRobotsTxt(httpClient, url, "CASM Tech");

        List<String> sitemaps = Arrays.asList(robotsTxt.getSitemapLocations());

        return sitemaps;
    }

    /**
     * TODO(andy) how do we use STANDARD_SITEMAP_LOCS - won't this mess with "hasSiteMaps()"?
     */
    public List<String> getSitemaps(Source source) {

        String url = source.get(Source.LINK);

        url = Util.ensureHTTP(url, false);

        url = followRedirects(url);

        Set<String> sitemaps = new HashSet<>();

        // Attempt to discover sitemap location from robots.txt
        SitemapParser sitemapParser = new SitemapParser();
        try {
            Set<String> sitemapLocations = sitemapParser.getSitemapLocations(url);
            sitemaps.addAll(sitemapLocations);
        } catch (InvalidSitemapUrlException e) {
            //pass
        }

        if(sitemaps.isEmpty()) {
            // Try standard ones
            String _url = url;
            sitemaps.addAll(STANDARD_SITEMAP_LOCS.stream().map(s->_url+(_url.endsWith("/")?"":"/")+s).collect(Collectors.toList()));
        }

        List<String> contactableSitemaps = checkURLs(new ArrayList<>(sitemaps));

        return contactableSitemaps;
    }


    public List<String> checkURLs(List<String> urls) {
        Client client = ClientBuilder.newClient();

        List<String> pass = new ArrayList<>();

        for (String url : urls ) {
            if(checkURL(url, client)) {
                pass.add(url);
            }
        }

        return pass;
    }

    public boolean checkURL(String url, Client client) {
        WebTarget target = client.target(url);

        try {
            Invocation.Builder invocationBuilder = target.request();

            invocationBuilder.get(String.class);

        } catch (WebApplicationException e) {

            return false;
        }

        return true;
    }

    public boolean checkURL(String url) {
        Client client = ClientBuilder.newClient();

        return checkURL(url, client);
    }

    public List<String> getSitemaps2(Source source) {
        String url = source.get(Source.LINK);

        if(url == null || url.isEmpty()) {
            logger.warn("empty URL {}", (String)source.get(Source.STANDARD_NAME));
            return new ArrayList<>();
        }

        url = Util.ensureHTTP(url, false);
        url = followRedirects(url);

        List<String> sitemaps = checkStandardLocs(url);

        if(sitemaps.isEmpty()) {

            Map<String, String> robots = getRobots(url);

            if(robots.containsKey(SITEMAP)) {
                String[] sm = robots.get(SITEMAP).split(",");
                sitemaps.addAll(Arrays.asList(sm));
            }
        }

        return sitemaps;
    }

    public String followRedirects(String url)  {
        Client client = ClientBuilder.newClient();

        WebTarget target = client.target(url);
//        target.property(ClientProperties.FOLLOW_REDIRECTS, Boolean.TRUE);
        try {
            Invocation.Builder invocationBuilder = target.request();

            invocationBuilder.get(String.class);

        } catch ( WebApplicationException e) {
            if(e.getResponse().getStatus() >= 300 && e.getResponse().getStatus() < 400) {
                String redirect = (String)e.getResponse().getHeaders().getFirst("Location");

                return followRedirects(redirect);
            }
            logger.warn(url + " : " + e.getMessage());
            return url;
        } catch (ProcessingException e) {
            logger.warn(url + " : " + e.getMessage());
            return url;
        }

        return url;
    }
}
