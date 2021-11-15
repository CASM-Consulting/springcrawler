package com.casm.acled.entities.sourcelist;

import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.VersionedEntity;

import java.util.Map;

public class SourceList extends VersionedEntity<SourceList> {

    public static final String LIST_NAME = "LIST_NAME";
    public static final String DESK_ID = "DESK_ID";
    public static final String KEYWORDS = "KEYWORDS";
    public static final String TIMEZONE = "TIMEZONE";
    public static final String CRAWL_ACTIVE = "CRAWL_ACTIVE";
    public static final String MANUAL_ACTIVE = "MANUAL_ACTIVE";
    public static final String FROM = "FROM";
    public static final String TO = "TO";
    public static final String NOTES = "NOTES";
    public static final String BACK_CODING = "BACK_CODING";

    public SourceList(EntitySpecification entitySpecification, String version, Map<String, Object> data, Integer id) {
        super(entitySpecification, version, data, id);
    }
}