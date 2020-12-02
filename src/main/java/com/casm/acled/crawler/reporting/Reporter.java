package com.casm.acled.crawler.reporting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public interface Reporter {

    Logger logger = LoggerFactory.getLogger(Reporter.class);

    Reporter report(Report report);

    Reporter report(Collection<Report> reports);

    List<Report> reports();

//    default String randomRunId() {
//        if(runId() == null){
//            runId(UUID.randomUUID().toString());
//            logger.info("reporter run id : " + runId());
//        }
//        return runId();
//    }
//    Reporter runId(String runId);
//    String runId();

    default Report assignRunId(Report report) {
        return report.runId(getRunId(report.id(), report.timestamp()));
    }

    default String getRunId(Integer id, Instant timestamp){
        String date = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.systemDefault()).format(timestamp);
        return id + "~" + date;
    }

    List<Report> getRunReports(String runId);

    default List<Report> getRunReports(Integer id, Event event) {
        return getRunReports(id).stream()
                .filter(r -> r.event().equals(event.toString()))
                .collect(Collectors.toList());
    }

    /**
     * Get all reports for events concerning a particular source that happened today.
     */
    default List<Report> getRunReports(Integer id) {
        return getRunReports(getRunId(id, Instant.now())).stream()
                .filter(r -> r.id().equals(id))
                .collect(Collectors.toList());
    }


//    public Reporter reports(Predicate<Report> filter) {
//        return new Reporter().report(reports.stream().filter(filter).collect(Collectors.toList()));
//    }
}
