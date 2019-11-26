package com.casm.acled.crawler;

import com.casm.acled.crawler.utils.Utils;
import com.google.gson.Gson;
import com.norconex.collector.http.doc.HttpDocument;
import com.norconex.collector.http.doc.HttpMetadata;
import com.norconex.collector.http.processor.IHttpDocumentProcessor;
import com.norconex.commons.lang.file.ContentType;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.nutch.parse.filter.Post;
import org.apache.nutch.parse.forum.splitter.GeneralSplitterFactory;
import org.apache.nutch.parse.forum.splitter.IForumSplitter;
import org.apache.nutch.splitter.utils.POJOHTMLMatcherDefinition;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.susx.tag.norconex.controller.ContinuousController;

import javax.rmi.CORBA.Util;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;

public class ACLEDScraperPreProcessor implements IHttpDocumentProcessor {

    protected static final Logger logger = LoggerFactory.getLogger(ACLEDScraperPreProcessor.class);

    public static final String SCRAPERS = "casm.jqm.scrapers";
    public static final String SCRAPEDJSON = "scraped.json";

    public static final String article = "field.name/article";
    public static final String title = "field.name/title";
    public static final String date = "field.name/date";

    public static final String metaDATE = "date";
    public static final String metaTITLE = "title";
    public static final String metaARTICLE = "article";

    private static Map<String, GeneralSplitterFactory> scraperJson = new HashMap<>();
    private final Gson gson;


    public ACLEDScraperPreProcessor(Path scraperLocation) {

        gson = new Gson();
        initScrapers(scraperLocation);

    }

    private void initScrapers(Path scraperLocation) {
        File[] scrapers = scraperLocation.toFile().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                boolean accepted = (name.endsWith("json")) ? true : false;
                return accepted;
            }
        });
        for(File file : scrapers){
            try {
                String processed = Utils.processJSON(file);
                Map<String, List<Map<String, String>>> scraperDefs = buildScraperDefinition(GeneralSplitterFactory.parseJsonTagSet(processed));
                logger.info("Adding scraper: " + file.getParentFile().getName());
                scraperJson.put(file.getName().replace(".json",""), new GeneralSplitterFactory(scraperDefs));
                logger.info("Added scraper for: " + file.getName() + " " + scraperJson.get(file.getName().replace(".json","")));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IncorrectScraperJSONException e) {
                e.printStackTrace();
            }
        }
    }


    public WebPage scrape_page(WebPage page) throws ScraperNotFoundException, MalformedURLException {


        String domain = Utils.getDomain(page.getUrl());

        GeneralSplitterFactory factory = scraperJson.get(domain.split("\\.")[0]);
        if(factory == null){
            logger.warn("No logger was found for the domain " + domain);
            throw new ScraperNotFoundException(domain);
        }

        IForumSplitter splitter = factory.create();

        LinkedList<Post> newspages = splitter.split(Jsoup.parse(page.getHtml()));
        if(newspages.size() > 0) {
            Post newspage = newspages.get(0);

            if(newspage.containsKey(article)) {
                page.setArticle(newspage.get(article).get(0));
            }
            if (newspage.containsKey(title)){
                page.setTitle(newspage.get(title).get(0));
            }
            if(newspage.containsKey(date)){
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

            String json = gson.toJson(webPage,WebPage.class);

            if(webPage.getArticle() != null && webPage.getArticle().length() > 0) {
                List<String> pages = new ArrayList<>();
                pages.add(json);
                doc.getMetadata().put(SCRAPEDJSON, pages);
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
