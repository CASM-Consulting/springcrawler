package com.casm.acled.crawler.reporting;

import com.casm.acled.dao.entities.CrawlReportDAO;
import com.casm.acled.entities.crawlreport.CrawlReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Primary
public class DBReporter implements Reporter {

    private final List<Report> reports;

    @Autowired
    private CrawlReportDAO crawlReportDAO;

    private String runId;

    public DBReporter() {
        reports = new ArrayList<>();
    }

    public DBReporter report(Report report) {
        report = assignRunId(report);
        crawlReportDAO.create(report.toCrawlReport());
        return this;
    }

    public DBReporter report(Collection<Report> reports) {
        crawlReportDAO.create(reports.stream().map(this::assignRunId).map(Report::toCrawlReport).collect(Collectors.toList()));
        return this;
    }

    @Override
    public List<Report> getRunReports(String runId) {
        return crawlReportDAO.getBy(CrawlReport.RUN_ID, runId).stream().map(Report::of).collect(Collectors.toList());
    }

    private static DBReporter reporter;

    public static synchronized DBReporter get() {
        if(reporter == null) {
            reporter = new DBReporter();
        }
        return reporter;
    }

    public List<Report> reports() {
        return crawlReportDAO.getAll().stream().map(Report::of).collect(Collectors.toList());
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
