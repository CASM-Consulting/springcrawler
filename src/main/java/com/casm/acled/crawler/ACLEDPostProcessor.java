package com.casm.acled.crawler;

// casm
import com.casm.acled.camunda.BusinessKeys;
import com.casm.acled.crawler.dates.DateUtil;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;

// json

// norconex
import com.norconex.collector.http.doc.HttpDocument;
import com.norconex.collector.http.doc.HttpMetadata;
import com.norconex.collector.http.processor.IHttpDocumentProcessor;

// http
import org.apache.http.client.HttpClient;

// casm
import com.casm.acled.dao.entities.ArticleDAO;

// logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.susx.tag.norconex.jobqueuemanager.CrawlerArguments;

// java
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.casm.acled.crawler.utils.Util.metadataGet;

/**
 * Commits the scraped data produced by @ACLEDScraperPreProcessor to the relevant fields in acled_article
 */
public class ACLEDPostProcessor implements IHttpDocumentProcessor {

    protected static final Logger logger = LoggerFactory.getLogger(ACLEDPostProcessor.class);

    private final ArticleDAO articleDAO;
    private final SourceDAO sourceDAO;
    private final SourceListDAO sourceListDAO;
    private final boolean sourceRequired;

    public ACLEDPostProcessor(ArticleDAO articleDAO, SourceDAO sourceDAO,
                              SourceListDAO sourceListDAO, boolean sourceRequired) {

        this.articleDAO = articleDAO;
        this.sourceDAO = sourceDAO;
        this.sourceListDAO = sourceListDAO;
        this.sourceRequired = sourceRequired;

    }

    private boolean previouslyScraped(HttpDocument doc) {
        String val = metadataGet(doc.getMetadata(), CrawlerArguments.PREVIOUSLYSCRAPED);
        if(val == null) {
            return false;
        } else {
            return Boolean.valueOf(val);
        }
    }

    @Override
    public void processDocument(HttpClient httpClient, HttpDocument doc) {

        if(!previouslyScraped(doc)) {

            HttpMetadata metadata = doc.getMetadata();
            String articleText = metadataGet(metadata, CrawlerArguments.SCRAPEDARTICLE);
            String date = metadataGet(metadata,CrawlerArguments.SCRAPEDATE);

            String title = metadataGet(metadata, CrawlerArguments.SCRAPEDTITLE);
            StringBuilder text = new StringBuilder();
            Article article = EntityVersions.get(Article.class)
                    .current();

            text.append(date).append("\n");

            if(title != null) {
                text.append(title).append("\n");
                article = article.put(Article.TITLE, title);
            }

            text.append(articleText);

            Optional<LocalDate> parsedDate = DateUtil.getDate(date);

            String url = doc.getReference();

            article = article.put(Article.TEXT, text.toString())
                    .put(Article.URL, url);

            if(parsedDate.isPresent()) {
                article = article.put(Article.DATE, parsedDate.get());
            }

            LocalDate crawlDate = DateUtil.fromDateString(metadataGet(metadata, ACLEDMetadataPreProcessor.CRAWLDATE));

            article = article.put(Article.CRAWL_DATE, crawlDate);

            String seed = metadataGet(metadata, ACLEDMetadataPreProcessor.LINK);

            Optional<Source> source = sourceDAO.getByUnique(Source.LINK, seed);

            if(source.isPresent()) {
                article = article.put(Article.SOURCE_ID, source.get().id());
                List<SourceList> lists = sourceListDAO.bySource(source.get());
                for (SourceList list : lists) {
                    String bk = BusinessKeys.generate(list.get(SourceList.LIST_NAME));
                    articleDAO.create(article.businessKey(bk));
                }
            } else  if(!sourceRequired) {
//                logger.info("Source not present - adding without source.");
                articleDAO.create(article);
            } else{
                logger.warn("Skipping import: source required and not present.");
            }

        }
    }
}
