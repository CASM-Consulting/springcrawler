package com.casm.acled.crawler;

import com.casm.acled.camunda.BusinessKeys;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.norconex.collector.http.doc.HttpDocument;
import com.norconex.collector.http.processor.IHttpDocumentProcessor;
import org.apache.http.client.HttpClient;
import com.casm.acled.dao.entities.ArticleDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ACLEDPostProcessor implements IHttpDocumentProcessor {

    protected static final Logger logger = LoggerFactory.getLogger(DateFilter.class);

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
            logger.error("Failed to import page to spring: " + doc.getReference() + " " + e.getMessage());
        }

    }
}
