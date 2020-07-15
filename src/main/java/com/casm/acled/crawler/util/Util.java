package com.casm.acled.crawler.util;


import com.casm.acled.camunda.BusinessKeys;
import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.scraper.ACLEDScraper;
import com.casm.acled.crawler.scraper.dates.*;
import com.casm.acled.dao.entities.*;
import com.casm.acled.dao.util.ExportCSV;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.desk.Desk;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;

import com.casm.acled.entities.sourcesourcelist.SourceSourceList;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.opencsv.CSVReader;
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


import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


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

            articleDAO.update(article);
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

                    articleDAO.update(article);
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

        sourceDAO.update(source);
    }


    public void exportSourceData() {


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
                    .put(Source.CRAWL_SCRAPER_PATH, "/home/sw206/git/springcrawler/testscrapers/generic")
                    .put(Source.TIMEZONE, "Europe/London")
                    .put(Source.LOCALES, ImmutableList.of("en"))
                    .put(Source.DATE_FORMAT, ImmutableList.of("ISO:/yyyy-MM-dd"))
                    ;
            sources.add(source);
        }
        sources = sourceDAO.create(sources);

        SourceList list = EntityVersions.get(SourceList.class).current()
                .put(SourceList.LIST_NAME, "fake-net");

        list = sourceListDAO.create(list);

        link(sources, list);

    }

    public void run(String... args) throws Exception {
//        deleteNonMatchingArticles();
//        recoverArticleDates();
//        linkExisting();

//        createFakeNetSourceList();

        Source defaultSource = EntityVersions.get(Source.class).current();
//        exportSourceListArticlesToCSV(Paths.get("mexico-2018"), "mexico-back-code-2018");

        List<Source> sources = importSourcesFromCSV(Paths.get("/home/sw206/git/acledcamundaspringboot/data/europe/balkans-source-list.csv"), defaultSource);
        SourceList sourceList = createSourceList("balkans", "(clashing demonstrate demonstrated demonstraters demonstrates demonstrating demonstration demonstrations demonstrator demonstrator demonstrators detonated explode exploded explodes exploding explosion explosions gun fire gunfire kidnapping kill killed killer killers killing knifed lynched lynching march (lower case only) marched marches marching mob justice Molotov picket picketers picketing protest protested protester protesters protesting protestor protestors protests raid raided raiding raids rallied rallies rallying rape raped rapes raping rapist revolt revolted revolts riot rioted rioter rioters rioting riots set on fire shooter shooters shooting shoots shot stab stabbed stabbing strike striked strikes threw stones throwing stones to shoot turmoil unrest vigilante vigilantism violence wounded wounding wounds aktivista aktivisti bitka bitke bodenje bomba bombardovanje bombardovano bombaš bombaši bombe boreći se boriti se demonstracija demonstracije demonstrant demonstranti demonstrira demonstrirajući demonstrirali demonstrirati detonirana ekplodirala eksplozija eksplodira eksplodirati eksplozija eksplozije eksplozivna granatirano iz zasede iz zasjede izboden izbosti izveli raciju izvevši raciju kamenovali kamenovanje kidnapovanje linčovan linčovanje marš marširali marširanje maršovi Molotovljev napad napadač napadači napadajući napadi napadnut napasti iz zasede nasilje nemir okršaj okupili se okupivši se okupljeni osvetnik osvetništvo otmica pobuna pobune pobunili se pobunjeni prebijanje prebijen pretučen prosvjed prosvjedi prosvjednici prosvjednik prosvjedovali prosvjedovanje protest protestant protesti protestirali protestovali protestovanje pucanje pucao pucati pucnjava racija racije rane ranjavanje ranjen revolt revolti revoltirani rulja shod silovana silovanja silovanje silovatelj silujući strelac strelci sukob sukobi sukobili se sukobivši se ubica ubice ubijen ubistvo ubiti ubojica ubojice ubojstvo udarac udaren udari upucan zapaljen zaseda zasede zasjeda zasjede žrtva žrtve bastisje betejë bomba bombardim bombë demonstratë demonstrim demonstrues dhunë dhunë eksplodim forcë konflikt kryengritës kryengritje marshim ndeshje përdhunim përdhunime përdhunuar përplasje plagosur pritë protestat protestë protestuan protestuar protestues protestuesit protestuesit qitje revoltë rrahje rrebelim shkatërrues shpërthim shqetësim sulm sulmuar sulmues sulmuesit të xhiruar Therrja trazim trazire trazirë vetëgjyqësisë viktima vrarë vras vrasje zjarr активиста активисти битка битке бодење бомба бомбардовање бомбардовано бомбаш бомбаши бомбе борећи се борити се демонстрација демонстрације демонстрант демонстранти демонстрира демонстрирајући демонстрирали демонстрирати детонирана екплодирала експлозија експлодира експлодирати експлозија експлозије експлозивна гранатирано из заседе из засједе избоден избости извели рацију извевши рацију каменовали каменовање киднаповање линчован линчовање марш марширали марширање маршови Молотовљев напад нападач нападачи нападајући напади нападнут напасти из заседе насиље немир окршај окупили се окупивши се окупљени осветник осветништво отмица побуна побуне побунили се побуњени пребијање пребијен претучен просвјед просвједи просвједници просвједник просвједовали просвједовање протест протестант протести протестирали протестовали протестовање пуцање пуцао пуцати пуцњава рација рације ране рањавање рањен револт револти револтирани руља сход силована силовања силовање силоватељ силујући стрелац стрелци сукоб сукоби сукобили се сукобивши се убица убице убијен убиство убити убојица убојице убојство ударац ударен удари упуцан запаљен заседа заседе засједа засједе жртва жртве )");
//        link(sources, sourceList);

//        String mexico2018Query = getKeywordsFromCSV(Paths.get("/home/sw206/Dropbox/acled/spec/Mexico Backcoding Keywords_0520.csv"));
//        List<Source> sources = importSourcesFromCSV(Paths.get("/home/sw206/Dropbox/acled/spec/mexico-backcode-2018.csv"), defaultSource.put(Source.LANGUAGES, ImmutableList.of("Spanish") ));
//        SourceList sourceList = createSourceList("mexico-back-code-2018", mexico2018Query);
//        link(sources, sourceList);
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Util.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
}