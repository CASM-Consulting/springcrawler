package com.casm.acled.crawler.management;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Reporting {

    private final List<Report> reports;

    public Reporting() {
        reports = new ArrayList<>();
    }

    public void report(String event, Object id, String type, String format, Object... args) {
        reports.add(Report.of(event, id.toString(), type, String.format(format, args))) ;
    }

    public static class Report {
        private final Instant timestamp;
        private final String event;
        private final String id;
        private final String type;
        private final String message;

        public Report(String event, String id, String type, String message) {
            this.event = event;
            this.id = id;
            this.type = type;
            this.message = message;
            timestamp = Instant.now();
        }

        public static Report of(String event, String id, String type, String message) {
            return new Report(event, id, type, message);
        }
    }

    private static Reporting reporting;

    public static synchronized Reporting get() {
        if(reporting == null) {
            reporting = new Reporting();
        }
        return reporting;
    }
}
