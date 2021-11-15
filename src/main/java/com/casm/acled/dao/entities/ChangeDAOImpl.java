package com.casm.acled.dao.entities;

import com.casm.acled.dao.Tables;
import com.casm.acled.dao.VersionedEntityDAOImpl;
import com.casm.acled.dao.rowmappers.VersionedEntityRowMapperFactory;
import com.casm.acled.entities.change.Change;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Repository
@Primary
public class ChangeDAOImpl extends VersionedEntityDAOImpl<Change> implements ChangeDAO  {

    public ChangeDAOImpl(@Autowired JdbcTemplate jdbcTemplate,
                         @Autowired VersionedEntityRowMapperFactory rowMapperFactory,
                         @Value(Tables.T_CHANGE) String table) {
        super(jdbcTemplate, table, Change.class, rowMapperFactory.of(Change.class));
    }
}
