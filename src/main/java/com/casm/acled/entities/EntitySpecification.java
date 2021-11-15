package com.casm.acled.entities;

import com.casm.acled.entities.validation.FieldValidators;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.casm.acled.entities.EntityField.builder;
import static com.casm.acled.entities.validation.FieldValidators.PASS;


public class EntitySpecification implements Serializable {

    private final Map<String, EntityField> fields;

    private final Set<String> unique;

    public EntitySpecification() {
        this(ImmutableMap.of(), ImmutableSet.of());
    }

    public EntitySpecification(Map<String, EntityField> fields, Set<String> unique) {
        this.fields = ImmutableMap.copyOf(fields);
        this.unique = unique;
    }

    public <T> EntitySpecification add(String name,
                                       String label,
                                       Class<T> klass,
                                       boolean required,
                                       String displayType,
                                       FieldValidators<T> validator,
                                       Map<String, Object> meta) {

        return add(new EntityField<>(name, label, klass, required, displayType, validator, meta, null));
    }

    public <T> EntitySpecification add(String name,
                                       String label,
                                       Class<T> klass,
                                       String displayType,
                                       FieldValidators<T> validator,
                                       Map<String, Object> meta) {

        return add(name, label, klass, false, displayType, validator, meta);
    }

    public <T> EntitySpecification add(String name, Class<T> klass, FieldValidators<T> validator) {
        return add(name, name, klass, null, validator, ImmutableMap.of());
    }

    public <T> EntitySpecification add(String name, String label, Class<T> klass, String displayType) {
        return add(name, label, klass, displayType, PASS, ImmutableMap.of());
    }

    public <T> EntitySpecification add(String name, String label, Class<T> klass, boolean required) {
        return add(name, label, klass, required, null, PASS, ImmutableMap.of());
    }

    public <T> EntitySpecification add(String name, String label, Class<T> klass, String displayType, boolean required) {
        return add(name, label, klass, required, displayType, PASS, ImmutableMap.of());
    }

    public <T> EntitySpecification add(String name, Class<T> klass, String displayType) {
        return add(name, name, klass, displayType, PASS, ImmutableMap.of());
    }

    public <T> EntitySpecification add(String name, Class<T> klass, String displayType, boolean required) {
        return add(name, name, klass, required, displayType, PASS, ImmutableMap.of());
    }

    public <T> EntitySpecification add(String name, String label, Class<T> klass) {
        return add(name, label, klass, null, PASS, ImmutableMap.of());
    }

    public <T> EntitySpecification add(String name, Class<T> klass) {
        return add(name, klass, PASS);
    }

    public <T> EntitySpecification add(String name, Class<T> klass, boolean required) {
        return add(name, klass, null, required);
    }

    public EntitySpecification add(EntityField field) {
        return new EntitySpecification(ImmutableMap.<String, EntityField>builder()
                .putAll(fields)
                .put(field.getName(), field)
                .build(),
                unique
        );
    }

    public EntitySpecification unique(String unique) {
        return new EntitySpecification(fields,ImmutableSet.of(unique));
    }

    public EntitySpecification unique(Set<String> unique) {
        return new EntitySpecification(fields,unique);
    }

    public Collection<EntityField> fields() {
        return fields.values();
    }

    public Set<String> unique() {
        return unique;
    }

    public Set<String> names() {
        return fields.keySet();
    }

    public <T> EntityField<T> get(String name) {
        if(fields.containsKey(name)) {
            return fields.get(name);
        } else {
            throw new NoSuchEntityFieldException(name);
        }
    }

    public static EntitySpecification empty() {
        return new EntitySpecification();
    }

//    public EntitySpecification business() {
//        return add(builder(Process.BUSINESS_KEY, Process.BUSINESS_KEY, String.class)
//                .hide(Process.ALL)
//                .build() );
//    }
//
//    public EntitySpecification maybeHistorical() {
//
//        return add(builder(Entities.HISTORICAL, Entities.HISTORICAL, Boolean.class)
//                .hide(Process.ALL)
//                .build() );
//    }
//
//    public EntitySpecification deletable() {
//
//        return add(builder(Entities.DELETED, Entities.DELETED, Boolean.class)
//                .hide(Process.ALL)
//                .build() );
//    }


    public boolean has(String name) {
        return fields.containsKey(name);
    }
}
