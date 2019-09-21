/*
 * Copyright 2019 the original author or authors.
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

package org.bremersee.web.reactive.function.client.proxy;

import static org.bremersee.web.reactive.function.client.proxy.InvocationUtils.findRequestMappingValue;
import static org.bremersee.web.reactive.function.client.proxy.InvocationUtils.putToMultiValueMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * @author Christian Bremer
 */
public interface RequestHeadersBuilder {

  void addHeaders(InvocationParameters parameters, HttpHeaders headers);

  default void build(InvocationParameters parameters, HttpHeaders headers) {
    addAccepts(parameters, headers);
    addContentType(parameters, headers);
    addHeaders(parameters, headers);
  }

  default void addAccepts(final InvocationParameters parameters, final HttpHeaders headers) {
    final Method method = parameters.getMethod();
    findRequestMappingValue(
        method,
        mapping -> mapping.produces().length > 0,
        mapping -> mapping.produces()[0])
        .ifPresent(accepts -> headers.set(HttpHeaders.ACCEPT, accepts));
  }

  default void addContentType(final InvocationParameters parameters, final HttpHeaders headers) {
    final Method method = parameters.getMethod();
    findRequestMappingValue(
        method,
        mapping -> mapping.consumes().length > 0,
        mapping -> mapping.consumes()[0])
        .ifPresent(accepts -> headers.set(HttpHeaders.CONTENT_TYPE, accepts));
  }

  static RequestHeadersBuilder defaultBuilder() {
    return new Default();
  }

  class Default implements RequestHeadersBuilder {

    @Override
    public void addHeaders(final InvocationParameters parameters, final HttpHeaders headers) {
      final Method method = parameters.getMethod();
      final Object[] args = parameters.getArgs();
      final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
      for (int i = 0; i < parameterAnnotations.length; i++) {
        for (final Annotation annotation : parameterAnnotations[i]) {
          if (annotation instanceof RequestHeader) {
            final RequestHeader param = (RequestHeader) annotation;
            final String name = param.name();
            final Object value = args[i];
            putToMultiValueMap(name, value, headers);
          }
        }
      }
    }
  }

}
