package com.casm.acled.entities;

import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class VersionedEntitySupplier<V extends VersionedEntity<V>> {

    protected final ImmutableMap<String, Constructor<? extends V>> versions;
    protected final Class<V> klass;

    protected VersionedEntitySupplier(Map<String, Constructor<? extends V>> versions, Class<V> klass) {
        this.versions = ImmutableMap.<String, Constructor<? extends V>>builder()
                .putAll(versions)
                .build();
        this.klass = klass;
    }

    public V current() {
        return newInstance(versions.get(currentVersion()));
    }

    public Class<V> getBaseClass() {
        return klass;
    }

    abstract public String currentVersion();

    public V get(String version) {
        return newInstance(versions.get(version));
    }

    private V newInstance(Constructor<? extends V> constructor) {
        return newInstance(constructor, ImmutableMap.of(), null, null);
    }
    private V newInstance(Constructor<? extends V> constructor, Map<String, Object> data, Long id, String businessKey) {
        try {
            return constructor.newInstance(data, id);
        } catch (IllegalArgumentException | IllegalAccessException | InstantiationException | InvocationTargetException _e) {
            try {
                return constructor.newInstance(data, id, businessKey);
            } catch (IllegalArgumentException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
//                e.printStackTrace();
                throw new VersionedEntityException(e);
            }
//            e.printStackTrace();
//            throw new VersionedEntityException(e);
        }
    }

    public Set<Class<? extends V>> getAllClassVersions() {
        return versions.values().stream().map(Constructor::getDeclaringClass).collect(Collectors.toSet());
    }

    protected static <V extends VersionedEntity> Constructor<V> constructorConstructor(Class<V> klass) {
        try {
            Constructor<V> constructor = klass.getConstructor(Map.class, Integer.class);
            return constructor;
        } catch (NoSuchMethodException _e) {
            try {
                Constructor<V> constructor = klass.getConstructor(Map.class, Integer.class, String.class);
                return constructor;
            } catch (NoSuchMethodException e) {

                e.printStackTrace();
                throw new VersionedEntityException(e);
            }
        }
    }
}
