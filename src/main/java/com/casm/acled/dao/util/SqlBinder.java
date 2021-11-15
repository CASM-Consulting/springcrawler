package com.casm.acled.dao.util;

import com.casm.acled.dao.sql.SqlFragment;
import com.casm.acled.entities.EntityField;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Map;

public class SqlBinder {

    private static final Logger LOG = LoggerFactory.getLogger(SqlBinder.class);

    private final StringBuilder sql;

    private final ImmutableMap.Builder<String,String> valueBuilder;

    public SqlBinder(String sql) {
        this(new StringBuilder(sql), new ImmutableMap.Builder<>());
    }

    private SqlBinder(StringBuilder sql, ImmutableMap.Builder<String,String> valueBuilder) {
        this.sql = sql;
        this.valueBuilder = valueBuilder;
    }

    public static SqlBinder sql(String sql, String... more) {
        StringBuilder sb = new StringBuilder(sql);
        for(String m : more) {
            sb.append(" ");
            sb.append(m);
        }
        return new SqlBinder(sb, new ImmutableMap.Builder<>());
    }


    public SqlBinder append(SqlFragment... sqls) {
        for(SqlFragment _sql : sqls) {
            if(_sql == null) {
                LOG.warn("sql fragment was null");
            } else {
                sql.append(" ");
                sql.append(_sql.sql());
            }
        }
        return this;
    }

    public SqlBinder ref(String jsonRef) {
        return append("data->>'"+jsonRef+"'");
    }

    public <T> SqlBinder ref(EntityField<T> field) {
        String ref;
        if(field.getKlass().equals(LocalDate.class)) {
            ref = "(data->>'"+field.getName()+"')::date";
        } else {
            ref =  "data->>'"+field.getName()+"'";
        }
        return append(ref);
    }

    public SqlBinder append(String... sqls) {
        for(String _sql : sqls) {
            sql.append(" ");
            sql.append(_sql);
        }
        return this;
    }

    public SqlBinder append(SqlBinder... binders) {
        for(SqlBinder binder : binders) {
            append(binder.sql.toString());
            valueBuilder.putAll(binder.valueBuilder.build());
        }
        return this;
    }

    public SqlBinder bind(String key, String value) {
        valueBuilder.put(key, value);
        return this;
    }

    public SqlBinder bind(Map<String, String> values) {
        valueBuilder.putAll(values);
        return this;
    }

    public String bind() {
        StringSubstitutor sub = new StringSubstitutor(valueBuilder.build());
        String result = sub.replace(sql);

        return result;
    }
}
