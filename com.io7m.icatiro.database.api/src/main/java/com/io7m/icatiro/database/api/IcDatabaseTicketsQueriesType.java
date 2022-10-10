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

import com.io7m.icatiro.model.IcTicket;
import com.io7m.icatiro.model.IcTicketComment;
import com.io7m.icatiro.model.IcTicketCommentCreation;
import com.io7m.icatiro.model.IcTicketCreation;
import com.io7m.icatiro.model.IcTicketID;
import com.io7m.icatiro.model.IcTicketSearch;
import com.io7m.icatiro.model.IcTicketSummary;

import java.util.Optional;

/**
 * The database queries involving tickets.
 */

public non-sealed interface IcDatabaseTicketsQueriesType
  extends IcDatabaseQueriesType
{
  /**
   * Retrieve a ticket.
   *
   * @param id The ticket ID
   *
   * @return The ticket, if it exists
   *
   * @throws IcDatabaseException On errors
   */

  Optional<IcTicket> ticketGet(IcTicketID id)
    throws IcDatabaseException;

  /**
   * Retrieve a ticket.
   *
   * @param id The ticket ID
   *
   * @return The ticket
   *
   * @throws IcDatabaseException On errors
   */

  IcTicket ticketGetRequire(IcTicketID id)
    throws IcDatabaseException;

  /**
   * Check if a ticket exists.
   *
   * @param id The ticket ID
   *
   * @return {@code true} if the ticket exists
   *
   * @throws IcDatabaseException On errors
   */

  boolean ticketExists(IcTicketID id)
    throws IcDatabaseException;

  /**
   * Search for tickets.
   *
   * @param parameters The search parameters
   *
   * @return The ticket summaries as a paginated query
   *
   * @throws IcDatabaseException On errors
   */

  @IcDatabaseRequiresUser
  IcDatabaseTicketSearchType ticketSearch(
    IcTicketSearch parameters)
    throws IcDatabaseException;

  /**
   * Create a new ticket.
   *
   * @param creation The creation information
   *
   * @return The new ticket
   *
   * @throws IcDatabaseException On errors
   */

  @IcDatabaseRequiresUser
  IcTicketSummary ticketCreate(
    IcTicketCreation creation)
    throws IcDatabaseException;

  /**
   * Create a new ticket comment.
   *
   * @param creation The creation information
   *
   * @return The new ticket comment
   *
   * @throws IcDatabaseException On errors
   */

  @IcDatabaseRequiresUser
  IcTicketComment ticketCommentCreate(
    IcTicketCommentCreation creation)
    throws IcDatabaseException;
}
