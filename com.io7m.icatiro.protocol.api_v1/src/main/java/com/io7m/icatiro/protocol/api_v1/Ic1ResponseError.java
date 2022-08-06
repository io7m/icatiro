/*
 * Copyright © 2022 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.icatiro.protocol.api_v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Objects;
import java.util.UUID;

/**
 * An error response.
 *
 * @param requestId The request ID
 * @param errorCode The error code
 * @param message   The humanly-readable error message
 */

@JsonDeserialize
@JsonSerialize
public record Ic1ResponseError(
  @JsonProperty(value = "RequestID", required = true)
  UUID requestId,
  @JsonProperty(value = "ErrorCode", required = true)
  String errorCode,
  @JsonProperty(value = "Message", required = true)
  String message)
  implements Ic1ResponseType
{
  /**
   * An error response.
   *
   * @param requestId The request ID
   * @param errorCode The error code
   * @param message   The humanly-readable error message
   */

  @JsonCreator
  public Ic1ResponseError
  {
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(errorCode, "errorCode");
    Objects.requireNonNull(message, "message");
  }
}
