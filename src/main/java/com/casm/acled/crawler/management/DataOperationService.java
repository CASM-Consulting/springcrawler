package com.casm.acled.crawler.management;

import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.spring.CrawlService;
import com.casm.acled.crawler.util.Util;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.dao.entities.SourceSourceListDAO;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.VersionedEntity;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import org.apache.commons.csv.*;
import org.apache.commons.lang3.BooleanUtils;
import org.jline.reader.LineReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class DataOperationService {

    protected static final Logger logger = LoggerFactory.getLogger(DataOperationService.class);

    @Autowired
    private DataOperationService dataOperationService;

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

    public void copy(CrawlArgs crawlArgs) {

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

    public void linkSourceToSourceList(CrawlArgs crawlArgs) throws Exception {

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

        for (Source source : sources){
            sourceSourceListDAO.link(source, sourceList);
        }

    }

    public void unlinkSourceFromSourceList(CrawlArgs crawlArgs) throws Exception {

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

        for (Source source : sources) {
            sourceSourceListDAO.unlink(source, sourceList);
        }

    }

    public String getFied(CrawlArgs crawlArgs, String field) {
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

    public String setField(CrawlArgs crawlArgs, String field, String value) {

        if (crawlArgs.source!=null) {
            Source source = crawlArgs.source;
            if (isInt(source, field)) {
                source = source.put(field, Integer.parseInt(value));
            }
            else if (isBoolean(source, field)) {
                source = source.put(field, BooleanUtils.toBoolean(value));
            }
            else if (isList(source, field)) {
                return String.format("set List field is not supported by set method, please use add method");
            }
            else {
                source = source.put(field, value);
            }

            sourceDAO.upsert(source);

            return String.format("value set successfully");
        }

        else if (!crawlArgs.sourceLists.isEmpty()) {
            SourceList sourceList = crawlArgs.sourceLists.get(0);
            if (isInt(sourceList, field)) {
                sourceList = sourceList.put(field, Integer.parseInt(value));
            }
            else if (isBoolean(sourceList, field)) {
                sourceList = sourceList.put(field, BooleanUtils.toBoolean(value));
            }
            else if (isList(sourceList, field)) {
                return String.format("set List field is not supported by set method, please use add method");
            }
            else {
                sourceList = sourceList.put(field, value);
            }

            sourceListDAO.upsert(sourceList);

            return String.format("value set successfully");

        }
        else {
            return String.format("source or sourcelist should be provided");
        }

    }

    // add field/property value to existing list
    public String addValue(CrawlArgs crawlArgs, String field, String value) {

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

    public String showValue(CrawlArgs crawlArgs) {

        if (crawlArgs.source!=null) {
            Source source = crawlArgs.source;
            return String.format("ID: %s%nData: %s", source.id(), source);

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

    // in which sense it means to set null to corresponding field.
    public String deleteValue(CrawlArgs crawlArgs, String field) {


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

    // batch update all source values via sourcelist, modify all source under given sourcelist
    // it might have some logic problem concerning update list field... just leave it like this for now (act like add for list).
    public String updateValue(CrawlArgs crawlArgs, String field, String value) {

        checkNull(crawlArgs.sourceLists, "Source List required");

        SourceList sourceList = crawlArgs.sourceLists.get(0);
        List<Source> sources = sourceDAO.byList(sourceList);

        for (Source source: sources) {

            if (isInt(source, field)) {
                source = source.put(field, Integer.parseInt(value));
            }
            else if (isBoolean(source, field)) {
                source = source.put(field, BooleanUtils.toBoolean(value));
            }
            else if (isList(source, field)) {
                List<String> values;

                if(source.hasValue(field)) {
                    values = source.get(field);
                } else {
                    values = new ArrayList<>();
                }

                values.add(value);

                source = source.put(field, values);
            }
            else {
                source = source.put(field, value);
            }

            source = source.put(field, value);
            sourceDAO.upsert(source);
        }

        return String.format("successfully update value for all sources under the given sourcelist");

    }

    public String dump(CrawlArgs crawlArgs) {

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

            Path outputPath = crawlArgs.path.resolve(source.get(Source.STANDARD_NAME)+"-"+crawlArgs.from+"-"+crawlArgs.to+".csv");

            mapToCSV(filteredArticles, outputPath);

            return String.format("export to %s successfully", outputPath.toString());
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

            Path outputPath = crawlArgs.path.resolve(sourceList.get(SourceList.LIST_NAME)+"-"+crawlArgs.from+"-"+crawlArgs.to+".csv");

            mapToCSV(filteredArticles, outputPath);

            return String.format("export to %s successfully", outputPath.toString());
        }
        else {
            return String.format("source or sourcelist should be provided");
        }

    }


    private void checkNull(Object value, String message) {
        if(value == null) {
            logger.error(message);
            throw new RuntimeException(message);
        }
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

    private <V extends VersionedEntity<V>> boolean isList(V entity, String field) {
        if(entity.spec().get(field).getKlass().isAssignableFrom(List.class)) {
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

    private <V extends VersionedEntity<V>> boolean isInt(V entity, String field){
        if (entity.spec().get(field).getKlass().isAssignableFrom(Integer.class)){
            return true;
        } else {
            return false;
        }
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




}
