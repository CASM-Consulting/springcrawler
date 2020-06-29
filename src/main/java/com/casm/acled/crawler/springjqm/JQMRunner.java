package com.casm.acled.crawler.springjqm;

import com.casm.acled.crawler.Crawl;
import com.casm.acled.crawler.spring.CrawlService;
import com.casm.acled.crawler.util.CustomLoggerRepository;
import com.enioka.jqm.handler.JobManagerProvider;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.spi.DefaultRepositorySelector;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RepositorySelector;
import org.apache.log4j.spi.RootLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
public class JQMRunner implements Runnable {

    @Autowired
    private CrawlService crawlService;

    @Autowired(required=false)
    private Map<String, String> runtimeParameters;

    @Autowired(required=false)
    private JobManagerProvider jmp;

    @Override
    public void run() {

        runtimeParameters = jmp.getObject().parameters();

        System.out.println("Job " + jmp.getObject().jobInstanceID() + " starting");

        System.out.println(runtimeParameters);

        int sourceListId = Integer.parseInt( runtimeParameters.get( Crawl.SOURCE_LIST_ID ) );
        int sourceId = Integer.parseInt( runtimeParameters.get( Crawl.SOURCE_ID ) );
        boolean skipKeywords = Boolean.parseBoolean( runtimeParameters.get( Crawl.SKIP_KEYWORD_FILTER ) );

        if( runtimeParameters.containsKey(Crawl.FROM) && runtimeParameters.containsKey(Crawl.TO) ) {

            LocalDate from = LocalDate.parse( runtimeParameters.get( Crawl.FROM ) );
            LocalDate to = LocalDate.parse( runtimeParameters.get( Crawl.TO ) );

            crawlService.run(sourceListId, sourceId, from, to, skipKeywords);
        } else {

            crawlService.run(sourceListId, sourceId, skipKeywords);
        }

        System.out.println("Job " + jmp.getObject().jobInstanceID() + " is done!");
    }
}
