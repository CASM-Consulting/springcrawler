package com.casm.acled.dao;

import com.casm.acled.camunda.variables.Process;
import com.casm.acled.dao.rowmappers.VersionedEntityRowMapper;
import com.casm.acled.dao.rowmappers.VersionedEntityRowMapperFactory;
import com.casm.acled.dao.sql.Join;
import com.casm.acled.dao.sql.Where;
import com.casm.acled.dao.util.SqlBinder;
import com.casm.acled.entities.EntityField;
import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.VersionedEntity;
//import com.casm.acled.queryspecification.QuerySpecification;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class VersionedEntityDAOImpl<V extends VersionedEntity<V>>  implements VersionedEntityDAO<V> {
    protected final JdbcTemplate jdbcTemplate;
    protected final VersionedEntityRowMapper<V> rowMapper;
    protected final Class<V> klass;
    protected final String table;
    protected final ImmutableList<String> fields;

    @Autowired
    protected VersionedEntityRowMapperFactory rowMapperFactory;

//    @Override
//    public List<V> searchEmbedded(QuerySpecification q) {
//        throw new AssertionError("not implemented");
//    }

    public VersionedEntityDAOImpl(JdbcTemplate jdbcTemplate,
                                  String table,
                                  Class<V> klass,
                                  VersionedEntityRowMapper<V> rowMapper
    ) {
        this(jdbcTemplate, table, klass, rowMapper, ImmutableList.of("data"));
    }
    public VersionedEntityDAOImpl(JdbcTemplate jdbcTemplate,
                                  String table,
                                  Class<V> klass,
                                  VersionedEntityRowMapper<V> rowMapper,
                                  ImmutableList<String> fields
    ) {
        JDBCObjectValidation.validTable(table);
        this.jdbcTemplate = jdbcTemplate;
        this.table = table;
        this.klass = klass;
        this.rowMapper = rowMapper;
        this.fields = fields;
    }

