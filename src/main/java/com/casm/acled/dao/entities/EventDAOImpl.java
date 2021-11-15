package com.casm.acled.dao.entities;

//import com.casm.acled.WhereClauseGenerator;
import com.casm.acled.camunda.variables.Entities;
import com.casm.acled.dao.CancelCreateException;
import com.casm.acled.dao.JDBCObjectValidation;
import com.casm.acled.dao.Tables;
import com.casm.acled.dao.VersionedEntityDAOImpl;
import com.casm.acled.dao.rowmappers.VersionedEntityRowMapperFactory;
import com.casm.acled.dao.util.SqlBinder;
import com.casm.acled.entities.actor.Actor;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.desk.Desk;
import com.casm.acled.entities.event.Event;
import com.casm.acled.entities.location.Location;
import com.casm.acled.entities.source.Source;
//import com.casm.acled.queryspecification.QueryField;
//import com.casm.acled.queryspecification.QuerySpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Transactional
@Repository
@Primary
public class EventDAOImpl extends VersionedEntityDAOImpl<Event> implements EventDAO {
    private static final Logger LOG = LoggerFactory.getLogger(EventDAOImpl.class);

    // XXX: Must be kept in sync with the alias actually used in joinedSql.

    private static final Map<String, String> CHILD_FIELD_ALIASES;
    static {
        Map<String, String> map = new HashMap<>();
        map.put(Event.EVENT_LOCATION, "L");
        map.put(Event.ACTOR1, "ACTOR1");
        map.put(Event.ASSOC_ACTOR_1, "ASSOC_ACTOR1");
        map.put(Event.ACTOR2, "ACTOR2");
        map.put(Event.ASSOC_ACTOR_2, "ASSOC_ACTOR2");
        CHILD_FIELD_ALIASES = Collections.unmodifiableMap(map);
    }
    private static final String BASE_TABLE_ALIAS = "E";
    

    private final ArticleEventDAO articleEventDAO;
    private final VersionedEntityRowMapperFactory rowMapperFactory;

    public EventDAOImpl(
        @Autowired JdbcTemplate jdbcTemplate,
        @Autowired VersionedEntityRowMapperFactory rowMapperFactory,
        @Autowired ArticleEventDAO articleEventDAO,
        @Value(Tables.T_EVENT) String table
    ) {
        super(jdbcTemplate, table, Event.class, rowMapperFactory.of(Event.class));
        this.articleEventDAO = articleEventDAO;
        this.rowMapperFactory = rowMapperFactory;
    }
//
//    @Override
//    public List<Event> searchEmbedded(QuerySpecification q) {
//        SqlBinder sql = joinedSql();
//
//        sql.append("WHERE");
//        WhereClauseGenerator whereClause = new WhereClauseGenerator(sql);
//
//
//        for (QueryField v: q.getFrom()) {
//            whereClause.addFromFilter(v.getEntity(), getTableAlias(v.getParentField()));
//        }
//        for (QueryField v: q.getTo()) {
//            whereClause.addToFilter(v.getEntity(), getTableAlias(v.getParentField()));
//        }
//        for (QueryField v: q.getValue()) {
//            whereClause.addValueFilter(v.getEntity(), getTableAlias(v.getParentField()));
//        }
//
//
//        List<Object> arguments = whereClause.getArguments();
//
//        // If no critera were specified, just run the original query.
//        String sqlToRun = whereClause.hadCriteria() ? sql.bind() : joinedSql().bind();
//
//        // list out stuff for debugging
//        LOG.info("Generated SQL is {}", sqlToRun);
//        for (Object o : arguments) {
//            LOG.info("Argument was: {}", o);
//        }
//
//        return groupResults(jdbcTemplate.query(
//            sqlToRun, rowMapperFactory.of(klass), arguments.toArray()
//        ));
//    }

    private String getTableAlias(@Nullable String parentField) {
        String alias;

        if (parentField == null) {
            alias = BASE_TABLE_ALIAS;
        } else {
            alias = CHILD_FIELD_ALIASES.get(parentField);

            if (alias == null) {
                throw new RuntimeException("The parent field was not found: " + parentField);
            }
        }

        return alias;
    }

