package com.casm.acled.entities.crawlreport;

import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.VersionedEntity;

import java.util.Map;

public class CrawlReport extends VersionedEntity<CrawlReport> {
    public static final String EVENT = "EVENT";
    public static final String TIMESTAMP = "TIMESTAMP";
    public static final String ID = "ID";
    public static final String TYPE = "TYPE";
    public static final String REPORTER_TYPE = "REPORTER_TYPE";
    public static final String MESSAGE = "MESSAGE";
    public static final String RUN_ID = "RUN_ID";

    public CrawlReport(EntitySpecification entitySpecification, String version, Map<String, Object> data, Integer id) {
        super(entitySpecification, version, data, id);
    }
}
