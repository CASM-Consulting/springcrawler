package com.casm.acled.crawler.scraper;

import com.casm.acled.camunda.BusinessKeys;
import com.casm.acled.crawler.ACLEDMetadataPreProcessor;
import com.casm.acled.crawler.ACLEDScraperPreProcessor;
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
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.susx.tag.norconex.scraping.GeneralSplitterFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ACLEDScraper extends BaseScraper {

    @Autowired
    private ArticleDAO articleDAO;
    @Autowired
    private SourceDAO sourceDAO;
    @Autowired
    private SourceListDAO sourceListDAO;

//    private final ArticleDAO articleDAO;
//    private final SourceDAO sourceDAO;
//    private final SourceListDAO sourceListDAO;


    public ACLEDScraper(Map<String, GeneralSplitterFactory> scrapers,
                        ArticleDAO articleDAO, SourceDAO sourceDAO, SourceListDAO sourceListDAO) {

        super(scrapers);
        this.articleDAO = articleDAO;
        this.sourceDAO = sourceDAO;
        this.sourceListDAO = sourceListDAO;

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


}
