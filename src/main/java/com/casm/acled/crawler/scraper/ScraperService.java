package com.casm.acled.crawler.scraper;


import com.casm.acled.camunda.variables.Process;
import com.casm.acled.crawler.Crawl;
import com.casm.acled.crawler.reporting.*;
import com.casm.acled.crawler.scraper.dates.CompositeDateParser;
import com.casm.acled.crawler.scraper.dates.DateParser;
import com.casm.acled.crawler.scraper.keywords.KeywordsService;
import com.casm.acled.crawler.util.Util;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.CrawlReportDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.crawlreport.CrawlReport;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ibm.icu.util.ULocale;
import com.norconex.collector.core.CollectorException;
import com.norconex.collector.http.client.impl.GenericHttpClientFactory;
import com.norconex.collector.http.doc.HttpDocument;
import com.norconex.collector.http.fetch.HttpFetchResponse;
import com.norconex.collector.http.fetch.impl.GenericDocumentFetcher;
import com.norconex.collector.http.pipeline.importer.HttpImporterPipelineUtilProxy;
import com.norconex.commons.lang.io.CachedInputStream;
import com.norconex.commons.lang.io.CachedStreamFactory;
import com.opencsv.CSVReader;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ScraperService {

    protected static final Logger logger = LoggerFactory.getLogger(ScraperService.class);

    private static Path scraperDir = Paths.get("/home/sw206/git/acled-scrapers");

    @Autowired
    private SourceDAO sourceDAO;

    @Autowired
    private ArticleDAO articleDAO;

    @Autowired
    private Reporter reporter;

    @Autowired
    private KeywordsService keywordsService;

    @Autowired
    private CrawlReportDAO crawlReportDAO;


    public void checkScraperCoverage(Path scraperDir, SourceList sourceList) {

        List<Source> sources = sourceDAO.byList(sourceList);

        for(Source source: sources) {
//            if(!Util.isDisabled(source)) {
                if(Util.scraperExists(scraperDir, source)) {
                    reporter.report(Report.of(Event.SCRAPER_FOUND)
                            .id(source.id())
                            .message(source.get(Source.NAME))
                    );
                } else {
                    reporter.report(Report.of(Event.SCRAPER_NOT_FOUND)
                            .message(source.get(Source.LINK))
                            .id(source.id())
                    );
                }
//            }
        }
    }

    public void checkExampleURLs(Path scraperDir, SourceList sourceList) {

        List<Source> sources = sourceDAO.byList(sourceList);
        for(Source source : sources) {
            if(Util.scraperExists(scraperDir, source)) {

               checkExampleURLs(scraperDir, source);
            }
        }
    }


    public String getText(Source source, String url) {
//        String id = Util.getID(source);
        ACLEDScraper scraper = ACLEDScraper.load(scraperDir, source, reporter);

        HttpDocument document = scrapeURL(scraper, url);

        String article = document.getMetadata().getString(ScraperFields.SCRAPED_ARTICLE);

        return article;
    }

    public void checkExampleURLs(Path scraperDir, Source source) {
        String id = Util.getID(source);

        ACLEDScraper scraper = ACLEDScraper.load(scraperDir.resolve(id), source, reporter);
        checkExampleURLs(scraper, source);
    }

    public HttpDocument scrapeURL(ACLEDScraper scraper, String url) {
        GenericDocumentFetcher fetcher = new GenericDocumentFetcher();

        HttpClient client = new GenericHttpClientFactory().createHTTPClient("www.acleddata.com");
        CachedInputStream inputStream = new CachedStreamFactory(10 * 1096, 10 * 1096).newInputStream("");

        HttpDocument document = new HttpDocument(url, inputStream);
        try {

            HttpFetchResponse response = fetcher.fetchDocument(client, document);
            int statusCode = response.getStatusCode();
            if(statusCode >= 200 && statusCode < 300) {

                HttpImporterPipelineUtilProxy.enhanceHTTPHeaders(document.getMetadata());
                HttpImporterPipelineUtilProxy.applyMetadataToDocument(document);
                scraper.processDocument(client, document);

                return document;
            } else {
                throw new ReportingException(Report.of(Event.SCRAPE_ERROR).message("status %d - %s", statusCode, url));
            }
        } catch (CollectorException e) {

            logger.error(e.getMessage(), e);
            String staceTrace = ExceptionUtils.getStackTrace(e);
            throw new ReportingException(Report.of(Event.ERROR).message("%s - %s", url, staceTrace));
        }
    }

    public void checkExampleURLs(ACLEDScraper scraper, Source source) {

        GenericDocumentFetcher fetcher = new GenericDocumentFetcher();

        HttpClient client = new GenericHttpClientFactory().createHTTPClient("www.acleddata.com");
        CachedInputStream inputStream = new CachedStreamFactory(10 * 1024, 10 * 1024).newInputStream("");
        List<String> exampleURLs = source.get(Source.EXAMPLE_URLS);

        for(String exampleURL : exampleURLs) {
            try {

                HttpDocument document = new HttpDocument(exampleURL, inputStream);
                fetcher.fetchDocument(client, document);
                HttpImporterPipelineUtilProxy.enhanceHTTPHeaders(document.getMetadata());
                HttpImporterPipelineUtilProxy.applyMetadataToDocument(document);
                scraper.processDocument(client, document);
            } catch (IllegalStateException | CollectorException e){
                reporter.report(Report.of(Event.ERROR)
                        .type(Source.class.getName())
                        .id(source.id())
                        .message(e.getMessage())
                );
            }
        }
    }


    public void checkScraperFromFile(Path parent, Path path, Reporter reporter, int i) {

        String name = path.getFileName().toString();

        Map<String, String> lastScrape = Util.getLastScrape(path);

        String url = lastScrape.get("url");

        List<Source> sources = findSourceByURL(url);

        if(sources.size() != 1) {
            return;
        }
        if(url == null) {
            reporter.report(Report.of(Event.NO_EXAMPLES).message(name));
        } else {

//            Source dummySource = EntityVersions.get(Source.class).current()
//                    .id(0)
//                    .put(Source.LINK, name);

            Source source = sources.get(0);

            ACLEDScraper scraper = ACLEDScraper.load(parent, source, reporter);
//            String id = Util.getID(url);


            testScraper(scraper, url, source.id());
        }
    }

    public Article testScraper(ACLEDScraper scraper, String url, int id) {
        HttpDocument document;

        Article base = EntityVersions.get(Article.class).current();

        try {
            document = scrapeURL(scraper, url);
        } catch (ReportingException e) {
            reporter.report(e.get().id(id));
            return base;
        } catch (Exception e) {
            String staceTrace = ExceptionUtils.getStackTrace(e);
            reporter.report(Report.of(Event.SCRAPE_ERROR).id( id ).message("%s  - %s: %s", url, e.getMessage(), staceTrace));
            return base;
        }


        String text = document.getMetadata().getString(ScraperFields.SCRAPED_ARTICLE);
        String date = document.getMetadata().getString(ScraperFields.SCRAPED_DATE);
        String title = document.getMetadata().getString(ScraperFields.SCRAPED_TITLE);

        if(text != null && date != null && title != null) {
            //missing fields already reported
            reporter.report(Report.of(Event.SCRAPE_PASS).id(id).message("%s",url));
        } else {
            reporter.report(Report.of(Event.SCRAPE_FAIL).id(id).message("%s",url));
        }
        Article article = base.put(Article.URL, url);

        if(title != null) {
            article = article.put(Article.TITLE, title);
        }
        if(date != null) {
            article = article.put(Article.SCRAPE_DATE, date);
        }
        if(text != null) {
            article = article.put(Article.TEXT, text);
        }

        return article;
    }

    public void checkScrapersFromFile(Path path, Reporter reporter) {
        try {

            AtomicInteger i = new AtomicInteger(0);

            Files.walk(path, 1).forEach(p -> {
                try {

                    checkScraperFromFile(path, p, reporter, i.getAndIncrement());
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    String staceTrace = ExceptionUtils.getStackTrace(e);
                    reporter.report(Report.of(Event.ERROR).message("%s - %s", p.toString(), staceTrace));
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Source> findSourceByURL(String url) {

        Source blank = EntityVersions.get(Source.class).current();
        Source search = EntityVersions.get(Source.class).current()
                .put(Source.LINK,  Util.getDomain(url) );

        List<Source> sources = sourceDAO.search(blank, blank, search);

        return sources;
    }

    public List<Map<String,String>> getEvalData(Path csvPath) {

        try (
            Reader reader = java.nio.file.Files.newBufferedReader(csvPath);
            CSVReader csvReader = new CSVReader(reader);
        ) {
            Iterator<String[]> itr = csvReader.iterator();
            String[] headers = itr.next();

            List<Map<String,String>> evalData = new ArrayList<>();

            Map<String,Integer> headerMap = new HashMap<>();

            for(int i = 0; i < headers.length; ++i) {
                headerMap.put(headers[i], i);
            }


            while(itr.hasNext()) {
                String[] row = itr.next();

                Map<String,String> datum = ImmutableMap.<String,String>builder()
                    .put("STANDARD_NAME", row[headerMap.get("Standard Source Name")])
                    .put("TEXT", row[headerMap.get("Article Text")])
                    .put("DATE", row[headerMap.get("Article Date")])
                    .put("TITLE", row[headerMap.get("Article Title")])
                    .put("URL", row[headerMap.get("Article URL")])
                    .put("USED", row[headerMap.get("Article Used in Coding (Y/N)")])
                .build();

                evalData.add(datum);
            }

            return evalData;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean toBool(String YN) {
        if(YN.equalsIgnoreCase("Y")) {
            return true;
        } else {
            return false;
        }
    }

    public void importEvalCsv(Path csv, String businessKey) {
        List<Article> articles = evalCsv2Articles(csv);
        ListIterator<Article> itr = articles.listIterator();
        while(itr.hasNext()) {
            Article article = itr.next();
            article = article.put(Process.BUSINESS_KEY, businessKey);
            itr.set(article);
        }
        articleDAO.create(articles);
    }


    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yy");

    public List<Article> evalCsv2Articles(Path csv) {
        List<Map<String,String>> data = getEvalData(csv);

        Article base = EntityVersions.get(Article.class).current();

        List<Article>  articles = new ArrayList<>();

        for(Map<String,String> datum : data) {

            String url = datum.get("URL");
            String name = datum.get("STANDARD_NAME");

            Optional<Source> maybeSource = sourceDAO.byName(name);

            Article article = base
                .put(Article.TEXT, datum.get("TEXT"))
                .put(Article.URL, url)
                .put(Article.TITLE, datum.get("TITLE"))
                .put(Article.DATE, formatter.parse(datum.get("DATE")))
                .put(Article.NOTES, name);

            if(maybeSource.isPresent()) {
                Source source = maybeSource.get();
                article = article.put(Article.SOURCE_ID, source.id());
            }
            articles.add(article);

        }

        return articles;
    }

    public List<Article> matchArticlesByUrl(Article article) {

        List<Article> matches = articleDAO.getBy(Article.URL, article.get(Article.URL))
                .stream()
                .filter(a -> !a.hasBusinessKey() || !a.businessKey().equalsIgnoreCase(article.businessKey()))
                .collect(Collectors.toList());



        return matches;
    }

    public Map<Integer, CrawlReport> getReportIds(String runId) {
        Map<Integer, CrawlReport> ids = crawlReportDAO.getBy(CrawlReport.RUN_ID, runId)
                .stream().collect(Collectors.toMap(r -> r.get(CrawlReport.ID), r->r));

        return ids;
    }

    public List<Article> getRemaining(String runId, Event event) {
        List<CrawlReport> remaining = crawlReportDAO.getBy(CrawlReport.RUN_ID, runId)
                .stream().filter(r -> r.get(CrawlReport.EVENT).equals(event.toString())).collect(Collectors.toList());

        List<Article> articles = remaining.stream().map(r -> articleDAO.getById((Integer)r.get(CrawlReport.ID)).get()).collect(Collectors.toList());

        return articles;
    }

    public List<Article> getRemainingInverted(String runId, Event event) {
        List<CrawlReport> remaining = crawlReportDAO.getBy(CrawlReport.RUN_ID, runId)
                .stream().filter(r -> !r.get(CrawlReport.EVENT).equals(event.toString())).collect(Collectors.toList());

        List<Article> articles = remaining.stream().map(r -> articleDAO.getById((Integer)r.get(CrawlReport.ID)).get()).collect(Collectors.toList());

        return articles;
    }

    public void matchArticlesByUrl(List<Article> articles) {

        for(Article article : articles) {

            List<Article> matches = matchArticlesByUrl(article);

            if(matches.isEmpty()) {
                reporter.report(Report.of(Event.ARTICLE_NO_MATCH).id(article.id()).message(article.get(Article.URL)));
            } else if(matches.size() == 1) {
                reporter.report(Report.of(Event.ARTICLE_MATCH).id(article.id()).message(article.get(Article.URL)));
            } else {
                reporter.report(Report.of(Event.ARTICLE_TOO_MANY_MATCHES).id(article.id()).message(article.get(Article.URL)));
            }
        }
    }

    public void checkArticlesSource(List<Article> articles) {

        for(Article article : articles) {
            if(article.hasValue(Article.SOURCE_ID)) {
                reporter.report(Report.of(Event.SOURCE_FOUND).id(article.id()).message(article.get(Article.URL)));
            } else {
                reporter.report(Report.of(Event.SOURCE_NOT_FOUND).id(article.id()).message(article.get(Article.URL)));
            }
        }
    }


    public void checkArticlesScraperExists(Path scraperDir, List<Article> articles) {
        for(Article article : articles) {
            checkArticleScraperExists(scraperDir, article);
        }
    }
    public void checkArticleScraperExists(Path scraperDir, Article article) {

        Source source = sourceDAO.getById(article.get(Article.SOURCE_ID)).get();

        if(Util.scraperExists(scraperDir, source)) {
            reporter.report(Report.of(Event.SCRAPER_FOUND).id(article.id()).message(article.get(Article.URL)));
        } else {
            reporter.report(Report.of(Event.SCRAPER_NOT_FOUND).id(article.id()).message(article.get(Article.URL)));
        }
    }

    public void checkArticlesScraperFunction(Path scraperDir, List<Article> articles) {

        for(Article article : articles) {
            checkArticleScraperFunction(scraperDir, article);
        }
    }
    public Article checkArticleScraperFunction(Path scraperDir, Article article) {

        Source source = sourceDAO.getById(article.get(Article.SOURCE_ID)).get();

        Reporter reporter = new InMemoryReporter();

        ACLEDScraper scraper = ACLEDScraper.load(scraperDir, source, reporter);

        String url = article.get(Article.URL);
        int id = article.id();

        Article scraped = testScraper(scraper, url, id);

        if(scraped.hasValue(Article.SCRAPE_DATE)) {
            article = article.put(Article.SCRAPE_DATE, scraped.get(Article.SCRAPE_DATE));
            articleDAO.update(article);
        }

        return scraped;
    }

    private List<ULocale> getLocales(Source source) {
        return ((List<String>)source.get(Source.LOCALES)).stream().map(ULocale::new).collect(Collectors.toList());
    }

    private boolean dateInRange(LocalDateTime localDateTime, Map<LocalDateTime, LocalDateTime> acceptableRanges) {
        for(Map.Entry<LocalDateTime, LocalDateTime> entry : acceptableRanges.entrySet()) {

            if(localDateTime.isAfter(entry.getKey()) && localDateTime.isBefore(entry.getValue())) {
                return true;
            }
        }

        return false;
    }

    public void checkArticlesDateParse(List<Article> articles, Map<LocalDateTime, LocalDateTime> acceptableRanges) {
        for(Article article : articles) {

            checkArticleDateParse(article, acceptableRanges);
        }
    }
    public void checkArticleDateParse(Article article, Map<LocalDateTime, LocalDateTime> acceptableRanges) {

        int id = article.id();
        String url = article.get(Article.URL);
        Source source = sourceDAO.getById(article.get(Article.SOURCE_ID)).get();

        List<String> dateFormatSpecs = source.get(Source.DATE_FORMAT);

        if(dateFormatSpecs == null || dateFormatSpecs.isEmpty()) {
            reporter.report(Report.of(Event.DATE_PARSER_NOT_FOUND).id(id).message(url));
            return;
        }

        DateParser dateParser = CompositeDateParser.of(dateFormatSpecs);

        dateParser = dateParser.locale(getLocales(source));

        String rawDate = article.get(Article.SCRAPE_DATE);

        Optional<LocalDateTime> maybeDateTime = dateParser.parse(rawDate);

        if(maybeDateTime.isPresent()) {

            if(dateInRange(maybeDateTime.get(), acceptableRanges)) {

                reporter.report(Report.of(Event.DATE_PARSE_SUCCESS).id(id).message(url));
            } else {

                reporter.report(Report.of(Event.DATE_PARSE_INCORRECT).id(id).message(url));
            }

        } else {

            reporter.report(Report.of(Event.DATE_PARSE_FAILED).id(id).message(url));
        }

    }


    public void eval(Path scrapers, Path csv, SourceList list) {
        List<Map<String,String>> evalData = getEvalData(csv);
        String query = keywordsService.getKeyword(list);

        for(Map<String,String> datum : evalData) {

            String name = datum.get("STANDARD_NAME");
            Optional<Source> maybeSource = sourceDAO.byName(name);

            if(maybeSource.isPresent()) {
                Source source = maybeSource.get();

                if(Util.scraperExists(scrapers,source) ) {

                    ACLEDScraper scraper = ACLEDScraper.load(scrapers, source, reporter);

                    String url = datum.get("URL");
                    boolean used = toBool(datum.get("USED"));

                    try {

                        Article article = testScraper(scraper, url, source.id());

                        if(article.hasValue(Article.TEXT)) {
                            String text = article.get(Article.TEXT);
                            boolean matched = keywordsService.test(query, text);
                            if(matched && used) {
                                reporter.report(Report.of(Event.SCRAPE_TEST_TP).id(source.id()).message("%s %s", name, url));
                            } else if(!matched && used) {
                                reporter.report(Report.of(Event.SCRAPE_TEST_FN).id(source.id()).message("%s %s", name, url));
                            } else if(matched && !used) {
                                reporter.report(Report.of(Event.SCRAPE_TEST_FP).id(source.id()).message("%s %s", name, url));
                            } else if(!matched && !used) {
                                reporter.report(Report.of(Event.SCRAPE_TEST_TN).id(source.id()).message("%s %s", name, url));
                            }
                        }
                    } catch (ReportingException e) {
                        reporter.report(e.get().id(source.id()));
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                        String staceTrace = ExceptionUtils.getStackTrace(e);
                        reporter.report(Report.of(Event.ERROR).id(source.id()).message("%s - %s", url, staceTrace));
                    }
                } else {
                    reporter.report(Report.of(Event.SCRAPER_NOT_FOUND).id(source.id()).message(name));
                }
            } else {
                reporter.report(Report.of(Event.SOURCE_NOT_FOUND).message(name));
            }

        }

    }

    public void outputResults(Path path, Map<Article, CrawlReport> results) {

        List<String> headers = ImmutableList.of(
                "ID",
                "URL",
                "TEXT",
                "TITLE",
                "DATE",
                "SCRAPE_DATE",
                "EVENT",
                "MESSAGE"
        );
        try (
                final PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(path, StandardOpenOption.CREATE_NEW), StandardCharsets.UTF_8)), false);
                final CSVPrinter csv = new CSVPrinter(writer, CSVFormat.EXCEL.withQuoteMode(QuoteMode.NON_NUMERIC));
        ) {
            csv.printRecord(headers);

            for (Map.Entry<Article, CrawlReport> e : results.entrySet()) {

                Article a = e.getKey();
                CrawlReport c = e.getValue();
                String ID= Integer.toString(a.id());
                String URL= a.get(Article.URL);
                String TEXT= a.get(Article.TEXT);
                String TITLE = a.get(Article.TITLE);
                String DATE = a.get(Article.DATE);
                String SCRAPE_DATE = a.get(Article.SCRAPE_DATE);
                String EVENT = c.get(CrawlReport.EVENT);
                String MESSAGE = c.get(CrawlReport.MESSAGE);


                List<String> row = new ArrayList<>();
                row.add(ID);
                row.add(URL);
                row.add(TEXT);
                row.add(TITLE);
                row.add(DATE);
                row.add(SCRAPE_DATE);
                row.add(EVENT);
                row.add(MESSAGE);

                csv.printRecord( row );
            }
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
