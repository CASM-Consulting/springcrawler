package com.casm.acled.entities.validation;

import com.casm.acled.entities.VersionedEntity;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EntityValidators<V extends VersionedEntity<V>> implements EntityValidator<V> {

    private final List<EntityValidator<V>> backend;
    private final List<Map<String,String>> frontend;

    public EntityValidators() {
        this(ImmutableList.of(), ImmutableList.of());
    }

    public EntityValidators(List<EntityValidator<V>> backend, List<Map<String,String>> frontend) {
        this.backend = backend;
        this.frontend = frontend;
    }

    public static <V extends VersionedEntity<V>> EntityValidators<V> ofFrontend(String... names) {
        return new EntityValidators<V>().withFrontend(names);
    }

    public static <V extends VersionedEntity<V>> EntityValidators<V> ofBackend(EntityValidator<V>... validators) {
        return new EntityValidators<V>().withBackend(validators);
    }

    public EntityValidators<V> withFrontend(String... names) {

        ImmutableList.Builder<Map<String,String>> builder = new ImmutableList.Builder<>();
        return new EntityValidators<>(
                backend,
                builder.addAll(frontend)
                        .addAll(Arrays.stream(names).map(n -> ImmutableMap.of("name", n)).collect(Collectors.toList()))
                .build()
        );
    }

    public EntityValidators<V> withBackend(EntityValidator<V>... validators) {

        ImmutableList.Builder<EntityValidator<V>> builder = new ImmutableList.Builder<>();
        return new EntityValidators<>(
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
    public List<List<ValidationMessage>> validate(List<V> entities) {

        List<List<ValidationMessage>> batchMessages = new ArrayList<>();

        for(EntityValidator<V> validator : backend) {
            List<List<ValidationMessage>> validatorMessages = new ArrayList<>(entities.size());

            for(int i = 0; i < entities.size(); ++i) {
                if(batchMessages.size() == i) {
                    batchMessages.add(new ArrayList<>());
                }

                batchMessages.get(i).addAll(validator.validate(entities).get(i));
            }

        }

        return batchMessages;
    }

    public static final EntityValidators PASS = new EntityValidators() {
        @Override
        public List<List<ValidationMessage>> validate(List entities) {
            //no news is good news
            return ImmutableList.of();
        }
    };
}
