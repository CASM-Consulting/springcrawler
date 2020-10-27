package com.casm.acled.crawler.management;

import com.casm.acled.crawler.Crawl;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.enioka.jqm.api.JobRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class JQMJobProvider implements JobProvider {

    @Autowired
    private SourceDAO sourceDAO;
    @Autowired
    private SourceListDAO sourceListDAO;

    public static final String appName = "JQMSpringCollectorV1";
    public static final String JQM_USER = "crawler-submission-service";

    public JQMJobProvider() {

    }

    public void setSourceDAO(SourceDAO sourceDAO) {
        this.sourceDAO = sourceDAO;
    }

    public void setSourceListDAO(SourceListDAO sourceListDAO) {
        this.sourceListDAO = sourceListDAO;
    }



    @Override
    public Job getJob(int id, CrawlArgs args) {
        return new JQMJob(sourceDAO.getById(id).get(), args);
    }

    @Override
    public void setPID(int id, int pid) {
        Source source = sourceDAO.getById(id).get().put(Source.CRAWL_JOB_ID, pid);
        sourceDAO.upsert(source);
    }

    @Override
    public void clearPID(int id) {
        Source source = sourceDAO.getById(id).get().remove(Source.CRAWL_JOB_ID);
        sourceDAO.overwrite(source);
    }

    @Override
    public List<Job> getJobs(CrawlArgs args) {

        Set<Source> sources = new HashSet<>();

        // If specific source is specified use it
        if (args.source != null){
            sources.add(args.source);
        } else {
            List<SourceList> lists;

            // Otherwise, if a source list is specified use that
            if (args.sourceLists != null && !args.sourceLists.isEmpty()) {
                lists = args.sourceLists;
            } else {
                // Otherwise use all source lists
                lists = sourceListDAO.getAll();
            }

            for(SourceList list : lists) {
                List<Source> listSources = sourceDAO.byList(list);
                sources.addAll(listSources);
            }
        }

        List<Job> jobs = new ArrayList<>();

        for (Source source: sources) {
            if (source.isFalse(Source.CRAWL_DISABLED)){
                JQMJob job = new JQMJob(source, args);
                jobs.add(job);
            }
        }

        return jobs;
    }
}
