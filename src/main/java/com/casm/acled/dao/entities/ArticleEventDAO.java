package com.casm.acled.dao.entities;

import com.casm.acled.dao.LinkDAO;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.articleevent.ArticleEvent;
import com.casm.acled.entities.event.Event;

public interface ArticleEventDAO extends LinkDAO<Article, Event, ArticleEvent> {


}
