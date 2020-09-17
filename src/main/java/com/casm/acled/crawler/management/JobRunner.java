package com.casm.acled.crawler.management;

import java.util.List;

/**
 * Run jobs and list currently running jobs.
 * Use to find running state of Jobs.
 */
public interface JobRunner {

    List<Job> getJobs();

    Job getJob(int jobPID);

    void runJob(Job j);

}
