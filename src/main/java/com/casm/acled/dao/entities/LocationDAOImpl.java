package com.casm.acled.dao.entities;

import com.casm.acled.dao.Tables;
import com.casm.acled.dao.VersionedEntityDAOImpl;
import com.casm.acled.dao.rowmappers.VersionedEntityRowMapperFactory;
import com.casm.acled.dao.util.SqlBinder;
import com.casm.acled.entities.location.Location;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
@Repository
@Primary
public class LocationDAOImpl extends VersionedEntityDAOImpl<Location> implements LocationDAO  {

    public LocationDAOImpl(@Autowired JdbcTemplate jdbcTemplate,
                           @Autowired VersionedEntityRowMapperFactory rowMapperFactory,
                           @Value(Tables.T_LOCATION) String table) {
        super(jdbcTemplate, table, Location.class, rowMapperFactory.of(Location.class));
    }

    @Override
    public List<String> countries() {
        String sql = new SqlBinder("SELECT DISTINCT data->>'${country}' FROM ${table} ORDER BY data->>'${country}' ASC")
                .bind("table", table)
                .bind("country", Location.COUNTRY)
                .bind();

        List<String> countries = jdbcTemplate.query(sql, (r, i)-> r.getString(1));

        return countries;
    }

    @Override
    public List<Location> byName(String name) {
        String sql = new SqlBinder("SELECT * FROM ${table} AS ST")
            .append("WHERE ST.data->>'${location_name_column}' = ?")
            .bind("table", table)
            .bind("location_name_column", Location.LOCATION)
            .bind();
        List<Location> locations = query(sql, name);
        return locations;
    }

    @Override
    public List<Location> byCountry(String country) {
        String sql = new SqlBinder("SELECT * FROM ${table} AS ST")
                .append("WHERE ST.data->>'${location_country_column}' = ?")
                .bind("table", table)
                .bind("location_country_column", Location.COUNTRY)
                .bind();
        List<Location> locations = query(sql, country);
        return locations;
    }


    @Override
    public List<Location> byDesk(Integer deskId) {

        String sql = SqlBinder.sql("SELECT L.id AS L_ID,",
                "L.data AS L_DATA",
                "FROM ${table} AS L",
                "LEFT JOIN ${join_table} AS LD ON (LD.id1 = L.id)",
                "WHERE LD.id2 = ?")
                .bind("table", table)
                .bind("join_table", Tables.T_LOCATION_DESK)
                .bind();

        List<Location> locations = query(sql, deskId);
        return locations;
    }

}
