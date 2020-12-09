package com.casm.acled.crawler.management;

import com.enioka.jqm.api.*;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Component
public class JQMJobRunner implements JobRunner {

    protected static final Logger logger = LoggerFactory.getLogger(JQMJobRunner.class);

    private final JqmClient client;
    private final List<Job> jobs;

    @Autowired
    private JobProvider jobProvider;

    @Autowired
    private ConfigService pathService;

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
    public Optional<Job> getJob(int jobPID) {
        try {
            JobInstance job = client.getJob(jobPID);
            return Optional.of(new JQMJob(job));
        } catch (JqmInvalidRequestException | JqmClientException e){
            logger.info("When getting existing JQM Job: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void runJob(Job job) {
        JQMJob j = (JQMJob)job;

        System.out.println(ImmutableMap.copyOf(j.getJobRequest().getParameters()).toString());

        int pid = client.enqueue(j.getJobRequest());

        // Save job PID
        jobProvider.setPID(job.id(), pid);

        // Block until job is no longer in a starting state in order to deal with JQM concurrency issues
        awaitStart(pid);
    }

    /**
     * Block until job is no longer in a starting state.
     * Return the state that permitted the function to stop polling.
     */
    private State awaitStart(int pid){
        System.out.print("Awaiting start... ");
        while (true){
            // We have to re-get the job every time to get the updated state.
            State state = client.getJob(pid).getState();
            switch (state) {
                case RUNNING:
                case ENDED:
                case KILLED:
                case CRASHED:
                case CANCELLED:
                    // job is no longer waiting start, so we can stop blocking
                    System.out.println(state);
                    // a polite extra second
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return state;
                default:
                    // job still hasn't started so wait for a second and try again
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
            }
        }
    }

}
