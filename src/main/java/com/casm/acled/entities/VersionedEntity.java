package com.casm.acled.entities;

import com.casm.acled.camunda.variables.Process;
import com.casm.acled.entities.event.Event;
import com.casm.acled.entities.event.EventVersions;
import com.casm.acled.entities.validation.EntityValidators;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.*;

/**
 *
 * For better or worse, we've ended up with recursive generics here. This is potentially justifiable with the following
 * rationale:
 *  - All versioned entities are essentially the same structure (a map), just with different allowed fields
 *  (field spec). However, we want distinct types so Jackson and Jersey know what to do with them at runtime.
 *  - We want VersionedEntities to be immutable and have a fluent API, so they need to return instances of themselves.
 *  Therefore V must be specified as the entity, e.g. Article, and Articles must be of type VersionedEntity.
 *  - This article explains more : https://www.sitepoint.com/self-types-with-javas-generics/
 *
 * @param <V>
 */
public abstract class VersionedEntity<V extends VersionedEntity<V>> implements Serializable {

    public static final String VERSION = "_version";
    public static final String ID = "_id";

    protected final EntitySpecification entitySpec;
    protected final String version;
    protected final ImmutableMap<String, Object> data;
    protected final Optional<Integer> id;

    protected VersionedEntity() {
        this( new EntitySpecification(), "0", new HashMap<>(), null);
    }

    public VersionedEntity(EntitySpecification entitySpec, String version) {
        this(entitySpec, version, new HashMap<>(), null);
    }

    public VersionedEntity(EntitySpecification entitySpec, String version, Map<String, Object> data, Integer id) {
        this.data = ImmutableMap.class.isAssignableFrom(data.getClass()) ? (ImmutableMap<String, Object>)data : ImmutableMap.copyOf(data);
        this.entitySpec = entitySpec;
        this.version = version;
        this.id = Optional.ofNullable(id);
    }

    public boolean notNull(String key) {
        return get(key) != null;
    }

    public boolean isTrue(String key) {
        Boolean bool = get(key);
        if( bool == null || !bool) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isFalse(String key){
        return !isTrue(key);
    }

    public <T> T get(String key) {
        if(entitySpec.has(key)) {
            return (T) data.get(key);
        } else {
            throw new NoSuchEntityFieldException(key);
        }
    }

    public List get(Iterable<String> keys) {
        List out = new ArrayList();
        for(String key : keys) {
            if(entitySpec.has(key)) {
                out.add(data.get(key));
            } else {
                throw new NoSuchEntityFieldException(key);
            }
        }
        return out;
    }

    public V remove(String key) {
        Map<String, Object> newData;
        if(data.containsKey(key)) {
            newData = new HashMap<>(data);
            newData.remove(key);
        } else {
            newData = data;
        }
        return newInstance(new ImmutableMap.Builder<String, Object>().putAll(newData).build());
    }

    public <T> V put(String key, T value) {
        EntityField<T> field = entitySpec.get(key);

//        field.getValidator().validate(this, field, value);
        Map<String, Object> newData;
        if(data.containsKey(key)) {
            newData = new HashMap<>(data);
            newData.remove(key);
        } else {
            newData = data;
        }

        return newInstance(new ImmutableMap.Builder<String, Object>().putAll(newData).put(key, value).build());
    }

    public V putAll(Map<String, Object> additionalData) {

        Map<String, Object> newData = new HashMap<>(data);

        for(Map.Entry<String, Object> entry : newData.entrySet()) {

            String key = entry.getKey();
            Object value = entry.getValue();
            EntityField field = entitySpec.get(key);

//            field.validate(value);
            if(data.containsKey(key)) {
                newData = new HashMap<>(data);
                newData.remove(key);
            } else {
                newData = data;
            }
        }

        return newInstance(new ImmutableMap.Builder<String, Object>().putAll(newData).build());
    }

    public EntitySpecification spec() {
        return entitySpec;
    }

    public EntityValidators<V> getValidators() {
        return EntityValidators.PASS;
    }

    public String version() {
        return version;
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public int id() {
        if(id.isPresent()) {
            return id.get();
        } else {
            throw new VersionedEntityException("Entity instance not coupled with database entry.");
        }
    }


    public <T> boolean equals(String field, T value) {
        if(hasValue(field) && get(field).equals(value)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean hasBusinessKey() {
        return data.containsKey(Process.BUSINESS_KEY);
    }

    public boolean hasValue(String key) {
        return data.containsKey(key) && get(key) != null;
    }

    public V businessKey(String businessKey) {
        return put(Process.BUSINESS_KEY, businessKey);
    }

    public String businessKey() {
        if(hasBusinessKey()) {
//            return get(Process.BUSINESS_KEY);
            return get("business_key");
        } else {
            throw new VersionedEntityException("Business Key not set.");
        }
    }

    public boolean hasId() {
        return id.isPresent();
    }

    public V id(Integer id) {
        return newInstance(data, id);
    }

    public V withoutId() {
//        HashMap<String, Object> temp = new HashMap<>(data);
        return newInstance(data, null);
    }

    protected V newInstance(Map<String, Object> data) {

        return newInstance(data, id.orElse(null));
    }

    protected V newInstance(Map<String, Object> data, Integer id) {
        try {
            Constructor<V> constructor = (Constructor<V>)this.getClass().getConstructor(Map.class, Integer.class);
            return constructor.newInstance(data, id);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new VersionedEntityException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof VersionedEntity)) return false;

        VersionedEntity<?> that = (VersionedEntity<?>) o;

        return new EqualsBuilder()
                .append(data, that.data)
                .append(entitySpec, that.entitySpec)
                .append(version, that.version)
//                .append(id, that.id)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(data)
                .append(entitySpec)
                .append(version)
//                .append(id)
                .toHashCode();
    }

    @Override
    public String toString() {
        return data.toString();
    }

    public static void main(String[] args) {

        Event event = new EventVersions().get("v1");

        event = event.put("ISO", "UK").put("EVENT_DATE", LocalDate.of(2012, 9,3));

        String iso = event.get("ISO");
        LocalDate date = event.get("EVENT_DATE");

        event = event.id(123);

        System.out.println(iso);
        System.out.println(date);
    }
}
