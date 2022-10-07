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


package com.io7m.icatiro.server.internal.tickets;

import com.io7m.icatiro.database.api.IcDatabaseException;
import com.io7m.icatiro.database.api.IcDatabaseTicketsQueriesType;
import com.io7m.icatiro.model.IcPage;
import com.io7m.icatiro.model.IcValidityException;
import com.io7m.icatiro.protocol.tickets.IcTCommandTicketSearchBegin;
import com.io7m.icatiro.protocol.tickets.IcTResponseTicketSearchBegin;
import com.io7m.icatiro.protocol.tickets.IcTResponseType;

/**
 * {@code IcTCommandTicketSearchBegin}
 */

public final class IcTCmdTicketSearchBegin
  extends IcTCmdAbstract<IcTCommandTicketSearchBegin>
{
  /**
   * {@code IcTCommandTicketSearchBegin}
   */

  public IcTCmdTicketSearchBegin()
  {

  }

  @Override
  protected IcTResponseType executeActual(
    final IcTCommandContext context,
    final IcTCommandTicketSearchBegin command)
    throws IcValidityException, IcDatabaseException
  {
    final var session =
      context.userSession();
    final var tickets =
      context.transaction()
        .queries(IcDatabaseTicketsQueriesType.class);

    final var ticketPager =
      session.setTicketParameters(command.parameters());

    final var pageCurrent =
      ticketPager.pageCurrent(tickets);

    return new IcTResponseTicketSearchBegin(
      context.requestId(),
      new IcPage<>(
        pageCurrent,
        ticketPager.pageNumber(),
        ticketPager.pageCount(),
        ticketPager.pageFirstOffset()
      )
    );
  }
}
