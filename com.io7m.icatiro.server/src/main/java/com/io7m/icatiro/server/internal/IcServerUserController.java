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

import com.io7m.icatiro.database.api.IcDatabaseTicketListPaging;
import com.io7m.icatiro.database.api.IcDatabaseTicketListPagingType;
import com.io7m.icatiro.model.IcTicketListParameters;

import java.util.Objects;
import java.util.UUID;

/**
 * A controller for a single user session.
 */

public final class IcServerUserController
{
  private final UUID userId;
  private final String sessionId;
  private IcTicketListParameters ticketListParameters;
  private IcDatabaseTicketListPagingType ticketPaging;

  /**
   * A controller for a single user session.
   *
   * @param inUserId    The user ID
   * @param inSessionId The session ID
   */

  public IcServerUserController(
    final UUID inUserId,
    final String inSessionId)
  {
    this.userId =
      Objects.requireNonNull(inUserId, "userId");
    this.sessionId =
      Objects.requireNonNull(inSessionId, "sessionId");
    this.ticketListParameters =
      IcTicketListParameters.defaults();
    this.ticketPaging =
      IcDatabaseTicketListPaging.create(this.userId, this.ticketListParameters);
  }

  /**
   * @return The most recent ticket list parameters
   */

  public IcTicketListParameters ticketListParameters()
  {
    return this.ticketListParameters;
  }

  /**
   * @return The most recent ticket paging handler
   */

  public IcDatabaseTicketListPagingType ticketPaging()
  {
    return this.ticketPaging;
  }

  /**
   * Set the ticket listing parameters.
   *
   * @param ticketParameters The ticket parameters
   */

  public void setTicketListParameters(
    final IcTicketListParameters ticketParameters)
  {
    this.ticketListParameters =
      Objects.requireNonNull(ticketParameters, "ticketParameters");

    if (!Objects.equals(this.ticketPaging.pageParameters(), ticketParameters)) {
      this.ticketPaging =
        IcDatabaseTicketListPaging.create(this.userId, ticketParameters);
    }
  }
}
