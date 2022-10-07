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
import com.io7m.icatiro.database.api.IcDatabaseUsersQueriesType;
import com.io7m.icatiro.model.IcUser;
import com.io7m.icatiro.model.IcValidityException;
import com.io7m.icatiro.protocol.tickets.IcTCommandPermissionGrant;
import com.io7m.icatiro.protocol.tickets.IcTResponsePermissionGrant;
import com.io7m.icatiro.protocol.tickets.IcTResponseType;
import com.io7m.icatiro.server.internal.command_exec.IcCommandExecutionFailure;

import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.OPERATION_NOT_PERMITTED;

/**
 * {@code IcTCommandPermissionGrant}
 */

public final class IcTCmdPermissionGrant
  extends IcTCmdAbstract<IcTCommandPermissionGrant>
{
  /**
   * {@code IcTCommandPermissionGrant}
   */

  public IcTCmdPermissionGrant()
  {

  }

  @Override
  protected IcTResponseType executeActual(
    final IcTCommandContext context,
    final IcTCommandPermissionGrant command)
    throws IcValidityException, IcDatabaseException, IcCommandExecutionFailure
  {
    final var session =
      context.userSession();
    final var users =
      context.transaction()
        .queries(IcDatabaseUsersQueriesType.class);

    final var sourceUser =
      session.user();

    final var permission = command.permission();
    if (!sourceUser.permissions().impliesScoped(permission)) {
      throw context.failFormatted(
        403,
        OPERATION_NOT_PERMITTED,
        "errorPermissionGrant",
        permission
      );
    }

    final var targetUser =
      users.userGetRequire(command.targetUser());

    final var newPermissions =
      targetUser.permissions()
        .toBuilder()
        .add(permission)
        .build();

    users.userPut(new IcUser(
      targetUser.id(),
      targetUser.name(),
      targetUser.emails(),
      newPermissions
    ));

    return new IcTResponsePermissionGrant(context.requestId());
  }
}
