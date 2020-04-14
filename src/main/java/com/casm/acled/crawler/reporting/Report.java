package com.casm.acled.crawler.reporting;

import java.time.Instant;

public class Report {
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

    public Report message(String format, Object... args) {
        return new Report(event, id, type,  String.format(format, args));
    }

    public Report append(String format, Object... args) {
        return new Report(event, id, type, this.message + " " + String.format(format, args));
    }

    public Report id(Object id) {
        return new Report(event, id.toString(), type, message);
    }
    public Report type(Object type) {
        return new Report(event, id, type.toString(), message);
    }

    public static Report of(Object event, Object id, String type, String format, Object... args) {
        return new Report(event.toString(), id == null ? null  : id.toString(), type, format == null ? null : String.format(format, args));
    }

    public static Report of(Object event, Object id, String type) {
        return of(event, id, type, null);
    }

    public static Report of(Object event, Object id) {
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
    public String id() {
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
}
