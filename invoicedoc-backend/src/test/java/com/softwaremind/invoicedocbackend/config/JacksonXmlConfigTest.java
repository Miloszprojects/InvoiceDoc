package com.softwaremind.invoicedocbackend.config;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class JacksonXmlConfigTest {

    private final JacksonXmlConfig config = new JacksonXmlConfig();

    @Test
    @DisplayName("xmlMapper bean should be created and serialize simple map to XML")
    void xmlMapperBeanShouldBeCreatedAndSerializeXml() throws Exception {
        XmlMapper mapper = config.xmlMapper();

        assertThat(mapper).isNotNull();

        Map<String, String> data = new LinkedHashMap<>();
        data.put("name", "Test");
        data.put("value", "123");

        String xml = mapper.writeValueAsString(data);

        assertAll(
                () -> assertThat(xml).contains("Test"),
                () -> assertThat(xml).contains("123"),
                () -> assertThat(xml).contains("name"),
                () -> assertThat(xml).contains("value")
        );
    }
}
