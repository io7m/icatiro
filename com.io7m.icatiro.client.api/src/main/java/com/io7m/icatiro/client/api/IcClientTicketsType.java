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

package com.io7m.icatiro.client.api;

import com.io7m.icatiro.model.IcPage;
import com.io7m.icatiro.model.IcTicket;
import com.io7m.icatiro.model.IcTicketComment;
import com.io7m.icatiro.model.IcTicketCommentCreation;
import com.io7m.icatiro.model.IcTicketCreation;
import com.io7m.icatiro.model.IcTicketID;
import com.io7m.icatiro.model.IcTicketSearch;
import com.io7m.icatiro.model.IcTicketSummary;

/**
 * Method related to tickets.
 */

public interface IcClientTicketsType
{
  /**
   * Create a ticket.
   *
   * @param create The ticket creation info
   *
   * @return The ticket
   *
   * @throws IcClientException    On errors
   * @throws InterruptedException On interruption
   */

  IcTicketSummary ticketCreate(IcTicketCreation create)
    throws IcClientException, InterruptedException;

  /**
   * Start searching for tickets.
   *
   * @param parameters The search parameters
   *
   * @return The first page of results
   *
   * @throws IcClientException    On errors
   * @throws InterruptedException On interruption
   */

  IcPage<IcTicketSummary> ticketSearchBegin(
    IcTicketSearch parameters)
    throws IcClientException, InterruptedException;

  /**
   * Get the next page of search results.
   *
   * @return The page of results
   *
   * @throws IcClientException    On errors
   * @throws InterruptedException On interruption
   */

  IcPage<IcTicketSummary> ticketSearchNext()
    throws IcClientException, InterruptedException;

  /**
   * Get the previous page of search results.
   *
   * @return The page of results
   *
   * @throws IcClientException    On errors
   * @throws InterruptedException On interruption
   */

  IcPage<IcTicketSummary> ticketSearchPrevious()
    throws IcClientException, InterruptedException;

  /**
   * Create a ticket comment.
   *
   * @param create The ticket comment creation info
   *
   * @return The ticket
   *
   * @throws IcClientException    On errors
   * @throws InterruptedException On interruption
   */

  IcTicketComment ticketCommentCreate(IcTicketCommentCreation create)
    throws IcClientException, InterruptedException;

  /**
   * Retrieve a ticket.
   *
   * @param id The ticket id
   *
   * @return The ticket
   *
   * @throws IcClientException    On errors
   * @throws InterruptedException On interruption
   */

  IcTicket ticketGet(IcTicketID id)
    throws IcClientException, InterruptedException;
}
