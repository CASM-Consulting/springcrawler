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

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ACLEDScraperPreProcessor implements IHttpDocumentProcessor {

    public static final String SCRAPERS = "casm.jqm.scrapers";
    public static final String SCRAPEDJSON = "scraped.json";

    public static final String article = "article";
    public static final String title = "title";
    public static final String date = "date";

    private Map<String, GeneralSplitterFactory> scraperJson;
    private final Gson gson;



    public ACLEDScraperPreProcessor(Path scraperLocation) {

        scraperJson = new HashMap<>();
        gson = new Gson();
        initScrapers(scraperLocation);

    }

    private void initScrapers(Path scraperLocation) {
        File[] scrapers = scraperLocation.toFile().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith("json");
            }
        });
        for(File file : scrapers){
            try {
                Map<String, List<Map<String, String>>> scraperDefs = buildScraperDefinition(GeneralSplitterFactory.getTagSetFromJson(file.toPath()));
                scraperJson.put(file.getName(), new GeneralSplitterFactory(scraperDefs));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public WebPage scrape_page(WebPage page) throws ScraperNotFoundException, MalformedURLException {


        String domain = Utils.getDomain(page.getUrl());
        GeneralSplitterFactory factory = scraperJson.get(domain.split("\\.")[0]);
        if(factory == null){
            throw new ScraperNotFoundException(domain);
        }

        IForumSplitter splitter = factory.create();

        Post newspage = splitter.split(Jsoup.parse(page.html)).getFirst();

        if(newspage.containsKey(article)) {
            page.setArticle(newspage.get(article).get(0));
        }
        if (newspage.containsKey(title)){
            page.setTitle(newspage.get(title).get(0));
        }
        if(newspage.containsKey(date)){
            page.setDate(newspage.get(date).get(0));
        }


        return page;

    }

    /**
     * Transform json pojo object to splitter structure
     * @param matcherList
     * @return
     */
    public Map<String, List<Map<String, String>>> buildScraperDefinition(List<POJOHTMLMatcherDefinition> matcherList) {

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
                // todo: Error handling
            } catch (MalformedURLException e) {
                // todo: Error handling
            }

            String json = gson.toJson(webPage);

            doc.getMetadata().put(SCRAPEDJSON, Collections.singletonList(json));

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
