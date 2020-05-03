package com.casm.acled.crawler.reporting;

import com.casm.acled.dao.entities.CrawlReportDAO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class InMemoryReporter implements Reporter {

    private final List<Report> reports;

    public InMemoryReporter() {
        reports = new ArrayList<>();
    }

    public InMemoryReporter report(Report report) {
        reports.add(report);
        return this;
    }

    public InMemoryReporter report(Collection<Report> reports) {
        reports.addAll(reports);
        return this;
    }

    private static InMemoryReporter reporter;

    public static synchronized InMemoryReporter get() {
        if(reporter == null) {
            reporter = new InMemoryReporter();
        }
        return reporter;
    }

    public List<Report> reports() {
        return new ArrayList<>(reports);
    }

//    public Reporter reports(Predicate<Report> filter) {
//        return new Reporter().report(reports.stream().filter(filter).collect(Collectors.toList()));
//    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(Report report : reports){
            sb.append(report.toString());
            sb.append(String.format("%n"));
        }
        return sb.toString();
    }
}
