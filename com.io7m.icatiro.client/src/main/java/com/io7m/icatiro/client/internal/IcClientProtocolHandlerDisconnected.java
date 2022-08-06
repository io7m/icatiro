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

package com.io7m.icatiro.client.internal;

import com.io7m.icatiro.client.api.IcClientException;
import com.io7m.icatiro.model.IcUser;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.Objects;

/**
 * The "disconnected" protocol handler.
 */

public final class IcClientProtocolHandlerDisconnected
  implements IcClientProtocolHandlerType
{
  private final HttpClient httpClient;
  private final IcStrings strings;

  /**
   * The "disconnected" protocol handler.
   *
   * @param inStrings    The string resources
   * @param inHttpClient The HTTP client
   */

  public IcClientProtocolHandlerDisconnected(
    final IcStrings inStrings,
    final HttpClient inHttpClient)
  {
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.httpClient =
      Objects.requireNonNull(inHttpClient, "httpClient");
  }

  @Override
  public IcClientProtocolHandlerType login(
    final String user,
    final String password,
    final URI base)
    throws IcClientException, InterruptedException
  {
    return IcProtocolNegotiation.negotiateProtocolHandler(
      this.httpClient,
      this.strings,
      user,
      password,
      base
    );
  }

  private IcClientException notLoggedIn()
  {
    return new IcClientException(
      this.strings.format("notLoggedIn")
    );
  }

  @Override
  public IcUser userSelf()
    throws IcClientException
  {
    throw this.notLoggedIn();
  }
}
