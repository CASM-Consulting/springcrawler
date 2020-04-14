package com.casm.acled.crawler.reporting;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class Reporter {

    private final List<Report> reports;

    public Reporter() {
        reports = new ArrayList<>();
    }

    public Reporter report(Report report) {
        reports.add(report);
        return this;
    }

    public Reporter report(Collection<Report> reports) {
        this.reports.addAll(reports);
        return this;
    }

    private static Reporter reporter;

    public static synchronized Reporter get() {
        if(reporter == null) {
            reporter = new Reporter();
        }
        return reporter;
    }

    public List<Report> reports() {
        return reports;
    }

    public Reporter reports(Predicate<Report> filter) {
        return new Reporter().report(reports.stream().filter(filter).collect(Collectors.toList()));
    }

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
