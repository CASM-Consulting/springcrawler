package com.casm.acled.dao.entities;

import com.casm.acled.dao.HasDesk;
import com.casm.acled.dao.VersionedEntityDAO;
import com.casm.acled.entities.location.Location;

import java.util.List;

public interface LocationDAO extends VersionedEntityDAO<Location>, HasDesk<Location> {
    List<String> countries();
    List<Location> byName(String name);
    List<Location> byCountry(String country);
//    List<Location> byDesk(Integer deskId);
}
