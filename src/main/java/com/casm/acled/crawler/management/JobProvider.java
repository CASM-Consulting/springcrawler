package com.casm.acled.crawler.management;

import java.util.List;
import java.util.Map;

/**
 * Manages the instantiation of potential Jobs from Sources.
 * Can record the PID of a Source's Job.
 */
public interface JobProvider {

    List<Job> getJobs(CrawlArgs args);

    Job getJob(int id, CrawlArgs args);

    void setPID(int id, int pid);

}
