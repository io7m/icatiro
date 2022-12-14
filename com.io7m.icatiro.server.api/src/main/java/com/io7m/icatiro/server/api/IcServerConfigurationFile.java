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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Objects;
import java.util.Optional;

/**
 * The server configuration file.
 *
 * @param brandingConfiguration The branding configuration
 * @param mailConfiguration     The mail configuration
 * @param httpConfiguration     The HTTP configuration
 * @param databaseConfiguration The database configuration
 * @param historyConfiguration  The history configuration
 * @param openTelemetry         The OpenTelemetry configuration
 * @param rateLimit             The rate limiting configuration
 * @param idstore               The idstore configuration
 */

@JsonDeserialize
@JsonSerialize
public record IcServerConfigurationFile(
  @JsonProperty(value = "Branding", required = true)
  IcServerBrandingConfiguration brandingConfiguration,
  @JsonProperty(value = "Mail", required = true)
  IcServerMailConfiguration mailConfiguration,
  @JsonProperty(value = "HTTP", required = true)
  IcServerHTTPConfiguration httpConfiguration,
  @JsonProperty(value = "Database", required = true)
  IcServerDatabaseConfiguration databaseConfiguration,
  @JsonProperty(value = "History", required = true)
  IcServerHistoryConfiguration historyConfiguration,
  @JsonProperty(value = "RateLimiting", required = true)
  IcServerRateLimitConfiguration rateLimit,
  @JsonInclude(JsonInclude.Include.NON_ABSENT)
  @JsonProperty(value = "OpenTelemetry")
  Optional<IcServerOpenTelemetryConfiguration> openTelemetry,
  @JsonProperty(value = "Idstore", required = true)
  IcServerIdstoreConfiguration idstore)
  implements IcServerJSONConfigurationElementType
{
  /**
   * The server configuration file.
   *
   * @param brandingConfiguration The branding configuration
   * @param mailConfiguration     The mail configuration
   * @param httpConfiguration     The HTTP configuration
   * @param databaseConfiguration The database configuration
   * @param historyConfiguration  The history configuration
   * @param openTelemetry         The OpenTelemetry configuration
   * @param rateLimit             The rate limiting configuration
   * @param idstore               The idstore configuration
   */

  @JsonCreator
  public IcServerConfigurationFile
  {
    Objects.requireNonNull(brandingConfiguration, "brandingConfiguration");
    Objects.requireNonNull(mailConfiguration, "mailConfiguration");
    Objects.requireNonNull(httpConfiguration, "httpConfiguration");
    Objects.requireNonNull(databaseConfiguration, "databaseConfiguration");
    Objects.requireNonNull(historyConfiguration, "historyConfiguration");
    Objects.requireNonNull(rateLimit, "rateLimit");
    Objects.requireNonNull(openTelemetry, "openTelemetry");
    Objects.requireNonNull(idstore, "idstore");
  }
}
