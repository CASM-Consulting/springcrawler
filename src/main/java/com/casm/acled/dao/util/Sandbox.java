package com.casm.acled.dao.util;

import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.dao.entities.ActorDAO;
import com.casm.acled.dao.entities.DeskDAO;
import com.casm.acled.dao.entities.LocationDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.desk.Desk;
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

// We have to exclude these classes, because they only work in a web context.
//@EnableAutoConfiguration(exclude={CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class})
// We need the special object mapper, though.
@Import(ObjectMapperConfiguration.class)
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao"})
public class Sandbox implements CommandLineRunner {

    private final ActorDAO actorDAO;
    private final LocationDAO locationDAO;
    private final SourceDAO sourceDAO;
    private final DeskDAO regionDAO;

    @Autowired
    public Sandbox(ActorDAO actorDAO,
                      LocationDAO locationDAO,
                      SourceDAO sourceDAO,
                      DeskDAO regionDAO
                   ) {
        this.actorDAO = actorDAO;
        this.locationDAO = locationDAO;
        this.sourceDAO = sourceDAO;
        this.regionDAO = regionDAO;
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Sandbox.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }

    public void run(String... args) throws Exception {

//        insertSourceRegion();
        createRegions();
    }

    private void insertSourceRegion() {

        sourceDAO.getAll().forEach(s->sourceDAO.overwrite(s.put("REGION", "middle-east")));

    }

    private void createRegions() {
        createRegion("Africa");
        createRegion("Middle East");
        createRegion("East Asia");
        createRegion("Europe");
    }
    private void createRegion(String regionName) {

        Desk region = EntityVersions.get(Desk.class).current()
                .put(Desk.DESK_NAME, regionName);

        regionDAO.create(region);
    }
}