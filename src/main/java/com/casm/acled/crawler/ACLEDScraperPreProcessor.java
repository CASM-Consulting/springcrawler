package com.casm.acled.crawler;

// gson
import com.google.gson.Gson;

// crawling imports
import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import com.norconex.collector.http.doc.HttpDocument;
import com.norconex.collector.http.doc.HttpMetadata;
import com.norconex.collector.http.processor.IHttpDocumentProcessor;
import com.norconex.commons.lang.file.ContentType;

// jsoup
import org.jsoup.Jsoup;
//
//// logging imports
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// java imports
import java.io.*;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

// utils for domain resolution etc..
import com.casm.acled.crawler.utils.Utils;
import uk.ac.susx.tag.norconex.scraping.GeneralSplitterFactory;
import uk.ac.susx.tag.norconex.scraping.IForumSplitter;
import uk.ac.susx.tag.norconex.scraping.POJOHTMLMatcherDefinition;
import uk.ac.susx.tag.norconex.scraping.Post;

public class ACLEDScraperPreProcessor implements IHttpDocumentProcessor {

    protected static final Logger logger = LoggerFactory.getLogger(ACLEDScraperPreProcessor.class);

    public static final String SCRAPERS = "casm.jqm.scrapers";
//    public static final String SCRAPEDJSON = "scraped.json";
    public static final String SCRAPEDARTICLE = "scraped.article";
    public static final String SCRAPEDATE = "scraped.date";
    public static final String SCRAPEDTITLE = "scraped.title";

    public static final String article = "field.name/article";
    public static final String title = "field.name/title";
    public static final String date = "field.name/date";

    public static final String metaDATE = "date";
    public static final String metaTITLE = "title";
    public static final String metaARTICLE = "article";

    // Used if you wish the pre-processor to contain all scrapers.
    public static Map<String, GeneralSplitterFactory> scrapersJson = new HashMap<>();

    // Used if you wish the pre-processor to only be repsonsible for a single scraper.
    public static GeneralSplitterFactory scraperJson;

    private final Gson gson;


