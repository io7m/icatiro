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
import com.io7m.icatiro.database.api.IcDatabaseProjectsQueriesType;
import com.io7m.icatiro.model.IcValidityException;
import com.io7m.icatiro.protocol.tickets.IcTCommandProjectCreate;
import com.io7m.icatiro.protocol.tickets.IcTResponseProjectCreate;
import com.io7m.icatiro.protocol.tickets.IcTResponseType;
import com.io7m.icatiro.server.internal.IcSecurityException;

import static com.io7m.icatiro.model.IcPermission.PROJECT_CREATE;

/**
 * {@code IcTCommandProjectCreate}
 */

public final class IcTCmdProjectCreate
  extends IcTCmdAbstract<IcTCommandProjectCreate>
{
  /**
   * {@code IcTCommandProjectCreate}
   */

  public IcTCmdProjectCreate()
  {

  }

  @Override
  protected IcTResponseType executeActual(
    final IcTCommandContext context,
    final IcTCommandProjectCreate command)
    throws IcValidityException, IcDatabaseException, IcSecurityException
  {
    final var transaction =
      context.transaction();
    final var projects =
      transaction.queries(IcDatabaseProjectsQueriesType.class);

    transaction.userIdSet(context.userSession().user().id());

    final var project =
      projects.projectCreate(
        command.title(),
        command.shortName()
      );

    context.permissionCheck(project.id(), PROJECT_CREATE);
    return new IcTResponseProjectCreate(context.requestId(), project);
  }
}
