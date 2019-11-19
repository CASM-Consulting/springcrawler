package com.casm.acled.crawler.spring;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.jmx.ParentAwareNamingStrategy;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;
import org.springframework.jmx.support.RegistrationPolicy;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.UUID;

//@EnableMBeanExport(registration= RegistrationPolicy.IGNORE_EXISTING)
@Configuration
@PropertySource(value= {"classpath:application.properties"})
public class HikariConfiguration extends HikariConfig {

    @Autowired
    Environment environment;
//
//    @Value("${spring.datasource.driver-class-name}")
//    private String driverName;
//
//    @Value("${spring.datasource.url}")
//    private String url;
//
//    @Value("${spring.datasource.username}")
//    private String userName;
//
//    @Value("${spring.datasource.password}")
//    private String password;

    @Bean
    @Primary
    public DataSource dataSource() throws SQLException {
        HikariDataSource dataSource = new HikariDataSource(this);
        dataSource.setPoolName("dataSource_" + UUID.randomUUID().toString());
        dataSource.setJdbcUrl("jdbc:postgresql://127.0.0.1:5432/postgres");
        dataSource.setUsername("postgres");
        dataSource.setPassword("postgres");
        dataSource.setDriverClassName("org.postgresql.Driver");
        return dataSource;
    }

//    @Bean
//    @Primary
//    public DataSource dataSource() throws SQLException {
//        HikariDataSource dataSource = new HikariDataSource(this);
//        dataSource.setPoolName("dataSource_" + UUID.randomUUID().toString());
////        dataSource.setJdbcUrl("jdbc:postgresql://127.0.0.1:5432/postgres");
////        dataSource.setUsername("postgres");
////        dataSource.setPassword("postgres");
////        dataSource.setDriverClassName("org.postgresql.Driver");
//        dataSource.setJdbcUrl(url);
//        dataSource.setDriverClassName(driverName);
//        dataSource.setUsername(userName);
//        dataSource.setPassword(password);
//        return dataSource;
//    }

    @Bean
    @ConditionalOnMissingBean(value = ObjectNamingStrategy.class, search = SearchStrategy.CURRENT)
    public ParentAwareNamingStrategy objectNamingStrategy() {
        ParentAwareNamingStrategy namingStrategy = new ParentAwareNamingStrategy(new AnnotationJmxAttributeSource());
        namingStrategy.setDefaultDomain("domain_" + UUID.randomUUID().toString());
        return namingStrategy;
    }
}