//    public JdbcTemplate template() {
//        return jdbcTemplate;
//    }

    @Override
    public String table() {
        return table;
    }

    @Override
    public V decode(String json) {
        return rowMapper.decode(json);
    }

    @Override
    public List<V> getByBusinessKey(String key) {
        return getBy(Process.BUSINESS_KEY, key);
    }

    @Override
    public List<V> getAll() {
        String sql = SqlBinder.sql("SELECT * FROM ${table}")
                .bind("table", table)
                .bind();

        List<V> results = jdbcTemplate.query(sql, rowMapper);

        ListIterator<V> itr = results.listIterator();

        while(itr.hasNext()) {
            V entity = itr.next();
            if(entity == null) {
                itr.remove();
            }
        }
        return results;
    }

    @Override
    public <T> List<V> getBy(String field, T value) {
        JDBCObjectValidation.validField(field);
        String sql = "SELECT * FROM ${table} WHERE data->>'${field}' = ?";
        sql = SqlBinder.sql(sql)
                .bind("table", table)
                .bind("field", field)
                .bind();

        List<V> results = new ArrayList<>(query(sql, value));

        ListIterator<V> itr = results.listIterator();

        while(itr.hasNext()) {
            V entity = itr.next();
            if(entity == null) {
                itr.remove();
            }
        }

        return results;
    }

    @Override
    public Optional<V> getById(int id) {
        String sql = select(" WHERE id = ? ")
                .bind();

        Optional<V> result = Optional.empty();

        List<V> results = jdbcTemplate.query(sql, rowMapper, id);

        if(results.size()==1) {
            result = Optional.of(results.get(0));
        }

        return result;
    }

    @Override
    public <T> Optional<V> getByUnique(String field, T value) {

        List<V> results = getBy(field, value);

        Optional<V> result = Optional.empty();

        if(results.size()==1) {
            result = Optional.of(results.get(0));
        }

        return result;
    }


    /**
     * test in com.casm.acled.dao.entities.LocationDAOImplTest
     * @param field
     * @param <T>
     * @return
     */
    @Override
    public <T> List<T> getDistinct(String field, Join join, Where where) {
        SqlBinder sql = SqlBinder.sql("SELECT DISTINCT (${field}) FROM ${table}")
            .bind("field", field)
            .bind("table", table);

        List<T> out;

        if(join != null) {
            sql.append(join);
        }
        if(where != null) {
            sql.append(where);
            Object[] values = where.collectValues().toArray();
            out = jdbcTemplate.query(sql.bind(), (rs, i) -> (T)rs.getString(1), values);
        } else {
            out = jdbcTemplate.query(sql.bind(), (rs, i) -> (T)rs.getString(1) );
        }

        out.removeIf(Objects::isNull);

        return out;
    }

    @Override
    public <T> List<T> getDistinct(String field, Join join, V where) {
        SqlBinder sql = SqlBinder.sql("SELECT DISTINCT (data->>'${field}') FROM ${table}")
                .bind("field", field)
                .bind("table", table);

        List<T> out;

        if(join != null) {
            sql.append(join);
        }
        if(where != null) {
            List<Object> args = new ArrayList<>();
            addWhere(sql, args, null, null, where);
//            Object[] values = where.collectValues().toArray();
            out = jdbcTemplate.query(sql.bind(), (rs, i) -> (T)rs.getString(1), args.toArray());
        } else {
            out = jdbcTemplate.query(sql.bind(), (rs, i) -> (T)rs.getString(1) );
        }

        out.removeIf(Objects::isNull);

        return out;
    }


    @Override
    public List<V> query(Where where) {
        SqlBinder sql = select()
                .append(where.sql());

        Object[] values = where.collectValues().toArray();

        List<V> out = jdbcTemplate.query(sql.bind(), rowMapper, values);

        out.removeIf(Objects::isNull);

        return out;
    }


    protected V preCreate(V entity) throws CancelCreateException {
        return entity;
    }

    protected V postCreate(V entity) {
        return entity;
    }


    protected void setAdditionalFields(V entity, PreparedStatement stmt) throws SQLException {

    }

    @Override
    public List<V> create(final List<V> input) {
        final List<V> entities = new ArrayList<>(input);

        if(entities.isEmpty()) {
            return entities;
        }


        //pre-hook
        ListIterator<V> itr = entities.listIterator();
        while(itr.hasNext()) {
            V entity = itr.next();
            try {

                entity = preCreate(entity);
                itr.set(entity);
            } catch (CancelCreateException e){

                itr.remove();
            }
        }

        String template = "(" + StringUtils.repeat("?", "," , fields.size()) + ")";

        String placeHolders = StringUtils.repeat(template, "," ,entities.size());

        String columns = StringUtils.join(fields, ",");

        SqlBinder binder = SqlBinder.sql("INSERT INTO ${table} (${columns}) VALUES", placeHolders)
                .bind("columns", columns);

        // Upsert if necessary, but currently only on unique fields within the json-blob.
        if(!entities.get(0).spec().unique().isEmpty()) {
            binder = binder.append(" ON CONFLICT(");
            String uniques = StringUtils.join(entities.get(0).spec().unique().stream().map(s->"(data->>'"+s+"')").collect(Collectors.toSet()), ",");
            binder = binder.append(uniques);
            binder = binder.append(") DO UPDATE SET data = ${table}.data || EXCLUDED.data");
        }


        String sql = binder.append(" RETURNING id")
                .bind("table", table)
                .bind();



        KeyHolder keyHolder = new GeneratedKeyHolder();

        int n = jdbcTemplate.update((con) -> {

            PreparedStatement stmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            for(int i = 0; i < entities.size(); ++i) {

                V entity = entities.get(i);

                String blob = rowMapper.encode(entity);
                PGobject jsonObject = new PGobject();

                jsonObject.setType("jsonb");
                jsonObject.setValue(blob);

                stmt.setObject(i+1, jsonObject);
                setAdditionalFields(entity, stmt);
            }
            return stmt;
        }, keyHolder);

        List<Integer> keyList = keyHolder.getKeyList().stream().map(m->(Integer)m.get("id")).collect(Collectors.toList());

        List<V> coupled = new ArrayList<>(entities);
        ListIterator<V> jtr = coupled.listIterator();

        int i = 0;

        while(jtr.hasNext()) {
            int id = keyList.get(i);

            V entity = jtr.next();
            entity = entity.id(id);
            entity = postCreate(entity);

            jtr.set(entity);
            ++i;
        }

        return ImmutableList.copyOf(coupled);
    }

    @Override
    public V create(V entity) {
        return create(ImmutableList.of(entity)).get(0);
    }

    @Override
    public void overwrite(List<V> entities) {
        //TODO: Make this a batch sql statement
        for(V entity : entities) {
            overwrite(entity);
        }
    }

    @Override
    public void overwrite(V entity) {
        try {
            String sql = SqlBinder.sql("UPDATE ${table} SET data = ? WHERE id = ?")
                    .bind("table", table)
                    .bind();

            String blob = rowMapper.encode(entity);
            PGobject jsonObject = new PGobject();
            jsonObject.setType("jsonb");
            jsonObject.setValue(blob);
            jdbcTemplate.update(sql, jsonObject, entity.id());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clear() {
        String sql = SqlBinder.sql("DELETE FROM ${table}")
                .bind("table", table)
                .bind();

        jdbcTemplate.execute(sql);
    }

//    protected <T> String getTable(Class<T> other) {
//
//    }

    protected List<V> query(String sql, Object... args) {
        return jdbcTemplate.query(sql, rowMapper, args);
    }


    protected SqlBinder select(String... sqls) {
        SqlBinder sql = SqlBinder.sql("SELECT * FROM ${table} ")
            .append(sqls)
            .bind("table", table);
        return sql;
    }

    @Override
    public List<V> search(V from, V to, V value){
        SqlBinder sql = SqlBinder.sql("SELECT * FROM ${table} WHERE");

        List<Object> args = new ArrayList<>();

        boolean hasCriteria = addWhere(sql, args, from, to, value);

        if(!hasCriteria) {
            sql = SqlBinder.sql("SELECT * FROM ${table} ");
        }

        sql.bind("table", table);

        return jdbcTemplate.query(sql.bind(), rowMapperFactory.ofDefault(klass), args.toArray());
    }


    protected <T> String ref(EntityField<T> field) {
        String ref;
        if(field.getKlass().equals(LocalDate.class)) {
            ref = "(data->>'"+field.getName()+"')::date";
        } else {
            ref =  "data->>'"+field.getName()+"'";
        }

        return ref;
    }


    protected boolean addWhere(SqlBinder sql, List<Object> args, V from, V to, V value) {
        boolean hasCriteria = false;

        EntitySpecification spec = from.spec();
        for(EntityField field : spec.fields()) {
            String fieldName = field.getName();
            if (from.hasValue(fieldName)) {
                if(hasCriteria) {
                    sql.append("AND");
                }
                Object arg = from.get(field.getName());
                args.add(arg);
                String op = "=";
                if(to.hasValue(fieldName)) {
                    op = ">=";
                }
                sql.append(ref(field), op, "?");
                hasCriteria = true;
            }
            if (to.hasValue(fieldName)) {
                if(hasCriteria) {
                    sql.append("AND");
                }
                Object arg = to.get(field.getName());
                args.add(arg);
                String op = "=";
                if(from.hasValue(fieldName)) {
                    op = "<=";
                }
                sql.append(ref(field), op, "?");
                hasCriteria = true;
            }
            if(value.hasValue(fieldName)) {
                if(hasCriteria) {
                    sql.append("AND");
                }
                Object arg = value.get(field.getName());
                String op = "=";
                if(field.getKlass().equals(String.class)) {
                    op = "ILIKE";
                    arg = arg.toString().isEmpty()? "": "%" + arg + "%";
                }
                args.add(arg);
                sql.append(ref(field), op, "?");
                hasCriteria = true;
            }
            sql.bind(fieldName, fieldName);
        }

        return hasCriteria;
    }

    @Override
    public void delete(List<V> entities) {
        for(V entity : entities) {
            delete(entity.id());
        }
    }

    @Override
    public void delete(V entity) {
        delete(entity.id());
    }

    @Override
    public void delete(int id ) {
        String sql = SqlBinder
                .sql("DELETE FROM ${table}")
                .append("WHERE id = ?")
                .bind("table", table)
                .bind();

        jdbcTemplate.update(sql, id);
//        query(sql, id);
    }

}

