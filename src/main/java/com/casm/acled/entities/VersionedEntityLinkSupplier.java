package com.casm.acled.entities;

import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public abstract class VersionedEntityLinkSupplier<V extends VersionedEntityLink<V>> extends VersionedEntitySupplier<V> {

    protected VersionedEntityLinkSupplier(Map<String, Constructor<? extends V>> versions, Class<V> klass) {
        super(versions, klass);
    }

    @Override
    public V current() {
        return newInstance(versions.get(currentVersion()), ImmutableMap.of(), null, null);
    }

    public V get(String version) {
        return get(version, null, null);
    }

    public V get(String version, Integer id1, Integer id2) {
        return newInstance(versions.get(version), ImmutableMap.of(), id1, id2);
    }

    private V newInstance(Constructor<? extends V> constructor, Map<String, Object> data, Integer id1, Integer id2) {
        try {
            return constructor.newInstance(data, null, id1, id2);
        } catch (IllegalArgumentException | IllegalAccessException | InstantiationException | InvocationTargetException e) {

            throw new VersionedEntityException(e);
        }
    }

    protected static <V extends VersionedEntity> Constructor<V> constructorConstructor(Class<V> klass) {
        try {
            Constructor<V> constructor = klass.getConstructor(Map.class, Integer.class, Integer.class, Integer.class);
            return constructor;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new VersionedEntityException(e);
        }
    }
}
