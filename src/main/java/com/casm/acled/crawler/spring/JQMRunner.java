package com.casm.acled.crawler.spring;

import com.beust.jcommander.JCommander;
import com.enioka.jqm.handler.JobManagerProvider;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Resource(name = "runtimeParameters")
//    @Autowired
    private Map<String, String> runtimeParameters;

    @Resource
    private JobManagerProvider jmp;

    @Override
    public void run()
    {

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

        crawlerService.run(crawlerArguments);
        System.out.println("Job " + jmp.getObject().jobInstanceID() + " is done!");
    }
}
