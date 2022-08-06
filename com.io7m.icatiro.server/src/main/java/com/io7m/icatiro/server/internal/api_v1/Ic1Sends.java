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


package com.io7m.icatiro.server.internal.api_v1;

import com.io7m.icatiro.error_codes.IcErrorCode;
import com.io7m.icatiro.protocol.api.IcProtocolException;
import com.io7m.icatiro.protocol.api_v1.Ic1MessageType;
import com.io7m.icatiro.protocol.api_v1.Ic1Messages;
import com.io7m.icatiro.protocol.api_v1.Ic1ResponseError;
import com.io7m.icatiro.services.api.IcServiceType;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

/**
 * Convenient functions to send messages.
 */

public final class Ic1Sends implements IcServiceType
{
  private final Ic1Messages messages;

  /**
   * Convenient functions to send messages.
   *
   * @param inMessages A message codec
   */

  public Ic1Sends(
    final Ic1Messages inMessages)
  {
    this.messages = Objects.requireNonNull(inMessages, "messages");
  }

  /**
   * Send an error message.
   *
   * @param response   The servlet response
   * @param requestId  The request ID
   * @param statusCode The HTTP status code
   * @param errorCode  The error code
   * @param message    The message
   *
   * @throws IOException On errors
   */

  public void sendError(
    final HttpServletResponse response,
    final UUID requestId,
    final int statusCode,
    final IcErrorCode errorCode,
    final String message)
    throws IOException
  {
    this.send(
      response,
      statusCode,
      new Ic1ResponseError(requestId, errorCode.id(), message)
    );
  }

  /**
   * Send a message.
   *
   * @param response   The servlet response
   * @param statusCode The HTTP status code
   * @param message    The message
   *
   * @throws IOException On errors
   */

  public void send(
    final HttpServletResponse response,
    final int statusCode,
    final Ic1MessageType message)
    throws IOException
  {
    response.setStatus(statusCode);
    response.setContentType(Ic1Messages.CONTENT_TYPE);

    try {
      final var data = this.messages.serialize(message);
      response.setContentLength(data.length + 2);
      try (var output = response.getOutputStream()) {
        output.write(data);
        output.write('\r');
        output.write('\n');
      }
    } catch (final IcProtocolException e) {
      throw new IOException(e);
    }
  }

  @Override
  public String description()
  {
    return "Public errors service.";
  }

  @Override
  public String toString()
  {
    return "[Ic1Sends 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode()));
  }
}
