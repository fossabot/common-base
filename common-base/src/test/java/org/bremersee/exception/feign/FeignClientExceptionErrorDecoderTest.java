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

package org.bremersee.exception.feign;

import static org.bremersee.http.converter.ObjectMapperHelper.getJsonMapper;
import static org.bremersee.http.converter.ObjectMapperHelper.getXmlMapper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import feign.Request;
import feign.Request.HttpMethod;
import feign.Response;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.bremersee.TestHelper;
import org.bremersee.exception.model.RestApiException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * The feign client exception error decoder test.
 *
 * @author Christian Bremer
 */
public class FeignClientExceptionErrorDecoderTest {

  private static final FeignClientExceptionErrorDecoder decoder
      = new FeignClientExceptionErrorDecoder();

  /**
   * Test decode json.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDecodeJson() throws Exception {
    final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    final RestApiException expected = TestHelper.restApiException();
    //noinspection unchecked
    final Response response = Response
        .builder()
        .request(Request
            .create(
                HttpMethod.GET,
                "http://example.org",
                new HashMap<>(),
                null,
                StandardCharsets.UTF_8))
        .body(getJsonMapper().writeValueAsBytes(expected))
        .headers((Map) headers)
        .reason("Something bad")
        .status(500)
        .build();
    final Exception actual = decoder.decode("getSomething", response);
    assertNotNull(actual);
    assertTrue(actual instanceof FeignClientException);
    assertEquals(500, ((FeignClientException) actual).status());
    assertEquals(expected, ((FeignClientException) actual).getRestApiException());
  }

  /**
   * Test decode xml.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDecodeXml() throws Exception {
    final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE);
    final RestApiException expected = TestHelper.restApiException();
    //noinspection unchecked
    final Response response = Response
        .builder()
        .request(Request
            .create(
                HttpMethod.GET,
                "http://example.org",
                new HashMap<>(),
                null,
                StandardCharsets.UTF_8))
        .body(getXmlMapper().writeValueAsBytes(expected))
        .headers((Map) headers)
        .reason("Nothing found")
        .status(404)
        .build();
    final Exception actual = decoder.decode("getSomethingThatNotNotExists", response);
    assertNotNull(actual);
    assertTrue(actual instanceof FeignClientException);
    assertEquals(404, ((FeignClientException) actual).status());
    assertEquals(expected, ((FeignClientException) actual).getRestApiException());
  }

  /**
   * Test decode something else.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDecodeSomethingElse() throws Exception {
    final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE);
    final String body = getXmlMapper().writeValueAsString(TestHelper.otherResponse());
    //noinspection unchecked
    final Response response = Response
        .builder()
        .request(Request
            .create(
                HttpMethod.GET,
                "http://example.org",
                new HashMap<>(),
                null,
                StandardCharsets.UTF_8))
        .body(body.getBytes(StandardCharsets.UTF_8))
        .headers((Map) headers)
        .reason("Something bad")
        .status(500)
        .build();
    final Exception actual = decoder.decode("getSomething", response);
    assertNotNull(actual);
    assertTrue(actual instanceof FeignClientException);
    assertEquals(500, ((FeignClientException) actual).status());
    assertNotNull(((FeignClientException) actual).getRestApiException());
    //noinspection ConstantConditions
    assertEquals(body, ((FeignClientException) actual).getRestApiException().getMessage());
  }

  /**
   * Test decode empty response.
   */
  @Test
  public void testDecodeEmptyResponse() {
    final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE);
    final String body = "";
    //noinspection unchecked
    final Response response = Response
        .builder()
        .request(Request
            .create(
                HttpMethod.GET,
                "http://example.org",
                new HashMap<>(),
                null,
                StandardCharsets.UTF_8))
        .body(body.getBytes(StandardCharsets.UTF_8))
        .headers((Map) headers)
        .reason("Something bad")
        .status(500)
        .build();
    final Exception actual = decoder.decode("getNothing", response);
    assertNotNull(actual);
    assertTrue(actual instanceof FeignClientException);
    assertEquals(500, ((FeignClientException) actual).status());
  }

}
