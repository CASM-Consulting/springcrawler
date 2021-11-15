package com.casm.acled.dao.entities.history;

import com.casm.acled.camunda.variables.Entities;
import com.casm.acled.dao.entities.ArticleEventDAOImpl;
import com.casm.acled.dao.rowmappers.VersionedEntityRowMapperFactory;
import com.casm.acled.entities.articleevent.ArticleEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Repository
public class ArticleEventHistoryDAOImpl extends ArticleEventDAOImpl implements ArticleEventHistoryDAO {

    public ArticleEventHistoryDAOImpl(@Autowired JdbcTemplate jdbcTemplate,
                                      @Autowired VersionedEntityRowMapperFactory rowMapperFactory) {
        super(jdbcTemplate, "ACLED_hi_article_event", rowMapperFactory);
    }

    @Override
    protected ArticleEvent preCreate(ArticleEvent articleEvent) {
        articleEvent = articleEvent.put(Entities.HISTORICAL, true);
        return articleEvent;
    }
}
