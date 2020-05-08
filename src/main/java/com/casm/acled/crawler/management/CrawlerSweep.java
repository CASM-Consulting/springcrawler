package com.casm.acled.crawler.management;


import com.casm.acled.crawler.Crawl;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.scraper.ACLEDScraper;
import com.casm.acled.crawler.Util;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.enioka.jqm.api.JobRequest;
import com.enioka.jqm.api.JqmClient;
import com.enioka.jqm.api.JqmClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private final static String JQM_APP_NAME = "JQMSpringCollectorV1";
    private static final String JQM_USER = "crawler-submission-service";

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

    private boolean hasScraper(Source source, Path scraperDir) {
        try {
            String id = Util.getID(source);
            if(ACLEDScraper.validPath(scraperDir.resolve(id))) {
                return true;
            } else {
                return false;
            }
        } catch (IllegalArgumentException e){
            logger.warn(e.getMessage());
            return false;
        }
    }

    public void sweepSourceList(SourceList sourceList, Path scraperDir) {

        List<Source> sources = sourceDAO.byList(sourceList);

        List<Source> sourcesWithScrapers = sources.stream()
                .filter(s-> hasScraper(s, scraperDir))
                .collect(Collectors.toList());

        submitJobs(sourcesWithScrapers, sourceList.id());
    }

    public void sweepAvailableScrapers(Path scraperDir) throws IOException {

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

        submitJobs(sourcesWithScrapers, 1);
    }


    public void submitJobs(List<Source> sources, Integer sourceListId) {
        for(Source source : sources) {
            JobRequest jobRequest = JobRequest.create(JQM_APP_NAME, JQM_USER);
            jobRequest.addParameter( Crawl.SOURCE_ID, Integer.toString( source.id() ) );
            jobRequest.addParameter( Crawl.SOURCE_LIST_ID, sourceListId.toString() );
            jobRequest.addParameter( Crawl.SKIP_KEYWORD_FILTER, Boolean.TRUE.toString() );

//            jobRequest.addParameter( Crawl.FROM, LocalDate.now().minusDays(7).toString() );
//            jobRequest.addParameter( Crawl.TO, LocalDate.now().toString() );

            client.enqueue(jobRequest);
        }
    }

}
