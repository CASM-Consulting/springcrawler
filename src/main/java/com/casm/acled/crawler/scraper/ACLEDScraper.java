package com.casm.acled.crawler.scraper;

// gson

// crawling imports
import com.casm.acled.crawler.ScraperNotFoundException;
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

public class ACLEDScraper implements IHttpDocumentProcessor {

    protected static final Logger logger = LoggerFactory.getLogger(ACLEDScraper.class);

    public static final String SCRAPERS = "casm.jqm.scrapers";

    public static final String ARTICLE = "field.name/article";
    public static final String TITLE = "field.name/title";
    public static final String DATE = "field.name/date";

    public static final String metaDATE = "date";
    public static final String metaTITLE = "title";
    public static final String metaARTICLE = "article";


    public static final String JOB_JSON = "job.json";

    private final Path scraperPath;
    private GeneralSplitterFactory scraper;

    public ACLEDScraper(Path scraperPath) {
        this(scraperPath, JOB_JSON);
    }

    public ACLEDScraper(Path scraperPath, String jobJson) {
        this.scraperPath = scraperPath.resolve(jobJson);
        if(Files.notExists(scraperPath)) {
            throw new ScraperNotFoundException(scraperPath + " doesn't exist");
        }
    }

    public static ACLEDScraper load(Path path) throws IOException {
        ACLEDScraper scraper = new ACLEDScraper(path);
        scraper.loadScraper();
        return scraper;
    }

    private void loadScraper() throws IOException {

        String processed = Util.processJSON(scraperPath.toFile());

        Map<String, List<Map<String, String>>> scraperDef = buildScraperDefinition(GeneralSplitterFactory.parseJsonTagSet(processed));

        scraper = new GeneralSplitterFactory(scraperDef);
    }

    private Optional<String> maybeGet(Post post, String key) {
        if(post.containsKey(key) &&
                post.get(key).size() > 0  &&
                post.get(key).get(0).length() > 0) {
            return Optional.of(post.get(key).get(0));
        } else {
            return Optional.empty();
        }

    }



    public WebPage scrapePage(WebPage page) {

        IForumSplitter splitter = scraper.create();

        LinkedList<Post> posts = splitter.split(Jsoup.parse(page.getHtml()));

        if(posts.size() > 0) {
            Post post = posts.get(0);

            Optional<String> article = maybeGet(post, ARTICLE);
            Optional<String> date = maybeGet(post, DATE);
            Optional<String> title = maybeGet(post, TITLE);

            if(article.isPresent()) {
                page.setArticle(article.get());
            } else {
                //raise alarm!
            }

            if(title.isPresent()) {
                page.setTitle(title.get());
            } else {
                //raise alarm!
            }

            if(date.isPresent()) {
                page.setDate(date.get());
            } else {
                //raise alarm!
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

            try {
                IOUtils.copy(doc.getContent(), sw, doc.getContentEncoding());
            } catch (IOException e) {
                throw new RuntimeException("ERROR: Failed to retrieve web content for url: " + url);
            }

            final String html = sw.toString();

            final String parent = doc.getMetadata().getString(HttpMetadata.COLLECTOR_REFERRER_REFERENCE);
            final int depth = doc.getMetadata().getInt(HttpMetadata.COLLECTOR_DEPTH);

            final WebPage webPage = new WebPage(url,html,parent,depth);

            scrapePage(webPage);

            if(webPage.getArticle() != null && webPage.getArticle().length() > 0) {
                List<String> pages = new ArrayList<>();
                doc.getMetadata().put(ScraperFields.SCRAPEDARTICLE,Arrays.asList(webPage.getArticle()));
                if(webPage.getTitle() != null && webPage.getTitle().length() > 0) {
                    doc.getMetadata().put(ScraperFields.SCRAPEDTITLE, Arrays.asList(webPage.getTitle()));
                }
                if(webPage.getDate() != null && webPage.getDate().length() > 0) {
                    doc.getMetadata().put(ScraperFields.SCRAPEDATE, Arrays.asList(webPage.getDate()));
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