    private List<Event> groupResults(List<Event> events) {

        Map<Event, Event> grouped = new HashMap<>();
        for(Event event : events) {
            if(grouped.containsKey(event)) {
                Event existing = grouped.get(event);

                event = event.articles(mergeLists(event.articles(), existing.articles()));
                event = event.sources(mergeLists(event.sources(), existing.sources()));
                event = event.assocActors1(mergeLists(event.assocActors1(), existing.assocActors1()));
                event = event.assocActors2(mergeLists(event.assocActors2(), existing.assocActors2()));
            }

            grouped.put(event, event);
        }
        return new ArrayList<>(grouped.values());
    }

    /**
     * Merge two lists into a new single list, tolerating either list being null or empty.
     */
    private <T> List<T> mergeLists(List<T> a, List<T> b){
        return Stream.of(a, b)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
    }

    protected SqlBinder joinedSql() {
        return SqlBinder.sql(
                "SELECT E.id AS E_ID, E.data AS E_DATA, A.id as A_ID, A.data AS A_DATA, S.id AS S_ID, S.data AS S_DATA, ACTOR1.id AS ACTOR1_ID, ACTOR1.data AS ACTOR1_DATA, ACTOR2.id AS ACTOR2_ID, ACTOR2.data AS ACTOR2_DATA, ASSOC_ACTOR1.id AS ASSOC_ACTOR1_ID, ASSOC_ACTOR1.data AS ASSOC_ACTOR1_DATA, ASSOC_ACTOR2.id AS ASSOC_ACTOR2_ID, ASSOC_ACTOR2.data AS ASSOC_ACTOR2_DATA, L.id AS L_ID, L.data AS L_DATA",
                "FROM ${table} AS E",
                "LEFT JOIN ${join_table} AS AE ON (AE.id2 = E.id)",
                "LEFT JOIN ${article_table} AS A ON A.id = AE.id1",
                "LEFT JOIN ${source_table} AS S ON (A.data->>'SOURCE_ID')::int = S.id",
                "LEFT JOIN ${actor_table} AS ACTOR1 ON (E.data->>'ACTOR1')::int = ACTOR1.id",
                "LEFT JOIN ${actor_table} AS ACTOR2 ON (E.data->>'ACTOR2')::int = ACTOR2.id",
                "LEFT JOIN ${actor_table} AS ASSOC_ACTOR1 ON (E.data->'ASSOC_ACTOR1') ?? ASSOC_ACTOR1.id::text",
                "LEFT JOIN ${actor_table} AS ASSOC_ACTOR2 ON (E.data->'ASSOC_ACTOR2') ?? ASSOC_ACTOR2.id::text",
                "LEFT JOIN ${location_table} AS L ON (E.data->>'EVENT_LOCATION')::int = L.id"
        )
            .bind("table", table)
            .bind("join_table", Tables.T_ARTICLE_EVENT)
            .bind("article_table", Tables.T_ARTICLE)
            .bind("source_table", Tables.T_SOURCE)
            .bind("actor_table", Tables.T_ACTOR)
            .bind("location_table", Tables.T_LOCATION)
            ;
    }

    @Override
    protected List<Event> query(String sql, Object... args) {
        return groupResults(super.query(sql, args));
    }

    @Override
    public List<Event> create(List<Event> events) {
        events = new ArrayList<>(events);
        ListIterator<Event> itr = events.listIterator();

        while(itr.hasNext()) {
            Event event = itr.next();
            if(event.assocActors1() != null && !event.assocActors1().isEmpty()) {
                List<Integer> ids = event.assocActors1().stream().map(a->a.id()).collect(Collectors.toList());
                event = event.put(Event.ASSOC_ACTOR_1, ids);
            }
            if(event.assocActors2() != null && !event.assocActors2().isEmpty()) {
                List<Integer> ids = event.assocActors2().stream().map(a->a.id()).collect(Collectors.toList());
                event = event.put(Event.ASSOC_ACTOR_2, ids);
            }
            itr.set(event);
        }
        events = super.create(events);

//        for(Event event : events) {
//            for(Article article : event.articles()) {
//                articleEventDAO.link(article, event);
//            }
//        }
        return events;
    }

