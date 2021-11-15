package com.casm.acled.entities.articleevent;

import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.VersionedEntityLink;

import java.util.Map;

public class ArticleEvent extends VersionedEntityLink<ArticleEvent> {

    public static final String ARTICLE_ID = ID1;
    public static final String EVENT_ID = ID2;

    public ArticleEvent(EntitySpecification entitySpecification, String version, Map<String, Object> data, Integer id, Integer id1, Integer id2) {
        super(entitySpecification, version, data, id, id1, id2);
    }
}