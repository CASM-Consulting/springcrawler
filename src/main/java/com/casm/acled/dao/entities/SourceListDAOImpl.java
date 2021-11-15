package com.casm.acled.dao.entities;

import com.casm.acled.dao.Tables;
import com.casm.acled.dao.VersionedEntityDAOImpl;
import com.casm.acled.dao.rowmappers.VersionedEntityRowMapperFactory;
import com.casm.acled.dao.util.SqlBinder;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional
@Repository
@Primary
public class SourceListDAOImpl extends VersionedEntityDAOImpl<SourceList> implements SourceListDAO  {

    public SourceListDAOImpl(@Autowired JdbcTemplate jdbcTemplate,
                             @Autowired VersionedEntityRowMapperFactory rowMapperFactory,
                             @Value(Tables.T_SOURCE_LIST) String table) {
        super(jdbcTemplate, table, SourceList.class, rowMapperFactory.of(SourceList.class));
    }

    @Override
    public List<SourceList> bySource(Source source) {

        String sql = new SqlBinder("SELECT SLT.id, SLT.data FROM ${table} AS SLT")
                .append("JOIN ${source_source_list_table} AS SSLT ON SSLT.id2 = SLT.id")
                .append("WHERE SSLT.id1 = ?")
                .bind("table", table)
                .bind("source_source_list_table", Tables.T_SOURCE_SOURCE_LIST)
                .bind();

        List<SourceList> lists = query(sql, source.id());

        return lists;
    }

    @Override
    public List<SourceList> bySource(Integer id) {

        String sql = new SqlBinder("SELECT SLT.id, SLT.data FROM ${table} AS SLT")
                .append("JOIN ${source_source_list_table} AS SSLT ON SSLT.id2 = SLT.id")
                .append("WHERE SSLT.id1 = ?")
                .bind("table", table)
                .bind("source_source_list_table", Tables.T_SOURCE_SOURCE_LIST)
                .bind();

        List<SourceList> lists = query(sql, id);

        return lists;
    }

    @Override
    public Optional<SourceList> byName(String name) {
        return getByUnique(SourceList.LIST_NAME, name);
    }

    @Override
    public List<SourceList> byDesk(Integer id) {

        return getBy(SourceList.DESK_ID, id.toString());
    }
}
