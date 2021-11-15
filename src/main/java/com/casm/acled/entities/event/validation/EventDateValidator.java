package com.casm.acled.entities.event.validation;


//import com.casm.acled.camunda.BusinessKeys;
import com.casm.acled.camunda.variables.Process;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.event.Event;
import com.casm.acled.entities.sourcelist.SourceList;
import com.casm.acled.entities.validation.EntityValidator;
import com.casm.acled.entities.validation.ValidationMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.threeten.extra.YearWeek;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class EventDateValidator implements EntityValidator<Event> {

    private final SourceListDAO sourceListDAO;

    @Autowired
    public EventDateValidator(SourceListDAO sourceListDAO) {
        this.sourceListDAO = sourceListDAO;
    }


    private List<ValidationMessage> validate(Event event) {
        String businessKey = event.get(Process.BUSINESS_KEY);

//        String sourceListName = BusinessKeys.getSourceListName(businessKey);

//        SourceList sourceList = sourceListDAO.byName(sourceListName).get();
//
//        List<ValidationMessage> msgs = new ArrayList<>();
//        LocalDate eventDate = event.get(Event.EVENT_DATE);
//        if(eventDate != null) {
//
//            if (sourceList.isTrue(SourceList.BACK_CODING)) {
//
//                LocalDate from = sourceList.get(SourceList.FROM);
//                LocalDate to = sourceList.get(SourceList.TO);
//
//                if (eventDate.isBefore(from) || eventDate.isAfter(to)) {
//                    ValidationMessage message = ValidationMessage.warn("Date out of bounds.", eventDate.toString(), String.format("Should between %s and %s.", from.toString(), to.toString()));
//                    msgs.add(message);
//                }
//
//
//            } else {
//
//                YearWeek eventWeek = YearWeek.from(eventDate);
////                YearWsourceListNameeek businessWeek = BusinessKeys.getYearWeek(businessKey);
////                if(!eventWeek.equals(businessWeek)) {
////                    LocalDate from = businessWeek.atDay(DayOfWeek.MONDAY);
////                    LocalDate to = businessWeek.atDay(DayOfWeek.SUNDAY);
////
////                    ValidationMessage message = ValidationMessage.warn("Date out of bounds.", eventDate.toString(), String.format("Should between %s and %s.", from.toString(), to.toString()));
////                    msgs.add(message);
////                }
//            }
//        }

        return null;

    }

    @Override
    public List<List<ValidationMessage>> validate(List<Event> events) {

        List<List<ValidationMessage>> msgs = new ArrayList<>();

        for(Event event : events) {

            msgs.add(validate(event));
        }

        return msgs;
    }
}
