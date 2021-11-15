package com.casm.acled.dao.entities;

import com.casm.acled.dao.VersionedEntityDAO;
import com.casm.acled.entities.actor.Actor;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.desk.Desk;
import com.casm.acled.entities.event.Event;
import com.casm.acled.entities.location.Location;
import com.casm.acled.entities.source.Source;

import java.util.List;

public interface EventDAO extends VersionedEntityDAO<Event> {
    List<Event> byRegion(Desk region);
    List<Event> byLocation(Location location);
    List<Event> byArticle(Article article);
    List<Event> bySource(Source source);
    List<Event> byActor1(Actor actor);
    List<Event> byActor2(Actor actor);
    List<Event> byActorEither(Actor actor);
    List<Event> byActor(Actor actor);
    List<Event> byInteractionCode(String code);

    List<Event> getReviewed();
}
