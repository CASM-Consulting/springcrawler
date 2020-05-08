package com.casm.acled.crawler.scraper;

// casm
import com.casm.acled.crawler.scraper.dates.CustomDateMetadataFilter;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.source.Source;

// json

// norconex
import com.norconex.collector.http.HttpCollector;
import com.norconex.collector.http.doc.HttpDocument;
import com.norconex.collector.http.doc.HttpMetadata;
import com.norconex.collector.http.processor.IHttpDocumentProcessor;

// http
import com.norconex.jef4.status.JobState;
import org.apache.http.client.HttpClient;

// casm
import com.casm.acled.dao.entities.ArticleDAO;

// logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.susx.tag.norconex.jobqueuemanager.CrawlerArguments;

// java
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

import static com.casm.acled.crawler.Util.metadataGet;

/**
 * Commits the scraped data produced by @ACLEDScraperPreProcessor to the relevant fields in acled_article
 */
public class ACLEDImporter implements IHttpDocumentProcessor {

    protected static final Logger logger = LoggerFactory.getLogger(ACLEDImporter.class);

    private final ArticleDAO articleDAO;
    private final SourceDAO sourceDAO;
    private final SourceListDAO sourceListDAO;
    private final boolean sourceRequired;

    private Supplier<HttpCollector> collectorSupplier;
    private Integer maxArticles;

    public ACLEDImporter(ArticleDAO articleDAO, SourceDAO sourceDAO,
                         SourceListDAO sourceListDAO, boolean sourceRequired) {

        this.articleDAO = articleDAO;
        this.sourceDAO = sourceDAO;
        this.sourceListDAO = sourceListDAO;
        this.sourceRequired = sourceRequired;
    }

    public void setCollectorSupplier(Supplier<HttpCollector> collectorSupplier) {
        this.collectorSupplier = collectorSupplier;
    }

    public void setMaxArticles(Integer maxArticles) {
        this.maxArticles = maxArticles;
    }

    private boolean previouslyScraped(HttpDocument doc) {
        String val = metadataGet(doc.getMetadata(), CrawlerArguments.PREVIOUSLYSCRAPED);
        if(val == null) {
            return false;
        } else {
            return Boolean.valueOf(val);
        }
    }

    private synchronized void stop(HttpCollector collector) {
        if(collectorSupplier.get().getState().isOneOf(JobState.RUNNING)) {
            collectorSupplier.get().stop();
        }
    }

    private boolean stopAfterNArticlesFromSource(Source source) {
        if(maxArticles != null && articleDAO.bySource(source).size() >= maxArticles) {
            stop(collectorSupplier.get());
            return true;
        }
        return false;
    }

    @Override
    public void processDocument(HttpClient httpClient, HttpDocument doc) {

        if(!previouslyScraped(doc)) {

            HttpMetadata metadata = doc.getMetadata();

            String articleText = metadataGet(metadata, ScraperFields.SCRAPED_ARTICLE);
            String title = metadataGet(metadata, ScraperFields.SCRAPED_TITLE);
            String date = metadataGet(metadata, ScraperFields.SCRAPED_DATE);
            String standardDate = metadataGet(metadata, ScraperFields.STANDARD_DATE);

            StringBuilder text = new StringBuilder();
            Article article = EntityVersions.get(Article.class)
                    .current();

            text.append(date).append("\n");

            if(title != null) {
                text.append(title).append("\n");
                article = article.put(Article.TITLE, title);
            }

            text.append(articleText);


            String url = doc.getReference();

            article = article.put(Article.TEXT, text.toString())
                    .put(Article.SCRAPE_DATE, date)
                    .put(Article.URL, url);

            if(standardDate != null) {
                LocalDateTime parsedDate = CustomDateMetadataFilter.toDate(standardDate);
                article = article.put(Article.DATE, parsedDate.toLocalDate());
            }

            LocalDate crawlDate = LocalDate.now();

            article = article.put(Article.CRAWL_DATE, crawlDate);

            String seed = metadataGet(metadata, ACLEDMetadataPreProcessor.LINK);

            Optional<Source> maybeSource = sourceDAO.getByUnique(Source.LINK, seed);

            if(maybeSource.isPresent()) {
                if(!stopAfterNArticlesFromSource(maybeSource.get()) ) {

                    article = article.put(Article.SOURCE_ID, maybeSource.get().id());
//                List<SourceList> lists = sourceListDAO.bySource(source.get());
//                for (SourceList list : lists) {
//                    String bk = BusinessKeys.generate(list.get(SourceList.LIST_NAME));
                    articleDAO.create(article);
//                }

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
