package com.casm.acled.dao.entities;

import com.casm.acled.dao.VersionedEntityDAO;
import com.casm.acled.entities.desk.Desk;
import com.casm.acled.entities.sourcelist.SourceList;

public interface DeskDAO extends VersionedEntityDAO<Desk> {

    Desk bySourceList(SourceList sourceList);

}
