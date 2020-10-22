package com.casm.acled.crawler.management;

import com.casm.acled.crawler.reporting.Event;
import com.casm.acled.crawler.reporting.Report;
import com.casm.acled.crawler.reporting.ReportQueryService;
import com.casm.acled.crawler.reporting.ReportQueryService.EventCountSummary;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.scraper.ACLEDScraper;
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
        String url = source.get(Source.LINK);
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

    public interface Check {
        String header();
        Object check();
    }

    public static boolean passed(Object maybeBoolean){
        try {
            Boolean b = (Boolean)maybeBoolean;
            return b != null? b : false;
        } catch (ClassCastException e){
            return false;
        }
    }

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
            List<String> sitemaps = crawlService.getSitemaps(source);
            hasSiteMaps = !sitemaps.isEmpty();
            if((boolean)hasSiteMaps) {
                try {
                    Set<String> recent = crawlService.recentSitemapURLs(source.get(Source.LINK), sitemaps);
                    hasRecentSiteMaps = !recent.isEmpty();
                } catch (Exception e) {
                    hasRecentSiteMaps = e.getMessage();
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



        return checkValue.toArray(new String[checkValue.size()]);

    }

    public void exportCrawlerSourceList(CrawlArgs args) throws IOException {

        if (args.workingDir == null || args.path == null){
            throw new RuntimeException("Must specify a working directory (-wd) and path (-P) to export a SourceList.");
        }

        SourceList sourceList = args.sourceLists.get(0);

        Path path = args.workingDir.resolve(args.path);

        exportCrawlerSourcesToCSV(args.workingDir.resolve(path), sourceList);
    }

    public void importCrawlerSourceList(CrawlArgs args) throws IOException {

        if (args.workingDir == null || args.path == null){
            throw new RuntimeException("Must specify a working directory (-wd) and path (-P) to find the source list import file.");
        }

        Path path = args.workingDir.resolve(args.path);

        List<Source> sources = importCrawlerSourcesFromCSV(path, EntityVersions.get(Source.class).current());

        for(SourceList list : args.sourceLists) {
            for(Source source : sources) {
                sourceSourceListDAO.link(source, list);
            }
        }
    }

    public void checkSourceList(CrawlArgs args) {
        String [] header = {"Source ID", "connection", "scraperExists", "hasExamples", "hasDateFormat", "hasSiteMaps",
                "hasRecentSitemaps", "dateScraped", "dateParsed", "titleScraped", "articleScraped"};
//        String [] header = {"Source ID", "hasSiteMaps"};
        String [][] content = new String[][] {header};

        List<Source> sources = new ArrayList<>();
        if (args.source != null){
            sources.add(args.source);
        } else if (args.sourceLists != null && !args.sourceLists.isEmpty()) {
            SourceList sourceList = args.sourceLists.get(0);
            sources.addAll(sourceDAO.byList(sourceList));
        }
        if (sources.isEmpty()){
            throw new RuntimeException("No source list or source specified.");
        }

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
            String [] checkArray = checkSourceStatus(args, source);
            if (checkArray.length==0) {
                continue;
            }
            content = insertRow(content,content.length, checkArray);

        }

        TableModel model = new ArrayTableModel(content);
        TableBuilder tableBuilder = new TableBuilder(model);
        tableBuilder.addFullBorder(BorderStyle.fancy_light);
        System.out.println(tableBuilder.build().render(80));


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


    public void exportCrawlerSourcesToCSV(Path path, SourceList sourceList) throws IOException {
        List<Source> sources = sourceDAO.byList(sourceList);
        exportCrawlerSourcesToCSV(path, sources);
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

    private static final Set<String> importExportFields = ImmutableSet.of(Source.LINK, Source.EXAMPLE_URLS, Source.DATE_FORMAT,
            Source.LOCALES, Source.CRAWL_DISABLE_SITEMAPS, Source.CRAWL_DISABLE_SITEMAP_DISCOVERY, Source.CRAWL_SITEMAP_LOCATIONS,
            Source.SEED_URLS, Source.CRAWL_SCHEDULE, Source.TIMEZONE, Source.CRAWL_DEPTH);

    public void exportCrawlerSourcesToCSV(Path path, List<Source> sources) throws IOException {

        List<String> headers = ImmutableList.of("id", "field", "value");
        Set<String> fields = importExportFields;

        try (
                final OutputStream outputStream = java.nio.file.Files.newOutputStream(path, StandardOpenOption.CREATE);
                final PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)), false);
                final CSVPrinter csv = new CSVPrinter(writer, CSVFormat.EXCEL)
//                CSVWriter csv = new CSVWriter(writer)
        ) {

            csv.printRecord(headers);
//            csv.writeNext(headers.toArray(new String[]{}));

            for (Source source : sources) {

                String id = source.get(Source.STANDARD_NAME);

                for(String field : fields) {

                    List<String> values;

                    if(source.hasValue(field) ) {
                        if(isList(source, field) ) {
                            if(((List)source.get(field)).isEmpty()) {

                                values = ImmutableList.of("");
                            } else {

                                values = source.get(field);
                            }
                        } else {

                            values = ImmutableList.of(source.get(field));
                        }
                    } else {

                        values = ImmutableList.of("");
                    }

                    for(Object value : values){

                        List<String> row = new ArrayList<>();

                        row.add(id);
                        row.add(field);
                        row.add(value.toString());
                        csv.printRecord( row );
//                        csv.writeNext(row.toArray(new String[]{}));
                    }
                }
            }
        }
    }

    public List<Source> importCrawlerSourcesFromCSV(Path seedsPath, Source defaultSource) throws IOException {
        String ID = "id";
        String FIELD = "field";
        String VALUE = "value";

        Set<String> allowedFields = importExportFields;

        try (
                Reader reader = java.nio.file.Files.newBufferedReader(seedsPath);
                CSVParser csvReader = new CSVParser(reader, CSVFormat.EXCEL.withFirstRecordAsHeader());
//                CSVReader csvReader = new CSVReader(reader);
        ) {
//            Iterator<String[]> itr = csvReader.iterator();
//            String[] headers = itr.next();
//            Map<String,Integer> headerMap = new HashMap<>();
//            for(int i = 0; i < headers.stream(); ++i) {
//                headerMap.put(headers[i], i);
//            }

//            Map<String,Integer> headerMap = csvReader.getHeaderMap();

            Iterator<CSVRecord> itr = csvReader.iterator();

            Map<String, Source> sourceMap = new HashMap<>();

            while(itr.hasNext()) {
//                String[] row = itr.next();
//                List<String> row = itr.next();
//                String id = row[headerMap.get(ID)];
//                String field = row[headerMap.get(FIELD)];
//                String value = row[headerMap.get(VALUE)];
                CSVRecord record = itr.next();

                String id = record.get(ID);
                String field = record.get(FIELD);
                String value = record.isSet(VALUE) ? record.get(VALUE) : null;

                if(value == null || value.isEmpty()) {
                    continue;
                }

                if(!allowedFields.contains(field)) {
                    logger.warn("{} not allowed", field);
                }

                if (!value.isEmpty()){
                    sourceMap.compute(id, (i, source)->{
                        if(source == null) {
                            source = defaultSource.put(Source.STANDARD_NAME, id);
                        }

                        if(isList(source, field)) {

                            List<String> values;

                            if(source.hasValue(field)) {
                                values = source.get(field);
                            } else {
                                values = new ArrayList<>();
                            }

                            values.add(value);
                            return source.put(field, values);
                        } else if (isBoolean(source, field)){

                            return source.put(field, BooleanUtils.toBoolean(value));
                        }   else if (isInt(source, field)){

                            return source.put(field, Integer.parseInt(value));
                        }  else {

                            return source.put(field, value);
                        }
                    });
                }
            }

            List<Source> sources = sourceMap.values().stream().map(s-> {

                Optional<Source> maybeSource = sourceDAO.byName(s.get(Source.STANDARD_NAME));
                if(maybeSource.isPresent()) {
                    Source source = maybeSource.get();
                    s = s.id(source.id());
                }
                return s;
            }).collect(Collectors.toList());

            sourceDAO.upsert(sources);

            return sources;
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


    public void linkSourceToSourceList(Set<Source> sources, SourceList sourceList) {
        for (Source source : sources){
            sourceSourceListDAO.link(source, sourceList);
        }
    }

    public void unlinkSourceFromSourceList(Set<Source> sources, SourceList sourceList){
        for (Source source : sources) {
            sourceSourceListDAO.unlink(source, sourceList);
        }
    }
}
