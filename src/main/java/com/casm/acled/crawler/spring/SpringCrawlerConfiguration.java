package com.casm.acled.crawler.spring;

import com.casm.acled.configuration.ObjectMapperConfiguration;
import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
import org.camunda.bpm.spring.boot.starter.rest.CamundaBpmRestJerseyAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
@EnableAutoConfiguration(exclude={HibernateJpaAutoConfiguration.class, CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class, ValidationAutoConfiguration.class})
@Import({ObjectMapperConfiguration.class})
public class SpringCrawlerConfiguration {


    //this shouldn't be necessary, but application.properties seems to be ignored by JQM Spring
    @Bean
    @Primary
    public DataSource dataSource() {
        return DataSourceBuilder
                .create()
                .username("postgres")
                .url("jdbc:postgresql://localhost:6432/acled_camunda?preparedStatementCacheQueries=0&prepareThreshold=0")
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}
