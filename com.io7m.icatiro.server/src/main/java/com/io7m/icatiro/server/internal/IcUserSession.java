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

import com.io7m.icatiro.database.api.IcDatabaseTicketSearchType;
import com.io7m.icatiro.model.IcUser;
import com.io7m.idstore.user_client.api.IdUClientType;
import com.io7m.jaffirm.core.Preconditions;
import jakarta.servlet.http.HttpSession;

import java.util.Objects;
import java.util.Optional;

/**
 * A controller for a single user session.
 */

public final class IcUserSession
  implements AutoCloseable
{
  private final HttpSession httpSession;
  private final IdUClientType idClient;
  private final IcUser user;
  private Optional<IcDatabaseTicketSearchType> tickets;

  /**
   * A controller for a single user session.
   *
   * @param inUser        The user
   * @param inHttpSession The HTTP session
   * @param inIdClient    The ID client
   */

  public IcUserSession(
    final IcUser inUser,
    final HttpSession inHttpSession,
    final IdUClientType inIdClient)
  {
    this.user =
      Objects.requireNonNull(inUser, "inUser");
    this.httpSession =
      Objects.requireNonNull(inHttpSession, "inHttpSession");
    this.idClient =
      Objects.requireNonNull(inIdClient, "inIdClient");
    this.tickets =
      Optional.empty();
  }

  /**
   * @return The ticket paging object
   */

  public Optional<IcDatabaseTicketSearchType> ticketSearch()
  {
    return this.tickets;
  }

  @Override
  public void close()
    throws Exception
  {
    this.idClient.close();
  }

  /**
   * Set the new ticket search.
   *
   * @param search The ticket search
   */

  public void setTicketParameters(
    final IcDatabaseTicketSearchType search)
  {
    this.tickets =
      Optional.of(Objects.requireNonNull(search, "search"));
  }

  /**
   * @return The user
   */

  public IcUser user()
  {
    return this.user;
  }

  /**
   * Update the user.
   *
   * @param inUser The user
   */

  public void setUser(
    final IcUser inUser)
  {
    Preconditions.checkPreconditionV(
      Objects.equals(this.user.id(), inUser.id()),
      "Session user ID must match."
    );
  }
}
