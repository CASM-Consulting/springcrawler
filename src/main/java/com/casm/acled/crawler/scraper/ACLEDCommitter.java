package com.casm.acled.crawler.scraper;

import com.casm.acled.crawler.reporting.Event;
import com.casm.acled.crawler.reporting.Report;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.scraper.dates.ExcludingCustomDateMetadataFilter;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.source.Source;
import com.google.common.base.Strings;
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
import java.time.Instant;
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

import static com.casm.acled.crawler.reporting.Event.*;
import static com.casm.acled.crawler.scraper.ScraperFields.*;


public class ACLEDCommitter implements ICommitter {

    protected static final Logger logger = LoggerFactory.getLogger(ACLEDImporter.class);

    private final ArticleDAO articleDAO;
    private final Source source;
    private Integer maxArticles;
    private final SourceListDAO sourceListDAO;
    private final boolean sourceRequired;
    private Reporter reporter;
    private final boolean recordRaw;

    private Supplier<HttpCollector> collectorSupplier;


    public ACLEDCommitter(ArticleDAO articleDAO, Source source,
                          SourceListDAO sourceListDAO, boolean sourceRequired, boolean recordRaw,
                          Reporter reporter) {

        this.articleDAO = articleDAO;
        this.source = source;
        this.sourceListDAO = sourceListDAO;
        this.sourceRequired = sourceRequired;
        this.reporter = reporter;
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
        
        String url = properties.getString("document.reference");
        Report report = Report.of(source.id(), Source.class, ACLEDCommitter.class, url);

        String articleText = properties.getString(SCRAPED_ARTICLE);
        String title = properties.getString(SCRAPED_TITLE);
        String date = properties.getString(SCRAPED_DATE);
        String standardDate = properties.getString( STANDARD_DATE);

        boolean keywordPassed = properties.getBoolean(KEYWORD_PASSED);
        boolean datePassed = properties.getBoolean(DATE_PASSED);
        boolean scrapedPassed = keywordPassed && datePassed;

        /* =============================
         * Reporting
         * ========================== */
        // All references reaching this stage counted as "accepted", but they may not be committed (SCRAPE_PASS).
        reporter.report(report.event(REFERENCE_ACCEPTED));
        // Report whether article text was scraped
        if (Strings.isNullOrEmpty(articleText)){
            reporter.report(report.event(Event.SCRAPE_NO_ARTICLE));
        } else {
            // We scraped, so report whether keyword passing
            reporter.report(keywordPassed?
                    report.event(QUERY_MATCH) :
                    report.event(QUERY_NO_MATCH));
        }
        // Report whether article title was scraped
        if (Strings.isNullOrEmpty(title)){
            reporter.report(report.event(SCRAPE_NO_TITLE));
        }
        // Report whether article date was scraped
        if (Strings.isNullOrEmpty(date)){
            reporter.report(report.event(SCRAPE_NO_DATE));
        } else {
            // We scraped, so report whether article date was parsed
            if (Strings.isNullOrEmpty(standardDate)){
                reporter.report(report.event(DATE_PARSE_FAILED));
            } else {
                // We parsed, so report whether date within correct period
                reporter.report(datePassed?
                        report.event(DATE_MATCH) :
                        report.event(DATE_NO_MATCH));
            }
        }
        // Report whether article will be committed
        reporter.report(scrapedPassed?
                report.event(SCRAPE_PASS) :
                report.event(SCRAPE_FAIL));
        /* ============================ */

        if (scrapedPassed) {

            // qiwei added for record writing
            StringWriter writer = new StringWriter();
            try {
                IOUtils.copy(inputStream, writer, properties.getString("document.contentEncoding"));
            } catch (Exception ex) {
                throw new RuntimeException("ERROR: Failed to retrieve web content for url: " + url);
            }

            String rawHtml = writer.toString();

            Article article = EntityVersions.get(Article.class)
                    .current();

            if (title != null) {
                article = article.put(Article.TITLE, title);
            }

            article = article.put(Article.TEXT, articleText)
                    .put(Article.SCRAPE_DATE, date)
                    .put(Article.URL, url);

            if (properties.getString(ScraperFields.KEYWORD_HIGHLIGHT) != null) {
                article = article.put(Article.SCRAPE_KEYWORD_HIGHLIGHT, properties.getString(ScraperFields.KEYWORD_HIGHLIGHT));
            }

            if (standardDate != null) {
                LocalDateTime parsedDate = ExcludingCustomDateMetadataFilter.toDate(standardDate);
                article = article.put(Article.DATE, parsedDate.toLocalDate());
            }

            if (recordRaw) {
                article = article.put(Article.SCRAPE_RAW_HTML, rawHtml);
            }

            article = article.put(Article.CRAWL_DEPTH, properties.getInt("collector.depth"));
            article = article.put(Article.CRAWL_DATE, LocalDate.now());

            if (!stopAfterNArticlesFromSource(source)) {
                article = article.put(Article.SOURCE_ID, source.id());
                articleDAO.create(article);
            }
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
