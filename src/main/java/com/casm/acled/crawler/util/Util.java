package com.casm.acled.crawler.util;


import com.casm.acled.AcledObjectMapper;
import com.casm.acled.camunda.BusinessKeys;
import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.scraper.ACLEDScraper;
import com.casm.acled.crawler.scraper.dates.*;
import com.casm.acled.dao.VersionedEntityDAO;
import com.casm.acled.dao.VersionedEntityDAOs;
import com.casm.acled.dao.entities.*;
import com.casm.acled.dao.util.ExportCSV;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.VersionedEntity;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.desk.Desk;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;

import com.casm.acled.entities.sourcesourcelist.SourceSourceList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.*;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.opencsv.CSVReader;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
import org.camunda.bpm.spring.boot.starter.rest.CamundaBpmRestJerseyAutoConfiguration;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;


import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

//import org.springframework.shell.standard.ShellMethod;
//import org.springframework.shell.standard.ShellComponent;

// We have to exclude these classes, because they only work in a web context.
@EnableAutoConfiguration(exclude={CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class})
// We need the special object mapper, though.
@Import(ObjectMapperConfiguration.class)
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao", "com.casm.acled.crawler"})
public class Util implements CommandLineRunner {
    protected static final Logger logger = LoggerFactory.getLogger(Util.class);
    // keyword query specific to potential articles of interest to ACLED
//    public static String KEYWORDS2 = ".+(?:kill|killed|massacre|death|\\bdied\\b|\\bdead\\b|\\bbomb\\b|\\bbombed\\b|\\bbombing\\b|\\brebel\\b|\\battack\\b|\\battacked\\b|\\briot\\b|\\bbattle\\b|\\bprotest\\b|\\bclash\\b|\\bdemonstration\\b|\\bstrike\\b|\\bwound\\b|\\binjure\\b|\\bcasualty\\b|\\bdisplace\\b|\\bunrest\\b|\\bcasualties\\b|\\bvigilante\\b|\\btorture\\b|\\bmarch\\b|\\brape\\b).+";
    public static String KEYWORDS = ".*\\b(?:kill|massacre|death|died|dead|bomb|bombed|bombing|rebel|attack|attacked|riot|battle|protest|clash|demonstration|strike|wound|injure|casualty|displace|unrest|casualties|vigilante|torture|march|rape)\\b.*";
    public static List<String> KEYWORDS_LUCENE = ImmutableList.copyOf("kill massacre death died dead bomb bombed bombing rebel attack attacked riot battle protest clash demonstration strike wound injure casualty displace unrest casualties vigilante torture march rape".split(" "));
//    private static final String matchingKeywords = "\\b(?:kill|massacre|death|died|dead|bomb|bombed|bombing|rebel|attack|attacked|riot|battle|protest|clash|demonstration|strike|wound|injure|casualty|displace|unrest|casualties|vigilante|torture|march|rape)\\b";

    @Autowired
    private ArticleDAO articleDAO;

    @Autowired
    private SourceListDAO sourceListDAO;

    @Autowired
    private SourceSourceListDAO sourceSourceListDAO;

    @Autowired
    private SourceDAO sourceDAO;

    @Autowired
    private DeskDAO deskDAO;

    @Autowired
    private ExportCSV exportCSV;

    @Autowired
    private VersionedEntityDAOs entityDAOs;

    private static final Pattern PROTOCOL = Pattern.compile(".*:[/]{2}", Pattern.CASE_INSENSITIVE);

    public static String ensureHTTP(String url, boolean https) {

        Matcher matcher = PROTOCOL.matcher(url.toLowerCase());

        if(!matcher.find()) {
            // get rid of any previous or malformed protocol
            //url = matcher.replaceFirst("");
            url =  (https) ? "https://" + url : "http://" + url;
        }

        return url;
    }

    public static String getID(Source source) {
        String[] startURL =((String) source.get(Source.LINK)).split(",");

        String id = Util.getID(startURL[0]);

        return id;
    }

    public static String getID(String url)  {
        String id = Util.getDomain(url).replaceAll("\\.","");
        return id;
    }


    // Returns the originating domain of a given url - minus any trailing 'www'
    public static String getDomain(String urlString)  {
        urlString = ensureHTTP(urlString, true);
        try {
            URI uri = URI.create(urlString);
            String host = uri.getHost();
            return (host.startsWith("www")) ? host.substring(4) : host;
        } catch (IllegalStateException e) {
            logger.error("{} ", urlString);
            throw e;
        }
    }

    // Processes a M52 job json to scraper rules
    public static String processJobJSON(String json) {
        JSONObject jobj = new JSONObject(json);
        return jobj.getJSONArray("components").getJSONObject(0).getJSONObject("opts").getJSONArray("fields").toString();
    }

    public static String processScraperJSON(String json){
        return null;
    }

    // returns a web scraper based on a job spect of last_scrape file
    public static String processJSON(File scraperLocation) throws IOException {
        String json = Files.asCharSource(scraperLocation, Charset.defaultCharset()).read();
        return processJobJSON(json);
    }

