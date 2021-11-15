package com.casm.acled.entities.event.validation;

import com.casm.acled.entities.event.Event;
import com.casm.acled.entities.validation.EntityValidators;
import com.casm.acled.entities.validation.genericvalidators.FullDuplicate;
import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EventValidators extends EntityValidators<Event> {

    public EventValidators(@Autowired DupDateIntTypeLoc dupDateIntTypeLoc,
                           @Autowired DupDateTypeLoc dupDateTypeLoc,
                           @Autowired EventDateValidator eventDateValidator
                           ) {
        super(ImmutableList.of(
                dupDateIntTypeLoc,
                dupDateTypeLoc,
                new FullDuplicate<>(),
                eventDateValidator
        ), ImmutableList.of());
    }

}
