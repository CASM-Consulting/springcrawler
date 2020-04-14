package com.casm.acled.crawler.springjqm;

import com.beust.jcommander.JCommander;
import com.casm.acled.crawler.spring.CrawlerService;
import com.enioka.jqm.handler.JobManagerProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Component;
import uk.ac.susx.tag.norconex.jobqueuemanager.CrawlerArguments;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class JQMRunner implements Runnable {
    @Autowired
    private CrawlerService crawlerService;

    @Autowired(required=false)
    private Map<String, String> runtimeParameters;

    @Autowired(required=false)
    private JobManagerProvider jmp;

    @Override
    public void run() {

//        throw new RuntimeException("debug here!");

        runtimeParameters = jmp.getObject().parameters();

        System.out.println("Job " + jmp.getObject().jobInstanceID() + " starting");

        System.out.println(runtimeParameters);
        List<String> splitArgs = new ArrayList<>();
        for(Map.Entry<String,String> entry : runtimeParameters.entrySet()){
            splitArgs.add(entry.getKey());
            splitArgs.add(entry.getValue().split("\\s+")[1]);
        }

        String[] corrArgs = splitArgs.toArray(new String[splitArgs.size()]);

        CrawlerArguments crawlerArguments = new CrawlerArguments();
        JCommander.newBuilder()
                .addObject(crawlerArguments)
                .build()
                .parse(corrArgs);

        try {

            crawlerService.run(crawlerArguments);
            System.out.println("Job " + jmp.getObject().jobInstanceID() + " is done!");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
