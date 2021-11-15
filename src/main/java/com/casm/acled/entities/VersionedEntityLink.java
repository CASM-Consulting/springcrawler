package com.casm.acled.entities;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public abstract class VersionedEntityLink<V extends VersionedEntityLink<V>> extends VersionedEntity<V> {

    public static final String ID1 = "_id1";
    public static final String ID2 = "_id2";

    private final Integer id1;
    private final Integer id2;

    public VersionedEntityLink(EntitySpecification entitySpec, String version, Map<String, Object> data, Integer id, Integer id1, Integer id2) {
        super(entitySpec, version, data, id);
        this.id1 = id1;
        this.id2 = id2;
    }

    protected V newInstance(Map<String, Object> data) {
        return newInstance(data, id.orElse(null), id1, id2);
    }

    protected V newInstance(Map<String, Object> data, Integer id, Integer id1, Integer id2) {
        try {
            Constructor<V> constructor = (Constructor<V>)this.getClass().getConstructor(Map.class, Integer.class, Integer.class, Integer.class);
            return constructor.newInstance(data, id, id1, id2);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new VersionedEntityException(e);
        }
    }

    @Override
    public V id(Integer id) {
        return newInstance(data, id, id1, id2);
    }

    public Integer id1() {
        return id1;
    }

    public V id1(Integer id1) {
        return newInstance(data, id.orElse(null), id1, id2);
    }

    public Integer id2() {
        return id2;
    }

    public V id2(Integer id2) {
        return newInstance(data, id.orElse(null), id1, id2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof VersionedEntityLink)) return false;

        VersionedEntityLink<?> that = (VersionedEntityLink<?>) o;

        return new EqualsBuilder()
                .append(data, that.data)
                .append(entitySpec, that.entitySpec)
                .append(version, that.version)
                .append(id1, that.id1)
                .append(id2, that.id2)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(data)
                .append(entitySpec)
                .append(version)
                .append(id1)
                .append(id2)
                .toHashCode();
    }
}
