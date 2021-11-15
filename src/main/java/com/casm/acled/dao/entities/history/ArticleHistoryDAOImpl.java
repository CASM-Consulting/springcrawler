package com.casm.acled.dao.entities.history;

import com.casm.acled.camunda.variables.Entities;
import com.casm.acled.dao.entities.ArticleDAOImpl;
import com.casm.acled.dao.rowmappers.VersionedEntityRowMapperFactory;
import com.casm.acled.entities.article.Article;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Repository
public class ArticleHistoryDAOImpl extends ArticleDAOImpl implements ArticleHistoryDAO {

    public ArticleHistoryDAOImpl(@Autowired JdbcTemplate jdbcTemplate,
                                 @Autowired ArticleEventHistoryDAO articleEventHistoryDAO,
                                 @Autowired VersionedEntityRowMapperFactory rowMapperFactory
    ) {
        super(jdbcTemplate, rowMapperFactory, articleEventHistoryDAO, "ACLED_hi_article");
    }

    @Override
    protected Article preCreate(Article article) {
        article = article.put(Entities.HISTORICAL, true);
        return article;
    }
}
