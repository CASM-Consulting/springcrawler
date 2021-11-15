package com.casm.acled.dao.rowmappers;

import com.casm.acled.entities.VersionedEntityLink;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class VersionedEntityLinkRowMapper<V extends VersionedEntityLink<V>> extends VersionedEntityRowMapper<V> {

    private static final Logger LOG = LoggerFactory.getLogger(VersionedEntityLinkRowMapper.class);

    public VersionedEntityLinkRowMapper(Class<V> klass , ObjectMapper om) {
        super(klass, om);
    }

    @Override
    public V mapRow(ResultSet row, int i) throws SQLException {
        Integer id = row.getInt(1);
        String data = row.getString(2);
        Integer id1 = row.getInt(3);
        Integer id2 = row.getInt(4);
        try {
            data = data == null ? "{}" : data;
            V obj = customMap(om.readValue(data, klass));
            obj = obj.id(id);
            obj = obj.id1(id1);
            obj = obj.id2(id2);
            return obj;
        } catch (NullPointerException e) {
            LOG.error("(id){} (id1){} (id2){} {} : {}", id, id1, id2, data, klass);
            return null;
//            throw e;
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
