package com.casm.acled.dao.entities;

import com.casm.acled.dao.LinkDAO;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.casm.acled.entities.sourcesourcelist.SourceSourceList;

public interface SourceSourceListDAO extends LinkDAO<Source, SourceList, SourceSourceList> {

}
