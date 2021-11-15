package com.casm.acled.dao.entities;

import com.casm.acled.dao.HasDesk;
import com.casm.acled.dao.VersionedEntityDAO;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;

import java.util.List;
import java.util.Optional;

public interface SourceListDAO extends VersionedEntityDAO<SourceList>, HasDesk<SourceList> {

    Optional<SourceList> byName(String name);

    List<SourceList> bySource(Source source);
    List<SourceList> bySource(Integer id);
//    List<SourceList> byDesk(Integer id);
}
