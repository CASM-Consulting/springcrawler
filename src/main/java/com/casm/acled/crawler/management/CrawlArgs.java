package com.casm.acled.crawler.management;

import com.beust.jcommander.Parameter;
import com.casm.acled.crawler.Crawl;
import com.casm.acled.crawler.springrunners.CrawlerSweepRunner;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.enioka.jqm.api.JobRequest;
import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class CrawlArgs {

    @Autowired
    private SourceDAO sourceDAO;

    @Autowired
    private SourceListDAO sourceListDAO;

    public class Raw {
        @Parameter(description = "Program")
        public String program;

        @Parameter(names = "-s", description = "Source")
        public List<String> sources;

        @Parameter(names = "-sl", description = "Source list")
        public String sourceList;

        @Parameter(names = "-n", description = "Quit after this many")
        public Integer maxArticles = -1;

        @Parameter(names = "-d", description = "Crawl depth")
        public Integer depth = 5;

        @Parameter(names = "-p", description = "Politeness delay")
        public Integer politeness = 100;

        @Parameter(names = "-ism", description = "Ignore site maps")
        public Boolean ignoreSiteMap = false;

        @Parameter(names = "-osm", description = "Only Site maps")
        public Boolean onlySiteMap = false;

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
    }


    public final Raw raw;

    public String program;

    public List<Source> sources;
    public static final String SOURCE_ID = "SOURCE_ID";

    public SourceList sourceList;
    public static final String SOURCE_LIST_ID = "SOURCE_LIST_ID";

    public Integer maxArticle;
    public static final String MAX_ARTICLES = "MAX_ARTICLES";

    public Integer depth;
    public static final String DEPTH = "DEPTH";

    public Integer politeness;
    public static final String POLITENESS = "POLITENESS";

    public Boolean ignoreSiteMap;
    public static final String IGNORE_SITE_MAP = "IGNORE_SITE_MAP";

    public Boolean onlySiteMap;
    public static final String ONLY_SITE_MAP = "ONLY_SITE_MAP";

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


    public CrawlArgs() {
        raw = new Raw();
    }
    
    public void init() {

        if(raw.sources != null) {
            sources = new ArrayList<>();
            for(String sourceName : raw.sources) {
                Optional<Source> maybeSource = sourceDAO.byName(sourceName);
                if(maybeSource.isPresent()) {
                    Source source = maybeSource.get();
                    sources.add(source);
                }
            }
        }

        if(raw.sourceList != null) {
            Optional<SourceList> maybeSourceList = sourceListDAO.byName(raw.sourceList);
            if(maybeSourceList.isPresent()) {
                sourceList = maybeSourceList.get();
                if(sources == null) {
                    sources = sourceDAO.byList(sourceList);
                }
            } else {
                throw new RuntimeException("Source List is required");
            }
        }

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
        onlySiteMap = raw.onlySiteMap;
        skipKeywords = raw.skipKeywords;
        if(raw.workingDir != null) {
            workingDir = Paths.get(raw.workingDir);
            workingDir.toFile().mkdirs();
        }
        if(raw.scrapersDir != null) {
            scrapersDir = Paths.get(raw.scrapersDir);
            scrapersDir.toFile().mkdirs();
        }



        program = raw.program;

//        if(raw.program != null ) {
//            switch (raw.program) {
//                default:
//                case "crawl":
//                    program = CrawlerSweepRunner.JQMSpringCollectorV1;
//                    break;
//            }
//        }

    }

    public List<JobRequest> toJobRequests() {

        List<JobRequest> requests = new ArrayList<>();

        for(Source source : sources) {
            JobRequest request = toJobRequest(source);
            requests.add(request);
        }

        return requests;
    }

    private JobRequest toJobRequest(Source source) {
        JobRequest jobRequest = JobRequest.create(program, CrawlerSweep.JQM_USER);

        jobRequest.addParameter( SOURCE_ID, Integer.toString( source.id() ) );
        jobRequest.addParameter( SOURCE_LIST_ID, Integer.toString( sourceList.id() ) );
        jobRequest.addParameter( SKIP_KEYWORDS, skipKeywords.toString() );
        jobRequest.addParameter( IGNORE_SITE_MAP, ignoreSiteMap.toString() );
        jobRequest.addParameter( ONLY_SITE_MAP, onlySiteMap.toString() );
        jobRequest.addParameter( DEPTH, Integer.toString( depth ) );
        jobRequest.addParameter( MAX_ARTICLES, Integer.toString( maxArticle ) );
        jobRequest.addParameter( POLITENESS, Integer.toString( politeness ) );
        jobRequest.addParameter( WORKING_DIR, workingDir.toString() );
        jobRequest.addParameter( SCRAPERS_DIR, scrapersDir.toString() );

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

        int sourceId = Integer.parseInt(runtimeParameters.get(SOURCE_ID));
        int sourceListId = Integer.parseInt(runtimeParameters.get(SOURCE_LIST_ID));

        sources = ImmutableList.of(sourceDAO.getById(sourceId).get());
        sourceList = sourceListDAO.getById(sourceListId).get();

        raw.depth = Integer.parseInt(runtimeParameters.get(DEPTH));
        raw.maxArticles = Integer.parseInt(runtimeParameters.get(MAX_ARTICLES));
        raw.politeness = Integer.parseInt(runtimeParameters.get(POLITENESS));
        raw.skipKeywords = Boolean.parseBoolean(runtimeParameters.get(SKIP_KEYWORDS));
        raw.ignoreSiteMap = Boolean.parseBoolean(runtimeParameters.get(IGNORE_SITE_MAP));
        raw.onlySiteMap = Boolean.parseBoolean(runtimeParameters.get(ONLY_SITE_MAP));
        raw.from = runtimeParameters.get(FROM);
        raw.to = runtimeParameters.get(TO);
        raw.workingDir = runtimeParameters.get(WORKING_DIR);
        raw.scrapersDir = runtimeParameters.get(SCRAPERS_DIR);

        init();
    }
}
