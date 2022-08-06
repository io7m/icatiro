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
import com.io7m.icatiro.model.IcPasswordException;
import com.io7m.icatiro.model.IcUser;
import com.io7m.icatiro.model.IcUserDisplayName;
import com.io7m.icatiro.model.IcUserEmail;
import com.io7m.icatiro.protocol.api.IcProtocolFromModel;
import com.io7m.icatiro.protocol.api.IcProtocolToModel;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Information for a single user.
 *
 * @param id            The user's ID
 * @param name          The user's name
 * @param email         The user's email
 * @param password      The user's password
 * @param created       The date the user was created
 * @param lastLoginTime The date the user last logged in
 */

public record Ic1User(
  @JsonProperty(value = "ID", required = true)
  UUID id,
  @JsonProperty(value = "Name", required = true)
  String name,
  @JsonProperty(value = "Email", required = true)
  String email,
  @JsonProperty(value = "Created", required = true)
  OffsetDateTime created,
  @JsonProperty(value = "LastLogin", required = true)
  OffsetDateTime lastLoginTime,
  @JsonProperty(value = "Password", required = true)
  Ic1Password password)
{
  /**
   * Information for a single user.
   *
   * @param id            The user's ID
   * @param name          The user's name
   * @param email         The user's email
   * @param password      The user's password
   * @param created       The date the user was created
   * @param lastLoginTime The date the user last logged in
   */

  public Ic1User
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(created, "created");
    Objects.requireNonNull(lastLoginTime, "lastLoginTime");
    Objects.requireNonNull(password, "password");
  }

  /**
   * Create a v1 user from the given model user.
   *
   * @param user The model user
   *
   * @return A v1 user
   *
   * @see #toUser()
   */

  @IcProtocolFromModel
  public static Ic1User ofUser(
    final IcUser user)
  {
    Objects.requireNonNull(user, "user");
    return new Ic1User(
      user.id(),
      user.name().value(),
      user.email().value(),
      user.created(),
      user.lastLoginTime(),
      Ic1Password.ofPassword(user.password())
    );
  }

  /**
   * Convert this to a model user.
   *
   * @return This as a model user
   *
   * @throws IcPasswordException On password errors
   * @see #ofUser(IcUser)
   */

  @IcProtocolToModel
  public IcUser toUser()
    throws IcPasswordException
  {
    return new IcUser(
      this.id,
      new IcUserDisplayName(this.name),
      new IcUserEmail(this.email),
      this.created,
      this.lastLoginTime,
      this.password.toPassword()
    );
  }
}
