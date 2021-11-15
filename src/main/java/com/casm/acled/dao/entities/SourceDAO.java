package com.casm.acled.dao.entities;

import com.casm.acled.dao.HasDesk;
import com.casm.acled.dao.VersionedEntityDAO;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;

import java.util.List;
import java.util.Optional;

public interface SourceDAO extends VersionedEntityDAO<Source>, HasDesk<Source> {

//    List<Source> byDesk(Integer deskId);
    List<Source> byList(SourceList list);
    Optional<Source> byName(String sourceName);
}
