package com.casm.acled.crawler.management;

import com.casm.acled.entities.source.Source;
import com.enioka.jqm.api.*;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Component
public class JQMJobRunner implements JobRunner {

    private final JqmClient client;
    private final List<Job> jobs;

    @Autowired
    private JobProvider jobProvider;

    public JQMJobRunner () {

        try (
                Reader reader = Files.newBufferedReader(Paths.get("jqm.properties"))
        ) {
            Properties properties = new Properties();
            properties.load(reader);
            client = JqmClientFactory.getClient("acled-spring-job-runner", properties, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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
