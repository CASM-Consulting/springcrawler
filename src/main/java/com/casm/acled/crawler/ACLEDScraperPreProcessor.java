package com.casm.acled.crawler;

// gson

// crawling imports
import com.casm.acled.crawler.utils.Util;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

// utils for domain resolution etc..
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

    private final Map<String, GeneralSplitterFactory> scraperCache;

    private final Path scraperPath;

    public static final String JOB_JSON = "job.json";
    private final String jobJson;
    private static final String SINGULAR_KEY = "SINGULAR_KEY";

    private final boolean singularScaper;

    public ACLEDScraperPreProcessor(Path scraperPath) {
        this(scraperPath, JOB_JSON);
    }

    public ACLEDScraperPreProcessor(Path scraperPath, String jobJson) {
        this.scraperPath = scraperPath;
        this.jobJson = jobJson;
        if(Files.notExists(scraperPath)) {
            throw new ScraperNotFoundException(scraperPath + " doesn't exist");
        } else if(Files.exists(scraperPath.resolve(this.jobJson))) {
            singularScaper = true;
        } else {
            singularScaper = false;
        }

        scraperCache = new HashMap<>();

    }
    private GeneralSplitterFactory loadScraper(String key) throws IOException, IncorrectScraperJSONException {
        File file;
        if(singularScaper) {
            key = ".";
        }
        file = scraperPath.resolve(key).resolve(jobJson).toFile();

        if(!file.exists()) {
            logger.warn("No scraper for {}", file.toString());

            throw new ScraperNotFoundException(file.toString());
        }
        String processed = Util.processJSON(file);

        Map<String, List<Map<String, String>>> scraperDef = buildScraperDefinition(GeneralSplitterFactory.parseJsonTagSet(processed));

        GeneralSplitterFactory scraper = new GeneralSplitterFactory(scraperDef);

        return scraper;
    }

    private synchronized GeneralSplitterFactory getScraper(String id) throws IOException, IncorrectScraperJSONException {
        GeneralSplitterFactory scraper;
        String key = id;
        if(singularScaper) {
            key = SINGULAR_KEY;
        }

        if(scraperCache.containsKey(key)) {
            scraper = scraperCache.get(key);
        } else {
            scraper = loadScraper(key);
            scraperCache.put(key, scraper);
        }

        return scraper;
    }

    public WebPage scrapePage(WebPage page) throws ScraperNotFoundException, IOException, IncorrectScraperJSONException {

        String domain = Util.getDomain(page.getUrl());

        // If there is a factory set for this preprocessor use that else search for one via the page's domain of origin
        // Prefered functionality is to set a single preprocessor (more robust)
        String id = domain.replaceAll("\\.","");
        GeneralSplitterFactory factory = getScraper(id);

        if(factory == null){
            logger.error("No scraper was found for the domain " + id);
            throw new ScraperNotFoundException(domain);
        }

        IForumSplitter splitter = factory.create();

        LinkedList<Post> newspages = splitter.split(Jsoup.parse(page.getHtml()));
        if(newspages.size() > 0) {
            Post newspage = newspages.get(0);

            if(newspage.containsKey(article) &&
                    newspage.get(article).size() > 0  &&
                    newspage.get(article).get(0).length() > 0) {
                page.setArticle(newspage.get(article).get(0));
            }
            else {
                return page;
            }
            if (newspage.containsKey(title) &&
                    newspage.get(title).size() > 0 &&
                    newspage.get(title).get(0).length() > 0){
                page.setTitle(newspage.get(title).get(0));
            }
            if(newspage.containsKey(date) &&
                    newspage.get(date).size() > 0 &&
                    newspage.get(date).get(0).length() > 0){
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
                scrapePage(webPage);
            } catch (ScraperNotFoundException e) {
                logger.warn("Scraper not found for article {}", url);
                throw e;
            } catch (IOException | IncorrectScraperJSONException e) {
                logger.warn(e.getMessage(), e);
                throw new RuntimeException(e);
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
