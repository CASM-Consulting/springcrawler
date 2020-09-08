package com.casm.acled.crawler.management;

import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.enioka.jqm.api.*;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.time.*;
import java.util.*;

@Component
public class JQMJobRunner implements JobRunner<JQMJob>{

    private final JqmClient client;
    private final List<JQMJob> jobs;

    @Autowired
    private SourceDAO sourceDAO;

    public JQMJobRunner () {

        Properties p = new Properties();
        p.put("com.enioka.jqm.ws.url", "http://localhost:50682/ws/client");
        p.put("com.enioka.jqm.ws.login", "root");
        p.put("com.enioka.jqm.ws.password", "password");
        JqmClientFactory.setProperties(p);

        client = JqmClientFactory.getClient();

        jobs = new ArrayList();
    }

    public void setSourceDAO(SourceDAO sourceDAO) {
        this.sourceDAO = sourceDAO;
    }

    public List<JQMJob> getJobs() {
        // it returns JobInstance but we need to wrap it into a JQMJob object.
        List<JobInstance> allJobs = client.getJobs();
        for (JobInstance j: allJobs) {
            jobs.add(new JQMJob(j));
        }
        return jobs;
    }

    public JQMJob getJob(int jobId) {
        // get jobinstance by ID;
        JobInstance job = client.getJob(jobId);
        return new JQMJob(job);
    }

    public void runJob(JQMJob j) {

        Source source = j.getSource();

        // need to decide, if job instance exist, should we run this new job request again using the same parameters???,
        // the following commented block show the functionality, assign jobinstance's parameters to jobrequest;
//        if (j.getJobInstance()!=null) {
//            j.setJobRequestParameters(j.getJobInstance().getParameters());
//        }

        System.out.println(ImmutableMap.copyOf(j.getJobRequest().getParameters()).toString());

        int id = client.enqueue(j.getJobRequest());
        JobInstance job = client.getJob(id); // do i need to get the job?

        source = source.put(Source.CRAWL_JOB_ID, id);

        sourceDAO.upsert(source); // should also save it;

    }

}
