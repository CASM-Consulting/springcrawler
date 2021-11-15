package com.casm.acled.entities.event.validation;//package com.casm.acled.entities.event.validation;
//
//import com.casm.acled.entities.validation.EntityFieldValidationException;
//import com.casm.acled.entities.validation.EntityFieldValidator;
//import com.casm.acled.entities.event.eventtypes.EventType_v1;
//
//public class EventTypeValidator_v1 extends EntityFieldValidator<String> {
//
//    @Override
//    public void validate(String val) {
//
//        if(!EventType_v1.TYPES.keySet().contains(val)) {
//            throw new EntityFieldValidationException(val + " is not a valid event type");
//        }
//    }
//}
