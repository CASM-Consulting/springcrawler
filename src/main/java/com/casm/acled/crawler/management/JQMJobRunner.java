package com.casm.acled.crawler.management;

import com.casm.acled.entities.source.Source;
import com.enioka.jqm.api.*;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.util.*;

@Component
public class JQMJobRunner implements JobRunner {

    private final JqmClient client;
    private final List<Job> jobs;

    @Autowired
    private JobProvider jobProvider;

    public JQMJobRunner () {

        Properties p = new Properties();
        p.put("com.enioka.jqm.ws.url", "http://localhost:50682/ws/client");
        p.put("com.enioka.jqm.ws.login", "root");
        p.put("com.enioka.jqm.ws.password", "password");
        JqmClientFactory.setProperties(p);

        client = JqmClientFactory.getClient();

        jobs = new ArrayList<>();
    }


    @Override
    public List<Job> getJobs() {
        // it returns JobInstance but we need to wrap it into a JQMJob object.
        List<JobInstance> allJobs = client.getJobs();
        for (JobInstance j: allJobs) {
            jobs.add(new JQMJob(j));
        }
        return jobs;
    }


    @Override
    public Job getJob(int jobPID) {
        JobInstance job = client.getJob(jobPID);
        return new JQMJob(job);
    }

    @Override
    public void runJob(Job job) {
        JQMJob j = (JQMJob)job;

        // TODO
        // need to decide, if job instance exist, should we run this new job request again using the same parameters???,
        // the following commented block show the functionality, assign jobinstance's parameters to jobrequest;
//        if (j.getJobInstance()!=null) {
//            j.setJobRequestParameters(j.getJobInstance().getParameters());
//        }

        System.out.println(ImmutableMap.copyOf(j.getJobRequest().getParameters()).toString());

        int pid = client.enqueue(j.getJobRequest());

        // Save job PID
        jobProvider.setPID(job.id(), pid);
    }

}
