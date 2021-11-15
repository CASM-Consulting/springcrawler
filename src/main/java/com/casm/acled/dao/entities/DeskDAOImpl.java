package com.casm.acled.dao.entities;

import com.casm.acled.dao.Tables;
import com.casm.acled.dao.VersionedEntityDAOImpl;
import com.casm.acled.dao.rowmappers.VersionedEntityRowMapperFactory;
import com.casm.acled.entities.desk.Desk;
import com.casm.acled.entities.sourcelist.SourceList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Repository
@Primary
public class DeskDAOImpl extends VersionedEntityDAOImpl<Desk> implements DeskDAO {

    public DeskDAOImpl(@Autowired JdbcTemplate jdbcTemplate,
                       @Autowired VersionedEntityRowMapperFactory rowMapperFactory,
                       @Value(Tables.T_DESK) String table) {
        super(jdbcTemplate, table, Desk.class, rowMapperFactory.of(Desk.class));
    }

    public Desk bySourceList(SourceList sourceList) {
        return getById(sourceList.get(SourceList.DESK_ID)).get();
    }

}
