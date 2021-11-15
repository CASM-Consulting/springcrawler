package com.casm.acled.dao;

import com.casm.acled.dao.rowmappers.VersionedEntityLinkRowMapper;
import com.casm.acled.dao.util.SqlBinder;
import com.casm.acled.entities.VersionedEntity;
import com.casm.acled.entities.VersionedEntityLink;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;


public abstract class LinkDAOImpl<L1 extends VersionedEntity<L1>, L2 extends VersionedEntity<L2>, V extends VersionedEntityLink<V>> extends VersionedEntityDAOImpl<V> implements LinkDAO<L1, L2, V> {

    private static final Logger LOG = LoggerFactory.getLogger(LinkDAOImpl.class);


    public LinkDAOImpl(JdbcTemplate jdbcTemplate,
                       String table,
                       Class<V> klass,
                       VersionedEntityLinkRowMapper<V> rowMapper) {
        super(jdbcTemplate, table, klass, rowMapper, ImmutableList.of("data", "id1", "id2"));
    }

    @Override
    protected void setAdditionalFields(V entity, PreparedStatement stmt) throws SQLException {
        stmt.setInt(2, entity.id1());
        stmt.setInt(3, entity.id2());
    }

    @Override
    public V link(int id1, int id2) {
        String sql = SqlBinder.sql("INSERT INTO ${table}(id1, id2) VALUES (?,?) ON CONFLICT DO NOTHING")
                .bind("table", table)
                .bind();
        try {
            jdbcTemplate.update(sql, id1, id2);
            return get(id1, id2).get();
        } catch (DuplicateKeyException e ) {
            //this shouldn't happen with 'ON CONFLICT DO NOTHING'
            LOG.warn(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void unlink(int id1, int id2) {
        String sql = SqlBinder.sql("DELETE FROM ${table} WHERE id1 = ? AND id2 = ?")
                .bind("table", table)
                .bind();
        try {
            jdbcTemplate.update(sql, id1, id2);
        } catch (DuplicateKeyException e ) {
            //this shouldn't happen with 'ON CONFLICT DO NOTHING'
            LOG.warn(e.getMessage(), e);
        }
    }

    @Override
    public void unlink1(int id1) {
        String sql = SqlBinder.sql("DELETE FROM ${table} WHERE id1 = ? ")
                .bind("table", table)
                .bind();
        try {
            jdbcTemplate.update(sql, id1);
        } catch (DuplicateKeyException e ) {
            //this shouldn't happen with 'ON CONFLICT DO NOTHING'
            LOG.warn(e.getMessage(), e);
        }
    }


    @Override
    public void unlink2(int id2) {
        String sql = SqlBinder.sql("DELETE FROM ${table} WHERE id2 = ?")
                .bind("table", table)
                .bind();
        try {
            jdbcTemplate.update(sql, id2);
        } catch (DuplicateKeyException e ) {
            //this shouldn't happen with 'ON CONFLICT DO NOTHING'
            LOG.warn(e.getMessage(), e);
        }
    }


    @Override
    public void clear() {
        String sql = SqlBinder.sql("DELETE FROM ${table}")
                .bind("table", table)
                .bind();

        jdbcTemplate.execute(sql);
    }



    @Override
    public List<V> getBy2(L2 l2) {
        String sql = SqlBinder.sql("SELECT id, data, id1, id2 FROM ${table} WHERE id2 = ?")
                .bind("table", table)
                .bind();

        return jdbcTemplate.query(sql, rowMapper, l2.id());
    }

    @Override
    public List<V> getBy1(L1 l1){
        String sql = SqlBinder.sql("SELECT id, data, id1, id2 FROM ${table} WHERE id1 = ?")
                .bind("table", table)
                .bind();

        return jdbcTemplate.query(sql, rowMapper, l1.id());
    }

    @Override
    public Optional<V> get(int id1, int id2){
        String sql = SqlBinder.sql("SELECT id, data, id1, id2 FROM ${table} WHERE id1 = ? AND id2 = ?")
                .bind("table", table)
                .bind();

        List<V> results = jdbcTemplate.query(sql, rowMapper, id1, id2);
        if(results.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(results.get(0));
        }
    }

    @Override
    public String table() {
        return table;
    }
}
