package com.casm.acled.crawler.spring;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.jmx.ParentAwareNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.core.env.Environment;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;
import org.springframework.jmx.support.RegistrationPolicy;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.UUID;

@EnableMBeanExport(registration= RegistrationPolicy.IGNORE_EXISTING)
@Configuration
public class HikariConfiguration extends HikariConfig {

    @Autowired
    Environment environment;

    @Bean
    public DataSource dataSource() throws SQLException {
        HikariDataSource dataSource = new HikariDataSource(this);
        dataSource.setPoolName("dataSource_" + UUID.randomUUID().toString());
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
