package com.casm.acled.crawler.management;


import com.casm.acled.crawler.Crawl;
import com.casm.acled.crawler.springrunners.CrawlerSweepRunner;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.enioka.jqm.api.*;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.*;
import java.util.*;

// the main thing, get it running and tested;
// one, decouple scheduler from spring-boot;
// two, parameterize schedule in terms of classes; dealing with jobs and jqm client;
// third,
// interface class that scheduler accept; abstract things are running, accept a source;

// two other classess to write: , schedule jobs, shedule job runner (run jobs and report which job is running); build interfaces;
// so bascially three classes: scheduler, schedulejob, schedulejobrunner (run jobs and report which job is running);

// for instance, if we dont touch current
// this to replace sweep



// to ask, the aim of scheduling, want to re-crawl these sites every several days?
// to ask, what is the potential deadline for this functionality?
// to ask, in the sourcelist, there are no other key-value pairs except the LIST_NAME, so basically it fails when trying to obtain CRAWL_ACTIVE...
// probably because crawlers have not been created anyway.
// to ask, the source does not have crawl_job id anyway.
// to ask, jqm properties not loaded properly. need to manually specify
// a little bit confused about these different parameters that source and source list have. Sourcelist is a one website's resources, source is a single url crawl.
// It seems that a lot of information that the crawlargs have does not have a record in the source and sourcelist;
// and I notice that when need to run a job again, we run it by creating a job with a single source ID?

// so for testing, what we could do is to let the test thing (schedulejobrunnerTest) pass a jqm client
// so basically, *schedulejobrunner* pass a client to *scheduler* to examine all jobs and decide run again or not.
// The *schedulejobrunnerTest* will manually create a client enqueued with jobs that have different states
//what about *schedulejob* class?? not very clear about this bit.
// we cannot get rid of JQM when testing cause that is what we are trying to test.

// the main thing, get it running and tested;
// one, decouple scheduler from spring-boot;
// two, parameterize schedule in terms of classes; dealing with jobs and jqm client;
// third,
// interface class that scheduler accept; abstract things are running, accept a source;

// two other classes to write: , schedule jobs, shedule job runner (run jobs and report which job is running); build interfaces;
// so bascially three classes: scheduler, schedulejob, schedulejobrunner (run jobs and report which job is running);

// for instance, if we dont touch current

//errors: why it tries to connect to Caused by: java.net.ConnectException: ConnectException invoking http://localhost:1789/ws/client/ji: Connection refused (Connection refused)
// jqm properties not loaded properly.

@Service
public class Scheduler implements Runnable {

    protected static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

    @Autowired
    private SourceDAO sourceDAO;
    @Autowired
    private SourceListDAO sourceListDAO;

    private final CronExpression defaultCronSchedule;

    private final Map<Integer, JobInstance> jobs;


    private final JqmClient client;

//    private final JobRunner jobRunner;

    public Scheduler() {

        Properties p = new Properties();
        p.put("com.enioka.jqm.ws.url", "http://localhost:50682/ws/client");
        p.put("com.enioka.jqm.ws.login", "root");
        p.put("com.enioka.jqm.ws.password", "password");
        JqmClientFactory.setProperties(p);

        client = JqmClientFactory.getClient();

        jobs = new HashMap<>();

        try {
            //tuesdays and fridays at 8 pm
            defaultCronSchedule = new CronExpression("0 0 20 ? * TUE,FRI *");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private void ensureSchedules() {

        for(JobInstance job : client.getJobs()) {
            jobs.put(job.getId(), job);
        }

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

    // to be implemented, report to admin
    private Action checkStillRunningFromLastTime(JobInstance job, CronExpression cron, Date prevRun, Date nextRun) {
        return Action.PASS;
    }

    // to be implemented, scheduled to run again.
    private Action checkShouldRunAgain(JobInstance job, CronExpression cron, Date prevRun, Date nextRun) {
        return Action.PASS;
    }

    // to be implemented , send email to admin
    private void reportJob(JobInstance job) {

    }

    // to be implemented
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

        JobRequest jobRequest = JobRequest.create("","");

        jobRequest.addParameter( Crawl.SOURCE_ID, Integer.toString( source.id() ) );
        // other information need : time span, and other things;

        int id = client.enqueue(jobRequest);

        JobInstance job = client.getJob(id);

        source.put(Source.CRAWL_JOB_ID, id);

        sourceDAO.upsert(source); // should also save it;

        return job;
    }

    private Set<Source> gatherSources() {
        List<SourceList> lists = sourceListDAO.getAll();

        Set<Source> globalActiveSources = new HashSet<>();

        for(SourceList list : lists) {
            List<Source> listSources = sourceDAO.byList(list);

            // when the value of the crawl_active would be changed? I can't find the assigment thing for this.
            // crawl_active returned null....
            // SourceList has not been set the CRAWL_ACTIVE value anyway, and also lack many other entries; need to handle error here; if not active then not run
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

    public static void main(String[] args) {

        Scheduler sh = new Scheduler();
        sh.run();

    }

}

