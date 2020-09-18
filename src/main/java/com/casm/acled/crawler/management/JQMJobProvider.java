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
    public Job getJob(int id) {
        return new JQMJob(sourceDAO.getById(id).get());
    }

    @Override
    public void setPID(int id, int pid) {
        Source source = sourceDAO.getById(id).get().put(Source.CRAWL_JOB_ID, pid);
        sourceDAO.upsert(source);
    }

    @Override
    public List<Job> getJobs(Map<String, String> params) {
        List<SourceList> lists = sourceListDAO.getAll();

        Set<Source> globalActiveSources = new HashSet<>();
        List<Job> allJobs = new ArrayList<>();

        for(SourceList list : lists) {
            List<Source> listSources = sourceDAO.byList(list);

            // TODO: where is crawl active set?
            if(list.isTrue(SourceList.CRAWL_ACTIVE)) {
                globalActiveSources.addAll(listSources);
            }
        }

        for (Source source: globalActiveSources) {
            JQMJob job = new JQMJob(source);
            allJobs.add(job);
        }

        return allJobs;
    }
}
