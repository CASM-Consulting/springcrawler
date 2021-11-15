package com.casm.acled.dao.entities;

import com.casm.acled.camunda.variables.Entities;
import com.casm.acled.dao.Tables;
import com.casm.acled.dao.VersionedEntityDAOImpl;
import com.casm.acled.dao.rowmappers.VersionedEntityRowMapperFactory;
import com.casm.acled.dao.util.SqlBinder;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.desk.Desk;
import com.casm.acled.entities.event.Event;
import com.casm.acled.entities.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
@Repository
@Primary
public class ArticleDAOImpl extends VersionedEntityDAOImpl<Article> implements ArticleDAO  {

    private static final Logger LOG = LoggerFactory.getLogger(VersionedEntityDAOImpl.class);

    private final ArticleEventDAO articleEventDAO;

    public ArticleDAOImpl(@Autowired JdbcTemplate jdbcTemplate,
                          @Autowired VersionedEntityRowMapperFactory rowMapperFactory,
                          @Autowired ArticleEventDAO articleEventDAO,
                          @Value(Tables.T_ARTICLE) String table) {
        super(jdbcTemplate,table, Article.class, rowMapperFactory.of(Article.class));
        this.articleEventDAO = articleEventDAO;
    }

    @Override
    public List<Article> byRegion(Desk desk) {

        String sql = SqlBinder.sql("SELECT * FROM ${table} WHERE ").bind();

        return query(sql);
    }

    @Override
    public List<Article> byEvent(Event event) {

        String sql = SqlBinder.sql("SELECT * FROM ${table}",
                "JOIN ${join_table} ON (${join_table}.id1 = ${table}.id)",
                "WHERE ${join_table}.id2 = ?")
                .bind("table", table)
                .bind("join_table", Tables.T_ARTICLE_EVENT)
                .bind();

        List<Article> results = query(sql, event.id());

        return results;
    }

    @Override
    public List<Article> bySource(Source source) {
        String sql = SqlBinder.sql("SELECT * FROM ${table} WHERE (data->>'${source_id_field}')::int = ?")
                .bind("table", table)
                .bind("source_id_field", Article.SOURCE_ID)
                .bind();

        return query(sql, source.id());
    }

    @Override
    public List<Article> byResearcher(String id) {
        return null;
    }

//    @Override
//    public List<Article> create(List<Article> articles) {
//        articles = super.create(articles);
//        for(Article article : articles) {
//            for(Event event : article.events()) {
//                articleEventDAO.link(article, event);
//            }
//        }
//        return articles;
//    }


    @Cacheable("articles")
    public List<Article> getByBusinessKeyCached(String businessKey) {
        return getByBusinessKey(businessKey);
    }

    @Override
    protected Article preCreate(Article article) {
        if(article.isTrue(Entities.HISTORICAL)) {

            article = article.put(Entities.HISTORICAL_ID, article.id());
        }
        return article;
    }
}