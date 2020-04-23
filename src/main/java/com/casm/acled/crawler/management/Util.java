package com.casm.acled.crawler.management;

import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.scraper.dates.DateTimeServiceRunner;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.dao.entities.SourceSourceListDAO;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.google.common.collect.ImmutableList;
import com.opencsv.CSVReader;
import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
import org.camunda.bpm.spring.boot.starter.rest.CamundaBpmRestJerseyAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

@EnableAutoConfiguration(exclude={HibernateJpaAutoConfiguration.class, CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class, ValidationAutoConfiguration.class})
// We need the special object mapper, though.
//@Import({ObjectMapperConfiguration.class, CLIRunner.ShutdownConfig.class})
@Import({ObjectMapperConfiguration.class})
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao", "com.casm.acled.crawler"})
public class Util implements CommandLineRunner {


    @Autowired
    private SourceListDAO sourceListDAO;

    @Autowired
    private SourceDAO sourceDAO;

    @Autowired
    private SourceSourceListDAO sourceSourceListDAO;


    private void insertDummySource() {
        String link = "http://www.0.com:5000";
        Source source = EntityVersions.get(Source.class).current()
                .put(Source.LINK, link)
                .put(Source.NAME, "fake net")
                .put(Source.STANDARD_NAME, "fake net")
                .put(Source.COUNTRY, "United Kingdom")
                ;

        source = sourceDAO.create(source);

        SourceList sourceList = EntityVersions.get(SourceList.class).current()
                .put(SourceList.LIST_NAME, "fake list");

        sourceList = sourceListDAO.create(sourceList);

        sourceSourceListDAO.link(source, sourceList);

    }

    private void updateDummySource() {
        Source source = sourceDAO.getByUnique(Source.STANDARD_NAME, "fake net").get();

        source = source.put(Source.DATE_FORMAT, ImmutableList.of("ISO:/yyyy-MM-dd/en_GB"));

        sourceDAO.update(source);
    }


    private void createSources(Path seedsPath) throws IOException {
        try (Reader reader = Files.newBufferedReader(seedsPath)) {
            CSVReader csvReader = new CSVReader(reader);
            Iterator<String[]> itr = csvReader.iterator();
            itr.next();
            while(itr.hasNext()) {
                String[] row = itr.next();

                Source source = EntityVersions.get(Source.class).current()
                        .put(Source.LINK, row[1])
                        .put(Source.STANDARD_NAME, row[0])
                        .put(Source.COUNTRY, row[2])
                        ;

                try {
                    sourceDAO.create(source);
//                    System.out.println(source);
                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                    //already exists?
                }
            }
        }
    }

    @Override
    public void run(String... args) throws Exception {

//        insertDummySource();
//        updateDummySource();
        createSources(Paths.get("seeds.csv"));
    }

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(Util.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(args);
        ctx.close();
    }
}
