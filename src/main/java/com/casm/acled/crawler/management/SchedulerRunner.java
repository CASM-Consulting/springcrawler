package com.casm.acled.crawler.management;

import com.casm.acled.crawler.Crawl;
import com.casm.acled.crawler.reporting.Event;
import com.casm.acled.crawler.reporting.Report;
import com.casm.acled.crawler.reporting.Reporter;

import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.source.Source;
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

import java.text.ParseException;
import java.time.*;
import java.util.*;

@ComponentScan(basePackages={"com.casm.acled.dao", "com.casm.acled.crawler"})
public class SchedulerRunner implements CommandLineRunner {
    // to ask, the project sometimes will run several applications for instance, when I only execute the SchedulerRunner, it sometimes run Crawler.. and ...;
    // the idea is that, we load all possible job requests from source, and compare them with current running/created job instances, then
    // about the crawl_JOB_ID, if it doesnt have one, then it doesnt run, just run it.

    // the whole pipeline: get all possible jobs from jobprovider, check if job is valid/runnable by makeJob method and JobRunner's get JOB, then choose too run them or not;
    // if crawl_job_id does not exist, then run it;
    // jobprovider provides all potential jobs, jobrunner will check them onebyone by using getJob(), and run it by runJob(); Only a job request object (wrapped as Job object) is passed

    protected static final Logger logger = LoggerFactory.getLogger(SchedulerRunner.class);

    @Autowired
    private SourceListDAO sourceListDAO;

    @Autowired
    private SourceDAO sourceDAO;

    @Autowired
    private Reporter reporter;

    private final CronExpression defaultCronSchedule;

    private final JQMJobRunner jobRunner; // to run job;

//    private final JQMJob job;

    private final JQMJobProvider jobProvider; // what's the effect of this in here.

    private enum Action {
        RUN,
        PASS
    }

