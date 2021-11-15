package com.casm.acled.dao.entities;

import com.casm.acled.dao.LinkDAOImpl;
import com.casm.acled.dao.Tables;
import com.casm.acled.dao.rowmappers.VersionedEntityRowMapperFactory;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.articleevent.ArticleEvent;
import com.casm.acled.entities.event.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Repository
@Primary
public class ArticleEventDAOImpl extends LinkDAOImpl<Article, Event, ArticleEvent> implements ArticleEventDAO {

    public ArticleEventDAOImpl(@Autowired JdbcTemplate jdbcTemplate,
                               @Value(Tables.T_ARTICLE_EVENT) String table,
                               @Autowired VersionedEntityRowMapperFactory rowMapperFactory) {
        super(jdbcTemplate, table, ArticleEvent.class, rowMapperFactory.ofLink(ArticleEvent.class));
    }

}
