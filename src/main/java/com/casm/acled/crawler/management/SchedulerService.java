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

    /**
     * Did job fail to finish running before it must be run again?
     */
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
        } else {
            if (jobEndTime.isAfter(prevRun) && jobEndTime.isBefore(nextRun)) {
                return Action.PASS;
            }
            else {
                return Action.PASS; // TODO is this right?
            }
        }
    }

    private void reportJob(Job job, Event e) {
        reporter.report(Report.of(e).id(job.id()).message(job.name()));
    }

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

        LocalDateTime now = timeProvider.getTime();

        // A job that's never been run before will default to crawling from a week ago
        LocalDate crawlFrom = now.minusDays(7).toLocalDate();
        // The to date will always be a couple months ahead to ensure we don't discount any data that's too recent
        LocalDate crawlTo = now.plusMonths(2).toLocalDate();

        LocalDateTime nextRun = fromDate(cron.getTimeAfter(toDate(now)));
        LocalDateTime prevRun = fromDate(getTimeBefore(toDate(now), cron));

        // pid could be null
        Optional<Integer> maybePid = job.pid();

        // check if it is a registered one;
        Optional<Job> maybeJob = maybePid.isPresent()? checkJobStatus(maybePid.get()) : Optional.empty();

        Action action;

        if(maybeJob.isPresent()) {
            Job curJob = maybeJob.get();

            // Set the from date to when the job last started (minusing a day for safety)
            crawlFrom = curJob.getStarted().minusDays(1).toLocalDate();

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
                job.setFromTo(crawlFrom, crawlTo);
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

    public void schedule(CrawlArgs args) throws Exception {

        List<Job> jobs = jobProvider.getJobs(args);

        for (Job job : jobs) {

            ensureSchedule(job);
        }
    }
}
