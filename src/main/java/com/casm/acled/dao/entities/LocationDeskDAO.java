package com.casm.acled.dao.entities;

import com.casm.acled.dao.LinkDAO;
import com.casm.acled.entities.desk.Desk;
import com.casm.acled.entities.location.Location;
import com.casm.acled.entities.locationdesk.LocationDesk;

public interface LocationDeskDAO extends LinkDAO<Location, Desk, LocationDesk> {

}
