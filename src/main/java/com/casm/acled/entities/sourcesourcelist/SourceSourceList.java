package com.casm.acled.entities.sourcesourcelist;

import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.VersionedEntityLink;

import java.util.Map;

public class SourceSourceList extends VersionedEntityLink<SourceSourceList> {

    public static final String SOURCE_ID = ID1;
    public static final String SOURCE_LIST_ID = ID2;
    public static final String KEYWORDS_DIFF = "KEYWORDS_DIFF";

    public SourceSourceList(EntitySpecification entitySpecification, String version, Map<String, Object> data, Integer id, Integer id1, Integer id2) {
        super(entitySpecification, version, data, id, id1, id2);
    }
}