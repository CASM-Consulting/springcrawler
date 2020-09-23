package com.casm.acled.crawler.management;

import com.casm.acled.crawler.Crawl;
import com.casm.acled.crawler.reporting.Event;
import com.enioka.jqm.api.JobInstance;
import com.enioka.jqm.api.JobRequest;

import com.casm.acled.entities.source.Source;
import com.enioka.jqm.api.State;
import org.quartz.CronExpression;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;


public class JQMJob implements Job {

    private Source source;
    private JobInstance jobInstance;

    public JQMJob (JobInstance jobInstance) {
        this(null, null, jobInstance);
    }


    public JQMJob (Source source) {
        this(source, null, null);
    }

    public JQMJob (Source source, JobRequest jobRequest, JobInstance jobInstance) {
        this.jobInstance = jobInstance;
        this.source = source;
    }


    public JobInstance getJobInstance () {
        return this.jobInstance;
    }

    public JobRequest getJobRequest () {
        JobRequest jobRequest = JobRequest.create(CrawlerSweep.JQM_APP_NAME, CrawlerSweep.JQM_USER)
                .addParameter(Crawl.SOURCE_ID, Integer.toString(source.id()))
                .addParameter(Source.CRAWL_SCHEDULE, source.get(Source.CRAWL_SCHEDULE)); // TODO: check that this is correct

        return jobRequest;
    }

    public JobRequest getJobRequestFromJobInstance (JobInstance job) {
        JobRequest j = JobRequest.create(job.getApplicationName(), job.getUser());
        j.setParameters(job.getParameters());
        return j;
    }

    @Override
    public Optional<Integer> pid() {
        return Optional.ofNullable(source.get(Source.CRAWL_JOB_ID));
    }

    @Override
    public String name() {
        return source.get(Source.STANDARD_NAME);
    }

    @Override
    public int id() {
        return source.id();
    }

    public String getSourceListId() {
        return getJobRequest().getParameters().get(Crawl.SOURCE_LIST_ID);
    }

    public CronExpression getSchedule() {
        try {
            ZoneId zoneId = ZoneId.of(source.get(Source.TIMEZONE));
            TimeZone timeZone = TimeZone.getTimeZone(zoneId);
            CronExpression cron = new CronExpression(getJobRequest().getParameters().get(Source.CRAWL_SCHEDULE));
            cron.setTimeZone(timeZone);
            return cron;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String state() {
        String state;
        State jqmState = jobInstance.getState();
        switch (jqmState) {
            case RUNNING:
                state = RUNNING;
                break;
            case ENDED:
                state = STOPPED;
                break;
            case HOLDED:
            case KILLED:
            case CRASHED:
                state = FAILED;
                break;
            case CANCELLED:
                state = CANCELLED;
                break;
            case SCHEDULED:
            case SUBMITTED:
            case ATTRIBUTED:
                state = STARTING;
                break;
            default:
                //never here...
                throw new RuntimeException("unknown job state " + jqmState);
        }
        return state;
    }

    @Override
    public LocalDateTime getStarted() {
        Calendar calendar = jobInstance.getBeganRunningDate();
        return LocalDateTime.ofInstant(calendar.toInstant(), calendar.getTimeZone().toZoneId());
    }

    @Override
    public LocalDateTime getStopped() {
        Calendar calendar = jobInstance.getEndDate();
        return LocalDateTime.ofInstant(calendar.toInstant(), calendar.getTimeZone().toZoneId());
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public Source getSource() {
        return this.source;
    }

    public void setJobRequestParameters(Map<String, String> params) {
        getJobRequest().setParameters(params);
    }


}
