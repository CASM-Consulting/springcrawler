package com.casm.acled.crawler.management;

import org.quartz.CronExpression;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Job's have conditional state (remains to be seen whether this is a good idea); the available state depends on how
 * the Job object was created:
 *   - If created by the JQM client, then only JobInstance will be populated
 *   - If created from Sources, then only JobRequest will be populated.
 */
public interface Job {

    // State descriptors (simplified from JQM states).
    String RUNNING = "RUNNING";
    String STOPPED = "STOPPED";
    String STARTING = "STARTING";
    String FAILED = "FAILED";
    String CANCELLED = "CANCELLED";

    String name();

    // ID of the source that generates this job
    int id();

    // PID of (maybe) running job
    Optional<Integer> pid();

    /**
     * Return Cron schedule, ensure that the Cron's timezone is appropriately contextualised to the Job's Source.
     */
    CronExpression getSchedule();

    String state();

    LocalDateTime getStarted();

    LocalDateTime getStopped();

}
