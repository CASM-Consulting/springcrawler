package com.casm.acled.entities.validation.genericvalidators;

import com.casm.acled.entities.VersionedEntity;
import com.casm.acled.entities.validation.ValidationMessage;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.*;
import java.util.function.Function;

public class PartialDuplicate<V extends VersionedEntity<V>> extends  FullDuplicate<V> {

    private class Key<V extends VersionedEntity<V>> {

        private final List<Function<V, ?>> getters;
        private final V entity;

        protected Key(V entity, List<Function<V, ?>> getters) {
            this.getters = getters;
            this.entity = entity;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key<V> key = (Key<V>) o;
            EqualsBuilder eb = new EqualsBuilder();

            for(Function<V, ?> getter : getters) {
                eb.append(getter.apply(entity), getter.apply(key.entity) );
            }

            return eb.isEquals();
        }

        @Override
        public int hashCode() {
            HashCodeBuilder hcb = new HashCodeBuilder(17, 37);

            for(Function<V, ?> getter : getters) {

                hcb.append(getter.apply(entity));
            }
            return hcb.toHashCode();
        }

        @Override
        public String toString(){
            return entity.toString();
        }
    }

    private final List<Function<V, ?>> getters;

    public PartialDuplicate(String message, Function<V, ?>... getters) {
        this.message = message;
        this.getters = Arrays.asList(getters);
    }

    private Key<V> key(V entity) {
        return new Key<>(entity, getters);
    }

    @Override
    public List<List<ValidationMessage>> validate(List<V> batch) {
        Map<Key<V>, Integer> dupCheck = new HashMap<>();
        Map<Key<V>, List<Integer>> dupes = new HashMap<>();

        batch = new ArrayList<>(batch);
        ListIterator<V> itr = batch.listIterator();
        //blank out id for dupe check
        while (itr.hasNext()) {
            itr.set(itr.next().withoutId());
        }

        for(int i = 0; i < batch.size(); ++i ) {
            Key<V> e = key(batch.get(i));
            Integer j = dupCheck.put(e, i);
            if(j!=null) {
                if(dupes.containsKey(e)) {
                    dupes.get(e).add(i);
                } else {
                    dupes.put(e, Lists.newArrayList(j, i)); // Fix: you cannot structurally modify the result of Arrays.asList(), so using it would make the above use of .add() fail.
                }
            }
        }

        int group = 1;
        List<List<ValidationMessage>> messages = new ArrayList<>();
        for(int i = 0; i < batch.size(); ++i ) {
            Key<V> e = key(batch.get(i));
            if(dupes.containsKey(e)) {
                messages.add(ImmutableList.of(getMessage(i, group, dupes.get(e), batch)));
                ++group;
            } else {
                messages.add(ImmutableList.of());
            }
        }

        return messages;
    }
}
