package com.casm.acled.configuration;

import com.casm.acled.AcledObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

// This seems to only function in tests.
@Configuration
public class ObjectMapperConfiguration {
    @Bean
    @Primary
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @Autowired
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder){

        ObjectMapper om = AcledObjectMapper.configure(builder.createXmlMapper(false).build());

        return om;
    }

}
