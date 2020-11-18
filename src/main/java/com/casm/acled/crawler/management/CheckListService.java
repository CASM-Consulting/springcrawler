package com.casm.acled.crawler.management;

import com.casm.acled.crawler.management.checks.Check;
import com.casm.acled.crawler.management.checks.CheckList;
import com.casm.acled.crawler.reporting.Event;
import com.casm.acled.crawler.reporting.Report;
import com.casm.acled.crawler.reporting.ReportQueryService;
import com.casm.acled.crawler.reporting.ReportQueryService.EventCountSummary;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.scraper.ACLEDCommitter;
import com.casm.acled.crawler.scraper.ACLEDScraper;
import com.casm.acled.crawler.scraper.ACLEDTagger;
import com.casm.acled.crawler.scraper.ScraperFields;
import com.casm.acled.crawler.scraper.ScraperService;
import com.casm.acled.crawler.scraper.dates.DateParser;
import com.casm.acled.crawler.scraper.dates.DateParsers;
import com.casm.acled.crawler.scraper.dates.DateTimeService;
import com.casm.acled.crawler.spring.CrawlService;
import com.casm.acled.crawler.util.Util;
import com.casm.acled.dao.entities.*;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.VersionedEntity;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.norconex.collector.http.doc.HttpDocument;
import com.norconex.importer.handler.tagger.impl.DOMTagger;
import org.apache.commons.csv.*;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.casm.acled.crawler.scraper.ScraperFields.*;

import org.springframework.shell.table.ArrayTableModel;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;

@Service
public class CheckListService {

    protected static final Logger logger = LoggerFactory.getLogger(CheckListService.class);

    @Autowired
    private CheckListService checkListService;

    @Autowired
    private DateTimeService dateTimeService;

    @Autowired
    private ScraperService scraperService;

    @Autowired
    private CrawlService crawlService;

    @Autowired
    private Reporter reporter;

    @Autowired
    private SourceDAO sourceDAO;

    @Autowired
    private SourceListDAO sourceListDAO;

    @Autowired
    private SourceSourceListDAO sourceSourceListDAO;

    @Autowired
    private ArticleDAO articleDAO;

    @Autowired
    private ReportQueryService reportQueryService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private EmailService emailService;


    private void attemptAllScrapers() {
        dateTimeService.setScrapersPath(Paths.get("allscrapers"));

        dateTimeService.attemptAllDateTimeParsers(DateParsers.ALL, DateTimeService.lastScrapeExampleGetter(Paths.get("/home/sw206/git/acled-scrapers")));

//        System.out.println(reporter.reports(r->r.event().equals(Event.DATE_PARSE_SUCCESS.name())));
//        System.out.println(reporter.reports(r->r.event().equals(Event.DATE_PARSE_FAILED.name())));
    }

    private Function<Source, List<String>> getFromArticles = s -> {
        List<Article> articles = articleDAO.bySource(s);
        return articles.stream()
                .filter(a -> a.hasValue(Article.SCRAPE_DATE))
                .map(a -> (String)a.get(Article.SCRAPE_DATE))
                .collect(Collectors.toList());
    };


    private void attemptSourceListExistingArticles(String name) {
        SourceList sourceList = sourceListDAO.getByUnique(SourceList.LIST_NAME, name).get();
        dateTimeService.attemptSourceListDateTimeParsers(sourceList, DateParsers.ALL, getFromArticles);

        for(Report report : reporter.reports()) {
            logger.info(report.toString());
        }

//        System.out.println(reporter.reports(r->r.event().equals(Event.DATE_PARSE_SUCCESS.name())));
//        System.out.println(reporter.reports(r->r.event().equals(Event.DATE_PARSE_FAILED.name())));
    }


    public void attemptSource(int id, DateParser dateParser) {

        Source source = sourceDAO.getById(id).get();
        dateTimeService.attemptDateTimeParse(source, ImmutableList.of(dateParser), getFromArticles);
    }