    // returns a web scraper based on a job spect of last_scrape file
    public static Map<String,String> getLastScrape(Path scraperPath)  {

        Function<String,String> clean = dirty -> dirty.replaceAll("[\\\\Q\\\\E]", "").trim();

        Map<String,String> lastScrape = new HashMap<>();
        try {

            Path path = scraperPath.resolve("last_scrape.json");
            if(java.nio.file.Files.exists(path)) {


                String json = Files.asCharSource(path.toFile(), Charset.defaultCharset()).read();
                JSONObject jobj = new JSONObject(json);
                String url = clean.apply(jobj.getString("url"));
                if(jobj.has("field.name/article_0")) {
                    String article = clean.apply(jobj.getString("field.name/article_0"));
                    lastScrape.put("article", article);
                }
                if(jobj.has("field.name/date_0")) {
                    String date = clean.apply(jobj.getString("field.name/date_0"));
                    lastScrape.put("date", date);
                }
                if(jobj.has("field.name/title_0")) {
                    String title = clean.apply(jobj.getString("field.name/title_0"));
                    lastScrape.put("title", title);
                }

                lastScrape.put("url", url);

            }

            return lastScrape;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    public static void main(String[] args) throws Exception {
//        getDomain("http://www.0.com:5000");
//    }


    //    public LocalDate simplDateFallback(String dateString) {
//
//    }

    public static String metadataGet(Map<String, List<String>> metadata, String key) {
        String value = null;
        if(metadata.containsKey(key) && metadata.get(key).size() > 0) {
            value = metadata.get(key).get(0);
        }
        return value;
    }

    public static void metadataPut(Map<String, List<String>> metadata, String key, String value) {
        metadata.put(key, ImmutableList.of(value));
    }

    public static void metadataAdd(Map<String, List<String>> metadata, String key, String value) {
        if(metadata.containsKey(key)) {
            metadata.put(key, new ImmutableList.Builder<String>()
                    .addAll(metadata.get(key))
                    .add(value).build());
        } else {
            metadataPut(metadata, key, value);
        }
    }

    public static boolean isDisabled(Source source) {
        Boolean crawlDisable = source.get(Source.CRAWL_DISABLED);
        return crawlDisable != null && crawlDisable;
    }

    public static boolean isScrapable(Path scraperDir, Source source) {
        return !isDisabled(source) && scraperExists(scraperDir, source);
    }

    public static boolean scraperExists(Path scraperDir, Source source) {
        try {
            Path path;
            if(source.hasValue(Source.CRAWL_SCRAPER_PATH)) {
                path = Paths.get((String)source.get(Source.CRAWL_SCRAPER_PATH));
            } else {
                String id = Util.getID(source);
                path = scraperDir.resolve(id);
            }
            if(ACLEDScraper.validPath(path)) {
                return true;
            } else {
                return false;
            }
        } catch (IllegalArgumentException e){
            logger.warn(e.getMessage());
            return false;
        }
    }

    public void recoverArticleDates() {

        for(Article article : articleDAO.getAll() ) {


            String[] text = ((String)article.get(Article.TEXT)).split("\n");

            if(text.length <= 1) {
                continue;
            }

            Optional<LocalDate> localDate  = DateUtil.getDate(text[1]);

            if(!localDate.isPresent()) {

                articleDAO.delete(article);
                continue;
            }

            article = article.put(Article.DATE, localDate);

            if(article.hasValue(Article.SOURCE_ID)) {

                int sourceId = article.get(Article.SOURCE_ID);

                List<SourceList> sourceLists = sourceListDAO.bySource(sourceId);

                for(SourceList sourceList : sourceLists) {

                    article = article.businessKey(BusinessKeys.generate(sourceList.get(SourceList.LIST_NAME), localDate.get()));
                }
            }

            articleDAO.upsert(article);
        }
    }


    private void deleteNonMatchingArticles() {
        Pattern pattern = Pattern.compile(KEYWORDS);
        for (Article article : articleDAO.getAll()) {
            String text = article.get(Article.TEXT);

            if(!pattern.matcher(text).matches()) {
                System.out.println("REMOVE " + text);
                articleDAO.delete(article);
            } else {

                //System.out.println("KEEP " + text);
            }
        }
    }

    private void linkExisting() {
        Map<String, Source> sources = sourceDAO.getAll().stream().filter(s->s.get(Source.LINK)!=null).collect(Collectors.toMap(s->s.get(Source.LINK), s -> s, (o, o2) -> o));

        for(Article article : articleDAO.getAll()) {
            String url = article.get(Article.URL);
            if(url == null) {
                continue;
            }
            for (Map.Entry<String, Source> e : sources.entrySet()) {

                if (url.contains(e.getKey())) {
                    int sourceId = e.getValue().id();
                    article = article.put(Article.SOURCE_ID, sourceId);

                    List<SourceList> sourceLists = sourceListDAO.bySource(sourceId);

                    LocalDate localDate = article.get(Article.DATE);

                    for(SourceList sourceList : sourceLists) {

                        article = article.businessKey(BusinessKeys.generate(sourceList.get(SourceList.LIST_NAME), localDate));
                    }

                    articleDAO.upsert(article);
//                    System.out.println("updating " + article.get(Article.URL));
                    break;
                }
            }

        }
    }



    private void insertDummySource() {
        String link = "http://www.0.com:5000";
        Source source = EntityVersions.get(Source.class).current()
                .put(Source.LINK, link)
                .put(Source.NAME, "fake net")
                .put(Source.STANDARD_NAME, "fake net")
                .put(Source.COUNTRY, "United Kingdom")
                ;

        source = sourceDAO.create(source);

        SourceList sourceList = EntityVersions.get(SourceList.class).current()
                .put(SourceList.LIST_NAME, "fake list");

        sourceList = sourceListDAO.create(sourceList);

        sourceSourceListDAO.link(source, sourceList);

    }

    private void updateDummySource() {
        Source source = sourceDAO.getByUnique(Source.STANDARD_NAME, "fake net").get();

        source = source.put(Source.DATE_FORMAT, ImmutableList.of("ISO:/yyyy-MM-dd/en_GB"));

        sourceDAO.upsert(source);
    }


    /*
        DOESN'T WORK, USE JSON
     */
    public void importSourceDataCSV(Path inputDir) throws IOException {
        Source defaultSource = EntityVersions.get(Source.class).current();
        importSourcesFromCSV(inputDir.resolve("sources.csv"), defaultSource);

        SourceList defaultSourceList = EntityVersions.get(SourceList.class).current();
        importSourcesFromCSV(inputDir.resolve("source-lists.csv"), defaultSource);

        SourceSourceList defaultSourceSourceList = EntityVersions.get(SourceSourceList.class).current();
        importSourcesFromCSV(inputDir.resolve("source-source-lists.csv"), defaultSource);
    }


    public void exportSourceDataJSON(Path outputDir) throws IOException {

        outputDir.toFile().mkdirs();

        ObjectMapper om = AcledObjectMapper.get();

        om.writeValue(outputDir.resolve("sources.json").toFile(), sourceDAO.getAll());
        om.writeValue(outputDir.resolve("source-lists.json").toFile(), sourceListDAO.getAll());
        om.writeValue(outputDir.resolve("source-source-lists.json").toFile(), sourceSourceListDAO.getAll());

    }

    public void importSourceDataJSON(Path outputDir) throws IOException {

        ObjectMapper om = AcledObjectMapper.get();

        List<Source> sources = om.readValue(outputDir.resolve("sources.json").toFile(), om.getTypeFactory().constructCollectionType(List.class, Source.class));
        List<SourceList> sourceLists = om.readValue(outputDir.resolve("source-lists.json").toFile(), om.getTypeFactory().constructCollectionType(List.class, SourceList.class));
        List<SourceSourceList> sourceSourceLists = om.readValue(outputDir.resolve("source-source-lists.json").toFile(), om.getTypeFactory().constructCollectionType(List.class, SourceSourceList.class));

        Map<Integer, Source> sourceIdMap = sources.stream().collect(Collectors.toMap(s->s.id(), s->s));
        Map<Integer, SourceList> sourceListIdMap = sourceLists.stream().collect(Collectors.toMap(sl->sl.id(), sl->sl));

        HashMultimap<String, String> map = HashMultimap.create();

        //map links of updated ids
        for(SourceSourceList ssl : sourceSourceLists) {

            Source source = sourceIdMap.get(ssl.id1());
            SourceList sourceList = sourceListIdMap.get(ssl.id2());

            if(source == null && sourceList == null) {
                continue;
            }

            String sourceName = source.get(Source.STANDARD_NAME);
            String listName = sourceList.get(SourceList.LIST_NAME);

            map.put(listName, sourceName);
        }


        //update sources by name
        for(Source source : sources ) {
            Optional<Source> maybeSource = sourceDAO.byName(source.get(Source.STANDARD_NAME));
            if(maybeSource.isPresent()) {
                source.id(maybeSource.get().id());
            }
            sourceDAO.create(source);
        }

        //update source lists by name
        for(SourceList sourceList : sourceLists ) {
            Optional<SourceList> maybeSourceList = sourceListDAO.byName(sourceList.get(SourceList.LIST_NAME));
            if(maybeSourceList.isPresent()) {
                sourceList.id(maybeSourceList.get().id());
            }
            sourceListDAO.create(sourceList);
        }

        //create links for updated source / source list refs
        for(Map.Entry<String, Collection<String>> entry : map.asMap().entrySet()) {

            String listName = entry.getKey();

            for(String sourceName : entry.getValue()) {

                Optional<Source> maybeSource = sourceDAO.byName(sourceName);
                Optional<SourceList> maybeList = sourceListDAO.byName(listName);

                if(maybeSource.isPresent() && maybeList.isPresent()) {

                    int sourceId = maybeSource.get().id();
                    int listId = maybeList.get().id();

                    sourceSourceListDAO.link(sourceId, listId);

                    logger.info("source {} <-> list {}", sourceId, listId);
                } else {

                    logger.info("{} {}", sourceName, listName);
                }
            }
        }
    }

    public void exportSourceDataCSV(Path outputDir) throws IOException {

        java.nio.file.Files.createDirectories(outputDir);

        Source missingSource = EntityVersions.get(Source.class).current().put(Source.STANDARD_NAME, "__MISSING__");
        SourceList missingSourceList = EntityVersions.get(SourceList.class).current().put(SourceList.LIST_NAME, "__MISSING__");

        exportEntityCSV(Source.class, sourceDAO,  outputDir, "sources.csv");
        exportEntityCSV(SourceList.class, sourceListDAO, outputDir, "source-lists.csv");
        exportEntityCSV(SourceSourceList.class, sourceSourceListDAO, outputDir, "sources-source-list.csv",
                ImmutableList.of(Source.STANDARD_NAME, SourceList.LIST_NAME), ssl -> ImmutableList.of(
                        sourceDAO.getById(ssl.id1()).orElse(missingSource).get(Source.STANDARD_NAME),
                        sourceListDAO.getById(ssl.id2()).orElse(missingSourceList).get(SourceList.LIST_NAME)
                )
        );
    }

    public <V extends VersionedEntity<V>> void exportEntityCSV(Class<V> klass, VersionedEntityDAO<V> dao, Path outputDir, String fileName) throws IOException {
        exportEntityCSV(klass, dao, outputDir, fileName, ImmutableList.of(), e -> ImmutableList.of() );
    }

    public <V extends VersionedEntity<V>> void exportEntityCSV(Class<V> klass, VersionedEntityDAO<V> dao, Path outputDir, String fileName, List<String> extraHeaders,
                                                               Function<V, List> extraFields) throws IOException {
        String name = klass.getName();
        int i = name.lastIndexOf(".");
        name = name.substring(i+1).toLowerCase();
        List<V> entities = dao.getAll();
        V entity = EntityVersions.get(klass).current();

        List<String> headers = new ArrayList<>(entity.spec().names());
        List<String> allHeaders = new ArrayList<>(headers);
        allHeaders.addAll(extraHeaders);


        try (
                final OutputStream outputStream = java.nio.file.Files.newOutputStream(outputDir.resolve(fileName), StandardOpenOption.CREATE_NEW);
                final PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)), false);
                final CSVPrinter csv = new CSVPrinter(writer, CSVFormat.EXCEL.withQuoteMode(QuoteMode.NON_NUMERIC))
        ) {
            csv.printRecord(allHeaders);

            for (V e : entities) {

                List row = e.get(headers);
                row.addAll(extraFields.apply(e));
                csv.printRecord( row );
            }
        }
    }

