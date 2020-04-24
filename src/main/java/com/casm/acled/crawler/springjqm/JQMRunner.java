package com.casm.acled.crawler.springjqm;

import com.casm.acled.crawler.management.Crawl;
import com.casm.acled.crawler.management.CrawlService;
import com.casm.acled.crawler.spring.CrawlerServiceOld;
import com.enioka.jqm.handler.JobManagerProvider;
import org.docx4j.wml.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.susx.tag.norconex.jobqueuemanager.CrawlerArguments;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

        if( runtimeParameters.containsKey(Crawl.FROM) && runtimeParameters.containsKey(Crawl.TO) ) {

            LocalDate from = LocalDate.parse( runtimeParameters.get( Crawl.FROM ) );
            LocalDate to = LocalDate.parse( runtimeParameters.get( Crawl.TO ) );

            crawlService.run(sourceListId, sourceId, from, to);
        } else {

            crawlService.run(sourceListId, sourceId);
        }

        System.out.println("Job " + jmp.getObject().jobInstanceID() + " is done!");
    }
}
