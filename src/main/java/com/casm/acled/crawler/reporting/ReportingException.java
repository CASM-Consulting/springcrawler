package com.casm.acled.crawler.reporting;

public class ReportingException extends RuntimeException {

    private final Report report;

    public ReportingException(Report report) {
        super(report.toString());
        this.report = report;
    }

    public Report get() {
        return report;
    }
}
