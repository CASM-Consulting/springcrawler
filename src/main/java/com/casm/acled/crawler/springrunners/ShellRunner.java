package com.casm.acled.crawler.springrunners;
import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.Crawl;
import com.casm.acled.crawler.management.CheckListService;
import com.casm.acled.crawler.management.CrawlArgs;
import com.casm.acled.crawler.management.CrawlArgsService;
import com.casm.acled.crawler.management.SchedulerService;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.dao.entities.SourceSourceListDAO;
import com.casm.acled.dao.util.ExportCSV;
import com.casm.acled.entities.EntityField;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import org.apache.commons.csv.*;
import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
import org.camunda.bpm.spring.boot.starter.rest.CamundaBpmRestJerseyAutoConfiguration;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import org.springframework.core.MethodParameter;
import org.springframework.shell.*;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Valid;
import org.jline.reader.LineReader;

import org.jsoup.Jsoup;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.io.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.OutputKeys;

import com.casm.acled.crawler.util.Util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;


@EnableAutoConfiguration(exclude={HibernateJpaAutoConfiguration.class, CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class, ValidationAutoConfiguration.class})
// We need the special object mapper, though.
//@Import({ObjectMapperConfiguration.class, CLIRunner.ShutdownConfig.class})
@Import({ObjectMapperConfiguration.class})
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao", "com.casm.acled.crawler"})
@ShellComponent
public class ShellRunner {

    protected static final Logger logger = LoggerFactory.getLogger(ShellRunner.class);

    @Autowired
    private CheckListService checkListService;

    @Autowired
    private Reporter reporter;

    @Autowired
    private CrawlArgsService argsService;

//    private CrawlArgs crawlArgs;

    @Autowired
    LineReader reader;

    @Autowired
    private ArticleDAO articleDAO;

    @Autowired
    private SourceDAO sourceDAO;

    @Autowired
    private SourceListDAO sourceListDAO;

    @Autowired
    private SourceSourceListDAO sourceSourceListDAO;

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private ExportCSV exportCSV;