    public boolean checkSchedule(Source source) {
        if(source.hasValue(Source.CRAWL_SCHEDULE) && source.hasValue(Source.TIMEZONE)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean checkConnection(Source source) {
        List<String> seedUrls = source.get(Source.SEED_URLS);

        String[] startURLs;

        if(seedUrls != null && !seedUrls.isEmpty()) {
            startURLs = seedUrls.toArray(new String[]{});
        } else {
            startURLs = ((String) source.get(Source.LINK)).split(",");
        }
        String url = startURLs[0];

        Client client = ClientBuilder.newClient();

        boolean pass = true;

//        if (url==null) {
//            return false;
//        }

//        target.property(ClientProperties.FOLLOW_REDIRECTS, Boolean.TRUE);
        try {

            WebTarget target = client.target(url);

            Invocation.Builder invocationBuilder = target.request(MediaType.TEXT_HTML);

            invocationBuilder.get(String.class);
        } catch ( WebApplicationException e) {
            if(e.getResponse().getStatus() >= 300 && e.getResponse().getStatus() < 400) {
                String redirect = (String)e.getResponse().getHeaders().getFirst("Location");

                reporter.report(Report.of(Event.SOURCE_LINK_REDIRECT).id(source.id()).message(url + " -> " + redirect));
            } else {

                reporter.report(Report.of(Event.SOURCE_LINK_INVALID).id(source.id()).message(url + " : " + e.getResponse().getStatus()));
            }
//            logger.warn(url + " : " + e.getMessage());
            pass = false;
        } catch ( IllegalArgumentException | ProcessingException e ) {

            reporter.report(Report.of(Event.SOURCE_LINK_INVALID).id(source.id()).message(url + " : " + e.getMessage()));
            pass = false;
        }
        return pass;
    }

    public boolean scraperExists(CrawlArgs args, Source source) {
        if(Util.scraperExists(args.scrapersDir, source)) {
            return true;
        } else {
            reporter.report(Report.of(Event.SCRAPER_NOT_FOUND).id(source.id()).message(source.get(Source.STANDARD_NAME)));
            return false;
        }
    }

    public boolean hasExamples(Source source) {
        List<String> exampleURLs = source.get(Source.EXAMPLE_URLS);

        if(exampleURLs == null || exampleURLs.isEmpty()) {
            reporter.report(Report.of(Event.NO_EXAMPLES).id(source.id()).message(source.get(Source.STANDARD_NAME)));
            return false;
        } else {
            return true;
        }
    }

    public boolean hasDateFormat(Source source) {
        List<String> formats = source.get(Source.DATE_FORMAT);

        if(formats == null || formats.isEmpty()) {
            reporter.report(Report.of(Event.DATE_PARSER_NOT_FOUND).id(source.id()).message(source.get(Source.STANDARD_NAME)));
            return false;
        } else {
            return true;
        }
    }

    public final Function<Source, List<String>> exampleGetter (ACLEDScraper scraper) {
        return (s) -> {

            List<HttpDocument> docs = scraperService.checkExampleURLs(scraper, s);
            List<String> dateExamples = docs.stream()
                    .filter(doc -> doc.getMetadata().containsKey(ScraperFields.SCRAPED_DATE) &&
                            !doc.getMetadata().getString(ScraperFields.SCRAPED_DATE).isEmpty())
                    .map(doc -> doc.getMetadata().getString(ScraperFields.SCRAPED_DATE))
                    .collect(Collectors.toList());

            return dateExamples;
        };
    }

    public boolean datesParse(CrawlArgs args, Source source) {
        ACLEDScraper scraper = ACLEDScraper.load(args.scrapersDir, source, reporter);

        ACLEDTagger acledTagger = new ACLEDTagger(args.scrapersDir, source);

//        DOMTagger domTagger = acledTagger.get();

        return dateTimeService.checkExistingPasses(source, exampleGetter(scraper));
    }


    public void checkSource(CrawlArgs args, Source source)  {

        boolean passed = false;

        boolean connection = checkConnection(source);
        boolean scraperExists = scraperExists(args, source);
        boolean hasExamples = hasExamples(source);
        boolean hasDateFormat = hasDateFormat(source);
        List<String> sitemaps = crawlService.getSitemaps(source);


        if(!sitemaps.isEmpty()) {
            reporter.report(Report.of(Event.HAS_SITE_MAPS).id(source.id()).message(source.get(Source.STANDARD_NAME)));

        }

        if(hasDateFormat && hasExamples && scraperExists) {

            boolean datesParsed = datesParse(args, source);

            if(datesParsed) {
                passed = true;
                reporter.report(Report.of(Event.SCRAPE_PASS).id(source.id()).message(source.get(Source.STANDARD_NAME)));
            }
        }

        if(!connection) {
            passed = false;
        }

        if(args.flagSet.contains(CrawlArgs.Flags.DISABLE_ON_FAIL) ) {
            if(passed) {
                source = source.put(Source.CRAWL_DISABLED, false);
            } else {
                source = source.put(Source.CRAWL_DISABLED, true);
            }
            sourceDAO.upsert(source);
        }

    }

    public static boolean passed(Object maybeBoolean){
        try {
            Boolean b = (Boolean)maybeBoolean;
            return b != null? b : false;
        } catch (ClassCastException e){
            return maybeBoolean instanceof String && ((String) maybeBoolean).startsWith("N/A");
        }
    }

    public CheckList checkSourceStatus2(CrawlArgs args, Source source){
        System.out.println("Checking source: " + source.get(Source.STANDARD_NAME));

        Check connection = Check.bool(() -> checkConnection(source));
        Check scraperExists = Check.bool(() -> scraperExists(args, source));
        Check hasExamples = Check.bool(() -> hasExamples(source));
        Check hasDateFormat = Check.bool(()-> hasDateFormat(source));

        Check hasSitemaps = Check.notApplicable("off");
        Check hasRecentSitemaps = Check.notApplicable();
        if (source.isFalse(Source.CRAWL_DISABLE_SITEMAPS)) {
            try {
                List<String> sitemaps = crawlService.getSitemaps(source);
                hasSitemaps = Check.bool(() -> !sitemaps.isEmpty());
                if (hasSitemaps.isPass()) {
                    hasRecentSitemaps = Check.bool(() -> !crawlService.recentSitemapURLs(source.get(Source.LINK), sitemaps).isEmpty());
                }
            } catch (Exception e){
                hasSitemaps = Check.failed(e.getMessage());
            }
        }

        boolean canScrapeExamples = Check.allPass(hasExamples, connection, scraperExists);

        Check dateParsed = Check.bool(()-> canScrapeExamples && datesParse(args, source));

        List<HttpDocument> scraped = canScrapeExamples? scraperService.checkExampleURLs(args.scrapersDir, source) : new ArrayList<>();

        Check titleScraped = Check.bool(() -> canScrapeExamples && scraped.stream().noneMatch(doc -> Strings.isNullOrEmpty(doc.getMetadata().getString(SCRAPED_TITLE))));
        Check dateScraped = Check.bool(() -> canScrapeExamples && scraped.stream().noneMatch(doc -> Strings.isNullOrEmpty(doc.getMetadata().getString(SCRAPED_DATE))));
        Check articleScraped = Check.bool(() -> canScrapeExamples && scraped.stream().noneMatch(doc -> Strings.isNullOrEmpty(doc.getMetadata().getString(SCRAPED_ARTICLE))));

        return CheckList.of(connection, scraperExists, hasExamples, hasDateFormat,
                            hasSitemaps, hasRecentSitemaps, dateScraped, dateParsed,
                            titleScraped, articleScraped);
    }

    @Deprecated
    public String [] checkSourceStatus(CrawlArgs args, Source source)  {

        System.out.println("Checking source: " + source.get(Source.STANDARD_NAME));

        Object connection;
        Object scraperExists;
        Object hasExamples;
        Object hasDateFormat;
        Object hasSiteMaps;
        Object hasRecentSiteMaps = "N/A";
        Object dateParsed;

        Object titleScraped;
        Object dateScraped;
        Object articleScraped;

        List<String> checkValue = new ArrayList<>();

        try {
            connection = checkConnection(source);
        }
        catch (Exception e) {
            connection = e.getMessage();
        }

        try {
            scraperExists = scraperExists(args, source);
        }
        catch (Exception e) {
            scraperExists = e.getMessage();
        }

        try {
            hasExamples = hasExamples(source);
        }
        catch (Exception e) {
            hasExamples = e.getMessage();
        }

        try {
            hasDateFormat = hasDateFormat(source);
        }
        catch (Exception e) {
            hasDateFormat = e.getMessage();
        }

        try {
            if (source.get(Source.CRAWL_DISABLE_SITEMAPS)){
                hasSiteMaps = "N/A (off)";
            } else {
                List<String> sitemaps = crawlService.getSitemaps(source);
                hasSiteMaps = !sitemaps.isEmpty();
                if ((boolean) hasSiteMaps) {
                    try {
                        Set<String> recent = crawlService.recentSitemapURLs(source.get(Source.LINK), sitemaps);
                        hasRecentSiteMaps = !recent.isEmpty();
                    } catch (Exception e) {
                        hasRecentSiteMaps = e.getMessage();
                    }
                }
            }
        }
        catch (Exception e) {
            hasSiteMaps = e.getMessage();
        }

        if (passed(hasExamples) && passed(connection) && passed(scraperExists)){

            if (passed(hasDateFormat)) {
                try {
                    dateParsed = datesParse(args, source);
                } catch (Exception e) {
                    dateParsed = e.getMessage();
                }
            } else {
                dateParsed = false;
            }

            List<HttpDocument> scraped = scraperService.checkExampleURLs(args.scrapersDir, source);

            titleScraped = dateScraped = articleScraped = true;
            for (HttpDocument doc : scraped){
                if (Strings.isNullOrEmpty(doc.getMetadata().getString(SCRAPED_TITLE))){
                    titleScraped = false;
                }
                if (Strings.isNullOrEmpty(doc.getMetadata().getString(SCRAPED_DATE))){
                    dateScraped = false;
                }
                if (Strings.isNullOrEmpty(doc.getMetadata().getString(SCRAPED_ARTICLE))){
                    articleScraped = false;
                }
            }

        } else {
            dateParsed = false;
            titleScraped = false;
            dateScraped = false;
            articleScraped = false;
        }

        boolean passed = passed(dateParsed) // implies connection, scraperExists, hasDateFormat, and dateScraped
                            && passed(titleScraped)
                            && passed(articleScraped)
                            && passed(hasSiteMaps)
                            && passed(hasRecentSiteMaps);

        checkValue.add(source.get(Source.STANDARD_NAME));
        checkValue.add(String.valueOf(connection));
        checkValue.add(String.valueOf(scraperExists));
        checkValue.add(String.valueOf(hasExamples));
        checkValue.add(String.valueOf(hasDateFormat));
        checkValue.add(String.valueOf(hasSiteMaps));
        checkValue.add(String.valueOf(hasRecentSiteMaps));
        checkValue.add(String.valueOf(dateScraped));
        checkValue.add(String.valueOf(dateParsed));
        checkValue.add(String.valueOf(titleScraped));
        checkValue.add(String.valueOf(articleScraped));
        checkValue.add(String.valueOf(passed));



        return checkValue.toArray(new String[checkValue.size()]);

    }

    public String checkSourceList(CrawlArgs args) {
        String [] header = {"Source ID", "connection", "scraperExists", "hasExamples", "hasDateFormat", "hasSiteMaps",
                "hasRecentSitemaps", "dateScraped", "dateParsed", "titleScraped", "articleScraped", "passed"};
//        String [] header = {"Source ID", "hasSiteMaps"};
        String [][] content = new String[][] {header};

        String name = "Source(s)";
        List<Source> sources = new ArrayList<>();
        if (args.source != null){
            sources.add(args.source);
            name = args.source.get(Source.STANDARD_NAME);
        } else if (args.sourceLists != null && !args.sourceLists.isEmpty()) {
            SourceList sourceList = args.sourceLists.get(0);
            name = sourceList.get(SourceList.LIST_NAME);
            sources.addAll(sourceDAO.byList(sourceList));
        }
        if (sources.isEmpty()){
            throw new RuntimeException("No source list or source specified.");
        }

        boolean anyFailed = false;

        for(Source source : sources) {

//            checkSource(args, source);
            // for debugging, use only one source;
//            if (source.get(Source.STANDARD_NAME).equals("Imagen del Golfo")) {
//                String [] checkArray = checkSourceStatus(args, source);
//                if (checkArray.length==0) {
//                    continue;
//                }
//                content = insertRow(content,content.length, checkArray);
//
//            }
//            String [] checkArray = checkSourceStatus(args, source);
//            if (checkArray.length==0) {
//                continue;
//            }
//            content = insertRow(content,content.length, checkArray);

            CheckList checks = checkSourceStatus2(args, source);

            if (!checks.isPass()){
                anyFailed = true;
            }


            String[] checkArray = checks.toTableRow(source.get(Source.STANDARD_NAME), true);
            content = insertRow(content, content.length, checkArray);

        }

        TableModel model = new ArrayTableModel(content);
        TableBuilder tableBuilder = new TableBuilder(model);
        tableBuilder.addFullBorder(BorderStyle.fancy_light);
        String table = tableBuilder.build().render(80);
        System.out.println(table);

        if (anyFailed && configService.isEmailConfigured()){

            emailService.sendHtmlMessage(configService.getEmail(), name + " failing", "<pre>"+table+"</pre>");
        }

        return table;
    }

    public void testURL(Source source, SourceList list, String url) {
//        scraperService.getText()

    }

    public static String[][] insertRow(String[][] m, int r, String[] data) {
        String[][] out = new String[m.length + 1][];
        for (int i = 0; i < r; i++) {
            out[i] = m[i];
        }
        out[r] = data;
        for (int i = r + 1; i < out.length; i++) {
            out[i] = m[i - 1];
        }
        return out;
    }

    public void checkCrawlReports(CrawlArgs crawlArgs, int numRuns) {

        if (crawlArgs.source!=null) {
            Source source = crawlArgs.source;
            checkSourceCrawlReports(source, numRuns);

        }
        else if (!crawlArgs.sourceLists.isEmpty()) {
            SourceList sourceList = crawlArgs.sourceLists.get(0);
            checkSourceListCrawlReports(sourceList, numRuns);

        }
        else {
            throw new RuntimeException("source or sourcelist should be provided");
        }

    }

    public void checkSourceCrawlReports(Source source, int numRuns){

        System.out.println("Source: " + source.get(Source.NAME));

        String [] header = {"Run ID", "References", "Committed", "No Keyword Match", "Date Irrelevant", "Date Parse Failed", "Date Missing", "Text Missing"};
        String [][] content = new String[][] {header};

        Map<String, EventCountSummary> summaryPerRun = reportQueryService
                .summaryPerRun(source.id(), numRuns);

        for (Map.Entry<String, EventCountSummary> entry : summaryPerRun.entrySet()) {

            EventCountSummary summary = entry.getValue();

            String[] data = new String[]{
                    entry.getKey(),
                    Integer.toString(summary.getCount(Event.REFERENCE_ACCEPTED)),
                    Integer.toString(summary.getCount(Event.SCRAPE_PASS)),
                    Integer.toString(summary.getCount(Event.QUERY_NO_MATCH)),
                    Integer.toString(summary.getCount(Event.DATE_NO_MATCH)),
                    Integer.toString(summary.getCount(Event.DATE_PARSE_FAILED)),
                    Integer.toString(summary.getCount(Event.SCRAPE_NO_DATE)),
                    Integer.toString(summary.getCount(Event.SCRAPE_NO_ARTICLE)),
            };

            content = insertRow(content, content.length, data);
        }

        TableBuilder tableBuilder = new TableBuilder(new ArrayTableModel(content));
        tableBuilder.addFullBorder(BorderStyle.fancy_light);
        System.out.println(tableBuilder.build().render(100));
    }

    public void checkSourceListCrawlReports(SourceList sourceList, int numRuns) {

        String [] header = {"Source", "Runs", "References", "Committed", "No Keyword Match", "Date Irrelevant", "Date Parse Failed", "Date Missing", "Text Missing"};
        String [][] content = new String[][] {header};

        for (Source source : sourceDAO.byList(sourceList)){

            Map<String, EventCountSummary> runToSummary = reportQueryService.summaryPerRun(source.id(), numRuns);

            String[] data;
            if (runToSummary.isEmpty()){
                data = new String[]{source.get(Source.STANDARD_NAME), "0", "","","","","","",""};
            } else {
                if (runToSummary.size() == 1){
                    EventCountSummary summary = runToSummary.values().iterator().next();
                    data = new String[]{
                            source.get(Source.STANDARD_NAME),
                            "1",
                            Integer.toString(summary.getCount(Event.REFERENCE_ACCEPTED)),
                            Integer.toString(summary.getCount(Event.SCRAPE_PASS)),
                            Integer.toString(summary.getCount(Event.QUERY_NO_MATCH)),
                            Integer.toString(summary.getCount(Event.DATE_NO_MATCH)),
                            Integer.toString(summary.getCount(Event.DATE_PARSE_FAILED)),
                            Integer.toString(summary.getCount(Event.SCRAPE_NO_DATE)),
                            Integer.toString(summary.getCount(Event.SCRAPE_NO_ARTICLE)),
                    };
                } else {
                    List<EventCountSummary> summaries = new ArrayList<>(runToSummary.values());
                    data = new String[]{
                            source.get(Source.STANDARD_NAME),
                            Integer.toString(runToSummary.size()),
                            diffAndRangeStr(Event.REFERENCE_ACCEPTED, summaries),
                            diffAndRangeStr(Event.SCRAPE_PASS, summaries),
                            diffAndRangeStr(Event.QUERY_NO_MATCH, summaries),
                            diffAndRangeStr(Event.DATE_NO_MATCH, summaries),
                            diffAndRangeStr(Event.DATE_PARSE_FAILED, summaries),
                            diffAndRangeStr(Event.SCRAPE_NO_DATE, summaries),
                            diffAndRangeStr(Event.SCRAPE_NO_ARTICLE, summaries)
                    };
                }
            }
            content = insertRow(content, content.length, data);
        }

        System.out.println("Sourcelist: " + sourceList.get(SourceList.LIST_NAME));

        TableBuilder tableBuilder = new TableBuilder(new ArrayTableModel(content));
        tableBuilder.addFullBorder(BorderStyle.fancy_light);
        System.out.println(tableBuilder.build().render(200));

        System.out.println("Format: <latest> (<diff from previous>) [<range over max 10 last runs>]");
    }

    private String diffAndRangeStr(Event event, List<EventCountSummary> summaries){
        int lastCount = summaries.get(0).getCount(event);
        int previousCount = summaries.get(1).getCount(event);
        int diff = lastCount - previousCount;

        String diffStr = String.format(diff < 0? " (%d)" :  " (+%d)" , diff);

        int maxCount = summaries.stream()
                            .mapToInt(s -> s.getCount(event))
                            .max().getAsInt();
        int minCount = summaries.stream()
                            .mapToInt(s -> s.getCount(event))
                            .min().getAsInt();

        String rangeStr = String.format(" [%d-%d]", minCount, maxCount);

        return lastCount + diffStr + rangeStr;
    }

    private <V extends VersionedEntity<V>> boolean isList(V entity, String field) {
        if(entity.spec().get(field).getKlass().isAssignableFrom(List.class)) {
            return true;
        } else {
            return false;
        }
    }

    private <V extends VersionedEntity<V>> boolean isInt(V entity, String field){
        if (entity.spec().get(field).getKlass().isAssignableFrom(Integer.class)){
            return true;
        } else {
            return false;
        }
    }

    private <V extends VersionedEntity<V>> boolean isBoolean(V entity, String field){
        if (entity.spec().get(field).getKlass().isAssignableFrom(Boolean.class)){
            return true;
        } else {
            return false;
        }
    }

    public void outputExampleURLCheck(CrawlArgs args) throws IOException {

        String TITLE = "title";
        String ARTICLE = "article";
        String DATE = "date";
        String ID = "id";
        String URL = "url";
        String SOURCE = "source";

        List<Source> sources;

        sources = sourceDAO.byList(args.sourceLists.get(0));

        Map<Source, List<List<String>>> data = new HashMap<>();

        for(Source source : sources) {

            if(scraperExists(args, source)) {

                List<HttpDocument> docs = scraperService.checkExampleURLs(args.scrapersDir, source);

                List<List<String>> lines = docs.stream().map(d -> ImmutableList.of(
                        d.getMetadata().getString(SCRAPED_TITLE, ""),
                        d.getMetadata().getString(ScraperFields.SCRAPED_ARTICLE, ""),
                        d.getMetadata().getString(ScraperFields.SCRAPED_DATE, ""),
                        d.getMetadata().getString("collector.url", "")
                )).collect(Collectors.toList());

                data.put(source, lines);
            }
        }

        String[] headers = {ID, TITLE, ARTICLE, DATE, URL, SOURCE};

        try (
                final OutputStream outputStream = java.nio.file.Files.newOutputStream(args.workingDir.resolve("example-urls.csv"), StandardOpenOption.CREATE);
                final PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)), false);
                final CSVPrinter csv = new CSVPrinter(writer, CSVFormat.EXCEL.withQuoteMode(QuoteMode.NON_NUMERIC))
        ) {

            csv.printRecord(headers);

            for (Map.Entry<Source, List<List<String>>> entry : data.entrySet()) {

                Source source = entry.getKey();

                for(List<String> values : entry.getValue()) {

                    List<String> row = ImmutableList.of(Integer.toString(
                            source.id()), values.get(0), values.get(1), values.get(2), values.get(3), source.get(Source.STANDARD_NAME));

                    csv.printRecord( row );
                }
            }
        }

    }

}