    public List<SourceList> importSourceListsFromCSV(Path seedsPath) throws IOException {
        try (
                Reader reader = java.nio.file.Files.newBufferedReader(seedsPath);
                CSVReader csvReader = new CSVReader(reader);
        ) {
            List<SourceList> sourceLists = new ArrayList<>();
            Iterator<String[]> itr = csvReader.iterator();
            String[] headers = itr.next();

            Map<String,Integer> headerMap = new HashMap<>();

            for(int i = 0; i < headers.length; ++i) {
                headerMap.put(headers[i], i);
            }

            while(itr.hasNext()) {
                String[] row = itr.next();

                String LIST_NAME  = row[headerMap.get(SourceList.LIST_NAME)];
                String KEYWORDS  = row[headerMap.get(SourceList.KEYWORDS)].trim();
                String DESK_NAME  = row[headerMap.get(Desk.DESK_NAME)].trim();

                Optional<Desk> maybeDesk = deskDAO.getByUnique(Desk.DESK_NAME, DESK_NAME);

                if(!maybeDesk.isPresent()) {
                    logger.warn("desk not found {}", DESK_NAME);
                    continue;
                }

                Desk desk = maybeDesk.get();

                SourceList list = EntityVersions.get(SourceList.class).current()
                        .put(SourceList.LIST_NAME, LIST_NAME)
                        .put(SourceList.KEYWORDS, KEYWORDS)
                        .put(SourceList.DESK_ID, desk.id())
                        ;

                try {
                    sourceLists.add(sourceListDAO.create(list));
//                    System.out.println(source);
                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                    //already exists?
                }
            }
            return sourceLists;
        }
    }

