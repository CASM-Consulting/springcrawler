package com.casm.acled.crawler.springjqm;

import com.casm.acled.crawler.Crawl;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.spring.CrawlService;
import com.enioka.jqm.handler.JobManagerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class JQMExampleCollectionRunner implements Runnable {

    protected static final Logger logger = LoggerFactory.getLogger(JQMExampleCollectionRunner.class);

    @Autowired
    private CrawlService crawlService;

    @Autowired
    private Reporter reporter;

    @Autowired(required=false)
    private Map<String, String> runtimeParameters;

    @Autowired(required=false)
    private JobManagerProvider jmp;

    @Override
    public void run()  {

        reporter.randomRunId();

        runtimeParameters = jmp.getObject().parameters();

        System.out.println("Job " + jmp.getObject().jobInstanceID() + " starting");

        System.out.println(runtimeParameters);

        int sourceListId = Integer.parseInt( runtimeParameters.get( Crawl.SOURCE_LIST_ID ) );
        int sourceId = Integer.parseInt( runtimeParameters.get( Crawl.SOURCE_ID ) );

        crawlService.collectExamples(sourceListId, sourceId);

        reporter.getRunReports().stream().forEach(r -> logger.info(r.toString()));
    }
}