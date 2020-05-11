package com.casm.acled.crawler;


import com.casm.acled.camunda.BusinessKeys;
import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.scraper.dates.*;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.dao.entities.SourceSourceListDAO;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.opencsv.CSVReader;
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

    private static final Pattern PROTOCOL = Pattern.compile(".*:[/]{2}", Pattern.CASE_INSENSITIVE);

    public static String ensureHTTP(String url, boolean https) {

        Matcher matcher = PROTOCOL.matcher(url.toLowerCase());

        if(matcher.find()) {
            // get rid of any previous or malformed protocol
            url = matcher.replaceFirst("");
        }
        url =  (https) ? "https://" + url : "http://" + url;

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
        URI uri = URI.create(urlString);
        String host = uri.getHost();
        return (host.startsWith("www")) ? host.substring(4) : host;
    }

    // Processes a M52 job json to scraper rules
    public static String processJobJSON(String json) {
        JSONObject jobj = new JSONObject(json);
        return jobj.getJSONArray("components").getJSONObject(0).getJSONObject("opts").getJSONArray("fields").toString();
    }

    public static String processScraperJSON(String json){
        // BUG FOUND - NEED TO USE job.json!!
        return null;
    }

    // returns a web scraper based on a job spect of last_scrape file
    public static String processJSON(File scraperLocation) throws IOException {
        String json = Files.asCharSource(scraperLocation, Charset.defaultCharset()).read();
        return (scraperLocation.getName().equals("last_scrape.json")) ? processScraperJSON(json) : processJobJSON(json);
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


    private List<Source> importSourcesFromCSV(Path seedsPath) throws IOException {
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

                String LINK  = row[headerMap.get(Source.LINK)];
                String STANDARD_NAME  = row[headerMap.get(Source.STANDARD_NAME)];
                String LANGUAGE  = row[headerMap.get(Source.LANGUAGE)];
                String CRAWL_DISABLED  = row[headerMap.get(Source.CRAWL_DISABLED)];
                List<String> EXAMPLE_URLS  =  gson.fromJson(row[headerMap.get(Source.EXAMPLE_URLS)], List.class);

                Source source = EntityVersions.get(Source.class).current()
                    .put(Source.STANDARD_NAME, STANDARD_NAME)
                    .put(Source.LINK, LINK)
                    .put(Source.LANGUAGE, LANGUAGE)
                    .put(Source.CRAWL_DISABLED, CRAWL_DISABLED)
                    .put(Source.EXAMPLE_URLS, EXAMPLE_URLS == null ? ImmutableList.of() : EXAMPLE_URLS);

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

    public void run(String... args) throws Exception {
//        deleteNonMatchingArticles();
//        recoverArticleDates();
//        linkExisting();

//        List<Source> sources = importSourcesFromCSV(Paths.get("/home/sw206/git/acledcamundaspringboot/data/europe/balkans-source-list.csv"));
//        SourceList sourceList = createSourceList("balkans", "( activist activists ambush ambushed ambushes ambushing arson assault assaulted attack attacked attacker attackers attacking attacks battle battled battles battling beaten beating blast bomb bombed bomber bombers bombing bombs casualties casualty clash clashed clashes clashing demonstrate demonstrated demonstraters demonstrates demonstrating demonstration demonstrations demonstrator demonstrator demonstrators detonated explode exploded explodes exploding explosion explosions \"gun fire\" gunfire kidnapping kill killed killer killers killing knifed lynched lynching march marched marches marching \"mob justice\" Molotov picket picketers picketing protest protested protester protesters protesting protestor protestors protests raid raided raiding raids rallied rallies rallying rape raped rapes raping rapist revolt revolted revolts riot rioted rioter rioters rioting riots \"set on fire\" shooter shooters shooting shoots shot stab stabbed stabbing strike striked strikes \"threw stones\" \"throwing stones\" \"to shoot\" turmoil unrest vigilante vigilantism violence wounded wounding wounds aktivista aktivisti bitka bitke bodenje bomba bombardovanje bombardovano bombaš bombaši bombe \"boreći se\" \"boriti se\" demonstracija demonstracije demonstrant demonstranti demonstrira demonstrirajući demonstrirali demonstrirati detonirana ekplodirala eksplozija eksplodira eksplodirati eksplozija eksplozije eksplozivna granatirano \"iz zasede\" \"iz zasjede\" izboden izbosti \"izveli raciju\" \"izvevši raciju\" kamenovali kamenovanje kidnapovanje linčovan linčovanje marš marširali marširanje maršovi Molotovljev napad napadač napadači napadajući napadi napadnut \"napasti iz zasede\" nasilje nemir okršaj \"okupili se\" \"okupivši se\" okupljeni osvetnik osvetništvo otmica pobuna pobune \"pobunili se\" pobunjeni prebijanje prebijen pretučen prosvjed prosvjedi prosvjednici prosvjednik prosvjedovali prosvjedovanje protest protestant protesti protestirali protestovali protestovanje pucanje pucao pucati pucnjava racija racije rane ranjavanje ranjen revolt revolti revoltirani rulja shod silovana silovanja silovanje silovatelj silujući strelac strelci sukob sukobi \"sukobili se\" \"sukobivši se\" ubica ubice ubijen ubistvo ubiti ubojica ubojice ubojstvo udarac udaren udari upucan zapaljen zaseda zasede zasjeda zasjede žrtva žrtve bastisje betejë bomba bombardim bombë demonstratë demonstrim demonstrues dhunë dhunë eksplodim forcë konflikt kryengritës kryengritje marshim ndeshje përdhunim përdhunime përdhunuar përplasje plagosur pritë protestat protestë protestuan protestuar protestues protestuesit protestuesit qitje revoltë rrahje rrebelim shkatërrues shpërthim shqetësim sulm sulmuar sulmues sulmuesit \"të xhiruar\" therrja trazim trazire trazirë vetëgjyqësisë viktima vrarë vras vrasje zjarr )");
//        link(sources, sourceList);
        List<Source> sources = importSourcesFromCSV(Paths.get("/home/sw206/Dropbox/acled/spec/mexico-backcode-2018.csv"));
        SourceList sourceList = createSourceList("mexico-back-code-2018", "");
        link(sources, sourceList);
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Util.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
}