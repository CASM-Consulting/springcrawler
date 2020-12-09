package com.casm.acled.crawler.management;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Run jobs and list currently running jobs.
 * Use to find running state of Jobs.
 */
public interface JobRunner {

    List<Job> getJobs();

    Optional<Job> getJob(int jobPID);

    void runJob(Job j);

}
