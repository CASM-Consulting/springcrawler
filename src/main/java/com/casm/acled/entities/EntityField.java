package com.casm.acled.entities;

import com.casm.acled.camunda.variables.Entities;
import com.casm.acled.camunda.variables.Process;
import com.casm.acled.dao.jackson.EncodingExceptionHandler;
import com.casm.acled.entities.validation.FieldValidators;
import com.casm.acled.entities.validation.FrontendValidators;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.casm.acled.entities.validation.FieldValidators.PASS;

public class EntityField<T> implements Serializable {

    private final Class<T> klass;
    private final String displayType;
    private final String name;
    private final String label;
    private final FieldValidators<T> validators;
    private final Map<String, Object> meta;
    private final boolean required;
    private final EncodingExceptionHandler<T> encodingExceptionHandler;


    public EntityField(String name, Class<T> klass) {
        this(name, name, klass, false, null, PASS, ImmutableMap.of(), null);
    }

    public EntityField(String name, String label, Class<T> klass, String displayType) {
        this(name, label, klass, false, displayType, PASS, ImmutableMap.of(), null);
    }

    public EntityField(String name, String label, Class<T> klass, FieldValidators<T> validators) {
        this(name, label, klass, false, null, validators, ImmutableMap.of(), null);
    }

    public EntityField(String name, String label, Class<T> klass, boolean required, String displayType,
                       FieldValidators<T> validators, Map<String, Object> meta, EncodingExceptionHandler<T> encodingExceptionHandler) {
        this.label = label;
        this.klass = klass;
        this.required = required;
        this.name = name;
        this.meta = ImmutableMap.copyOf(meta);
        this.displayType = displayType;
        if(required) {
            validators = validators.withFrontend(FrontendValidators.Field.REQUIRED);
        }
        this.validators = validators;
        this.encodingExceptionHandler = encodingExceptionHandler;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        EntityField<?> that = (EntityField<?>) o;

        return new EqualsBuilder()
                .append(klass, that.klass)
                .append(name, that.name)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(klass)
                .append(name)
                .toHashCode();
    }

    public Class<T> getKlass() {
        return klass;
    }

    public String getName() {
        return name;
    }
    public FieldValidators<T> getValidators() {
        return validators;
    }
    public String getLabel() {
        return label;
    }

//    public void validate(T value) {
//        validator.validate(value);
//    }

    public String getDisplayType() {
        return displayType;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public boolean isRequired() {
        return required;
    }


    public EncodingExceptionHandler<T> encodingExceptionHandler() {
        return encodingExceptionHandler;
    }


    /************************
     * Convenience builders
     ************************/


    public static <T> Builder<T> builder(String name, String label, Class<T> klass){
        return new Builder<>(name, klass).label(label);
    }

    public static class Builder<T> {

        private final String name;
        private final Class<T> klass;
        private boolean required;
        private String label;
        private String displayType;
        private Map<String, Object> meta;
        private FieldValidators<T> validators;
        private EncodingExceptionHandler<T> handler;

        public Builder(String name, Class<T> klass){
            this.name = name;
            this.label = name;
            this.klass = klass;
            this.displayType = null;
            this.required = false;
            this.meta = new HashMap<>();
            this.validators = PASS;
        }

        public Builder<T> label(String label){
            this.label = label;
            return this;
        }

        public Builder<T>  displayType(String displayType){
            this.displayType = displayType;
            return this;
        }

        public Builder<T>  putMeta(String key, Object value){
            meta.put(key, value);
            return this;
        }

        public Builder<T> constraintGroup(String... groups) {
            Set<String> grp = ImmutableSet.copyOf(groups);
            return putMeta(Entities.CONSTRAINT_GROUPS, grp);
        }

        public Builder<T> hide(String context) {
            return setContextVisibility(context, Process.HIDE);
        }

        public Builder<T> edit(String context) {
            return setContextVisibility(context, Process.EDIT);
        }

        public Builder<T> readOnly(String context) {
            return setContextVisibility(context, Process.READ_ONLY);
        }

        private Builder<T> setContextVisibility(String context, String visibility) {
            if(!meta.containsKey(Process.CONTEXT_CONDITION)) {
                meta.put(Process.CONTEXT_CONDITION, new HashMap<>());
            }
            HashMap<String,String> contextMap = new HashMap<>((HashMap<String,String>)meta.get(Process.CONTEXT_CONDITION));
            contextMap.put(context, visibility);
            meta.put(Process.CONTEXT_CONDITION, contextMap);
            return this;
        }

        public Builder<T> validators(FieldValidators<T> validators){
            this.validators = validators;
            return this;
        }

        public Builder<T> encodingException(EncodingExceptionHandler<T> handler) {
            this.handler = handler;
            return this;
        }

        public EntityField<T> build() {
            return new EntityField<>(name, label, klass, required, displayType, validators, meta, handler);
        }
    }
}
