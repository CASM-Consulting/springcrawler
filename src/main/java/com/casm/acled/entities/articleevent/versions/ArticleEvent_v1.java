package com.casm.acled.entities.articleevent.versions;

import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.articleevent.ArticleEvent;

import java.util.Map;

public class ArticleEvent_v1 extends ArticleEvent {

    private static final EntitySpecification SPECIFICATION = EntitySpecification
            .empty()
//            .maybeHistorical()
            .add(ARTICLE_ID, Integer.class)
            .add(EVENT_ID, Integer.class);

    public ArticleEvent_v1(Map<String, Object> data, Integer id, Integer id1, Integer id2) {
        super(SPECIFICATION, "v1", data, id, id1, id2);
    }

}
