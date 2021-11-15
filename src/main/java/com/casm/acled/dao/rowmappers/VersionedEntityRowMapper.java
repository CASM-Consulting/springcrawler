package com.casm.acled.dao.rowmappers;

import com.casm.acled.entities.VersionedEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class VersionedEntityRowMapper<V extends VersionedEntity<V>> implements RowMapper<V> {

    private static final Logger LOG = LoggerFactory.getLogger(VersionedEntityRowMapper.class);

    protected final Class<V> klass;
    protected ObjectMapper om;


    public VersionedEntityRowMapper(Class<V> klass , ObjectMapper om) {
        this.om = om;
        this.klass = klass;
    }

    protected V customMap(V obj) {
        return obj;
    }

    public String encode(V obj) {
        try {
//            LOG.info("inside getBlob");
            String result = om.writeValueAsString(obj);
//            LOG.info("serialization result is {}", result);
            return result;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public V mapRow(ResultSet row, int i) throws SQLException {
        Integer id = row.getInt(1);
        String data = row.getString(2);
        try {
            V obj = customMap(om.readValue(data, klass));
            obj = obj.id(id);

            return obj;
        } catch (NullPointerException e) {
            LOG.error("{} {} : {}", id, data, klass);
            return null;
//            throw e;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public V decode(String json) {
        try {
            return om.readValue(json, klass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    public ResultSetExtractor<V> resultSetExtractor() {
//        return (rs) -> mapRow(rs, 0);
//    }

//    public static void main(String[] args) throws Exception {
//
//        System.out.println(new ObjectMapper()
//                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
//                .configure(MapperFeature.USE_STD_BEAN_NAMING, true)
//                .writeValueAsString(new EventBuilder().createEvent()));
//    }
}