    @ShellMethod(value = "Copy a Source (-s) or SourceList (-sl) to a with a new name (-N) or suffix if flag 'S' is provided")
    public void copy(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {
        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        boolean suffix = crawlArgs.flagSet.contains("S");

        if( crawlArgs.source != null ) {
            Source copy = crawlArgs.source;
            String name = suffix ?
                    copy.get(Source.STANDARD_NAME) + crawlArgs.name :
                    crawlArgs.name;
            copy = copy.put(Source.STANDARD_NAME, name);
            sourceDAO.create(copy);
        } else if( !crawlArgs.sourceLists.isEmpty() ) {
            SourceList list = crawlArgs.sourceLists.get(0);
            String name = suffix ?
                    list.get(SourceList.LIST_NAME) + crawlArgs.name :
                    crawlArgs.name;

            list = list.put(SourceList.LIST_NAME, name);
            List<Source> sources = sourceDAO.byList(list);

            if(suffix) {

                sources = sources.stream()
                        .map(s -> s.put(Source.STANDARD_NAME, s.get(Source.STANDARD_NAME) + crawlArgs.name) )
                        .collect(Collectors.toList());

                sources = sourceDAO.create(sources);
            }

            list = sourceListDAO.create(list);

            for(Source source : sources) {
                sourceSourceListDAO.link(source, list);
            }

        }
    }


    @ShellMethod(value = "check source list (-sl)", key = "check")
    // probably should give a hint of potential parameters;
    // the help command still not working:
    // Action: Correct the classpath of your application so that it contains a single, compatible version of com.beust.jcommander.JCommander
    public void checkSourceList(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {
        reporter.randomRunId();

        CrawlArgs crawlArgs = argsService.get();
        crawlArgs.raw = args;

        crawlArgs.raw.program = "check";

        crawlArgs.init();

        checkListService.checkSourceList(crawlArgs);

//        reporter.getRunReports().stream().forEach(r -> logger.info(r.toString()));

    }

    @ShellMethod(value = "import source list (-sl)", key = "import")
    public void importSourceList(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) throws Exception{
        reporter.randomRunId();

        CrawlArgs crawlArgs = argsService.get();
        crawlArgs.raw = args;

        crawlArgs.raw.program = "import";

        crawlArgs.init();

        checkListService.importCrawlerSourceList(crawlArgs);

        reporter.getRunReports().stream().forEach(r -> logger.info(r.toString()));

    }

    @ShellMethod(value = "export source list (-sl)", key = "export")
    public void exportSourceList(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) throws Exception{
        reporter.randomRunId();

        CrawlArgs crawlArgs = argsService.get();
        crawlArgs.raw = args;

        crawlArgs.raw.program = "export";

        crawlArgs.init();

        checkListService.exportCrawlerSourceList(crawlArgs);

        reporter.getRunReports().stream().forEach(r -> logger.info(r.toString()));

    }

    public Set<Source> getSourcesFromNameCSV(Path csvPath) throws IOException {
        Set<Source> sources = new HashSet<>();

        try (
                Reader reader = java.nio.file.Files.newBufferedReader(csvPath);
                CSVParser csvReader = new CSVParser(reader,  CSVFormat.EXCEL )
        ){

            for (CSVRecord record : csvReader) {

                // If header name is specified, use it. Otherwise, get the first column value.
                String name = record.get(Source.STANDARD_NAME);

                // If name is present, look up source by name and add if found to results.
                if (name != null && !name.trim().isEmpty()) {
                    Optional<Source> maybeSource = sourceDAO.byName(name);
                    maybeSource.ifPresent(sources::add);
                }
            }
        }

        return sources;
    }

    private void checkNull(Object value, String message) {
        if(value == null) {
            logger.error(message);
            throw new RuntimeException(message);
        }
    }

    @ShellMethod(value = "Link a Source to a source list (-sl). Either using -s or sources can be read from CSV (-P).", key="link")
    public void linkSourceToSourceList(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) throws Exception{

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();
        checkNull(crawlArgs.sourceLists, "Source List required");

        SourceList sourceList = crawlArgs.sourceLists.get(0);

        Set<Source> sources = new HashSet<>();

        if (crawlArgs.path != null){
            sources.addAll(getSourcesFromNameCSV(crawlArgs.path));
            logger.info("Found {} sources from CSV {}", sources.size(), crawlArgs.path);
        }

        if (crawlArgs.source != null ){
            sources.add(crawlArgs.source);
        }

        checkListService.linkSourceToSourceList(sources, sourceList);
    }

    @ShellMethod(value = "unlink a Source (-s) from a source list (-sl). Either using -s or sources can be read from CSV (-P)", key="unlink")
    public void unlinkSourceFromSourceList(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) throws Exception {

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        checkNull(crawlArgs.sourceLists, "Source List required");

        SourceList sourceList = crawlArgs.sourceLists.get(0);

        Set<Source> sources = new HashSet<>();

        if (crawlArgs.path != null) {
            sources.addAll(getSourcesFromNameCSV(crawlArgs.path));
            logger.info("Found {} sources from CSV {}", sources.size(), crawlArgs.path);
        }

        if (crawlArgs.source != null ) {
            sources.add(crawlArgs.source);
        }

        checkListService.unlinkSourceFromSourceList(sources, sourceList);

    }

//    @ShellMethod
//    public void checkURL(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {
//        reporter.randomRunId();
//        CrawlArgs crawlArgs = argsService.get();
//        crawlArgs.raw = args;
//        crawlArgs.init();
//
//        checkListService.
//
//
//
//    }

    @ShellMethod(value = "output example urls ", key = "output")
    public void outputExampleURLCheck(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) throws Exception{
        reporter.randomRunId();

        CrawlArgs crawlArgs = argsService.get();
        crawlArgs.raw = args;

        crawlArgs.raw.program = "example-urls";

        crawlArgs.init();

        checkListService.outputExampleURLCheck(crawlArgs);

        reporter.getRunReports().stream().forEach(r -> logger.info(r.toString()));

    }

    // generic set / get commands for sources and source lists, in the form
    // generic, only handle single instance
    @ShellMethod(value = "get specific value from the corresponding field; usage: get -s/sl name -field value", key = "get")
    public String getField(@ShellOption(value = {"-sl"}, defaultValue = ShellOption.NULL) String sourceListName,
                           @ShellOption(value = {"-s"}, defaultValue = ShellOption.NULL) String sourceName,
                           @ShellOption({"-field"}) String field, // because -f already exists in crawlargs...
                           @ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();


        if (crawlArgs.source!=null) {
            Source source = crawlArgs.source;
            Object value = source.get(field);
            return value.toString();
        }

        else if (!crawlArgs.sourceLists.isEmpty()) {
            SourceList sourceList = crawlArgs.sourceLists.get(0);
            Object value = sourceList.get(field);
            return value.toString();
        }
        else {
            return String.format("source or sourcelist should be provided");
        }

    }

    @ShellMethod(value = "set specific value to the corresponding field; usage: set -s/sl name -field value -value value", key = "set")
    // generic, only handle single instance
    public <T> String setField(@ShellOption(value = {"-sl"}, defaultValue = ShellOption.NULL) String sourceListName,
                               @ShellOption(value = {"-s"}, defaultValue = ShellOption.NULL) String sourceName,
                               @ShellOption({"-field"}) String field, // because -f already exists in crawlargs...
                               @ShellOption({"-value"}) String value,
                               @ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {

        // this function is problematic, cuz need to know the field's class in advance. and casting str to that class in order to set data.
        // need to figure out the data class, in this function, we can directly change from string to int, but not generic enough

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        if (crawlArgs.source!=null) {
            Source source = crawlArgs.source;
//            Class<?> fieldClass = source.get(field).getClass();
            source = source.put(field, value);
            sourceDAO.upsert(source);

            return String.format("value set successfully");
        }

        else if (!crawlArgs.sourceLists.isEmpty()) {
            SourceList sourceList = crawlArgs.sourceLists.get(0);
            sourceList = sourceList.put(field, value);
            sourceListDAO.upsert(sourceList);

            return String.format("value set successfully");

        }
        else {
            return String.format("source or sourcelist should be provided");
        }

    }

    @ShellMethod(value = "add field/property value to existing list; usage: add -sl/s name -field value -value value", key = "add")
    public String addValue(@ShellOption(value = {"-sl"}, defaultValue = ShellOption.NULL) String sourceListName,
                           @ShellOption(value = {"-s"}, defaultValue = ShellOption.NULL) String sourceName,
                           @ShellOption({"-field"}) String field, // because -f already exists in crawlargs...
                           @ShellOption({"-value"}) String value,
                           @ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {

        // test command: add -s "Imagen del Golfo" -field CRAWL_SCHEDULE -value "*"

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        if (crawlArgs.source!=null) {
            Source source = crawlArgs.source;
            Object fieldValue = source.get(field);
            if (fieldValue instanceof List) {
                ((List) fieldValue).add(value);
                source = source.put(field, fieldValue);
                sourceDAO.upsert(source);
                return String.format("value added successfully");
            }
            else {
                return String.format("the field value is not a list object");
            }
        }

        else if (!crawlArgs.sourceLists.isEmpty()) {
            SourceList sourceList = crawlArgs.sourceLists.get(0);
            Object fieldValue = sourceList.get(field);
            if (fieldValue instanceof List) {
                ((List) fieldValue).add(value);
                sourceList = sourceList.put(field, fieldValue);
                sourceListDAO.upsert(sourceList);
                return String.format("value added successfully");
            }
            else {
                return String.format("the field value is not a list object");
            }

        }
        else {
            return String.format("source or sourcelist should be provided");
        }
    }

    @ShellMethod(value = "show source/sourcelist names and entries, if sourcelist(-sl), will show all source names and ids under it. usage: show -s/sl NAME", key = "show")
    public String showValue(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {

        // test sample: show -s "Imagen del Golfo"
        // test sample: show -sl "mexico-1"

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        if (crawlArgs.source!=null) {
            Source source = crawlArgs.source;
            return source.toString();

        }
        else if (!crawlArgs.sourceLists.isEmpty()) {
            StringBuilder printStr = new StringBuilder(String.format("%-30.30s  %-30.30s%n", "Source Name", "ID"));
            SourceList sourceList = crawlArgs.sourceLists.get(0);
            List<Source> sources = sourceDAO.byList(sourceList);
            for (Source source: sources) {
                String str = String.format("%-30.30s  %-30.30s%n", source.get(Source.STANDARD_NAME), source.id());
                printStr.append(str);
            }
            return printStr.toString();
        }
        else {
            return String.format("source or sourcelist should be provided");
        }

    }

    // this method cannot be compatible with CrawlArgs parameters for now.
    @ShellMethod(value = "delete source/sourcelist field value. usage: delete -s/sl name -field value", key = "delete")
    public String deleteValue(@ShellOption(value = {"-sl"}, defaultValue = ShellOption.NULL) String sourceListName,
                              @ShellOption(value = {"-s"}, defaultValue = ShellOption.NULL) String sourceName,
                              @ShellOption({"-field"}) String field, // because -f already exists in crawlargs...
                              @ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {

        // by saying clear, deleting the value, emm, does it mean to set it to null?

        String question = "Confirm to delete? \nyes/no";
        String result = ask(question);

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        if (result.equals("yes")) {
            if (crawlArgs.source != null) {
                Source source = crawlArgs.source;
                source = source.put(field, null);
                sourceDAO.upsert(source);

                return String.format("successfully delete value");

            } else if (!crawlArgs.sourceLists.isEmpty()) {
                SourceList sourceList = crawlArgs.sourceLists.get(0);
                sourceList = sourceList.put(field, null);
                sourceListDAO.upsert(sourceList);

                return String.format("successfully delete value");
            }
            else {
                return String.format("source or sourcelist should be provided");
            }
        }
        else {
            return String.format("deletion cancelled");
        }

    }

    // this method cannot be compatible with CrawlArgs parameters for now.
    @ShellMethod(value = "batch update all source values via sourcelist, modify all source under given sourcelist. usage: update -sl name -field value -value value", key = "update")
    public String updateValue(@ShellOption(value = {"-sl"}) String sourceListName,
                              @ShellOption({"-field"}) String field, // because -f already exists in crawlargs...
                              @ShellOption({"-value"}) String value,
                              @ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        checkNull(crawlArgs.sourceLists, "Source List required");

        SourceList sourceList = crawlArgs.sourceLists.get(0);
        List<Source> sources = sourceDAO.byList(sourceList);

        for (Source source: sources) {
            source = source.put(field, value);
            sourceDAO.upsert(source);
        }

        return String.format("successfully update value for all sources under the given sourcelist");

    }

    // this method cannot be compatible with CrawlArgs parameters for now.
    @ShellMethod(value = "download html from link provided, run the Jsoup pattern and print the results. usage: jsoup -l LINK -p JSOUP_PATTERN", key = "jsoup")
    public String jsoupSearch(@ShellOption({"-l","--link"}) String url,
                              @ShellOption({"-p","--pattern"}) String pattern) {

        // test sample: jsoup -l "https://imagendelgolfo.mx/xalapa/a-morena-en-veracruz-lo-persigue-fantasma-del-perredismo-aseveran/50047104" -p "div.siete60 div.SlaBLK22"
        org.jsoup.nodes.Document doc;
        try {
            doc = Jsoup.connect(url).get();
        }
        catch (IOException e) {
            return e.getMessage();
        }

        if (doc!=null) {
            Elements matched = doc.select(pattern);
            List<String> matchedText = matched.eachText();
            return String.join("\n", matchedText);

        }
        else {
            return String.format("doc is null");
        }
    }

    @ShellMethod(value = "clear PIDs, usage: clear-pids", key = "clear-pids")
    public String clearPIDs(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        schedulerService.clearPIDs(crawlArgs);

        return String.format("PIDs have been cleared");
    }

    @ShellMethod(value = "run scheduler, if no source list is specified, run all. Usage: schedule -wd WORKING-DIR -sd SCRAPER-DIR -d DEPTH [-sl SOURCE-LIST]", key = "schedule")
    public String schedule(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) throws Exception{
        reporter.randomRunId();

        CrawlArgs crawlArgs = argsService.get();
        crawlArgs.raw = args;
        crawlArgs.init();
        schedulerService.schedule(crawlArgs);

        return String.format("scheduling done");

    }

    @ShellMethod(value = "dump articles to local csv file, path should be specified to file. Usage: dump -s/sl name -f FROM-DATE -t TO-DATE -P OUTPUT-DIR", key = "dump")
    public String dump(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) throws Exception{

        // test sample: dump -s "Imagen del Golfo" -f "2020-09-01" -t "2020-09-24" -P "/Users/pengqiwei/Downloads/My/PhDs/acled_thing/exports/compare/Image_del_Golfo_2020-09-01-2020-09-24.csv"
        // test sample: dump -sl "mexico-1" -f "2020-09-01" -t "2020-09-24" -P "/Users/pengqiwei/Downloads/My/PhDs/acled_thing/exports/meixco-1-2020-09-01-2020-09-24.csv"
        // test sample: dump -s "Imagen del Golfo" -P "/Users/pengqiwei/Downloads/My/PhDs/acled_thing/exports/Imagen_del_Golfo.csv"
        // test sample: dump -sl "mexico-1" -P "/Users/pengqiwei/Downloads/My/PhDs/acled_thing/exports/mexico-1.csv"
        // test sample: dump -sl "mexico-1" -t "2020-09-24" -P "/Users/pengqiwei/Downloads/My/PhDs/acled_thing/exports/mexico-1-from-null-to-2020-09-24.csv"

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        LocalDate fromDate = crawlArgs.from;
        LocalDate toDate = crawlArgs.to;

        List<String> columns = Arrays.asList(Source.STANDARD_NAME,
                Article.URL, Article.TEXT, Article.DATE, Article.TITLE, Article.SCRAPE_KEYWORD_HIGHLIGHT);

        if (crawlArgs.source != null) {
            Source source = crawlArgs.source;
            List<Article> articles = articleDAO.bySource(source);

            List<Map<String, String>> filteredArticles = articles.stream()
                    .filter(d -> inbetween(d.get(Article.DATE), fromDate, toDate))
                    .filter(distinctByKey(d->d.get(Article.URL)))
                    .map(d -> toMapWithColumn(d, columns))
                    .collect(Collectors.toList());

            mapToCSV(filteredArticles, crawlArgs.path);

            return String.format("export to %s successfully", crawlArgs.path.toString());
        }
        else if (!crawlArgs.sourceLists.isEmpty()) {
            SourceList sourceList = crawlArgs.sourceLists.get(0);
            List<Source> sources = sourceDAO.byList(sourceList);
            List<Article> allArticles = new ArrayList<>();
            for (Source source: sources) {
                List<Article> articles = articleDAO.bySource(source);
                allArticles.addAll(articles);
            }

            List<Map<String, String>> filteredArticles = allArticles.stream().filter(d -> inbetween(d.get("DATE"), fromDate, toDate)).filter(distinctByKey(d->d.get("URL"))).map(d -> toMapWithColumn(d, columns)).collect(Collectors.toList());
            mapToCSV(filteredArticles, crawlArgs.path);

            return String.format("export to %s successfully", crawlArgs.path.toString());
        }
        else {
            return String.format("source or sourcelist should be provided");
        }

    }

    @ShellMethod(value = "generate JEF configuration for source/sourcelists. Usage: jef type name working_dir output_dir", key = "jef")
    public String jef(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args
//            @ShellOption({"-t", "--t"}) String type,
//                      @ShellOption({"-n","--name"}) String name,
//                      @ShellOption({"-wd", "--working-dir"}) String workingDir,
//                      @ShellOption({"-od","--output-dir"}) String outputDir
    ) {

        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        if (crawlArgs.source != null) {
            Source source = crawlArgs.source;
            Path outputPath = crawlArgs.path.resolve(Crawl.id(source)+"-jef.xml");

            generateDom(crawlArgs.workingDir, Arrays.asList(source), outputPath);

            return String.format("JEF configuration generated to %s successfully", outputPath.toString());

        } else if (!crawlArgs.sourceLists.isEmpty()) {
            SourceList sourceList = crawlArgs.sourceLists.get(0);
            String name = sourceList.get(SourceList.LIST_NAME);
            name = name.toLowerCase().replaceAll(" ", "-");
            List<Source> sources = sourceDAO.byList(sourceList);
            Path outputPath = crawlArgs.path.resolve(Util.getID(name)+"-jef.xml");
            generateDom(crawlArgs.workingDir, sources, outputPath);

            return String.format("JEF configuration generated to %s successfully", outputPath.toString());
        }
        else {
            return String.format("source or sourcelist should be provided");
        }

    }


    public Map<String, String> toMapWithColumn (Article article, List<String> columns) {
        Map<String, String> props = new LinkedHashMap();
        for (String column: columns) {
            Object value;
            if(column.equals(Source.STANDARD_NAME)) {
                value = sourceDAO.getById(article.get(Article.SOURCE_ID)).get().get(Source.STANDARD_NAME);
            } else {
                value = article.get(column);
            }
            String finalValue = value == null ? "" : value.toString();
            props.put(column, finalValue);
        }
        return props;
    }

    private static void mapToCSV(List<Map<String, String>> list, Path path){
        try {

            OutputStream outputStream = java.nio.file.Files.newOutputStream(path, StandardOpenOption.CREATE);
            PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)), false);
            CSVPrinter csv = new CSVPrinter(writer, CSVFormat.EXCEL.withQuoteMode(QuoteMode.NON_NUMERIC));


            List<String> headers = list.stream().flatMap(map -> map.keySet().stream()).distinct().collect(Collectors.toList());
            csv.printRecord(headers);


            for (Map<String, String> map: list) {
                List<String> row = new ArrayList<>();
                for (int i = 0; i < headers.size(); i++) {
                    String value = map.get(headers.get(i));
                    row.add(value);
                }
                csv.printRecord(row);

            }

            csv.close();

        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void generateDom(Path dir, List<Source> sources, Path outputDir) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();

            Element root = doc.createElement("jefmon-config");
            doc.appendChild(root);

            Element instanceName = doc.createElement("instance-name");
            instanceName.appendChild(doc.createTextNode("ACLED"));
            root.appendChild(instanceName);

            Element interval = doc.createElement("default-refresh-interval");
            interval.appendChild(doc.createTextNode("5"));
            root.appendChild(interval);

            Element paths = doc.createElement("monitored-paths");

            for (Source source: sources) {
                Element path = doc.createElement("path");
                String id = Crawl.id(source);
                Path combinedPath = dir.resolve(Paths.get( id, "progress", "latest"));
                path.appendChild(doc.createTextNode(combinedPath.toString()));
                paths.appendChild(path);

            }

            root.appendChild(paths);

            Element jobActions = doc.createElement("job-actions");

            Element action1 = doc.createElement("action");
            action1.appendChild(doc.createTextNode("com.norconex.jefmon.instance.action.impl.ViewJobSuiteLogAction"));
            Element action2 = doc.createElement("action");
            action2.appendChild(doc.createTextNode("com.norconex.jefmon.instance.action.impl.ViewJobLogAction"));

            jobActions.appendChild(action1);
            jobActions.appendChild(action2);

            root.appendChild(jobActions);

            TransformerFactory transformerFactory =  TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            DOMSource source = new DOMSource(doc);

            StreamResult result =  new StreamResult(outputDir.toFile());
            transformer.transform(source, result);
        }

        catch (Exception ex) {
            ex.printStackTrace();
        }


    }


    public boolean inbetween(LocalDate articleDate, LocalDate from, LocalDate to) {

        if (from==null && to!=null) {
            return (articleDate.isBefore(to)) || articleDate.isEqual(to);
        }

        if (from!=null && to==null) {
            return (articleDate.isAfter(from)) || articleDate.isEqual(from);
        }

        if (from==null && to==null) {
            return true;
        }

        return (articleDate.isBefore(to) && articleDate.isAfter(from)) || (articleDate.isEqual(to) || articleDate.isEqual(from));
    }

    public static <T> Predicate<T> distinctByKey(
            Function<? super T, ?> keyExtractor) {

        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    @Bean
    public ParameterResolver commandParameterResolver() {
        return new ParameterResolver(){

            @Override
            public boolean supports(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(List.class);
            }

            /**
             * This implementation simply returns all the words (arguments) present
             * 'Infinite arity'
             */
            @Override
            public ValueResult resolve(MethodParameter methodParameter, List<String> words) {
                return new ValueResult(methodParameter, words);
            }

            @Override
            public Stream<ParameterDescription> describe(MethodParameter parameter) {
                return Stream.of(ParameterDescription.outOf(parameter));
            }

            @Override
            public List<CompletionProposal> complete(MethodParameter parameter, CompletionContext context) {
                return Collections.emptyList();
            }
        };
    }

    public String ask(String question) {
        question = "\n" + question + " > ";
        return this.reader.readLine(question);
    }



    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(ShellRunner.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(args);
        logger.info("Spring Boot application started");
        ctx.close();
    }



}
