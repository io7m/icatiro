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


package com.io7m.icatiro.server.internal.tickets_v1;

import com.io7m.icatiro.error_codes.IcErrorCode;
import com.io7m.icatiro.protocol.IcProtocolException;
import com.io7m.icatiro.protocol.tickets.IcTMessageType;
import com.io7m.icatiro.protocol.tickets.IcTResponseError;
import com.io7m.icatiro.protocol.tickets.cb.IcT1Messages;
import com.io7m.icatiro.services.api.IcServiceType;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

/**
 * Convenient functions to send messages.
 */

public final class IcT1Sends implements IcServiceType
{
  private final IcT1Messages messages;

  /**
   * Convenient functions to send messages.
   *
   * @param inMessages A message codec
   */

  public IcT1Sends(
    final IcT1Messages inMessages)
  {
    this.messages = Objects.requireNonNull(inMessages, "messages");
  }

  /**
   * Send an error message.
   *
   * @param response   The servlet response
   * @param requestIc  The request ID
   * @param statusCode The HTTP status code
   * @param errorCode  The error code
   * @param message    The message
   *
   * @throws IOException On errors
   */

  public void sendError(
    final HttpServletResponse response,
    final UUID requestIc,
    final int statusCode,
    final IcErrorCode errorCode,
    final String message)
    throws IOException
  {
    this.send(
      response,
      statusCode,
      new IcTResponseError(requestIc, errorCode.id(), message)
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
    final IcTMessageType message)
    throws IOException
  {
    response.setStatus(statusCode);
    response.setContentType(IcT1Messages.contentType());

    try {
      final var data = this.messages.serialize(message);
      response.setContentLength(data.length);
      try (var output = response.getOutputStream()) {
        output.write(data);
      }
    } catch (final IcProtocolException e) {
      throw new IOException(e);
    }
  }

  @Override
  public String description()
  {
    return "Tickets message sending service.";
  }

  @Override
  public String toString()
  {
    return "[IcT1Sends 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }
}
