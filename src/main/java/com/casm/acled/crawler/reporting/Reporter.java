package com.casm.acled.crawler.reporting;

import com.casm.acled.crawler.management.CrawlerSweep;
import com.casm.acled.dao.entities.CrawlReportDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface Reporter {

    Logger logger = LoggerFactory.getLogger(Reporter.class);

    Reporter report(Report report);

    Reporter report(Collection<Report> reports);

    List<Report> reports();

    default String randomRunId() {
        if(runId() == null){
            runId(UUID.randomUUID().toString());
            logger.info("reporter run id : " + runId());
        }
        return runId();
    }
    Reporter runId(String runId);
    String runId();

    List<Report> getRunReports();

    default List<Report> getRunReports(Integer id, Event event) {
        return getRunReports().stream()
                .filter(r -> r.id().equals(id) && r.event().equals(event.toString()))
                .collect(Collectors.toList());
    }

    default List<Report> getRunReports(Integer id) {
        return getRunReports().stream()
                .filter(r -> r.id().equals(id))
                .collect(Collectors.toList());
    }

    default List<Report> getRunReports(Event event) {
        return getRunReports().stream()
                .filter(r -> r.event().equals(event.toString()))
                .collect(Collectors.toList());
    }


//    public Reporter reports(Predicate<Report> filter) {
//        return new Reporter().report(reports.stream().filter(filter).collect(Collectors.toList()));
//    }
}
