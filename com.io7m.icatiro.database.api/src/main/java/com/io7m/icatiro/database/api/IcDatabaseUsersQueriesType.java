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

package com.io7m.icatiro.database.api;



import com.io7m.icatiro.model.IcPassword;
import com.io7m.icatiro.model.IcUser;
import com.io7m.icatiro.model.IcUserDisplayName;
import com.io7m.icatiro.model.IcUserEmail;
import com.io7m.icatiro.model.IcUserSummary;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The database queries involving users.
 */

public non-sealed interface IcDatabaseUsersQueriesType
  extends IcDatabaseQueriesType
{
  /**
   * Create the initial user.
   *
   * @param id       The user ID
   * @param created  The creation time
   * @param userName The user name
   * @param email    The user email
   * @param password The hashed password
   *
   * @return The created user
   *
   * @throws IcDatabaseException On errors
   */

  IcUser userCreateInitial(
    UUID id,
    IcUserDisplayName userName,
    IcUserEmail email,
    OffsetDateTime created,
    IcPassword password)
    throws IcDatabaseException;

  /**
   * Create a user.
   *
   * @param userName The user name
   * @param email    The user email
   * @param password The hashed password
   *
   * @return The created user
   *
   * @throws IcDatabaseException On errors
   */

  @IcDatabaseRequiresUser
  default IcUser userCreate(
    final IcUserDisplayName userName,
    final IcUserEmail email,
    final IcPassword password)
    throws IcDatabaseException
  {
    return this.userCreate(
      UUID.randomUUID(),
      userName,
      email,
      OffsetDateTime.now(),
      password
    );
  }

  /**
   * Create a user.
   *
   * @param id       The user ID
   * @param created  The creation time
   * @param userName The user name
   * @param email    The user email
   * @param password The hashed password
   *
   * @return The created user
   *
   * @throws IcDatabaseException On errors
   */

  @IcDatabaseRequiresUser
  IcUser userCreate(
    UUID id,
    IcUserDisplayName userName,
    IcUserEmail email,
    OffsetDateTime created,
    IcPassword password)
    throws IcDatabaseException;

  /**
   * @param id The user ID
   *
   * @return A user with the given ID
   *
   * @throws IcDatabaseException On errors
   */

  Optional<IcUser> userGet(UUID id)
    throws IcDatabaseException;

  /**
   * @param id The user ID
   *
   * @return A user with the given ID
   *
   * @throws IcDatabaseException On errors
   */

  IcUser userGetRequire(UUID id)
    throws IcDatabaseException;

  /**
   * @param name The user name
   *
   * @return A user with the given name
   *
   * @throws IcDatabaseException On errors
   */

  Optional<IcUser> userGetForName(IcUserDisplayName name)
    throws IcDatabaseException;

  /**
   * @param name The user name
   *
   * @return A user with the given name
   *
   * @throws IcDatabaseException On errors
   */

  IcUser userGetForNameRequire(IcUserDisplayName name)
    throws IcDatabaseException;

  /**
   * @param email The user email
   *
   * @return A user with the given email
   *
   * @throws IcDatabaseException On errors
   */

  Optional<IcUser> userGetForEmail(IcUserEmail email)
    throws IcDatabaseException;

  /**
   * @param email The user email
   *
   * @return A user with the given email
   *
   * @throws IcDatabaseException On errors
   */

  IcUser userGetForEmailRequire(IcUserEmail email)
    throws IcDatabaseException;

  /**
   * Record the fact that the given user has logged in.
   *
   * @param id   The user ID
   * @param host The host from which the user logged in
   *
   * @throws IcDatabaseException On errors
   */

  void userLogin(
    UUID id,
    String host)
    throws IcDatabaseException;

  /**
   * Search for users that have an ID, email, or name that contains the given
   * string, case-insensitive.
   *
   * @param query The user query
   *
   * @return A list of matching users
   *
   * @throws IcDatabaseException On errors
   */

  List<IcUserSummary> userSearch(String query)
    throws IcDatabaseException;

  /**
   * Update the given user.
   *
   * @param id              The user ID
   * @param withDisplayName The new display name, if desired
   * @param withEmail       The new email, if desired
   * @param withPassword    The new password, if desired
   *
   * @throws IcDatabaseException On errors
   */

  @IcDatabaseRequiresUser
  void userUpdate(
    UUID id,
    Optional<IcUserDisplayName> withDisplayName,
    Optional<IcUserEmail> withEmail,
    Optional<IcPassword> withPassword)
    throws IcDatabaseException;
}
