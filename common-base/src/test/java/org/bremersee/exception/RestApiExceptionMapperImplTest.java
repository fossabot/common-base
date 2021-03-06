/*
 * Copyright 2018 the original author or authors.
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

package org.bremersee.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import org.bremersee.exception.RestApiExceptionMapperProperties.ExceptionMapping;
import org.bremersee.exception.RestApiExceptionMapperProperties.ExceptionMappingConfig;
import org.bremersee.exception.model.RestApiException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.HttpStatus;

/**
 * The rest api exception mapper impl test.
 *
 * @author Christian Bremer
 */
public class RestApiExceptionMapperImplTest {

  private static RestApiExceptionMapper mapper;

  /**
   * Setup test.
   */
  @BeforeClass
  public static void setup() {
    final RestApiExceptionMapperProperties properties = new RestApiExceptionMapperProperties();
    properties.setApiPaths(Collections.singletonList("/api/**"));
    mapper = new RestApiExceptionMapperImpl(properties, "test");
  }

  /**
   * Test get api paths.
   */
  @Test
  public void testGetApiPaths() {
    assertTrue(mapper.getApiPaths().contains("/api/**"));
  }

  /**
   * Test build 409.
   */
  @Test
  public void testBuild409() {
    final ServiceException exception = new ServiceException(409, "Either a or b", "TEST:4711");
    final RestApiException model = mapper.build(exception, "/api/something", null);
    assertNotNull(model);
    assertEquals(exception.getErrorCode(), model.getErrorCode());
    assertFalse(model.getErrorCodeInherited());
    assertEquals(exception.getMessage(), model.getMessage());
    assertEquals("/api/something", model.getPath());
    assertNull(model.getId());
  }

  /**
   * Test build 500.
   */
  @Test
  public void testBuild500() {
    final ServiceException exception = new ServiceException(500, "Something failed.", "TEST:4711");
    final RestApiException model = mapper.build(exception, "/api/something", null);
    assertNotNull(model);
    assertEquals(exception.getErrorCode(), model.getErrorCode());
    assertFalse(model.getErrorCodeInherited());
    assertEquals(exception.getMessage(), model.getMessage());
    assertEquals("/api/something", model.getPath());
    assertNotNull(model.getId());
  }

  /**
   * Test build with default exception mapping.
   */
  @Test
  public void testBuildWithDefaultExceptionMapping() {
    final RuntimeException exception = new RuntimeException("Something went wrong");
    final RestApiException model = mapper.build(
        exception, "/api/something", null);
    assertNotNull(model);
    assertNull(model.getErrorCode());
    assertFalse(model.getErrorCodeInherited());
    assertEquals(exception.getMessage(), model.getMessage());
    assertEquals("/api/something", model.getPath());
    assertNotNull(model.getId());
  }

  /**
   * Test build with default exception mapping and illegal argument exception.
   */
  @Test
  public void testBuildWithDefaultExceptionMappingAndIllegalArgumentException() {
    final IllegalArgumentException exception = new IllegalArgumentException();
    final RestApiException model = mapper.build(exception, "/api/illegal", null);
    assertNotNull(model);
    assertNull(model.getErrorCode());
    assertFalse(model.getErrorCodeInherited());
    assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), model.getMessage());
    assertEquals("/api/illegal", model.getPath());
    assertNull(model.getId());
    assertEquals(IllegalArgumentException.class.getName(), model.getClassName());
  }

  /**
   * Test build with configured exception mapping.
   */
  @Test
  public void testBuildWithConfiguredExceptionMapping() {
    final RestApiExceptionMapperProperties properties = new RestApiExceptionMapperProperties();
    properties.setApiPaths(Collections.singletonList("/null-api/**"));
    properties.getExceptionMappings().add(new ExceptionMapping(
        NullPointerException.class.getName(),
        503,
        "A variable is null.",
        "NULLPOINTER"));
    properties.getExceptionMappingConfigs().add(new ExceptionMappingConfig(
        NullPointerException.class.getName(),
        false,
        true,
        true,
        true,
        true,
        true,
        true));
    final RestApiExceptionMapper configuredMapper = new RestApiExceptionMapperImpl(
        properties, "configured");

    final NullPointerException exception = new NullPointerException();
    final RestApiException model = configuredMapper.build(
        exception, "/null-api/something", null);
    assertNotNull(model);
    assertEquals("NULLPOINTER", model.getErrorCode());
    assertFalse(model.getErrorCodeInherited());
    assertEquals("A variable is null.", model.getMessage());
    assertEquals("/null-api/something", model.getPath());
    assertNotNull(model.getId());
    assertNull(model.getClassName());
  }

}
