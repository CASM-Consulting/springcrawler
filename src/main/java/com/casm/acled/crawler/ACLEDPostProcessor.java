package com.casm.acled.crawler;

// casm
import com.casm.acled.camunda.BusinessKeys;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;

// json
import com.fasterxml.jackson.databind.ObjectMapper;

// norconex
import com.norconex.collector.http.doc.HttpDocument;
import com.norconex.collector.http.processor.IHttpDocumentProcessor;

// http
import org.apache.http.client.HttpClient;

// casm
import com.casm.acled.dao.entities.ArticleDAO;

// logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// java
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Commits the scraped data produced by @ACLEDScraperPreProcessor to the relevant fields in acled_article
 */
public class ACLEDPostProcessor implements IHttpDocumentProcessor {

    protected static final Logger logger = LoggerFactory.getLogger(ACLEDPostProcessor.class);

    private final ArticleDAO articleDAO;
    private final SourceDAO sourceDAO;
    private final SourceListDAO sourceListDAO;

    public ACLEDPostProcessor(ArticleDAO articleDAO, SourceDAO sourceDAO, SourceListDAO sourceListDAO) {
        this.articleDAO = articleDAO;
        this.sourceDAO = sourceDAO;
        this.sourceListDAO = sourceListDAO;
    }

    @Override
    public void processDocument(HttpClient httpClient, HttpDocument doc) {

        String scrapedJson = doc.getMetadata().get(ACLEDScraperPreProcessor.SCRAPEDJSON).get(0);

        ObjectMapper om = new ObjectMapper();

        try {
            Map<String, String> data = om.readValue(scrapedJson, Map.class);
            String articleText = data.get("article");
            String title = data.get("title");
            String date = data.get("date");

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

            logger.error("SEED = " + seed);
            Optional<Source> source = sourceDAO.getByUnique(Source.LINK, seed);

            if(source.isPresent()) {

                logger.error("INFO: Source present");

                logger.error("INFO: seed: " + seed);

                article = article.put(Article.SOURCE_ID, source.get().id());

                logger.error("INFO: seed: " + article.toString());

                List<SourceList> lists = sourceListDAO.bySource(source.get());

                logger.error("INFO  list size: " + lists.size());
                for (SourceList list : lists) {
                    String bk = BusinessKeys.generate(list.get(SourceList.LIST_NAME));
                    logger.error("INFO: " + bk);
                    articleDAO.create(article.businessKey(bk));
                    logger.error("INFO: Article created.");
                }
            }
            if(!source.isPresent()){
                logger.error("INFO: Source not present");
            }

        } catch (IOException e) {
            logger.error("Failed to import page to spring: " + doc.getReference() + " " + e.getMessage());
        }

    }
}
