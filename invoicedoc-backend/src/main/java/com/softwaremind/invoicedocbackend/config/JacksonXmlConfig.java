package com.softwaremind.invoicedocbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
@Configuration
public class JacksonXmlConfig {

    @Bean
    public XmlMapper xmlMapper() {
        return XmlMapper.builder()
                .findAndAddModules()
                .build();
    }
}
