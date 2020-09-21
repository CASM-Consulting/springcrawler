package com.casm.acled.crawler.management;

import com.casm.acled.crawler.Crawl;
import com.casm.acled.crawler.reporting.Event;
import com.casm.acled.crawler.reporting.Report;
import com.casm.acled.crawler.reporting.Reporter;

import com.enioka.jqm.api.*;

import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.*;
import java.util.*;

@Service
public class SchedulerService {
    //TODO
    // to ask, the project sometimes will run several applications for instance, when I only execute the SchedulerRunner, it sometimes run Crawler.. and ...;
    // the idea is that, we load all possible job requests from source, and compare them with current running/created job instances, then
    // about the crawl_JOB_ID, if it doesnt have one, then it doesnt run, just run it.

    // the whole pipeline: get all possible jobs from jobprovider, check if job is valid/runnable by makeJob method and JobRunner's get JOB, then choose too run them or not;
    // if crawl_job_id does not exist, then run it;
    // jobprovider provides all potential jobs, jobrunner will check them onebyone by using getJob(), and run it by runJob(); Only a job request object (wrapped as Job object) is passed

    protected static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    private final CronExpression defaultCronSchedule;

    private final JobRunner jobRunner;
    private final JobProvider jobProvider;
    private final Reporter reporter;
    private final TimeProvider timeProvider;

    private enum Action {
        RUN,
        PASS
    }

    public SchedulerService(@Autowired Reporter reporter, @Autowired TimeProvider timeProvider,
                            @Autowired JobRunner jobRunner, @Autowired JobProvider jobProvider) {

        this.reporter = reporter;
        this.timeProvider = timeProvider;
        this.jobProvider = jobProvider;
        this.jobRunner = jobRunner;


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

        ZonedDateTime zonedNow = timeProvider.getTime().atZone(zoneId);

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
    private Action checkStillRunningFromLastTime(Job job, Event e) {
        // TODO: does comparison even need to be made at this stage?
        reporter.report(Report.of(e).id(job.id()).message(job.name()));

        return Action.PASS;
    }

    // to be implemented, scheduled to run again. could involve cronexpression comparison
    private Action checkShouldRunAgain(Job job, CronExpression cron, LocalDateTime nextRun, LocalDateTime prevRun, LocalDateTime timeNow) {
        // if need to run again, the ending time should be after

        LocalDateTime jobEndTime = job.getStopped();
        if (jobEndTime.isBefore(prevRun)) {
            return Action.RUN;
        }
        else {
            if (jobEndTime.isAfter(prevRun) && jobEndTime.isBefore(nextRun)) {
                return Action.PASS;
            }
            else {
                return Action.PASS;
            }
        }
    }

    // to be implemented , send email to admin
    private void reportJob(Job job, Event e) {
        reporter.report(Report.of(e).id(job.id()).message(job.name()));
    }

    // to be implemented, how long between enquequed and current state; report to admin about this.
    private void checkTimeSinceSubmitted(Job job, LocalDateTime timeNow) {
        LocalDateTime jobStartTime = job.getStarted();
        String msg = String.format("The job is still starting; it started at %s and current time is %s", jobStartTime.toString(), timeNow.toString());
        reporter.report(Report.of(Event.JOB_STILL_STARTING).id(job.id()).message(job.name()+"||"+msg));
    }

    private Optional<Job> checkJobStatus(int jobPID) {

        Optional<Job> maybeJob = Optional.empty();
        try {

            Job curJob = jobRunner.getJob(jobPID);

            maybeJob = Optional.of(curJob);
        } catch (JqmInvalidRequestException e) {
            logger.info(e.getMessage());
            //job hasn't run;
        }
        return maybeJob;
    }

    public void ensureSchedule(Job job) {


        CronExpression cron = new CronExpression(defaultCronSchedule);

        cron = job.getSchedule();

//        ZoneId zoneId = ZoneId.of(source.get(Source.TIMEZONE));
//        TimeZone timeZone = TimeZone.getTimeZone(zoneId);
//        cron.setTimeZone(timeZone);

//        Date zonedNow = getZonedNow(zoneId);
        LocalDateTime now = timeProvider.getTime();
        LocalDateTime nextRun = fromDate(cron.getTimeAfter(toDate(now)));
        LocalDateTime prevRun = fromDate(getTimeBefore(toDate(now), cron));

        // check if it is a registered one;
        Optional<Job> maybeJob = checkJobStatus(job.pid());

        Action action;

        if(maybeJob.isPresent()) {
            Job curJob = maybeJob.get();
            String jobState = curJob.state();
            switch (jobState) {
                case Job.RUNNING:
                    // if it is still running, then it runs so slow and should directly report to admin;
                    action = checkStillRunningFromLastTime(job, Event.JOB_STILL_RUNNING);
                    break;
                case Job.STOPPED:
                    // to check if it is time to run the job again; modify code here if want to rerun job using the same parameters (assign curJob to job's jobinstance param);
                    action = checkShouldRunAgain(curJob, cron, nextRun, prevRun, now);
                    break;
                case Job.FAILED:
                    // if job crashed, we should definitely report that and pass it or rerun it?? not sure;
                    reportJob(job, Event.JOB_CRASHED);
                    action = Action.PASS;
                    break;
                case Job.CANCELLED:
                    reportJob(job, Event.JOB_CANCELLED);
                    action = Action.PASS;
                    break;
                case Job.STARTING:
                    checkTimeSinceSubmitted(job, now);
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

    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private LocalDateTime fromDate(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    public void schedule( ) throws Exception {

        // example parameters; probably we should decide if we should run job request using corresponding Jobinstance's parameters or using our own new parameters;
        Map<String, String> params = new HashMap<String, String>();
        params.put(Crawl.SKIP_KEYWORD_FILTER, Boolean.TRUE.toString());
        params.put(Crawl.FROM, LocalDate.of(2020, 8,21).toString());
        params.put(Crawl.TO, LocalDate.of(2020, 8,28).toString());

        List<Job> allPossibleJobs = jobProvider.getJobs(params);
        for (Job possibleJob : allPossibleJobs) {

            ensureSchedule(possibleJob);

        }

    }
}