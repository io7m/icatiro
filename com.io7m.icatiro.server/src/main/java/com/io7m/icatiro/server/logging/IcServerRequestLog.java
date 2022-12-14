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

package com.io7m.icatiro.server.logging;

import com.io7m.icatiro.server.internal.IcServerClock;
import com.io7m.icatiro.services.api.IcServiceDirectoryType;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * A request log implementation.
 */

public final class IcServerRequestLog implements RequestLog
{
  private final IcServerClock clock;
  private final Logger logger;

  /**
   * A request log implementation.
   *
   * @param services The service directory
   * @param name     The log name
   */

  public IcServerRequestLog(
    final IcServiceDirectoryType services,
    final String name)
  {
    Objects.requireNonNull(services, "services");

    this.clock =
      services.requireService(IcServerClock.class);
    this.logger =
      LoggerFactory.getLogger("com.io7m.icatiro.server.requestLog." + name);
  }

  @Override
  public void log(
    final Request request,
    final Response response)
  {
    try (var ignored0 =
           IcServerMDCRequestProcessor.mdcForRequest(request)) {
      try (var ignored1 =
             IcServerMDCResponseProcessor.open(this.clock, request, response)) {
        this.logger.info("");
      }
    }
  }
}
