package com.casm.acled.crawler.scraper;

import com.casm.acled.crawler.scraper.dates.ExcludingCustomDateMetadataFilter;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.source.Source;
import com.norconex.collector.http.HttpCollector;
import com.norconex.collector.http.doc.HttpDocument;
import com.norconex.collector.http.doc.HttpMetadata;
import com.norconex.committer.core.ICommitter;
import com.norconex.commons.lang.map.Properties;
import com.norconex.jef4.status.JobState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.susx.tag.norconex.jobqueuemanager.CrawlerArguments;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Supplier;


// qiwei added for testing, delete later:
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.io.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.IOUtils;


public class ACLEDCommitter implements ICommitter {

    protected static final Logger logger = LoggerFactory.getLogger(ACLEDImporter.class);

    private final ArticleDAO articleDAO;
    private final Source source;
    private Integer maxArticles;
    private final SourceListDAO sourceListDAO;
    private final boolean sourceRequired;
    private final boolean recordRaw;

    private Supplier<HttpCollector> collectorSupplier;


    public ACLEDCommitter(ArticleDAO articleDAO, Source source,
                         SourceListDAO sourceListDAO, boolean sourceRequired, boolean recordRaw) {

        this.articleDAO = articleDAO;
        this.source = source;
        this.sourceListDAO = sourceListDAO;
        this.sourceRequired = sourceRequired;
        maxArticles = null;
        this.recordRaw = recordRaw;
    }

    public void setCollectorSupplier(Supplier<HttpCollector> collectorSupplier) {
        this.collectorSupplier = collectorSupplier;
    }

    public void setMaxArticles(Integer maxArticles) {
        if(maxArticles != null && maxArticles >= 0) {
            this.maxArticles = maxArticles;
        }
    }

    private boolean previouslyScraped(Properties properties) {
        String val = properties.getString(CrawlerArguments.PREVIOUSLYSCRAPED);
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
    public void add(String s, InputStream inputStream, Properties properties) {
        // current problem is that the passing parameter is just the properties which is the metadata, so there is no way
        // to get the doc object, which is needed to get the reference.
        // probably could get reference by
        // properties.getString("document.reference")

        if(!previouslyScraped(properties)) {

            if(!properties.getBoolean(ScraperFields.KEYWORD_PASSED) || !properties.getBoolean(ScraperFields.DATE_PASSED) ) {
                return;
            }

            // qiwei added for record writing
            StringWriter writer = new StringWriter();
            try {
                IOUtils.copy(inputStream, writer, properties.getString("document.contentEncoding"));
            }
            catch (Exception ex) {
                String url = properties.getString("document.reference");
                throw new RuntimeException("ERROR: Failed to retrieve web content for url: " + url);
            }

            String rawHtml = writer.toString();

            String articleText = properties.getString( ScraperFields.SCRAPED_ARTICLE);
            String title = properties.getString( ScraperFields.SCRAPED_TITLE);
            String date = properties.getString( ScraperFields.SCRAPED_DATE);
            String standardDate = properties.getString( ScraperFields.STANDARD_DATE);

            Article article = EntityVersions.get(Article.class)
                    .current();

            if(title != null) {
                article = article.put(Article.TITLE, title);
            }

            // here, take url by searching this key value;
            String url = properties.getString("document.reference");

            article = article.put(Article.TEXT, articleText)
                    .put(Article.SCRAPE_DATE, date)
                    .put(Article.URL, url);

            if(properties.getString(ScraperFields.KEYWORD_HIGHLIGHT)!=null) {
                article = article.put(Article.SCRAPE_KEYWORD_HIGHLIGHT, properties.getString(ScraperFields.KEYWORD_HIGHLIGHT));
            }

            if(standardDate != null) {
                LocalDateTime parsedDate = ExcludingCustomDateMetadataFilter.toDate(standardDate);
                article = article.put(Article.DATE, parsedDate.toLocalDate());
            }

            if (recordRaw) {
                article = article.put(Article.SCRAPE_RAW_HTML, rawHtml);
            }

            int depth = properties.getInt("collector.depth");
            article = article.put(Article.CRAWL_DEPTH, depth);

            LocalDate crawlDate = LocalDate.now();

            article = article.put(Article.CRAWL_DATE, crawlDate);

            if(!stopAfterNArticlesFromSource(source) ) {
                article = article.put(Article.SOURCE_ID, source.id());
                articleDAO.create(article);
            }

            // qiwei added for testing output
            saveToLocal(article, Paths.get("/Users/pengqiwei/Downloads/My/PhDs/acled_thing/exports/test_scraper_tagger/Imagen del Golfo_scraper_test.csv"));
//            saveHtmlToLocal(rawHtml, url, Paths.get("/Users/pengqiwei/Downloads/My/PhDs/acled_thing/exports/test_scraper_tagger/Milenio_htmls.csv"));
        }
    }

    // qiwei added for testing output
    public void saveToLocal(Article article, Path path) {
        try {

            OutputStream outputStream = java.nio.file.Files.newOutputStream(path, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)), false);
            CSVPrinter csv = new CSVPrinter(writer, CSVFormat.EXCEL.withQuoteMode(QuoteMode.NON_NUMERIC));

            Map<String, String> map = toMapWithColumn(article, Arrays.asList("URL", "TEXT", "DATE", "TITLE","SCRAPE_RAW_HTML"));
            List<String> list = new ArrayList<String>(map.values());
            csv.printRecord(list);
            csv.close();

        }

        catch (Exception ex){
            ex.printStackTrace();

        }
    }

    public void saveHtmlToLocal(String str, String url, Path path) {
        try {

            OutputStream outputStream = java.nio.file.Files.newOutputStream(path, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)), false);
            CSVPrinter csv = new CSVPrinter(writer, CSVFormat.EXCEL.withQuoteMode(QuoteMode.NON_NUMERIC));

            List<String> list = Arrays.asList(url, str);
            csv.printRecord(list);
            csv.close();
        }

        catch (Exception ex){
            ex.printStackTrace();

        }

    }

    // qiwei added for testing output
    public Map<String, String> toMapWithColumn (Article article, List<String> columns) {
        Map<String, String> props = new LinkedHashMap();
        for (String column: columns) {
            Object value = article.get(column);
            String finalValue = value == null ? "" : value.toString();
            props.put(column, finalValue);
        }

        return props;
    }

    @Override
    public void remove(String s, Properties properties) {

    }

    @Override
    public void commit() {

    }
}
