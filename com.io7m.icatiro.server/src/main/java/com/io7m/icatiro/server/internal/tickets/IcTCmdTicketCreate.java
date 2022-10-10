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
import com.io7m.icatiro.database.api.IcDatabaseUsersQueriesType;
import com.io7m.icatiro.model.IcPermissionTicketwide;
import com.io7m.icatiro.model.IcValidityException;
import com.io7m.icatiro.protocol.tickets.IcTCommandTicketCreate;
import com.io7m.icatiro.protocol.tickets.IcTResponseTicketCreate;
import com.io7m.icatiro.protocol.tickets.IcTResponseType;
import com.io7m.icatiro.server.internal.IcSecurityException;

import static com.io7m.icatiro.model.IcPermission.TICKET_COMMENT;
import static com.io7m.icatiro.model.IcPermission.TICKET_CREATE;
import static com.io7m.icatiro.model.IcPermission.TICKET_READ;
import static com.io7m.icatiro.model.IcPermission.TICKET_WRITE;

/**
 * {@code IcTCommandTicketCreate}
 */

public final class IcTCmdTicketCreate
  extends IcTCmdAbstract<IcTCommandTicketCreate>
{
  /**
   * {@code IcTCommandTicketCreate}
   */

  public IcTCmdTicketCreate()
  {

  }

  @Override
  protected IcTResponseType executeActual(
    final IcTCommandContext context,
    final IcTCommandTicketCreate command)
    throws IcValidityException, IcDatabaseException, IcSecurityException
  {
    final var transaction =
      context.transaction();
    final var tickets =
      transaction.queries(IcDatabaseTicketsQueriesType.class);
    final var users =
      transaction.queries(IcDatabaseUsersQueriesType.class);

    final var session = context.userSession();
    final var user = session.user();
    transaction.userIdSet(user.id());

    /*
     * Create the ticket and then check that the user's permissions should
     * have allowed them to do so. The creation will be rolled back if the
     * permission check fails.
     */

    final var ticket =
      tickets.ticketCreate(command.creation());

    context.permissionCheck(ticket.ticketId(), TICKET_CREATE);

    /*
     * The user might not have permission to read/write tickets in the project,
     * but they should have read/write permission to tickets they have created.
     */

    final var ticketRead =
      new IcPermissionTicketwide(ticket.ticketId(), TICKET_READ);
    final var ticketWrite =
      new IcPermissionTicketwide(ticket.ticketId(), TICKET_WRITE);
    final var ticketComment =
      new IcPermissionTicketwide(ticket.ticketId(), TICKET_COMMENT);

    final var newPermissions =
      user.permissions().toBuilder()
        .add(ticketRead)
        .add(ticketWrite)
        .add(ticketComment)
        .build();

    final var newUser = user.withPermissions(newPermissions);
    users.userPut(newUser);
    session.setUser(newUser);

    return new IcTResponseTicketCreate(context.requestId(), ticket);
  }
}
