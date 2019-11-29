package com.casm.acled.crawler.scraper;

import com.casm.acled.camunda.BusinessKeys;
import com.casm.acled.crawler.ACLEDMetadataPreProcessor;
import com.casm.acled.crawler.ACLEDScraperPreProcessor;
import com.casm.acled.crawler.IncorrectScraperJSONException;
import com.casm.acled.crawler.utils.Utils;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.norconex.collector.http.doc.HttpDocument;
import org.apache.http.client.HttpClient;
import org.apache.nutch.parse.filter.Post;
import org.apache.nutch.parse.forum.splitter.GeneralSplitterFactory;
import org.apache.nutch.splitter.utils.POJOHTMLMatcherDefinition;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * General idea:
 * 1
 *  - have a scraper service sitting on a separate queue with seperate engines polling it
 *  - scraper jobs sent to queue via crawler
 *  - engines pick up the job and run through the scraper
 *  OR
 *  2
 *  - crawled pages written as files
 *  - scrapers running in the engine pick up the pages and write to disc - deleting json when done
 *
 *  2 probably better as is completely independent from the crawling architecture
 *
 */
public class Scraper {

    private ExecutorService service;
    private GeneralSplitterFactory factory;
    private String domain;
    private int jobID;

    private final ArticleDAO articleDAO;
    private final SourceDAO sourceDAO;
    private final SourceListDAO sourceListDAO;

    public Scraper(Path jsonScraper, String domain,
                   ArticleDAO articleDAO, SourceDAO sourceDAO, SourceListDAO sourceListDAO) {
        try {
            String json = Utils.processJSON(jsonScraper.toFile());
            List<POJOHTMLMatcherDefinition> rules = GeneralSplitterFactory.parseJsonTagSet(json);
            factory = new GeneralSplitterFactory(buildScraperDefinition(rules));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IncorrectScraperJSONException e) {
            e.printStackTrace();
        }
        service = Executors.newFixedThreadPool(2);

        this.articleDAO = articleDAO;
        this.sourceDAO = sourceDAO;
        this.sourceListDAO = sourceListDAO;

    }

    public void shutDown() throws InterruptedException {
        service.shutdown();
        service.awaitTermination(Integer.MAX_VALUE, TimeUnit.MINUTES);
    }

    public void shutDownNow() {
        service.shutdownNow();
    }

    public void parseCrawledDocument() {
        //read json of saved document.
        // or
    }

    public LinkedList<Post> scrapePage(String html) {
        return factory.create().split(Jsoup.parse(html));
    }

    public void processPage(HttpClient httpClient, HttpDocument doc){
        String scrapedJson = doc.getMetadata().get(ACLEDScraperPreProcessor.SCRAPEDJSON).get(0);

        ObjectMapper om = new ObjectMapper();

        try {
            Map<String, String> data = om.readValue(scrapedJson, Map.class);
            String articleText = data.get(ACLEDScraperPreProcessor.metaARTICLE);
            String title = data.get(ACLEDScraperPreProcessor.metaTITLE);
            String date = data.get(ACLEDScraperPreProcessor.metaDATE);

            String url = doc.getReference();

            String text = new StringBuilder()
                    .append(title)
                    .append("\n")
                    .append(date)
                    .append("\n")
                    .append(articleText)
                    .toString();

            Article article = EntityVersions.get(Article.class)
                    .current()
                    .put(Article.TEXT, text)
                    .put(Article.URL, url);


            String seed = doc.getMetadata().get(ACLEDMetadataPreProcessor.LINK).get(0);
            Optional<Source> source = sourceDAO.getByUnique(Source.LINK, seed);

            if(source.isPresent()) {

                article = article.put(Article.SOURCE_ID, source.get().id());

                List<SourceList> lists = sourceListDAO.bySource(source.get());
                for(SourceList list : lists) {
                    String bk = BusinessKeys.generate(list.get(SourceList.LIST_NAME));
                    articleDAO.create(article.businessKey(bk));

                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public static void main(String[] args) {
        // main argument from being enqueued by crawler?
    }

}
