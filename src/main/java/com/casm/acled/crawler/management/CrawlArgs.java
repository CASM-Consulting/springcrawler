package com.casm.acled.crawler.management;

import com.beust.jcommander.Parameter;
import com.casm.acled.crawler.Crawl;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.enioka.jqm.api.JobRequest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class CrawlArgs {

    public static class Flags {
        public static final String DISABLE_ON_FAIL = "DISABLE_ON_FAIL";
    }

    private final SourceDAO sourceDAO;

    private final SourceListDAO sourceListDAO;

    public static class Raw {
        @Parameter(names = "-p", description = "Program")
        public String program;

        @Parameter(names = "-jqm", description = "JQM Program")
        public String jqmProgram = "JQMSpringCollectorV1";

        @Parameter(names = "-s", description = "Source")
        public String source;

        @Parameter(names = "-sl", description = "Source lists")
        public List<String> sourceLists;

        @Parameter(names = "-cid", description = "Crawl ID override")
        public String crawlId;

        @Parameter(names = "-n", description = "Quit after this many")
        public Integer maxArticles = -1;

        @Parameter(names = "-d", description = "Crawl depth")
        public Integer depth = 0;

        @Parameter(names = "-pl", description = "Politeness delay")
        public Integer politeness = 100;

        @Parameter(names = "-ism", description = "Ignore site maps")
        public Boolean ignoreSiteMap = false;

//        @Parameter(names = "-osm", description = "Only Site maps")
//        public Boolean onlySiteMap = false;

        @Parameter(names = "-sk", description = "Skip keyword matching")
        public Boolean skipKeywords = false;

        @Parameter(names = "-f", description = "From date [yyyy-mm-dd]")
        public String from = null;

        @Parameter(names = "-t", description = "To date [yyyy-mm-dd]")
        public String to = null;

        @Parameter(names = "-wd", description = "Working directory")
        public String workingDir = null;

        @Parameter(names = "-sd", description = "Scrapers directory")
        public String scrapersDir = null;

        @Parameter(names = "-fl", description = "Flags")
        public List<String> flags;
    }


    public Raw raw;

    public String program;

    public String jqmProgram;

    public Source source;
    public static final String SOURCE = "SOURCE";

    public List<SourceList> sourceLists;
    public static final String SOURCE_LISTS = "SOURCE_LISTS";

    public String crawlId;
    public static final String CRAWL_ID = "CRAWL_ID";

    public Integer maxArticle;
    public static final String MAX_ARTICLES = "MAX_ARTICLES";

    public Integer depth;
    public static final String DEPTH = "DEPTH";

    public Integer politeness;
    public static final String POLITENESS = "POLITENESS";

    public Boolean ignoreSiteMap;
    public static final String IGNORE_SITE_MAP = "IGNORE_SITE_MAP";

//    public Boolean onlySiteMap;
//    public static final String ONLY_SITE_MAP = "ONLY_SITE_MAP";

    public Boolean skipKeywords;
    public static final String SKIP_KEYWORDS = "SKIP_KEYWORDS";

    public LocalDate from;
    public static final String FROM = "FROM";

    public LocalDate to;
    public static final String TO = "TO";

    public Path workingDir;
    public static final String WORKING_DIR = "WORKING_DIR";

    public Path scrapersDir;
    public static final String SCRAPERS_DIR = "SCRAPERS_DIR";

    public List<String> flags;
    public Set<String> flagSet;
    public static final String FLAGS = "FLAGS";


    public CrawlArgs(SourceDAO sourceDAO, SourceListDAO sourceListDAO) {
        raw = new Raw();
        this.sourceDAO = sourceDAO;
        this.sourceListDAO = sourceListDAO;
    }



    public void init() {

        if (sourceDAO != null && sourceListDAO != null) {

            if (raw.source != null) {
                source = sourceDAO.byName(raw.source).orElseThrow(()->new RuntimeException("Source Name not found: " + raw.source));
            }

            if (raw.sourceLists == null && source != null) {
                sourceLists = sourceListDAO.bySource(source);
            } else if(raw.sourceLists != null) {
                sourceLists = raw.sourceLists.stream()
                        .map(sl ->  {
                            Optional<SourceList> maybeSourceList = sourceListDAO.byName(sl);
                            if(maybeSourceList.isPresent()) {
                                return maybeSourceList.get();
                            } else {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }

        }

        crawlId = raw.crawlId;

        if(raw.from != null) {
            from = LocalDate.parse(raw.from);
        }

        if(raw.to != null) {
            to = LocalDate.parse(raw.to);
        }

        maxArticle = raw.maxArticles;
        depth = raw.depth;
        politeness = raw.politeness;
        ignoreSiteMap = raw.ignoreSiteMap;
//        onlySiteMap = raw.onlySiteMap;
        skipKeywords = raw.skipKeywords;
        if(raw.workingDir != null) {
            workingDir = Paths.get(raw.workingDir);
            workingDir.toFile().mkdirs();
        }
        if(raw.scrapersDir != null) {
            scrapersDir = Paths.get(raw.scrapersDir);
            scrapersDir.toFile().mkdirs();
        }

        if(raw.flags == null) {
            flags = new ArrayList<>();
        } else {
            flags = raw.flags;
        }
        flagSet = new HashSet<>(flags);


        program = raw.program;
        jqmProgram = raw.jqmProgram;

//        if(raw.program != null ) {
//            switch (raw.program) {
//                default:
//                case "crawl":
//                    program = CrawlerSweepRunner.JQMSpringCollectorV1;
//                    break;
//            }
//        }

    }

    // added to allow access these data;
    public SourceDAO getSourceDAO() {
        return this.sourceDAO;
    }

    public SourceListDAO getSourceListDAO() {
        return this.sourceListDAO;
    }

    public JobRequest toJobRequest() {
        return toJobRequest(source);
    }

    public JobRequest toJobRequest(Source source) {
        JobRequest jobRequest = JobRequest.create(jqmProgram, CrawlerSweep.JQM_USER);

        jobRequest.addParameter(SOURCE, source.get(Source.STANDARD_NAME) );

        if(sourceLists != null && ! sourceLists.isEmpty()) {
            jobRequest.addParameter(SOURCE_LISTS, sourceLists
                    .stream()
                    .map(sl -> (String)sl.get(SourceList.LIST_NAME) )
                    .collect(Collectors.joining(",")));
        }


        if(crawlId != null) {
            jobRequest.addParameter( CRAWL_ID, crawlId );
        }
        jobRequest.addParameter( SKIP_KEYWORDS, skipKeywords.toString() );
        jobRequest.addParameter( IGNORE_SITE_MAP, ignoreSiteMap.toString() );
//        jobRequest.addParameter( ONLY_SITE_MAP, onlySiteMap.toString() );
        if(source.hasValue(Source.CRAWL_DEPTH)) {
            jobRequest.addParameter( DEPTH, Integer.toString( source.get(Source.CRAWL_DEPTH) ) );
        } else {
            jobRequest.addParameter( DEPTH, Integer.toString( depth ) );
        }
        jobRequest.addParameter( MAX_ARTICLES, Integer.toString( maxArticle ) );
        jobRequest.addParameter( POLITENESS, Integer.toString( politeness ) );
        jobRequest.addParameter( WORKING_DIR, workingDir.toString() );
        jobRequest.addParameter( SCRAPERS_DIR, scrapersDir.toString() );

        if( flags != null ) {
            Gson gson = new Gson();
            String encoded = gson.toJson(flags);
            jobRequest.addParameter( FLAGS, encoded );
        }

        if( from != null ) {
            jobRequest.addParameter( Crawl.FROM, from.toString() );
        }

        if( to != null ) {
            jobRequest.addParameter( Crawl.TO, to.toString() );
        }

//        System.out.println(ImmutableMap.copyOf(jobRequest.getParameters()).toString());
        return jobRequest;
    }

    public void init(Map<String,String> runtimeParameters) {


        raw.source = runtimeParameters.get(SOURCE);

        if(runtimeParameters.get(SOURCE_LISTS) != null) {
            raw.sourceLists = Arrays.asList(runtimeParameters.get(SOURCE_LISTS).split(",").clone());
        }

        raw.crawlId = runtimeParameters.get(CRAWL_ID);
        raw.depth = Integer.parseInt(runtimeParameters.get(DEPTH));
        raw.maxArticles = Integer.parseInt(runtimeParameters.get(MAX_ARTICLES));
        raw.politeness = Integer.parseInt(runtimeParameters.get(POLITENESS));
        raw.skipKeywords = Boolean.parseBoolean(runtimeParameters.get(SKIP_KEYWORDS));
        raw.ignoreSiteMap = Boolean.parseBoolean(runtimeParameters.get(IGNORE_SITE_MAP));
//        raw.onlySiteMap = Boolean.parseBoolean(runtimeParameters.get(ONLY_SITE_MAP));
        raw.from = runtimeParameters.get(FROM);
        raw.to = runtimeParameters.get(TO);
        raw.workingDir = runtimeParameters.get(WORKING_DIR);
        raw.scrapersDir = runtimeParameters.get(SCRAPERS_DIR);

        if(runtimeParameters.containsKey(FLAGS)) {
            Gson gson = new Gson();
            raw.flags = gson.fromJson(runtimeParameters.get(FLAGS), new TypeToken<List<String>>(){}.getType());
        }

        init();
    }
}