    public List<SourceSourceList> importSourceSourceListsFromCSV(Path seedsPath) throws IOException {
        try (
                Reader reader = java.nio.file.Files.newBufferedReader(seedsPath);
                CSVReader csvReader = new CSVReader(reader);
        ) {
            List<SourceSourceList> sourceSourceList = new ArrayList<>();
            Iterator<String[]> itr = csvReader.iterator();
            String[] headers = itr.next();

            Map<String,Integer> headerMap = new HashMap<>();

            for(int i = 0; i < headers.length; ++i) {
                headerMap.put(headers[i], i);
            }

            while(itr.hasNext()) {
                String[] row = itr.next();

                String STANDARD_NAME  = row[headerMap.get(Source.STANDARD_NAME)].trim();
                String LIST_NAME  = row[headerMap.get(SourceList.LIST_NAME)].trim();

                Optional<Source> maybeSource = sourceDAO.byName(STANDARD_NAME);
                Optional<SourceList> maybeSourceList = sourceListDAO.byName(LIST_NAME);

                try {
                    if(maybeSource.isPresent() && maybeSourceList.isPresent()) {

                        sourceSourceListDAO.link(maybeSource.get(), maybeSourceList.get());
                    } else {

                        logger.warn("source or source list not found {} {}", STANDARD_NAME, LIST_NAME );

                    }
                } catch (RuntimeException e) {
                    logger.warn(e.getMessage(), e);
                    //already exists?
                }
            }
            return sourceSourceList;
        }
    }



