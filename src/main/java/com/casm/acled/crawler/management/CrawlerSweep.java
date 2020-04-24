package com.casm.acled.crawler.management;


import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.scraper.ACLEDScraper;
import com.casm.acled.crawler.utils.Util;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.entities.source.Source;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CrawlerSweep {

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

    public void sweepAvailableScrapers(Path scraperDir) throws IOException {

        Map<String, Source> sources = sourceDAO.getAll().stream()
                .filter(s-> {
                    if(s.get(Source.LINK)!=null) {
                        try {
                            Util.getID(s.get(Source.LINK));
                            return true;
                        } catch (IllegalArgumentException e){
                            logger.warn(e.getMessage());
                            return false;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toMap(
                    s->Util.getID(s.get(Source.LINK)),
                    Function.identity(),
                    (s1, s2) -> {
                        logger.warn("id clash {} {}, {}", s1.id(), s2.id(), Util.getID(s1.get(Source.LINK)));
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

        submitJobs(sourcesWithScrapers);
    }


    public void submitJobs(List<Source> sources) {
        for(Source source : sources) {
            JobRequest jobRequest = JobRequest.create(JQM_APP_NAME, JQM_USER);
            jobRequest.addParameter( Crawl.SOURCE_ID, Integer.toString( source.id() ) );
            jobRequest.addParameter( Crawl.SOURCE_LIST_ID, Integer.toString( 1 ) );

//            jobRequest.addParameter( Crawl.FROM, LocalDate.now().minusDays(7).toString() );
//            jobRequest.addParameter( Crawl.TO, LocalDate.now().toString() );

            client.enqueue(jobRequest);
        }
    }

}
