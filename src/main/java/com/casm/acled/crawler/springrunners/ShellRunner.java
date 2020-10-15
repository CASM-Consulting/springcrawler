package com.casm.acled.crawler.springrunners;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Strings;
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
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.casm.acled.entities.sourcesourcelist.SourceSourceList;
import net.sf.extjwnl.data.Exc;
import org.apache.commons.csv.QuoteMode;
import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
import org.camunda.bpm.spring.boot.starter.rest.CamundaBpmRestJerseyAutoConfiguration;
import org.jsoup.select.Elements;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
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

import javax.sound.sampled.Line;
import javax.validation.Valid;
import org.jline.reader.LineReader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.sql.*;
import java.io.*;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
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
    private SchedulerService schedulerService;

    @Autowired
    private ExportCSV exportCSV;


    @ShellMethod(value = "Copy a Source (-s) or SourceList (-sl) to a with a new name (-N)")
    public void copy(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {
        CrawlArgs crawlArgs = argsService.get(args);
        crawlArgs.init();

        if( crawlArgs.source != null ) {
            Source copy = crawlArgs.source.put(Source.STANDARD_NAME, crawlArgs.name);
            sourceDAO.create(copy);
        } else if( !crawlArgs.sourceLists.isEmpty() ) {
            SourceList list = crawlArgs.sourceLists.get(0).put(SourceList.LIST_NAME, crawlArgs.name);
            sourceListDAO.create(list);
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

    @ShellMethod(value = "link a Source (-s) to a source list (-sl)", key="link")
    public void linkSourceToSourceList(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) throws Exception{
        CrawlArgs crawlArgs = argsService.get();
        crawlArgs.raw = args;
        crawlArgs.init();

        checkListService.linkSourceToSourceList(crawlArgs);
    }

    @ShellMethod(value = "unlink a Source (-s) from a source list (-sl)", key="unlink")
    public void unlinkSourceFromSourceList(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) throws Exception{
        CrawlArgs crawlArgs = argsService.get();
        crawlArgs.raw = args;
        crawlArgs.init();

        checkListService.unlinkSourceFromSourceList(crawlArgs);
    }

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
    @ShellMethod(value = "get specific value from the corresponding field; usage: get type name field", key = "get")
    public String getField(@ShellOption({"-t", "--type"}) String type,
                         @ShellOption({"-n", "--name"}) String name,
                         @ShellOption({"-f", "--field"}) String field) {

        if (type.equals("source")) {
            Optional<Source> maybeSource = sourceDAO.byName(name);
            if (maybeSource.isPresent()) {
                Source source = maybeSource.get();
                String value = source.get(field);
                return String.format(value);
            }
            else {
                return String.format("source name does not exist");

            }
        }
        else if (type.equals("sourcelist")){
            Optional<SourceList> maybeSourceList = sourceListDAO.byName(name);
            if(maybeSourceList.isPresent()) {
                SourceList sourceList =  maybeSourceList.get();
                String value = sourceList.get(field);
                return String.format(value);
            }
            else {
                return String.format("source list name does not exist");
            }
        }
        else {
            return String.format("wrong type value, should be source or sourcelist");
        }

    }

    @ShellMethod(value = "set specific value to the corresponding field; usage: set type name field [value]", key = "set")
    // generic, only handle single instance
    // in the set method, probably need to update DAO???
    public String setField(@ShellOption({"-t", "--type"}) String type,
                         @ShellOption({"-n", "--name"}) String name,
                         @ShellOption({"-f", "--field"}) String field,
                         @ShellOption({"-v", "--value"}) String value) {

        CrawlArgs crawlArgs = argsService.get();

        if (type.equals("source")) {
            Optional<Source> maybeSource = sourceDAO.byName(name);
            if(maybeSource.isPresent()) {
                Source source =  maybeSource.get();
                source = source.put(field, value);
                sourceDAO.upsert(source);

                return String.format("value set successfully");
            }
            else {
                return String.format("source name does not exist");

            }
        }
        else if (type.equals("sourcelist")){
            Optional<SourceList> maybeSourceList = sourceListDAO.byName(name);
            if(maybeSourceList.isPresent()) {
                SourceList sourceList =  maybeSourceList.get();
                sourceList = sourceList.put(field, value);
                sourceListDAO.upsert(sourceList);

                return String.format("value set successfully");

            }
            else {
                return String.format("source list name does not exist");
            }
        }
        else {
            return String.format("wrong type value, should be source or sourcelist");
        }
    }

    @ShellMethod(value = "add field/property value to existing list; usage: add type name field [value]", key = "add")
    public String addValue(@ShellOption({"-t", "--type"}) String type,
                           @ShellOption({"-n", "--name"}) String name,
                           @ShellOption({"-f", "--field"}) String field,
                           @ShellOption({"-v", "--value"}) String value) {

        CrawlArgs crawlArgs = argsService.get();

        // test command: add source "Imagen del Golfo" CRAWL_SCHEDULE "*"

        if (type.equals("source")) {
            Optional<Source> maybeSource = sourceDAO.byName(name);
            if(maybeSource.isPresent()) {
                Source source =  maybeSource.get();
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
            else {
                return String.format("source name does not exist");

            }
        }
        else if (type.equals("sourcelist")){
            Optional<SourceList> maybeSourceList = sourceListDAO.byName(name);
            if(maybeSourceList.isPresent()) {
                SourceList sourceList =  maybeSourceList.get();
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
                return String.format("source list name does not exist");
            }
        }
        else {
            return String.format("wrong type value, should be source or sourcelist");
        }
    }

    @ShellMethod(value = "show source/sourcelist names and entries, if sourcelist, will show all source names and ids under it. usage: show source/sourcelist NAME", key = "show")
    public String showValue(@ShellOption({"-t", "--type"}) String type,
                           @ShellOption({"-n", "--name"}) String name) {

        // test sample: show source "Imagen del Golfo"
        // test sample: show sourcelist "mexico-1"

        CrawlArgs crawlArgs = argsService.get();

        if (type.equals("source")) {
            Optional<Source> maybeSource = sourceDAO.byName(name);
            if(maybeSource.isPresent()) {
                Source source =  maybeSource.get();
                return source.toString();
            }
            else {
                return String.format("source name does not exist");

            }
        }
        else if (type.equals("sourcelist")){
            StringBuilder printStr = new StringBuilder(String.format("%-30.30s  %-30.30s%n", "Source Name", "ID"));
            Optional<SourceList> maybeSourceList = sourceListDAO.byName(name);
            if(maybeSourceList.isPresent()) {
                SourceList sourceList =  maybeSourceList.get();
                List<Source> sources = sourceDAO.byList(sourceList);
                for (Source source: sources) {
//                    String str = String.format("Source Name: %s, Source ID: %s \n", source.get(Source.STANDARD_NAME), source.id());
                    String str = String.format("%-30.30s  %-30.30s%n", source.get(Source.STANDARD_NAME), source.id());
                    printStr.append(str);
                }

                return printStr.toString();
            }
            else {
                return String.format("source list name does not exist");
            }
        }
        else {
            return String.format("wrong type value, should be source or sourcelist");
        }

    }

    @ShellMethod(value = "delete source/sourcelist field value. usage: delete source/sourcelist name field", key = "delete")
    public String deleteValue(@ShellOption({"-t", "--type"}) String type,
                            @ShellOption({"-n", "--name"}) String name,
                             @ShellOption({"-f", "--field"}) String field) {

        // test sample: delete source "Imagen del Golfo" WATER

        // by saying clear, deleting the value, emm, does it mean to set it to null?
        String question = "Confirm to delete? \nyes/no";
        String result = ask(question);

        if (result.equals("yes")) {

            CrawlArgs crawlArgs = argsService.get();

            if (type.equals("source")) {
                Optional<Source> maybeSource = sourceDAO.byName(name);
                if(maybeSource.isPresent()) {
                    Source source =  maybeSource.get();
                    source = source.put(field, null);
                    sourceDAO.upsert(source);

                    return String.format("successfully delete value");
                }
                else {
                    return String.format("source name does not exist");

                }
            }
            else if (type.equals("sourcelist")){
                String printStr = "";
                Optional<SourceList> maybeSourceList = sourceListDAO.byName(name);
                if(maybeSourceList.isPresent()) {
                    SourceList sourceList =  maybeSourceList.get();
                    sourceList = sourceList.put(field, null);
                    sourceListDAO.upsert(sourceList);

                    return String.format("successfully delete value");
                }
                else {
                    return String.format("source list name does not exist");
                }
            }
            else {
                return String.format("wrong type value, should be source or sourcelist");
            }

        }
        else {
            return String.format("deletion stopped");
        }
    }


    @ShellMethod(value = "batch update all source values via sourcelist, modify all source under given sourcelist. usage: update name field value", key = "update")
    public String updateValue(@ShellOption({"-n", "--name"}) String name,
                              @ShellOption({"-f", "--field"}) String field,
                              @ShellOption({"-v","--value"}) String value) {

        CrawlArgs crawlArgs = argsService.get();

        Optional<SourceList> maybeSourceList = sourceListDAO.byName(name);
        if(maybeSourceList.isPresent()) {
            SourceList sourceList = maybeSourceList.get();
            List<Source> sources = sourceDAO.byList(sourceList);
            for (Source source: sources) {
                source = source.put(field, value);
                sourceDAO.upsert(source);
            }

            return String.format("successfully update value for all sources under the given sourcelist");
        }

        else {
            return String.format("source list name does not exist");
        }
    }

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
    public String clearPIDs() {

        CrawlArgs crawlArgs = argsService.get();

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

    @ShellMethod(value = "dump articles to local csv file. Usage: dump -t TYPE -n NAME -f FROM-DATE -t TO-DATE -od OUTPUT-DIR", key = "dump")
    public String dump(@ShellOption({"-t", "--type"}) String type,
                       @ShellOption({"-n", "--name"}) String name,
                       @ShellOption(value = {"-f", "--from-date"}, defaultValue = "null") String from,
                       @ShellOption(value = {"-t", "--to-date"}, defaultValue = "null") String to,
                       @ShellOption({"-od", "--output-dir"}) String dir) throws Exception{

        // test sample: dump source "Imagen del Golfo" "2020-09-01" "2020-09-24" "/Users/pengqiwei/Downloads/My/PhDs/acled_thing/exports/compare"
        // test sample: dump sourcelist "mexico-1" "2020-09-01" "2020-09-24" "/Users/pengqiwei/Downloads/My/PhDs/acled_thing/exports"
        // test sample: dump source "Imagen del Golfo" null null "/Users/pengqiwei/Downloads/My/PhDs/acled_thing/exports"
        // test sample: dump sourcelist "mexico-1" null null "/Users/pengqiwei/Downloads/My/PhDs/acled_thing/exports"
        // test sample: dump sourcelist "mexico-1" null "2020-09-24" "/Users/pengqiwei/Downloads/My/PhDs/acled_thing/exports"


        CrawlArgs crawlArgs = argsService.get();

        LocalDate fromDate = from.equals("null") ? null : LocalDate.parse(from);
        LocalDate toDate = to.equals("null") ? null : LocalDate.parse(to);

        Path path = Paths.get(dir, name+"-"+from+"-"+to+".csv");

        List<String> columns = Arrays.asList(Source.STANDARD_NAME,
                Article.URL, Article.TEXT, Article.DATE, Article.TITLE, Article.SCRAPE_KEYWORD_HIGHLIGHT);

        if (type.equals("source")) {
            Optional<Source> maybeSource = sourceDAO.byName(name);
            if (maybeSource.isPresent()) {
                List<Article> articles = articleDAO.bySource(maybeSource.get());

                List<Map<String, String>> filteredArticles = articles.stream()
                        .filter(d -> inbetween(d.get(Article.DATE), fromDate, toDate))
                        .filter(distinctByKey(d->d.get(Article.URL)))
                        .map(d -> toMapWithColumn(d, columns))
                        .collect(Collectors.toList());

                mapToCSV(filteredArticles, path);

                return String.format("export to %s successfully", path.toString());

            }

            else {
                return String.format("source name does not exist");
            }

        }
        else if (type.equals("sourcelist")) {
            Optional<SourceList> maybeSourceList = sourceListDAO.byName(name);
            if (maybeSourceList.isPresent()) {
                SourceList sourceList = maybeSourceList.get();
                List<Source> sources = sourceDAO.byList(sourceList);
                List<Article> allArticles = new ArrayList<>();;
                for (Source source: sources) {
                    List<Article> articles = articleDAO.bySource(source);
                    allArticles.addAll(articles);
                }

                List<Map<String, String>> filteredArticles = allArticles.stream().filter(d -> inbetween(d.get("DATE"), fromDate, toDate)).filter(distinctByKey(d->d.get("URL"))).map(d -> toMapWithColumn(d, columns)).collect(Collectors.toList());
                mapToCSV(filteredArticles, path);

                return String.format("export to %s successfully", path.toString());

            }
            else {
                return String.format("source list name does not exist");
            }


        }
        else {
            return String.format("wrong type value, should be source or sourcelist");
        }

    }

    @ShellMethod(value = "generate JEF configuration for source/sourcelists. Usage: jef type name working_dir output_dir", key = "jef")
    public String jef(@ShellOption({"-t", "--t"}) String type,
                      @ShellOption({"-n","--name"}) String name,
                      @ShellOption({"-wd", "--working-dir"}) String workingDir,
                      @ShellOption({"-od","--output-dir"}) String outputDir) {

        // test sample: jef sourcelist "mexico-1" "/Users/pengqiwei/Downloads/My/PhDs/acled_thing/exports" "/Users/pengqiwei/Downloads/My/PhDs/acled_thing/exports"
        // test sample: jef source "Imagen del Golfo" "/Users/pengqiwei/Downloads/My/PhDs/acled_thing/exports" "/Users/pengqiwei/Downloads/My/PhDs/acled_thing/exports"

        CrawlArgs crawlArgs = argsService.get();

        if (type.equals("source")) {
            Optional<Source> maybeSource = sourceDAO.byName(name);
            if (maybeSource.isPresent()) {
                Source source = maybeSource.get();
                Path outputPath = Paths.get(outputDir, Util.getID(source)+"-jef.xml");
                generateDom(workingDir, Arrays.asList(source), outputPath.toString());

                return String.format("JEF configuration generated to %s successfully", outputPath.toString());

            }
            else {

                return String.format("source name does not exist");

            }

        }
        else if (type.equals("sourcelist")) {
            Optional<SourceList> maybeSourceList = sourceListDAO.byName(name);
            if (maybeSourceList.isPresent()) {
                SourceList sourceList = maybeSourceList.get();
                List<Source> sources = sourceDAO.byList(sourceList);
                Path outputPath = Paths.get(outputDir, name+"-jef.xml");
                generateDom(workingDir, sources, outputPath.toString());

                return String.format("JEF configuration generated to %s successfully", outputPath.toString());
            }
            else {
                return String.format("source list name does not exist");
            }

        }
        else {
            return String.format("wrong type value, should be source or sourcelist");
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

    public void generateDom(String dir, List<Source> sources, String outputDir) {
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
                Path combinedPath = Paths.get(dir, Crawl.id(source), "progress", "latest");
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

            StreamResult result =  new StreamResult(new File(outputDir));
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