    public List<Source> importSourcesFromCSV(Path seedsPath, Source defaultSource) throws IOException {
        try (
            Reader reader = java.nio.file.Files.newBufferedReader(seedsPath);
            CSVReader csvReader = new CSVReader(reader);
        ) {
            List<Source> sources = new ArrayList<>();
            Iterator<String[]> itr = csvReader.iterator();
            String[] headers = itr.next();

            Map<String,Integer> headerMap = new HashMap<>();

            for(int i = 0; i < headers.length; ++i) {
                headerMap.put(headers[i], i);
            }

            Gson gson = new Gson();

            while(itr.hasNext()) {
                String[] row = itr.next();

                String STANDARD_NAME  = row[headerMap.get(Source.STANDARD_NAME)];
                String LINK  = row[headerMap.get(Source.LINK)].trim();
                String COUNTRY  = row[headerMap.get(Source.COUNTRY)].trim();
                String REGION  = row[headerMap.get(Source.REGION)].trim();
                String DATE_FORMAT  = row[headerMap.get(Source.DATE_FORMAT)].trim();
                Boolean CRAWL_DISABLED  = Boolean.valueOf(row[headerMap.get(Source.CRAWL_DISABLED)]);
                List<String> EXAMPLE_URLS  =  gson.fromJson(row[headerMap.get(Source.EXAMPLE_URLS)], List.class);

                Source source = EntityVersions.get(Source.class).current()
                    .put(Source.STANDARD_NAME, STANDARD_NAME)
                    .put(Source.LINK, LINK)
                    .put(Source.CRAWL_DISABLED, CRAWL_DISABLED)
                    .put(Source.EXAMPLE_URLS, EXAMPLE_URLS == null ? ImmutableList.of() : EXAMPLE_URLS);

                try {
                    List<String> LANGUAGES  = Arrays.asList(row[headerMap.get(Source.LANGUAGES)].split("[;,]"));
                    source = source.put(Source.LANGUAGES, LANGUAGES);
                } catch (NullPointerException e) {
                    source = source.put(Source.LANGUAGES, defaultSource.get(Source.LANGUAGES));
                }

                try {
                    sources.add(sourceDAO.create(source));
//                    System.out.println(source);
                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                    //already exists?
                }
            }
            return sources;
        }
    }

    public SourceList createSourceList(String name, String query) {
        SourceList sourceList = EntityVersions.get(SourceList.class).current()
                .put(SourceList.LIST_NAME, name)
                .put(SourceList.KEYWORDS, query)
                ;
        return sourceListDAO.create(sourceList);
    }

    public void link(List<Source> sources, SourceList sourceList) {
        for(Source source : sources) {
            sourceSourceListDAO.link(source, sourceList);
        }
    }

    public void exportSourceArticlesToCSV(Path dir, Source source) {

        String id = getID(source);

        try {
            exportCSV.articles(dir.resolve(id+".csv"), () -> articleDAO.bySource(source));
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }

    }

    public void exportSourceListArticlesToCSV(Path dir, String sourceListName) {

        SourceList sourceList = sourceListDAO.getByUnique(SourceList.LIST_NAME, sourceListName).get();
        List<Source> sources = sourceDAO.byList(sourceList);
        for(Source source : sources) {
            if(!source.isTrue(Source.CRAWL_DISABLED)) {

                exportSourceArticlesToCSV(dir, source);
            }
        }
    }

    public String getKeywordsFromCSV(Path path) throws IOException {

        try (
            Reader reader = java.nio.file.Files.newBufferedReader(path);
            CSVReader csvReader = new CSVReader(reader);
        ) {
            List<String> keywords = new ArrayList<>();
            Iterator<String[]> itr = csvReader.iterator();
            String[] headers = itr.next();

            while(itr.hasNext()) {
                String[] row = itr.next();

                String keyword = row[0].trim();
                if(keyword.contains(" ")) {
                    keyword = "\"" + keyword + "\"";
                }

                keywords.add(keyword);

            }

            String query = "(" + StringUtils.join(keywords, " ") + ")";

            return query;
        }
    }


