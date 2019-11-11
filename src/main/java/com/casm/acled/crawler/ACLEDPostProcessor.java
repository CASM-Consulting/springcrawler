package com.casm.acled.crawler;

import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.article.Article;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.norconex.collector.http.doc.HttpDocument;
import com.norconex.collector.http.processor.IHttpDocumentProcessor;
import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.casm.acled.dao.entities.ArticleDAO;


import java.io.IOException;
import java.util.Map;

public class ACLEDPostProcessor implements IHttpDocumentProcessor {


    private final ArticleDAO articleDAO;

    public ACLEDPostProcessor(ArticleDAO articleDAO) {
        this.articleDAO = articleDAO;
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


            articleDAO.create(article);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
