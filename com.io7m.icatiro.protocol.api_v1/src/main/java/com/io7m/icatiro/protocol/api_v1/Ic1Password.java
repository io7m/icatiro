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

package com.io7m.icatiro.protocol.api_v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.io7m.icatiro.model.IcPassword;
import com.io7m.icatiro.model.IcPasswordAlgorithms;
import com.io7m.icatiro.model.IcPasswordException;
import com.io7m.icatiro.protocol.api.IcProtocolFromModel;
import com.io7m.icatiro.protocol.api.IcProtocolToModel;

import java.util.Objects;

/**
 * Information for a password.
 *
 * @param algorithm The password algorithm
 * @param hash      The password hash
 * @param salt      The password salt
 */

@JsonDeserialize
@JsonSerialize
public record Ic1Password(
  @JsonProperty(value = "Algorithm", required = true)
  String algorithm,
  @JsonProperty(value = "Hash", required = true)
  String hash,
  @JsonProperty(value = "Salt", required = true)
  String salt)
{
  /**
   * Information for a single user.
   *
   * @param algorithm The password algorithm
   * @param hash      The password hash
   * @param salt      The password salt
   */

  public Ic1Password
  {
    Objects.requireNonNull(algorithm, "algorithm");
    Objects.requireNonNull(hash, "hash");
    Objects.requireNonNull(salt, "salt");
  }

  /**
   * Convert this to a model password.
   *
   * @return The model password
   *
   * @throws IcPasswordException On password errors
   */

  @IcProtocolToModel
  public IcPassword toPassword()
    throws IcPasswordException
  {
    return new IcPassword(
      IcPasswordAlgorithms.parse(this.algorithm),
      this.hash,
      this.salt
    );
  }

  /**
   * Convert a model password to a V1 password.
   *
   * @param password The model password
   *
   * @return A password
   */

  @IcProtocolFromModel
  public static Ic1Password ofPassword(
    final IcPassword password)
  {
    return new Ic1Password(
      password.algorithm().identifier(),
      password.hash(),
      password.salt()
    );
  }
}