    public void createFakeNetSourceList() {
        Source base =  EntityVersions.get(Source.class).current();

        List<Source> sources = new ArrayList<>();
        for(int i = 0; i < 20; ++i){
            String url = "http://www."+i+".com:5000";
            Source source = base.put(Source.LINK, url)
                    .put(Source.STANDARD_NAME, Integer.toString(i))
                    .put(Source.CRAWL_SCRAPER_PATH, "/Users/pengqiwei/Downloads/My/PhDs/acled_thing/springcrawler/testscrapers/generic")
                    .put(Source.TIMEZONE, "Europe/London")
                    .put(Source.LOCALES, ImmutableList.of("en"))
                    .put(Source.DATE_FORMAT, ImmutableList.of("ISO:/yyyy-MM-dd"))
                    .put(Source.CRAWL_SCHEDULE, "0 0 20 ? * TUE,FRI *")
                    ;
            sources.add(source);
        }
//        sources = sourceDAO.create(sources); // do upsert to overwrite
        sources = sourceDAO.upsert(sources); // do upsert to overwrite


        SourceList list = EntityVersions.get(SourceList.class).current()
                .put(SourceList.LIST_NAME, "fake-net");

        list = sourceListDAO.create(list);

        link(sources, list);

    }

//    @Override
    public void run(String... args) throws Exception {
//        deleteNonMatchingArticles();
//        recoverArticleDates();
//        linkExisting();

//        createFakeNetSourceList();

//        exportSourceDataCSV(Paths.get("all-source-data"));

//        exportSourceDataJSON(Paths.get("all-source-data"));
//
//        importSourceDataJSON(Paths.get("all-source-data"));

//        exportSourceDataJSON(Paths.get("all-source-data-2"));

//        Source defaultSource = EntityVersions.get(Source.class).current();
//        exportSourceListArticlesToCSV(Paths.get("mexico-2018"), "mexico-back-code-2018");

//        List<Source> sources = importSourcesFromCSV(Paths.get("/home/sw206/git/acledcamundaspringboot/data/europe/balkans-source-list.csv"), defaultSource);
//        SourceList sourceList = createSourceList("balkans", "(clashing demonstrate demonstrated demonstraters demonstrates demonstrating demonstration demonstrations demonstrator demonstrator demonstrators detonated explode exploded explodes exploding explosion explosions gun fire gunfire kidnapping kill killed killer killers killing knifed lynched lynching march (lower case only) marched marches marching mob justice Molotov picket picketers picketing protest protested protester protesters protesting protestor protestors protests raid raided raiding raids rallied rallies rallying rape raped rapes raping rapist revolt revolted revolts riot rioted rioter rioters rioting riots set on fire shooter shooters shooting shoots shot stab stabbed stabbing strike striked strikes threw stones throwing stones to shoot turmoil unrest vigilante vigilantism violence wounded wounding wounds aktivista aktivisti bitka bitke bodenje bomba bombardovanje bombardovano bombaš bombaši bombe boreći se boriti se demonstracija demonstracije demonstrant demonstranti demonstrira demonstrirajući demonstrirali demonstrirati detonirana ekplodirala eksplozija eksplodira eksplodirati eksplozija eksplozije eksplozivna granatirano iz zasede iz zasjede izboden izbosti izveli raciju izvevši raciju kamenovali kamenovanje kidnapovanje linčovan linčovanje marš marširali marširanje maršovi Molotovljev napad napadač napadači napadajući napadi napadnut napasti iz zasede nasilje nemir okršaj okupili se okupivši se okupljeni osvetnik osvetništvo otmica pobuna pobune pobunili se pobunjeni prebijanje prebijen pretučen prosvjed prosvjedi prosvjednici prosvjednik prosvjedovali prosvjedovanje protest protestant protesti protestirali protestovali protestovanje pucanje pucao pucati pucnjava racija racije rane ranjavanje ranjen revolt revolti revoltirani rulja shod silovana silovanja silovanje silovatelj silujući strelac strelci sukob sukobi sukobili se sukobivši se ubica ubice ubijen ubistvo ubiti ubojica ubojice ubojstvo udarac udaren udari upucan zapaljen zaseda zasede zasjeda zasjede žrtva žrtve bastisje betejë bomba bombardim bombë demonstratë demonstrim demonstrues dhunë dhunë eksplodim forcë konflikt kryengritës kryengritje marshim ndeshje përdhunim përdhunime përdhunuar përplasje plagosur pritë protestat protestë protestuan protestuar protestues protestuesit protestuesit qitje revoltë rrahje rrebelim shkatërrues shpërthim shqetësim sulm sulmuar sulmues sulmuesit të xhiruar Therrja trazim trazire trazirë vetëgjyqësisë viktima vrarë vras vrasje zjarr активиста активисти битка битке бодење бомба бомбардовање бомбардовано бомбаш бомбаши бомбе борећи се борити се демонстрација демонстрације демонстрант демонстранти демонстрира демонстрирајући демонстрирали демонстрирати детонирана екплодирала експлозија експлодира експлодирати експлозија експлозије експлозивна гранатирано из заседе из засједе избоден избости извели рацију извевши рацију каменовали каменовање киднаповање линчован линчовање марш марширали марширање маршови Молотовљев напад нападач нападачи нападајући напади нападнут напасти из заседе насиље немир окршај окупили се окупивши се окупљени осветник осветништво отмица побуна побуне побунили се побуњени пребијање пребијен претучен просвјед просвједи просвједници просвједник просвједовали просвједовање протест протестант протести протестирали протестовали протестовање пуцање пуцао пуцати пуцњава рација рације ране рањавање рањен револт револти револтирани руља сход силована силовања силовање силоватељ силујући стрелац стрелци сукоб сукоби сукобили се сукобивши се убица убице убијен убиство убити убојица убојице убојство ударац ударен удари упуцан запаљен заседа заседе засједа засједе жртва жртве )");
//        link(sources, sourceList);

//        String mexico2018Query = getKeywordsFromCSV(Paths.get("/home/sw206/Dropbox/acled/spec/Mexico Backcoding Keywords_0520.csv"));
//        List<Source> sources = importSourcesFromCSV(Paths.get("/home/sw206/Dropbox/acled/spec/mexico-backcode-2018.csv"), defaultSource.put(Source.LANGUAGES, ImmutableList.of("Spanish") ));
//        SourceList sourceList = createSourceList("mexico-back-code-2018", mexico2018Query);
//        link(sources, sourceList);

        Source source1 = sourceDAO.getByUnique(Source.STANDARD_NAME, "MiMorelia").get();
        Source source2 = sourceDAO.getByUnique(Source.STANDARD_NAME, "Milenio").get();

        List<Source> sources = ImmutableList.of(source1, source2);

        SourceList sourceList = createSourceList("mexico-1", "(activist activists ambush ambushed ambushes ambushing arso\n" +
                "n assault assaulted attack attacked attacker attackers attacking attacks battle battled battles beaten be\n" +
                "ating blast bomb bombed bomber bombers bombing bombs casualties casualty clash clashed clashes clashing d\n" +
                "emonstrate demonstrated demonstraters demonstrates demonstrating demonstration demonstrations detonated e\n" +
                "xplode exploded explodes exploding explosion explosions \"gun fire\" gunfire kidnapping killed killer kil\n" +
                "lers killing knifed lynched lynching march marched marches marching \"mob justice\" molotov picket picket\n" +
                "ers picketing protest protested protester protesters protesting protestor protestors protests raid raided\n" +
                " raiding raids rallied rallies rallying rape raped rapes raping rapist revolt revolts riot rioted rioter \n" +
                "rioters rioting riots \"set on fire\" shooter shooters shooting shoots shot stab stabbed stabbing strike \n" +
                "striked strikes \"threw stones\" \"throwing stones\" \"to shoot\" turmoil unrest vigilante vigilantism vi\n" +
                "olence wounded narco drugs cartel beheaded \"narco message\" \"public hanging of corpses\" \"burned body\n" +
                "\" \"burned bodies\" tied gagged \"shot of grace\" \"body encased in cement\" \"hit by a wooden board\" mu\n" +
                "tilated dismembered \"signs of torture\" \"human remains in plastic bags\" \"body wrapped in a blanket\" \n" +
                "ransom kidnapper kidnapped hooded masked cocaine methamphetamine marijuana \"signs of violence\" \"advanc\n" +
                "ed state of decomposition\" \"mass graves\" grave \"organized crime\" \"criminal band\" \"hunger strike\"\n" +
                " arrests arrest \"to rescue\" migrant immigrant immigrants seizure seize extortion \"crystal meth\" \"thr\n" +
                "oat slit\" abduction \"signs of restraint\" cardboard \"carboard message\" \"threatening message\" \"burn\n" +
                "ed with acid\" corpse \"signs of sexual violence\" \"signs of rape\" handcuffs handcuffed \"feet tied\" t\n" +
                "orso \"human head\" execute \"armed men\" criminals \"community police\" self-defense \"fuel theft\" \"bo\n" +
                "dy found inside of a bin\" \"narco blockage\" \"blockage of roads\" \"fire weapon\" displaced body \"bone\n" +
                " remains\" intercepted \"a chase\" captured \"drug dealing\" \"drug dealer\" \"poppy plants\" \"hands tie\n" +
                "d\" bodies activista defensor defensora activistas defensores defensoras emboscada emboscó emboscaron emb\n" +
                "oscados emboscando \"incendio provocado\" \"incendio malicioso\" \"incendio intencional\" agresión acoso \n" +
                "asalto atacado atacada atacados agredido agredida agredidos acosado acosada acosados asaltado asaltada as\n" +
                "altados agredieron acosaron asaltaron agredió acosó asaltó ataque \"ataque a balazos\" atacó atacaron ata\n" +
                "cante aggresor perpetrador asaltante atacantes agresores perpetradores asaltantes atacando ataques batall\n" +
                "a combate combatieron batallas combates golpeado sometido golpeada sometida golpeando sometiendo golperar\n" +
                "on golpeó explosión ráfaga voladura bomba bombardeado bombardero bombarderos bombardeando bombas víctimas\n" +
                " bajas muertos víctima baja muerto choque conflicto enfrentamiento chocó chocaron \"se enfrentó\" \"se en\n" +
                "frentaron\" choques enfrentamientos chocando enfrentando demostrar manfiestar demostraron manifestaron ma\n" +
                "nifestantes demuestra manifiesta demostrando manifestando demostración manifestación demostraciones manif\n" +
                "estaciones detonó explotar explotó explotaba explotando estallido explosiones estallidos disparo tiro tir\n" +
                "oteo disparos tiros tiroteos secuestro plagio rapto levantón asesinado asesinada ejecutado ejecutada ases\n" +
                "inaron asesinan asesino asesinos asesinato ejecución homicidio puñalado puñalada linchado linchada lincha\n" +
                "ndos lincharon ajusticiarion linchando ajusticiar marcha marchó marcharon marchas marchando \"justicia po\n" +
                "pular\" \"justicia a mano propia\" piquete plantón piqueteros \"realizando un piquete\" \"montando un piq\n" +
                "uete\" protesta protestó protestaron manifestante protestando protestas plantones incursión redada incurs\n" +
                "ionó incursionando incursiones redadas movilizó movilizaron movilizan movilizando violación violado viola\n" +
                "da viola violando violador violadora revuelta rebelión sublevación revueltas rebeliones sublevaciones dis\n" +
                "turbio motín desorden \"riña prisión\" \"riña cárcel\" \"riña penitenciario\" peleaban amontinó amotinaba\n" +
                "n \"causaban disturbios\" pelearon amontinaron \"causaron disturbios\" alborotador sublevado alborotadore\n" +
                "s sublevados desordenando alborotando sublevando desordenes alborotos disturbios \"prender fuego\" incedi\n" +
                "ar \"prendieron fuego\" incendiaron \"prendió fuego\" incendió tirador pistolero tiradores pistoleros bal\n" +
                "azo balazos \"ráfaga de balas\" \"dar balas\" \"dar plomo\" tiró tiraba tiraron \"dieron balas\" \"dio ba\n" +
                "las\" \"dieron plomo\" plomeó plomearlo acribillado acribilaron \"abrieron fuego\" \"abrió fuego\" ballea\n" +
                "ron dispararon disparan disaparo disparado disparada disparar punzada estocada apuñalado apuñalada punzad\n" +
                "o estocado apuñalar punzar estocar huelga golpe paro \"toma de\" \"toma del\" holgó golpearon tomaron tom\n" +
                "an huelgas golpes paros tomas \"lanzó piedras\" \"tiró piedras\" \"lanzaron piedras\" \"tiraron piedras\"\n" +
                " \"lanzando piedras\" balear balearon agitación crisis turbulencia malestar \"vigilante parapolicial\" co\n" +
                "munitario comunitarios \"vigilancia parapolicial\" violencia herida herido heridos heridas baleado balead\n" +
                "a baleadas baleados lesionado lesionados lesionada lesionadas narcos drogas estupefacientes narcóticos ca\n" +
                "rteles decapitado decapitados decapitada decapitadas \"narco mensaje\" ahorcamiento ahorcado ahorcada \"c\n" +
                "uerpo quemado\" \"cuerpo calcinado\" quemaduras \"cuerpos quemados\" \"cuerpos calcinados\" atado atada a\n" +
                "tados atadas amarrado amarrada amordazado amordazada \"tiro de gracia\" \"cuerpo encajonado en cemento\" \n" +
                "\"cuerpo encajonado en concreto\" \"cuerpo en concreto\" \"cuerpo en bloques de concreto\" tableado table\n" +
                "ada tablear tablearon tableados mutilado multilada multilados multiladas desmembrado descuartizado desmem\n" +
                "brados desmembrada desmembradas descuartizada descuartizados descuartizadas \"signos de tortura\" \"huell\n" +
                "a de tortura\" \"huellas de tortura\" \"restos humanos\" embolzado embolzada embolzados embolzadas encobi\n" +
                "jado encobijada encobijados encobijadas rescate rescates secuestrador plagiador secuestrado secuestrada s\n" +
                "ecuestrados secuestradas secuestraron plagiado plagiada plagiados plagiadas plagiaron raptado raptada rap\n" +
                "tados raptadas raptaron levantado levantada levantados levantadas levantaron \"privados de libertad\" \"p\n" +
                "rivado de su libertad\" \"privada de su libertad\" encapuchado encapuchada paristas encapuchados enmascar\n" +
                "ado enmascarada cocaina metanfetamina marihuana \"plantas de marihuana\" \"signos de violencia\" \"marcas\n" +
                " de violencia\" \"estado de descomposición\" \"putrefacción avanzada\" \"fosas comunes\" \"fosas clandest\n" +
                "inas\" fosa \"delincuencia organizada\" \"banda criminal\" pandillas \"grupo criminal\" \"huelga de hambr\n" +
                "e\" arrestos detenciones arresto arrestado arrestada arrestados arrestadas arrestaron detienen arrestan d\n" +
                "etener detuvieron rescatar rescantan rescataron rescatado rescatada rescatados rescatadas migrante migran\n" +
                "tes caravana \"caravana de migrantes\" inmigrante inmigrantes \"inmigrantes indocumentados\" \"inmigrante\n" +
                "s ilegales\" incautación decomiso incautar decomisar extorsión \"meta crystal\" crystal cristal degollado\n" +
                " degollada degollados secuestros \"señales de restricción\" \"signos de restricción\" cartulina \"mensaje\n" +
                " en cartulina\" \"mensaje amenazante\" \"quemado con ácido\" \"cadaver u occiso\" \"signos de violencia s\n" +
                "exual\" \"señales de violencia sexual\" \"signos señales de violación\" \"señales de violación\" esposas \n" +
                "esposado esposados \"atado de pies\" maniatado \"cabeza humana\" ejecutar ejecutaron ejecutan \"hombres a\n" +
                "rmados\" pandilleros delincuentes \"policia comunitaria\" autodefensas huachicolero entambado \"narco blo\n" +
                "queo\" bloqueo bloquean bloqueado bloqueada \"arma de fuego\" desplazado desplazada desplazados desplazad\n" +
                "as cuerpo cuerpos cadáver \"restos óseos\" interceptaron interceptada interceptado persecución capturar c\n" +
                "apturaron capturado capturada narcomenudeo narcomenudista narcomenudistas amapola \"plantas de amapola\"");

        link(sources, sourceList);
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Util.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
}