package com.casm.acled.dao.util;

import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.dao.entities.*;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.actor.Actor;
import com.casm.acled.entities.desk.Desk;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
//import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
//import org.camunda.bpm.spring.boot.starter.rest.CamundaBpmRestJerseyAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;


// We have to exclude these classes, because they only work in a web context.
//@EnableAutoConfiguration(exclude={CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class})
// We need the special object mapper, though.
@Import(ObjectMapperConfiguration.class)
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao"})
@Transactional
public class DummyData implements CommandLineRunner {


    @Autowired
    private ActorDAO actorDAO;

    @Autowired
    private ActorDeskDAO actorDeskDAO;

    @Autowired
    private ArticleDAO articleDAO;

    @Autowired
    private SourceListDAO sourceListDAO;

    @Autowired
    private SourceSourceListDAO sourceSourceListDAO;

    @Autowired
    private SourceDAO sourceDAO;

    @Autowired
    private SourceDeskDAO sourceDeskDAO;

    @Autowired
    private DeskDAO deskDAO;

    public void createDummyDesk() {

        Desk desk = EntityVersions.get(Desk.class).current()
                .put(Desk.DESK_NAME, "DummyDesk");

        SourceList sourceList = EntityVersions.get(SourceList.class).current()
                .put(SourceList.LIST_NAME, "DummyList");

        Source source = EntityVersions.get(Source.class).current()
                .put(Source.NAME, "DummySource")
                .put(Source.STANDARD_NAME, "DummySource")
                .put(Source.LINK, "DummyLink");

        Actor actor = EntityVersions.get(Actor.class).current()
                .put(Actor.ACTOR_NAME, "DummyActor")
                .put(Actor.INTER, "2")
                .put(Actor.VERIFIED, true);

        desk = deskDAO.create(desk);
        sourceList = sourceList.put(SourceList.DESK_ID, desk.id());
        sourceList = sourceListDAO.create(sourceList);
        source = sourceDAO.create(source);
        actor = actorDAO.create(actor);


        sourceSourceListDAO.link(source, sourceList);
        sourceDeskDAO.link(source, desk);
        actorDeskDAO.link(actor, desk);

    }

    public void run(String... args) throws Exception {
        createDummyDesk();
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(DummyData.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
}