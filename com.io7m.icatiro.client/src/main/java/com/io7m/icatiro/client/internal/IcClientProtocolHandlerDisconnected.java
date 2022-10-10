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
import com.io7m.icatiro.model.IcPage;
import com.io7m.icatiro.model.IcPermissionScopedType;
import com.io7m.icatiro.model.IcProject;
import com.io7m.icatiro.model.IcProjectShortName;
import com.io7m.icatiro.model.IcProjectTitle;
import com.io7m.icatiro.model.IcTicketCreation;
import com.io7m.icatiro.model.IcTicketSearch;
import com.io7m.icatiro.model.IcTicketSummary;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.NOT_LOGGED_IN;

/**
 * The "disconnected" protocol handler.
 */

public final class IcClientProtocolHandlerDisconnected
  implements IcClientProtocolHandlerType
{
  private final HttpClient httpClient;
  private final Locale locale;
  private final IcStrings strings;
  private IcClientProtocolHandlerType handler;

  /**
   * The "disconnected" protocol handler.
   *
   * @param inLocale     The locale
   * @param inStrings    The string resources
   * @param inHttpClient The HTTP client
   */

  public IcClientProtocolHandlerDisconnected(
    final Locale inLocale,
    final IcStrings inStrings,
    final HttpClient inHttpClient)
  {
    this.locale =
      Objects.requireNonNull(inLocale, "inLocale");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.httpClient =
      Objects.requireNonNull(inHttpClient, "httpClient");
  }

  @Override
  public IcNewHandler login(
    final String user,
    final String password,
    final URI base)
    throws IcClientException, InterruptedException
  {
    final var newHandler =
      IcProtocolNegotiation.negotiateProtocolHandler(
        this.locale,
        this.httpClient,
        this.strings,
        base
      );

    return newHandler.login(user, password, base);
  }

  private IcClientException notLoggedIn()
  {
    return new IcClientException(
      NOT_LOGGED_IN,
      this.strings.format("notLoggedIn")
    );
  }

  @Override
  public IcTicketSummary ticketCreate(
    final IcTicketCreation create)
    throws IcClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IcPage<IcTicketSummary> ticketSearchBegin(
    final IcTicketSearch parameters)
    throws IcClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IcPage<IcTicketSummary> ticketSearchNext()
    throws IcClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IcPage<IcTicketSummary> ticketSearchPrevious()
    throws IcClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public IcProject projectCreate(
    final IcProjectShortName shortName,
    final IcProjectTitle title)
    throws IcClientException
  {
    throw this.notLoggedIn();
  }

  @Override
  public void permissionGrant(
    final UUID targetUser,
    final IcPermissionScopedType permission)
    throws IcClientException
  {
    throw this.notLoggedIn();
  }
}
