package com.casm.acled.crawler.reporting;

import com.casm.acled.crawler.management.Crawl;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.crawlreport.CrawlReport;

import java.time.Instant;

public class Report {
    private final Instant timestamp;
    private final String event;
    private final Integer id;
    private final String type;
    private final String message;
    private final String businessKey;

    public Report(String event, Integer id, String type, String message, String businessKey) {
        this(event, id, type, message, businessKey, Instant.now());
    }
    public Report(String event, Integer id, String type, String message, String businessKey, Instant timestamp) {
        this.businessKey = businessKey;
        this.event = event;
        this.id = id;
        this.type = type;
        this.message = message;
        this.timestamp = timestamp;
    }

    public Report message(String format, Object... args) {
        if(args.length == 0) {
            return new Report(event, id, type, format, businessKey);
        } else {
            return new Report(event, id, type,  String.format(format, args), businessKey);
        }
    }

    public Report append(String format, Object... args) {
        return new Report(event, id, type, this.message + " " + String.format(format, args), businessKey);
    }

    public Report id(Integer id) {
        return new Report(event, id, type, message, businessKey);
    }

    public Report type(Object type) {
        return new Report(event, id, type.toString(), message, businessKey);
    }

    private Report timestamp(Instant timestamp) {
        return new Report(event, id, type, message, businessKey);
    }

    public static Report of(Object event, Integer id, String type, String format, Object... args) {
        return new Report(event.toString(), id, type, format == null ? null : String.format(format, args), null );
    }

    public static Report of(Object event, Integer id, String type) {
        return of(event, id, type, null);
    }

    public static Report of(Object event, Integer id) {
        return of(event, id, null, null);
    }

    public static Report of(Object event) {
        return of(event, null, null, null);
    }

    public Instant timestamp() {
        return timestamp;
    };
    public String event() {
        return event;
    };
    public Integer id() {
        return id;
    };
    public String type() {
        return type;
    };
    public String message() {
        return message;
    };

    @Override
    public String toString() {
        return "Report{" +
                "timestamp=" + timestamp +
                ", event='" + event + '\'' +
                ", id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

    public CrawlReport toCrawlReport() {
        CrawlReport cr = EntityVersions.get(CrawlReport.class).current();
        if(id != null) {
            cr = cr.put(CrawlReport.ID, id);
        }
        if(type != null) {
            cr = cr.put(CrawlReport.TYPE, type);
        }
        if(timestamp != null) {
            cr = cr.put(CrawlReport.TIMESTAMP, timestamp);
        }
        if(message != null) {
            cr = cr.put(CrawlReport.MESSAGE, message);
        }
        if(event != null) {
            cr = cr.put(CrawlReport.EVENT, event);
        }

        return cr;
    }

    public static Report of(CrawlReport cr) {
        Report r = new Report(cr.get(CrawlReport.EVENT),
                cr.get(CrawlReport.ID),
                cr.get(CrawlReport.TYPE),
                cr.get(CrawlReport.MESSAGE),
                cr.hasBusinessKey() ? cr.businessKey() : "",
                cr.get(CrawlReport.TIMESTAMP));
        return r;
    }
}
