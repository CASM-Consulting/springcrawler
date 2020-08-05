package com.casm.acled.crawler.spring;

import bithazard.sitemap.parser.SitemapParser;
import bithazard.sitemap.parser.model.InvalidSitemapUrlException;
import bithazard.sitemap.parser.model.UrlConnectionException;
import com.casm.acled.crawler.Crawl;
import com.casm.acled.crawler.management.CheckListService;
import com.casm.acled.crawler.management.CrawlArgs;
import com.casm.acled.crawler.scraper.ACLEDImporter;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.util.Util;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.google.common.collect.ImmutableList;
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
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;


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

    @Autowired
    private CrawlArgs args;

    @Autowired
    private CheckListService checkListService;

    public CrawlService() {

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

            ACLEDImporter importer = new ACLEDImporter(articleDAO, maybeSource.get(), sourceListDAO, true);
            importer.setMaxArticles(10);

            args.sources = ImmutableList.of(maybeSource.get());
            args.sourceList = maybesSourceList.get();
            args.depth = 3;

            Crawl crawl = new Crawl(args, importer, reporter, ImmutableList.of() );
//            crawl.getConfig().crawler().setIgnoreSitemap(false);
            crawl.run();
        } else {

            throw new RuntimeException("source or source list not found!");
        }
    }

    public void run(int sourceListId, int sourceId, LocalDate from, LocalDate to, boolean skipKeywords) {

        Optional<SourceList> maybesSourceList = sourceListDAO.getById(sourceListId);
        Optional<Source> maybeSource = sourceDAO.getById(sourceId);

        args.sources = ImmutableList.of(maybeSource.get());
        args.sourceList = maybesSourceList.get();
        args.from = from;
        args.to = to;
        args.skipKeywords = skipKeywords;

        run(args);
    }

    public void run(CrawlArgs args) {

        Source source = args.sources.get(0);

        if(args.onlySiteMap && ! checkListService.hasSiteMaps(source)) {

            logger.info("Quitting: only site maps requested - {} has no sitemap", (String) source.get(Source.STANDARD_NAME));

        } else {

            int sourceId = source.id();

            //ThreadGroup required for logger context, see CustomLoggerRepository
            ThreadGroup tg = new ThreadGroup(Integer.toString(sourceId));

//            ExecutorService executor = Executors.newSingleThreadExecutor();
//            Future<Void> future = executor.submit()

            Thread thread = new Thread(tg, () -> {

                List<String> sitemaps = getSitemaps(source);

                ACLEDImporter importer = new ACLEDImporter(articleDAO, source, sourceListDAO, true);

                Crawl crawl = new Crawl(args, importer, reporter, sitemaps);

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


    private static List<String> STANDARD_SITEMAP_LOCS = ImmutableList.of(
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

    public List<String> getSitemaps(Source source) {

        String url = source.get(Source.LINK);

        List<String> sitemaps;

        if(url == null || url.isEmpty()) {

            logger.warn("empty URL {}", (String)source.get(Source.STANDARD_NAME));
            sitemaps = new ArrayList<>();
        } else {

            SitemapParser sitemapParser = new SitemapParser();
            try {

                Set<String> sitemapLocations = sitemapParser.getSitemapLocations(url);
                sitemaps = new ArrayList<>(sitemapLocations);
            } catch (InvalidSitemapUrlException e) {

                try {

                    url = followRedirects(url);
                    Set<String> sitemapLocations = sitemapParser.getSitemapLocations(url);

                    sitemaps = new ArrayList<>(sitemapLocations);
                } catch (InvalidSitemapUrlException | UrlConnectionException ee) {

                    sitemaps = new ArrayList<>();
                }
            } catch (UrlConnectionException e) {

                sitemaps = new ArrayList<>();
            }
        }

        return sitemaps;
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
            Invocation.Builder invocationBuilder = target.request(MediaType.TEXT_HTML);

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