    @Override
    public void overwrite(Event event) {
        if(event.assocActors1() != null && !event.assocActors1().isEmpty()) {
            List<Integer> ids = event.assocActors1().stream().map(a->a.id()).collect(Collectors.toList());
            event = event.put(Event.ASSOC_ACTOR_1, ids);
        }
        if(event.assocActors2() != null && !event.assocActors2().isEmpty()) {
            List<Integer> ids = event.assocActors2().stream().map(a->a.id()).collect(Collectors.toList());
            event = event.put(Event.ASSOC_ACTOR_2, ids);
        }
        super.overwrite(event);
//        for(Article article : event.articles()) {
//            articleEventDAO.link(article, event);
//        }
    }

    @Override
    public List<Event> getAll() {
        String sql = joinedSql()
                .bind();

        return query(sql);
    }

    @Override
    public <T> List<Event> getBy(String field, T value) {
        JDBCObjectValidation.validField(field);
        String sql = joinedSql().append("WHERE E.data->>'${field}' = ?")
                .bind("field", field)
                .bind();

        List<Event> results = query(sql, value);

        return results;
    }

    public List<Event> byActor(Actor actor){
        String sql = joinedSql().append("WHERE (E.data->>'${actor1}')::int = ? " +
                                           "OR (E.data->>'${actor2}')::int = ? " +
                                           "OR (E.data->'${assocActor1}') ?? ?" +
                                           "OR (E.data->'${assocActor2}') ?? ?")
                .bind("actor1", Event.ACTOR1)
                .bind("actor2", Event.ACTOR2)
                .bind("assocActor1", Event.ASSOC_ACTOR_1)
                .bind("assocActor2", Event.ASSOC_ACTOR_2)
                .bind();

        List<Event> results = query(sql, actor.id(), actor.id(), Integer.toString(actor.id()), Integer.toString(actor.id())); // ಠ_ಠ
        return results;
    }

    @Override
    public Optional<Event> getById(int id) {
        String sql = joinedSql().append("WHERE E.id = ?")
                .bind();

        Optional<Event> result = Optional.empty();

        List<Event> results = query(sql, id);

        if(results.size()==1) {
            result = Optional.of(results.get(0));
        }

        return result;
    }

    @Override
    public List<Event> byRegion(Desk region) {
        return null;
    }

    public List<Event> byLocation(Location location){
        String sql = joinedSql().append("WHERE L.id = ?")
                .bind();
        List<Event> events = query(sql, location.id());
        return events;
    }

    @Override
    public List<Event> byArticle(Article article) {
        String sql = joinedSql().append("WHERE AE.id1 = ?")
                .bind();
        List<Event> events = query(sql, article.id());
        return events;
    }

    @Override
    public List<Event> bySource(Source source) {
        String sql = joinedSql().append("WHERE S.id = ?")
                .bind();
        List<Event> events = query(sql, source.id());
        return events;
    }

    @Override
    public List<Event> getReviewed() {
        String whereClause = String.format(
            "WHERE (E.data??'%s') AND (E.data??'%s')",
            Event.RM_CHECK, Event.GRM_CHECK
        );

        String sql = joinedSql().append(whereClause).bind();
        return query(sql);
    }

    @Override
    public List<Event> byActor1(Actor actor) {
        return null;
    }

    @Override
    public List<Event> byActor2(Actor actor) {
        return null;
    }

    @Override
    public List<Event> byActorEither(Actor actor) {
        return null;
    }

    @Override
    public List<Event> byInteractionCode(String code) {
        return null;
    }

    @Override
    protected Event preCreate(Event event) throws CancelCreateException {

        if(event.isTrue(Entities.HISTORICAL)) {

            event = event.put(Entities.HISTORICAL_ID, event.id()).withoutId();
        }

        return event;
    }
}
