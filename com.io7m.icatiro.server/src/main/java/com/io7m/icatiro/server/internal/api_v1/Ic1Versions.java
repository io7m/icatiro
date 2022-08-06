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

import com.io7m.icatiro.protocol.api.IcProtocolException;
import com.io7m.icatiro.protocol.api_v1.Ic1Messages;
import com.io7m.icatiro.protocol.versions.IcVMessages;
import com.io7m.icatiro.protocol.versions.IcVProtocolSupported;
import com.io7m.icatiro.protocol.versions.IcVProtocols;
import com.io7m.icatiro.services.api.IcServiceDirectoryType;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * A versioning servlet.
 */

public final class Ic1Versions extends HttpServlet
{
  private static final IcVProtocols PROTOCOLS =
    createProtocols();

  private final IcVMessages messages;

  /**
   * A versioning servlet.
   *
   * @param inServices The service directory
   */

  public Ic1Versions(
    final IcServiceDirectoryType inServices)
  {
    this.messages =
      inServices.requireService(IcVMessages.class);
  }

  private static IcVProtocols createProtocols()
  {
    final var supported = new ArrayList<IcVProtocolSupported>();
    supported.add(
      new IcVProtocolSupported(
        Ic1Messages.schemaId(),
        BigInteger.ONE,
        BigInteger.ZERO,
        "/api/1/0/"
      )
    );
    return new IcVProtocols(List.copyOf(supported));
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws IOException
  {
    try {
      response.setContentType(IcVMessages.contentType());
      response.setStatus(200);

      final var data = this.messages.serialize(PROTOCOLS);
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
}
