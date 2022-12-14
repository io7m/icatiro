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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.io7m.idstore.model.IdEmail;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Configuration for the part of the server that sends mail.
 *
 * @param transportConfiguration      The transport configuration
 * @param authenticationConfiguration The authentication configuration
 * @param senderAddress               The sender address
 * @param verificationExpiration      The maximum age of email verifications
 */

@JsonDeserialize
@JsonSerialize
public record IcServerMailConfiguration(
  @JsonProperty(value = "Transport", required = true)
  IcServerMailTransportConfigurationType transportConfiguration,
  @JsonProperty(value = "Authentication")
  @JsonInclude(JsonInclude.Include.NON_ABSENT)
  Optional<IcServerMailAuthenticationConfiguration> authenticationConfiguration,
  @JsonProperty(value = "Sender", required = true)
  String senderAddress,
  @JsonProperty(value = "VerificationExpiration", required = true)
  Duration verificationExpiration)
  implements IcServerJSONConfigurationElementType
{
  /**
   * Configuration for the part of the server that sends mail.
   *
   * @param transportConfiguration      The transport configuration
   * @param authenticationConfiguration The authentication configuration
   * @param senderAddress               The sender address
   * @param verificationExpiration      The maximum age of email verifications
   */

  public IcServerMailConfiguration
  {
    Objects.requireNonNull(
      transportConfiguration, "transportConfiguration");
    Objects.requireNonNull(
      authenticationConfiguration, "authenticationConfiguration");
    Objects.requireNonNull(
      senderAddress, "senderAddress");
    Objects.requireNonNull(
      verificationExpiration, "verificationExpiration");

    new IdEmail(senderAddress);
  }
}
