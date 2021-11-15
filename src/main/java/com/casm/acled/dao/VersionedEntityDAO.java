package com.casm.acled.dao;

import com.casm.acled.dao.sql.Join;
import com.casm.acled.dao.sql.Where;
import com.casm.acled.entities.VersionedEntity;
//import com.casm.acled.queryspecification.QuerySpecification;

import java.util.List;
import java.util.Optional;

public interface VersionedEntityDAO<V extends VersionedEntity<V>> {
    List<V> getAll();
    <T> List<V> getBy(String field, T value);
    List<V> getByBusinessKey(String key);

    <T> List<T> getDistinct(String field, Join join, Where where);
    <T> List<T> getDistinct(String field, Join join, V where);
    List<V> query(Where where);

    <T> Optional<V> getByUnique(String field, T value);
    Optional<V> getById(int id);

    V create(V entity);
    List<V> create(List<V> entities);

    default V upsert(V entity) {
        return create(entity);
    }
    default List<V> upsert(List<V> entities) {
        return create(entities);
    }

    void overwrite(V entity);
    void overwrite(List<V> entities);
    void clear();

    void delete(List<V> entity);
    void delete(V entity);
    void delete(int id);

    List<V> search(V from, V to, V value);
//    List<V> searchEmbedded(QuerySpecification q);

    String table();

    V decode (String data);

}
