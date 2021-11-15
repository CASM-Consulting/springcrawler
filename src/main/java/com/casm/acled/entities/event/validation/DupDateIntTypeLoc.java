package com.casm.acled.entities.event.validation;

import com.casm.acled.dao.entities.LocationDAO;
import com.casm.acled.entities.event.Event;
import com.casm.acled.entities.location.Location;
import com.casm.acled.entities.validation.genericvalidators.PartialDuplicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class DupDateIntTypeLoc extends PartialDuplicate<Event> {

    @Autowired
    public DupDateIntTypeLoc(final LocationDAO locationDAO) {
        super("Identical date, interaction code, type, and location name.",
                (e) -> e.get(Event.EVENT_DATE),
                (e) -> e.get(Event.SUB_EVENT_TYPE),
                (e) -> e.get(Event.INTERACTION),
                (e) -> {
                    Integer locationId = e.get(Event.EVENT_LOCATION);
                    if(locationId == null) {
                        return null;
                    }
                    Location location = locationDAO.getById(locationId).get();
                    return location.get(Location.LOCATION);
                }
        );
    }
}
