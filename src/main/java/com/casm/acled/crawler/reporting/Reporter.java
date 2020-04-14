package com.casm.acled.crawler.reporting;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Reporter {

    private final List<Report> reports;

    public Reporter() {
        reports = new ArrayList<>();
    }


    public void report(Report report) {
        reports.add(report);
    }

    private static Reporter reporter;

    public static synchronized Reporter get() {
        if(reporter == null) {
            reporter = new Reporter();
        }
        return reporter;
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