    public ACLEDScraperPreProcessor(Path scraperLocation) {

        gson = new Gson();
        try {
            if(Files.isDirectory(scraperLocation) && !Files.exists(Paths.get(scraperLocation.toAbsolutePath().toString(),"job.json"))) {
                logger.info("INFO: Provided a directory for scraper location - attempting to load all scrapers it contains.");
                initScrapers(scraperLocation);
            }
            else {
                logger.info("INFO: Provided a specific crawler to load and scrape pages with.");
                initScraper(scraperLocation);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed: when attempting to initialise web scraper(s): " + e.getMessage());
        }

    }

    /**
     * Initialise and build a single scraper.
     * @param scraperLocation
     */
    private void initScraper(Path scraperLocation){
        File file = getJobFile(scraperLocation).toFile();
        String processed = null;
        try {

            processed = Utils.processJSON(file);
            Map<String, List<Map<String, String>>> scraperDefs = buildScraperDefinition(GeneralSplitterFactory.parseJsonTagSet(processed));
            logger.info("Adding scraper: " + file.getName());
            scraperJson = new GeneralSplitterFactory(scraperDefs);
            logger.info("Scraper successfully added for: " + file.getName());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (IncorrectScraperJSONException e) {
            e.printStackTrace();
        }

    }

    private Path getJobFile(Path scraperLocation) {
        return Paths.get(scraperLocation.toAbsolutePath().toString(),"job.json");
    }

    /**
     * Initialise and build all scrapers in a single directory.
     * @param scrapersLocation
     */
    private void initScrapers(Path scrapersLocation) throws IOException {

        List<Path> scrapers = Files.walk(scrapersLocation)
                .filter(file -> file.getFileName().toString().equals("job.json"))
                .collect(Collectors.toList());
        System.out.println(scrapers.size());
        for (Path path : scrapers) {
            try {
                File file = path.toFile();
                String processed = Utils.processJSON(file);
                Map<String, List<Map<String, String>>> scraperDefs = buildScraperDefinition(GeneralSplitterFactory.parseJsonTagSet(processed));
                logger.info("Adding scraper: " + file.getParentFile().getName());
                scrapersJson.put(file.getParentFile().getName(), new GeneralSplitterFactory(scraperDefs));
                logger.info("Added scraper for: " + file.getParentFile().getName() + " " + scrapersJson.get(file.getParentFile().getName().replace(".json", "")));
                System.out.println("Added scraper for: " + file.getParentFile().getName() + " " + scrapersJson.get(file.getParentFile().getName().replace(".json", "")));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IncorrectScraperJSONException e) {
                e.printStackTrace();
            }
        }
    }

    public WebPage scrape_page(WebPage page) throws ScraperNotFoundException, MalformedURLException {


        String domain = Utils.getDomain(page.getUrl());


        // If there is a factory set for this preprocessor use that else search for one via the page's domain of origin
        // Prefered functionality is to set a single preprocessor (more robust)
        GeneralSplitterFactory factory = scraperJson;

        if(factory == null) {
            factory = scrapersJson.get(domain.replaceAll("\\.",""));
            if(factory == null){
                logger.error("No scraper was found for the domain " + domain.replaceAll("\\.",""));
                throw new ScraperNotFoundException(domain);
            }
        }

        IForumSplitter splitter = factory.create();

        LinkedList<Post> newspages = splitter.split(Jsoup.parse(page.getHtml()));
        if(newspages.size() > 0) {
            Post newspage = newspages.get(0);

            if(newspage.containsKey(article) && newspage.get(article).get(0).length() > 0) {
                page.setArticle(newspage.get(article).get(0));
            }
            else {
                return page;
            }
            if (newspage.containsKey(title) && newspage.get(title).get(0).length() > 0){
                page.setTitle(newspage.get(title).get(0));
            }
            if(newspage.containsKey(date) && newspage.get(date).get(0).length() > 0){
                page.setDate(newspage.get(date).get(0));
            }
        }

        return page;

    }

    /**
     * Transform json pojo object to splitter structure
     * @param matcherList
     * @return
     */
    public static Map<String, List<Map<String, String>>> buildScraperDefinition(List<POJOHTMLMatcherDefinition> matcherList) {

        Map<String, List<Map<String, String>>> fields = new HashMap<>();
        for(POJOHTMLMatcherDefinition matcher : matcherList) {
            List<Map<String, String>> tags = matcher.getTagDefinitions();
            fields.put(matcher.field,tags);
        }
        return fields;

    }



    @Override
    public void processDocument(HttpClient httpClient, HttpDocument doc) {

        if(isText(doc)) {

            final String url = doc.getReference();

            StringWriter sw = new StringWriter();
            doc.getContent().rewind();
            try {
                IOUtils.copy(doc.getContent(), sw, doc.getContentEncoding());
            } catch (IOException e) {
                throw new RuntimeException("ERROR: Failed to retrieve web content for url: " + url);
            }

            final String html = sw.toString();

            final String parent = doc.getMetadata().getString(HttpMetadata.COLLECTOR_REFERRER_REFERENCE);
            final int depth = doc.getMetadata().getInt(HttpMetadata.COLLECTOR_DEPTH);

            final WebPage webPage = new WebPage(url,html,parent,depth);
            try {
                scrape_page(webPage);
            } catch (ScraperNotFoundException e) {
                logger.warn("Scraper not found for article ");
            } catch (MalformedURLException e) {
                logger.warn("Malformed url exception");
            }

            if(webPage.getArticle() != null && webPage.getArticle().length() > 0) {
                List<String> pages = new ArrayList<>();
                doc.getMetadata().put(SCRAPEDARTICLE,Arrays.asList(webPage.getArticle()));
                if(webPage.getTitle() != null && webPage.getTitle().length() > 0) {
                    doc.getMetadata().put(SCRAPEDTITLE, Arrays.asList(webPage.getTitle()));
                }
                if(webPage.getDate() != null && webPage.getDate().length() > 0) {
                    doc.getMetadata().put(SCRAPEDATE, Arrays.asList(webPage.getDate()));
                }
            }

        }

    }


    /**
     * Class used to serialise web page metadata to json
     * TODO: Make scraped metadata less hardcoded
     */
    public class WebPage {

        private final String url;
        private final String html;
        private final String parent;
        private final int depth;
        private String article;
        private String title;
        private String date;

        public WebPage(String url, String html, String parent, int depth) {
            this.url = url;
            this.html = html;
            this.parent = parent;
            this.depth = depth;
        }

        public String getArticle() {return article;}
        public String getTitle() {return title;}
        public String getDate() {return date;}
        public String getUrl() {return url;}
        public String getHtml() {return html;}
        public String getParent() {return parent;}
        public int getDepth() {return depth;}

        public void setArticle(String article) {
            this.article = article;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setDate(String date) {
            this.date = date;
        }

    }

    // Check this is text based only content for M52
    public boolean isText(HttpDocument doc) {
        ContentType ct = doc.getContentType();
        String contenFam = ct.getContentFamily().getId();
        return (ContentType.TEXT.getContentFamily().getId().equals(contenFam) || ContentType.HTML.getContentFamily().getId().equals(contenFam) ||
                ContentType.CSV.getContentFamily().getId().equals(contenFam) || ContentType.XML.getContentFamily().getId().equals(contenFam)) ?
                true : false;
    }

}
