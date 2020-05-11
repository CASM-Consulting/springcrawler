package com.casm.acled.crawler.reporting;

import com.casm.acled.dao.entities.CrawlReportDAO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface Reporter {


    Reporter report(Report report);

    Reporter report(Collection<Report> reports);

    List<Report> reports();

    default String randomRunId() {
        if(runId() == null){
            runId(UUID.randomUUID().toString());
        }
        return runId();
    }
    Reporter runId(String runId);
    String runId();

    List<Report> getRunReports();

//    public Reporter reports(Predicate<Report> filter) {
//        return new Reporter().report(reports.stream().filter(filter).collect(Collectors.toList()));
//    }
}
