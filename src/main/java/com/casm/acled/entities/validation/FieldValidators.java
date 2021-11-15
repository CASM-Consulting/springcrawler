package com.casm.acled.entities.validation;

import com.casm.acled.entities.EntityField;
import com.casm.acled.entities.VersionedEntity;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FieldValidators<T> implements FieldValidator<T> {

    private final List<FieldValidator<T>> backend;
    private final List<Map<String,String>> frontend;

    public FieldValidators() {
        this(ImmutableList.of(), ImmutableList.of());
    }

    public FieldValidators(List<FieldValidator<T>> backend, List<Map<String,String>> frontend) {
        this.backend = backend;
        this.frontend = frontend;
    }

    public static <T> FieldValidators<T> ofFrontend(String... names) {
        return new FieldValidators<T>().withFrontend(names);
    }

    public static <T> FieldValidators<T> ofBackend(FieldValidator<T>... validators) {
        return new FieldValidators<T>().withBackend(validators);
    }

    public FieldValidators<T> withFrontend(String... names) {

        ImmutableList.Builder<Map<String,String>> builder = new ImmutableList.Builder<>();
        return new FieldValidators<>(
                backend,
                builder.addAll(frontend)
                        .addAll(Arrays.stream(names).map(n -> ImmutableMap.of("name", n)).collect(Collectors.toList()))
                .build()
        );
    }

    public FieldValidators<T> withBackend(FieldValidator<T>... validators) {

        ImmutableList.Builder<FieldValidator<T>> builder = new ImmutableList.Builder<>();
        return new FieldValidators<>(
                builder.addAll(backend)
                        .addAll(Arrays.stream(validators).collect(Collectors.toList()))
                        .build(),
                frontend
        );
    }

    public List<Map<String, String>> getFrontend() {
        return frontend;
    }

    @Override
    public <V extends VersionedEntity<V>> List<ValidationMessage> validate(VersionedEntity<V> entity, EntityField<T> field, T val) {

        List<ValidationMessage> messages = new ArrayList<>();

        for(FieldValidator<T> validator : backend) {

            messages.addAll(validator.validate(entity, field, val));
        }

        return messages;
    }

    public static final FieldValidators PASS = new FieldValidators() {
        @Override
        public List<ValidationMessage> validate(VersionedEntity entity, EntityField field, Object val) {
            //no news is good news
            return ImmutableList.of();
        }
    };
}
