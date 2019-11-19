package com.casm.acled.crawler.spring;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.jmx.ParentAwareNamingStrategy;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.UUID;

@Configuration
//@PropertySource(value= {"classpath:application.properties"})
public class HikariConfiguration extends HikariConfig {

    {
        this.setJdbcUrl("jdbc:postgresql://127.0.0.1:5432/postgres");
        this.setUsername("postgres");
    }

//    private static HikariConfig config = new HikariConfig();

//    {
//        config.setJdbcUrl("jdbc:postgresql://127.0.0.1:5432/postgres");
//        config.setUsername("postgres");
//        config.setPassword("postgres");
//        config.setDriverClassName("org.postgresql.Driver");
//    }

//    @Autowired
//    Environment environment;

    @Bean
//    @Primary
    public DataSource dataSource() throws SQLException {
        HikariDataSource dataSource = new HikariDataSource(this);
        dataSource.setPoolName("dataSource_" + "flimflam");
        //UUID.randomUUID().toString()
        return dataSource;
    }

    @Bean
    @ConditionalOnMissingBean(value = ObjectNamingStrategy.class, search = SearchStrategy.CURRENT)
    public ParentAwareNamingStrategy objectNamingStrategy() {
        ParentAwareNamingStrategy namingStrategy = new ParentAwareNamingStrategy(new AnnotationJmxAttributeSource());
        namingStrategy.setDefaultDomain("domain_" + UUID.randomUUID().toString());
        return namingStrategy;
    }

}
