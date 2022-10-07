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


package com.io7m.icatiro.server.internal;

import com.io7m.icatiro.server.api.IcServerIdstoreConfiguration;
import com.io7m.icatiro.services.api.IcServiceType;
import com.io7m.idstore.user_client.IdUClients;
import com.io7m.idstore.user_client.api.IdUClientException;
import com.io7m.idstore.user_client.api.IdUClientFactoryType;
import com.io7m.idstore.user_client.api.IdUClientType;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;

/**
 * Idstore clients.
 */

public final class IcIdentityClients implements IcServiceType
{
  private final IcServerIdstoreConfiguration idstore;
  private final IdUClientFactoryType clients;
  private final Locale locale;

  private IcIdentityClients(
    final Locale inLocale,
    final IcServerIdstoreConfiguration inIdstore,
    final IdUClientFactoryType inClients)
  {
    this.locale =
      Objects.requireNonNull(inLocale, "locale");
    this.idstore =
      Objects.requireNonNull(inIdstore, "idstore");
    this.clients =
      Objects.requireNonNull(inClients, "clients");
  }

  /**
   * Create an idstore client service.
   *
   * @param inLocale The locale
   * @param idstore  The idstore server configuration
   *
   * @return A client service
   */

  public static IcIdentityClients create(
    final Locale inLocale,
    final IcServerIdstoreConfiguration idstore)
  {
    Objects.requireNonNull(inLocale, "inLocale");
    Objects.requireNonNull(idstore, "idstore");

    return new IcIdentityClients(
      inLocale,
      idstore,
      new IdUClients()
    );
  }

  /**
   * Create a new client.
   *
   * @return A new client
   *
   * @throws IdUClientException   On client errors
   * @throws InterruptedException On interruption
   */

  public IdUClientType createClient()
    throws IdUClientException, InterruptedException
  {
    return this.clients.create(this.locale);
  }

  /**
   * @return The base URI of the idstore server
   */

  public URI baseURI()
  {
    return this.idstore.baseURI();
  }

  @Override
  public String description()
  {
    return "Identity client service.";
  }

  @Override
  public String toString()
  {
    return "[IcIdentityClients 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }

  /**
   * @return The password reset URI for the idstore server
   */

  public URI passwordResetURI()
  {
    return this.idstore.passwordResetURI();
  }
}
