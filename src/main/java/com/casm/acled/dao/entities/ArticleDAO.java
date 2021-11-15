package com.casm.acled.dao.entities;

import com.casm.acled.dao.VersionedEntityDAO;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.desk.Desk;
import com.casm.acled.entities.event.Event;
import com.casm.acled.entities.source.Source;

import java.util.List;

public interface ArticleDAO extends VersionedEntityDAO<Article> {

    List<Article> byRegion(Desk region);
    List<Article> byEvent(Event event);
    List<Article> bySource(Source source);
    List<Article> byResearcher(String id);

    List<Article> getByBusinessKeyCached(String businessKey);

}
