package com.casm.acled.crawler.management;


import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.enioka.jqm.api.*;
import com.norconex.jef4.suite.JobSuite;
import org.checkerframework.checker.units.qual.A;
import org.docx4j.wml.R;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.*;
import java.util.*;


@Service
public class Scheduler implements Runnable {

    protected static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

    @Autowired
    private SourceDAO sourceDAO;
    @Autowired
    private SourceListDAO sourceListDAO;

    private final CronExpression defaultCronSchedule;

    private final Map<Integer, JobInstance> jobs;

    public Scheduler() {

        JqmClient client = JqmClientFactory.getClient();

        jobs = new HashMap<>();

        for(JobInstance job : client.getJobs()) {
            jobs.put(job.getId(), job);
        }

        try {
            //tuesdays and fridays at 8 pm
            defaultCronSchedule = new CronExpression("0 0 20 ? * TUE,FRI *");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private void ensureSchedules() {
        Set<Source> sources = gatherSources();

        for(Source source : sources) {
            ensureSchedule(source);
        }
    }

    private Optional<JobInstance> getJob(Source source) {
        Integer jobId = source.get(Source.CRAWL_JOB_ID);
        Optional<JobInstance> maybeJob = Optional.empty();
        if(jobId != null) {
            try {
                JqmClient client = JqmClientFactory.getClient();

                JobInstance job = client.getJob(jobId);

                maybeJob = Optional.of(job);
            } catch (JqmInvalidRequestException e) {
                logger.info(e.getMessage());
                //job hasn't run;
            }
        }
        return maybeJob;
    }

    private Date getZonedNow(ZoneId zoneId) {
        ZonedDateTime zonedNow = LocalDateTime.now().atZone(zoneId);

        Date date = Date.from(zonedNow.toInstant());

        return date;

    }

    private CronExpression cron(String expression) {
        try {
            CronExpression cron = new CronExpression(expression);
            return cron;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private Action checkStillRunningFromLastTime(JobInstance job, CronExpression cron, Date prevRun, Date nextRun) {
        return Action.PASS;
    }

    private Action checkShouldRunAgain(JobInstance job, CronExpression cron, Date prevRun, Date nextRun) {
        return Action.PASS;
    }

    private void reportJob(JobInstance job) {

    }

    private void checkTimeSinceSubmitted(JobInstance job) {

    }

    private enum Action {
        RUN,
        PASS
    }
    public void ensureSchedule(Source source) {

        CronExpression cron = new CronExpression(defaultCronSchedule);
        if(source.hasValue(Source.CRAWL_SCHEDULE)) {
            try {
                String expression = source.get(Source.CRAWL_SCHEDULE);
                cron = new CronExpression(expression);
            } catch (ParseException e) {
                //TODO: journal error and stick with default.
//                throw new RuntimeException(e);
            }
        }
        ZoneId zoneId = ZoneId.of(source.get(Source.TIMEZONE));
        TimeZone timeZone = TimeZone.getTimeZone(zoneId);
        cron.setTimeZone(timeZone);

        Date zonedNow = getZonedNow(zoneId);
        Date nextRun = cron.getTimeAfter(zonedNow);
        Date prevRun = cron.getTimeBefore(zonedNow);

        Optional<JobInstance> maybeJob = getJob(source);

        Action action;

        if(maybeJob.isPresent()) {
            JobInstance job = maybeJob.get();
            State jobState = job.getState();
            switch (jobState) {
                case RUNNING:
                    action = checkStillRunningFromLastTime(job, cron, nextRun, prevRun);
                    break;
                case ENDED:
                    action = checkShouldRunAgain(job, cron, nextRun, prevRun);
                    break;
                case HOLDED:
                case KILLED:
                case CRASHED:
                case CANCELLED:
                    reportJob(job);
                    action = Action.PASS;
                    break;
                case SCHEDULED:
                case SUBMITTED:
                case ATTRIBUTED:
                    checkTimeSinceSubmitted(job);
                default:
                //all good
                    action = Action.PASS;
            }
        } else {

            action = Action.RUN;
        }

        switch (action) {
            case RUN:
                runCrawl(source);
                break;
            case PASS:
            default:

        }

    }


    private JobInstance runCrawl(Source source) {

        JqmClient client = JqmClientFactory.getClient();

        JobRequest jobRequest = JobRequest.create("","");

        jobRequest.addParameter( CrawlRun.SOURCE_ID, Integer.toString( source.id() ) );

        int id = client.enqueue(jobRequest);

        JobInstance job = client.getJob(id);

        source.put(Source.CRAWL_JOB_ID, id);

        return job;
    }

    private Set<Source> gatherSources() {
        List<SourceList> lists = sourceListDAO.getAll();

        Set<Source> globalActiveSources = new HashSet<>();

        for(SourceList list : lists) {
            List<Source> listSources = sourceDAO.byList(list);
            if(list.get(SourceList.CRAWL_ACTIVE)) {
                globalActiveSources.addAll(listSources);
            }
        }

        return globalActiveSources;
    }


    @Override
    public void run() {

        ensureSchedules();
    }
}