    public SchedulerRunner() {

        jobProvider = new JQMJobProvider();
        jobRunner = new JQMJobRunner();


        try {
            //tuesdays and fridays at 8 pm
            defaultCronSchedule = new CronExpression("0 0 20 ? * TUE,FRI *");
            // below one minute update is just for testing convenience;
//            defaultCronSchedule = new CronExpression("0 */1 * * * ?");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    // given current time, calculate the time that should run previously according to cron expression
    public Date getTimeBefore(Date endTime, CronExpression cron) {
        final Date after = cron.getTimeAfter(endTime);

        if (after == null) return null;

        long afterMillis = after.getTime();

        long upper = afterMillis;
        long lower = Long.MIN_VALUE;

        //establish a lower bound (and refine the upper while we're at it)
        for (long i = 1L; i > 0; i <<= 1L) {
            long candidateMillis = afterMillis - i;
            Date candidate = cron.getTimeAfter(new Date(candidateMillis));
            if (candidate == null) {
                return null;
            }
            if (candidate.equals(after)) {
                upper = candidateMillis - 1;
            } else {
                lower = candidateMillis;
                break;
            }
        }

        //do a binary search for the threshold value
        while (lower < (upper - 1)) {
            long middle = (lower + upper) >>> 1;

            Date candidate = cron.getTimeAfter(new Date(middle));
            if (candidate == null) {
                return null;
            }

            if (candidate.equals(after)) {
                upper = middle - 1;
            } else {
                lower = middle;
            }
        }
        return cron.getTimeAfter(new Date(lower));
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

    // to be implemented, running so slowly and should report to admin. could involve cronexpression comparison
    private Action checkStillRunningFromLastTime(Source source, Event e) {
        // emm, not sure if need to compare anything, since it is still running then it surely runs so slow and just need to report directly.
        reporter.report(Report.of(e).id(source.id()).message(source.get(Source.STANDARD_NAME)));

        return Action.PASS;
    }

    // to be implemented, scheduled to run again. could involve cronexpression comparison
    private Action checkShouldRunAgain(JobInstance job, CronExpression cron, Date nextRun, Date prevRun, Date timeNow) {
        // if need to run again, the ending time should be after

        Date jobEndTime = job.getEndDate().getTime();
        if (jobEndTime.before(prevRun)) {
            return Action.RUN;
        }
        else {
            if (jobEndTime.after(prevRun) && jobEndTime.before(nextRun)) {
                return Action.PASS;
            }
            else {
                return Action.PASS;
            }
        }
    }

    // to be implemented , send email to admin
    private void reportJob(Source source, Event e) {
        reporter.report(Report.of(e).id(source.id()).message(source.get(Source.STANDARD_NAME)));
    }

    // to be implemented, how long between enquequed and current state; report to admin about this.
    private void checkTimeSinceSubmitted(JobInstance job, Date timenow, Source source) {
        Date jobStartTime = job.getBeganRunningDate().getTime();
        String msg = String.format("The job is still under attributing, it starts from %s and current time is %s", jobStartTime.toString(), timenow.toString());
        reporter.report(Report.of(Event.JOB_STILL_ATTRIBUTED).id(source.id()).message(source.get(Source.STANDARD_NAME)+"||"+msg));
    }

    private Optional<JobInstance> checkJobStatus(Source source) {
        Integer jobId = source.get(Source.CRAWL_JOB_ID);
        Optional<JobInstance> maybeJob = Optional.empty();
        if(jobId != null) {
            try {

                JobInstance curJob = jobRunner.getJob(jobId).getJobInstance();

                maybeJob = Optional.of(curJob);
            } catch (JqmInvalidRequestException e) {
                logger.info(e.getMessage());
                //job hasn't run;
            }
        }
        return maybeJob;
    }

    public void ensureSchedule(JQMJob job) {

        // the incoming jobs are provided by JQMJobProvider, so it will have source parameter for sure.
        Source source = job.getSource();

        CronExpression cron = new CronExpression(defaultCronSchedule);

        // should also handle the getSchedule method in Job
        if(job.getSchedule()!=null) {
            try {
                String expression = job.getSchedule();
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
        Date prevRun = getTimeBefore(zonedNow, cron);

        // check if it is a registered one;
        Optional<JobInstance> maybeJob = checkJobStatus(source);

        Action action;

        if(maybeJob.isPresent()) {
            JobInstance curJob = maybeJob.get();
            State jobState = curJob.getState();
            switch (jobState) {
                case RUNNING:
                    // if it is still running, then it runs so slow and should directly report to admin;
                    action = checkStillRunningFromLastTime(source, Event.JOB_STILL_RUNNING);
                    break;
                case ENDED:
                    // to check if it is time to run the job again; modify code here if want to rerun job using the same parameters (assign curJob to job's jobinstance param);
                    action = checkShouldRunAgain(curJob, cron, nextRun, prevRun, zonedNow);
                    break;
                case HOLDED:
                case KILLED:
                case CRASHED:
                    // if job crashed, we should definitely report that and pass it or rerun it?? not sure;
                    reportJob(source, Event.JOB_CRASHED);
                    action = Action.PASS;
                    break;
                case CANCELLED:
                    reportJob(source, Event.JOB_CANCELLED);
                    action = Action.PASS;
                    break;
                case SCHEDULED:
                case SUBMITTED:
                case ATTRIBUTED:
                    checkTimeSinceSubmitted(curJob, zonedNow, source);
                    action = Action.PASS;
                    break;
                default:
                    //all good
                    action = Action.PASS;
            }
        } else {

            action = Action.RUN;
        }

        switch (action) {
            case RUN:
                jobRunner.runJob(job);
                break;
            case PASS:
            default:

        }

    }

    @Override
    public void run(String... args) throws Exception {

        // tried to remove these three lines and add @Component to JQMJobRunner and JQMJobProvider classes,
        // but their sourceDAO and sourceListDAO are still null, so I add them back here.
        jobProvider.setSourceDAO(sourceDAO);
        jobProvider.setSourceListDAO(sourceListDAO);
        jobRunner.setSourceDAO(sourceDAO);

        // example parameters; probably we should decide if we should run job request using corresponding Jobinstance's parameters or using our own new parameters;
        Map<String, String> params = new HashMap<String, String>();
        params.put(Crawl.SKIP_KEYWORD_FILTER, Boolean.TRUE.toString());
        params.put(Crawl.FROM, LocalDate.of(2020, 8,21).toString());
        params.put(Crawl.TO, LocalDate.of(2020, 8,28).toString());

        List<JQMJob> allPossibleJobs = jobProvider.getJobs(params);
        for (JQMJob possibleJob : allPossibleJobs) {

            ensureSchedule(possibleJob);

        }

    }

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(SchedulerRunner.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(args);
        logger.info("Spring Boot application started");
        ctx.close();
    }
}
