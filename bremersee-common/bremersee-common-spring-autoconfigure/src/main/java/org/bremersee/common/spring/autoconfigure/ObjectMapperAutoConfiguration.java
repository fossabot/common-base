/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bremersee.common.spring.autoconfigure;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import org.bremersee.geojson.GeoJsonObjectMapperModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.jackson.JacksonProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import javax.annotation.PostConstruct;

/**
 * @author Christian Bremer
 */
@Configuration
@ConditionalOnClass(name = {"org.springframework.http.converter.json.Jackson2ObjectMapperBuilder"})
@ConditionalOnWebApplication
public class ObjectMapperAutoConfiguration implements Jackson2ObjectMapperBuilderCustomizer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private JacksonProperties properties = new JacksonProperties();

    @Autowired(required = false)
    public void setProperties(JacksonProperties properties) {
        if (properties != null) {
            this.properties = properties;
        }
    }

    @PostConstruct
    public void init() {
        // @formatter:off
        log.info("\n"
                + "**********************************************************************\n"
                + "*  Jackson2 ObjectMapper Auto Configuration                          *\n"
                + "**********************************************************************\n"
                + "* - Adds 'JacksonAnnotationIntrospector' as primary                  *\n"
                + "*   and 'JaxbAnnotationIntrospector' as secondary                    *\n"
                + "*   annotation introspector.                                         *\n"
                + "* - Adds 'GeoJsonObjectMapperModule' to the mapper.                  *\n"
                + "**********************************************************************");
        // @formatter:on
    }

    @Override
    public void customize(Jackson2ObjectMapperBuilder jacksonObjectMapperBuilder) {

        // see http://wiki.fasterxml.com/JacksonJAXBAnnotations
        AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
        AnnotationIntrospector secondary = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
        AnnotationIntrospectorPair pair = new AnnotationIntrospectorPair(primary, secondary);
        jacksonObjectMapperBuilder.annotationIntrospector(pair);

        // http://wiki.fasterxml.com/JacksonFAQDateHandling
        // http://docs.spring.io/spring-boot/docs/current/reference/html/howto-spring-mvc.html#howto-customize-the-jackson-objectmapper
        if (properties.getSerialization().get(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) == null) {
            properties.getSerialization().put(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, Boolean.FALSE);
            jacksonObjectMapperBuilder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
        if (Boolean.TRUE.equals(properties.getSerialization().get(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS))
                && properties.getSerialization().get(SerializationFeature.WRITE_DATES_WITH_ZONE_ID) == null) {
            properties.getSerialization().put(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, Boolean.TRUE);
            jacksonObjectMapperBuilder.featuresToEnable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID);
        }

        jacksonObjectMapperBuilder.modules(new GeoJsonObjectMapperModule());
    }

}
