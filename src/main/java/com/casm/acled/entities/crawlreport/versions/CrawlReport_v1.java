package com.casm.acled.entities.crawlreport.versions;

import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.crawlreport.CrawlReport;

import java.time.Instant;
import java.util.Map;

public class CrawlReport_v1 extends CrawlReport {


    private static final EntitySpecification SPECIFICATION = EntitySpecification.empty()
//            .business()
            .add(TIMESTAMP, Instant.class)
            .add(EVENT, String.class)
            .add(ID, Integer.class)
            .add(TYPE, String.class)
            .add(REPORTER_TYPE, String.class)
            .add(MESSAGE, String.class)
            .add(RUN_ID, String.class)
            ;

    public CrawlReport_v1(Map<String, Object> data, Integer id) {
        super(SPECIFICATION, "v1", data, id);
    }

}
