package com.casm.acled.crawler.management;

import com.casm.acled.crawler.Crawl;
import com.enioka.jqm.api.JobInstance;
import com.enioka.jqm.api.JobRequest;

import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import org.quartz.CronExpression;

import java.time.LocalDateTime;


// talking about the jobInsance, does it stands for jobrequest or simply a jobInsance instance?
// because the client will only return jobInsance instances and when generating jobs from sources, it can only generate jobrequest;

// job interface,
public interface Job {
    // probably only should have jobrequest object and delete jobInstance object.
    // only source;

    String RUNNING = "RUNNING";
    String STOPPED = "STOPPED";
    String STARTING = "STARTING";
    String FAILED = "FAILED";
    String CANCELLED = "CANCELLED";

    String name();

    int id();

    int pid();

    CronExpression getSchedule();

    String state();

    LocalDateTime getStarted();
    LocalDateTime getStopped();

}
