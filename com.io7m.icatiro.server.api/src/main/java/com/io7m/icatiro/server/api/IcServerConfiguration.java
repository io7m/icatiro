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

package com.io7m.icatiro.server.api;

import com.io7m.icatiro.database.api.IcDatabaseConfiguration;
import com.io7m.icatiro.database.api.IcDatabaseFactoryType;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * The configuration for a server.
 *
 * @param clock                 The clock
 * @param databaseConfiguration The database configuration for the server
 * @param databases             The factory of databases that will be used for
 *                              the server
 * @param locale                The locale
 * @param userApiAddress        The user API address
 * @param userViewAddress       The user view address
 * @param mailConfiguration     The mail server configuration
 * @param branding              The branding configuration
 * @param history               The history configuration
 * @param openTelemetry         The OpenTelemetry configuration
 * @param rateLimit             The rate limiting configuration
 * @param idstore               The idstore configuration
 */

public record IcServerConfiguration(
  Locale locale,
  Clock clock,
  IcDatabaseFactoryType databases,
  IcDatabaseConfiguration databaseConfiguration,
  IcServerMailConfiguration mailConfiguration,
  IcServerHTTPServiceConfiguration userApiAddress,
  IcServerHTTPServiceConfiguration userViewAddress,
  IcServerBrandingConfiguration branding,
  IcServerHistoryConfiguration history,
  IcServerRateLimitConfiguration rateLimit,
  Optional<IcServerOpenTelemetryConfiguration> openTelemetry,
  IcServerIdstoreConfiguration idstore)
{
  /**
   * The configuration for a server.
   *
   * @param clock                 The clock
   * @param databaseConfiguration The database configuration for the server
   * @param databases             The factory of databases that will be used for
   *                              the server
   * @param locale                The locale
   * @param userApiAddress        The user API address
   * @param userViewAddress       The user view address
   * @param mailConfiguration     The mail server configuration
   * @param branding              The branding configuration
   * @param history               The history configuration
   * @param openTelemetry         The OpenTelemetry configuration
   * @param rateLimit             The rate limiting configuration
   * @param idstore               The idstore configuration
   */

  public IcServerConfiguration
  {
    Objects.requireNonNull(clock, "clock");
    Objects.requireNonNull(databaseConfiguration, "databaseConfiguration");
    Objects.requireNonNull(databases, "databases");
    Objects.requireNonNull(locale, "locale");
    Objects.requireNonNull(mailConfiguration, "mailConfiguration");
    Objects.requireNonNull(userApiAddress, "userApiAddress");
    Objects.requireNonNull(userViewAddress, "userViewAddress");
    Objects.requireNonNull(branding, "branding");
    Objects.requireNonNull(history, "history");
    Objects.requireNonNull(openTelemetry, "openTelemetry");
    Objects.requireNonNull(rateLimit, "rateLimit");
    Objects.requireNonNull(idstore, "idstore");
  }

  /**
   * @return The current time based on the configuration's clock
   */

  public OffsetDateTime now()
  {
    return OffsetDateTime.now(this.clock)
      .withNano(0);
  }
}
