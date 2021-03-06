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

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;

/**
 * Authentication exception that is thrown by the client that do the OAuth2 Password Flow.
 *
 * @author Christian Bremer
 */
public class PasswordFlowAuthenticationException extends AuthenticationException
    implements HttpStatusAware {

  private HttpStatus httpStatus;

  /**
   * Instantiates a new password flow authentication exception.
   *
   * @param httpStatus the http status
   * @param body       the body
   */
  public PasswordFlowAuthenticationException(final HttpStatus httpStatus, final String body) {
    super(buildMessage(httpStatus, body));
    this.httpStatus = httpStatus;
  }

  @Override
  public int status() {
    return httpStatus != null ? httpStatus.value() : HttpStatus.UNAUTHORIZED.value();
  }

  private static String buildMessage(final HttpStatus httpStatus, final String body) {
    if ((httpStatus == null || !StringUtils.hasText(httpStatus.getReasonPhrase()))
        && !StringUtils.hasText(body)) {
      return RestApiExceptionUtils.NO_MESSAGE_VALUE;
    }
    if (httpStatus == null) {
      return body;
    }
    final StringBuilder sb = new StringBuilder();
    sb.append("Server returned status code ").append(httpStatus.value());
    if (StringUtils.hasText(httpStatus.getReasonPhrase())) {
      sb.append(" (").append(httpStatus.getReasonPhrase()).append(")");
    }
    if (StringUtils.hasText(body)) {
      sb.append(": ").append(body);
    }
    return sb.toString();
  }
}
