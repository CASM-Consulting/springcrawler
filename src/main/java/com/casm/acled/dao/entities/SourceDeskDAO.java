package com.casm.acled.dao.entities;

import com.casm.acled.dao.LinkDAO;
import com.casm.acled.entities.desk.Desk;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcedesk.SourceDesk;

public interface SourceDeskDAO extends LinkDAO<Source, Desk, SourceDesk> {

}
