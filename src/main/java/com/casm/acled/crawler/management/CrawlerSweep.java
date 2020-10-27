package com.casm.acled.crawler.management;


import com.casm.acled.crawler.Crawl;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.scraper.ACLEDScraper;
import com.casm.acled.crawler.util.Util;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.enioka.jqm.api.JobRequest;
import com.enioka.jqm.api.JqmClient;
import com.enioka.jqm.api.JqmClientFactory;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CrawlerSweep {

    private static final Path ALL_SCRAPERS = Paths.get("/home/sw206/git/acled-scrapers");

    protected static final Logger logger = LoggerFactory.getLogger(CrawlerSweep.class);

    private final JqmClient client;

    public final static String JQM_APP_NAME = "JQMSpringCollectorV1";
    public static final String JQM_USER = "crawler-submission-service";

    @Autowired
    private SourceDAO sourceDAO;
 
    @Autowired
    private Reporter reporter;

    public CrawlerSweep() {
        try (
            Reader reader = Files.newBufferedReader(Paths.get("jqm.properties"))
        ) {
            Properties properties = new Properties();
            properties.load(reader);
            client = JqmClientFactory.getClient("acled-spring-jqm", properties, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sweep(List<CrawlArgs> args) {

        List<JobRequest> jobs = args.stream().map(CrawlArgs::toJobRequest).collect(Collectors.toList());

        for(JobRequest job : jobs) {
            client.enqueue(job);
            try {
                Thread.sleep(10*1000);
            } catch (InterruptedException e) {}

        }

    }

    private boolean hasScraper(Source source, Path scraperDir) {
        try {
            Path path;
            if(source.hasValue(Source.CRAWL_SCRAPER_PATH)) {
                path = Paths.get(source.get(Source.CRAWL_SCRAPER_PATH));
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

    public void sweepSourceList(String app, SourceList sourceList, Path scraperDir, LocalDate from, LocalDate to, Boolean skipKeywords) {

        List<Source> sources = sourceDAO.byList(sourceList);

        List<Source> sourcesWithScrapers = sources.stream()
                .filter(s-> Util.isScrapable(scraperDir, s))
                .collect(Collectors.toList());

        submitJobs(app, sourcesWithScrapers, sourceList.id(), from, to, skipKeywords);
    }

    public void sweepAvailableScrapers(String app, Path scraperDir) throws IOException {

        Map<String, Source> sources = sourceDAO.getAll().stream()
                .filter(s-> hasScraper(s, scraperDir))
                .collect(Collectors.toMap(
                    s->Util.getID(s),
                    Function.identity(),
                    (s1, s2) -> {
                        logger.warn("id clash {} {}, {}", s1.id(), s2.id(), Util.getID(s1));
                        return s1;
                    }
                ));


        List<Source> sourcesWithScrapers = Files.walk(scraperDir)
            .filter(ACLEDScraper::validPath)
            .filter(p-> {
                String id = p.getFileName().toString();
                if(sources.containsKey(id)) {
                    return true;
                } else {
                    logger.warn("source not found for scraper {}", id);
                    return false;
                }
            })
            .map(p->sources.get(p.getFileName().toString()))
            .collect(Collectors.toList());

        submitJobs(app, sourcesWithScrapers, 1, null, null, Boolean.TRUE);
    }

    public void submitJobs(String app, List<Source> sources, Integer sourceListId, LocalDate from, LocalDate to, Boolean skipKeywords) {
        for(Source source : sources) {
            submitJob(app, source, sourceListId, from, to, skipKeywords);
            try{Thread.sleep(1000);}catch (InterruptedException e) {}
        }
    }

    public void submitJob(String app, Source source, Integer sourceListId, LocalDate from, LocalDate to, Boolean skipKeywords) {
        JobRequest jobRequest = JobRequest.create(app, JQM_USER);
        jobRequest.addParameter( Crawl.SOURCE_ID, Integer.toString( source.id() ) );
        jobRequest.addParameter( Crawl.SOURCE_LIST_ID, sourceListId.toString() );
        jobRequest.addParameter( Crawl.SKIP_KEYWORD_FILTER, skipKeywords.toString() );

        if(from!=null) {
            jobRequest.addParameter( Crawl.FROM, from.toString() );
        }
        if(to!=null) {
            jobRequest.addParameter( Crawl.TO, to.toString() );
        }

        System.out.println(ImmutableMap.copyOf(jobRequest.getParameters()).toString());
        client.enqueue(jobRequest);
    }

}
