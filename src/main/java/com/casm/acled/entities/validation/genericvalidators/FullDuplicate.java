package com.casm.acled.entities.validation.genericvalidators;

import com.casm.acled.entities.VersionedEntity;
import com.casm.acled.entities.validation.EntityValidator;
import com.casm.acled.entities.validation.ValidationMessage;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.*;
import java.util.stream.Collectors;

public class FullDuplicate<V extends VersionedEntity<V>> implements EntityValidator<V> {

    protected String message = "Identical entries.";

    @Override
    public List<List<ValidationMessage>> validate(List<V> batch) {
        Map<V, Integer> dupCheck = new HashMap<>();
        Map<V, List<Integer>> dupes = new HashMap<>();

        batch = new ArrayList<>(batch);
        ListIterator<V> itr = batch.listIterator();
        //blank out id for dupe check
        while (itr.hasNext()) {
            itr.set(itr.next().withoutId());
        }

        for(int i = 0; i < batch.size(); ++i ) {
            V e = batch.get(i);
            Integer j = dupCheck.put(e, i);
            if(j!=null) {
                if(dupes.containsKey(e)) {
                    dupes.get(e).add(i);
                } else {
                    dupes.put(e, Lists.newArrayList(j, i));  // Fix: you cannot structurally modify the result of Arrays.asList(), so using it would make above use of .add() fail.
                }
            }
        }

        int group = 1;
        List<List<ValidationMessage>> messages = new ArrayList<>();
        for(int i = 0; i < batch.size(); ++i ) {
            V e = batch.get(i);
            if(dupes.containsKey(e)) {
                messages.add(ImmutableList.of(getMessage(i, group, dupes.get(e), batch)));
                ++group;
            } else {
                messages.add(ImmutableList.of());
            }
        }

        return messages;
    }


    protected String[] getOthers(List<Integer> ints, int i, List<V> batch) {

        return ints.stream().map(j-> {
            if(batch.get(j).hasId()) {
                j = batch.get(j).id();
            } else {
                j = j+1;
            }
            return j;
        }).filter(j->j!=i).map(j->Integer.toString(j)).collect(Collectors.toList()).toArray(new String[]{});
    }

    protected ValidationMessage getMessage(int index, int group, List<Integer> others, List<V> batch) {

        if(batch.get(index).hasId()) {
            index = batch.get(index).id();
        } else {
            index = index +1;
        }

        return ValidationMessage.warn(message,
                "Same as " + String .join(",", Arrays.asList(getOthers(others, index, batch))) + ".", "",
                getOthers(others, index, batch));
    }
}
