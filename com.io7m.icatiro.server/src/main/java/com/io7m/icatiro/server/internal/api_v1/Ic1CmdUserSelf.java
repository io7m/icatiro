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


package com.io7m.icatiro.server.internal.api_v1;

import com.io7m.icatiro.database.api.IcDatabaseUsersQueriesType;
import com.io7m.icatiro.protocol.api_v1.Ic1CommandUserSelf;
import com.io7m.icatiro.protocol.api_v1.Ic1ResponseType;
import com.io7m.icatiro.protocol.api_v1.Ic1ResponseUserSelf;
import com.io7m.icatiro.protocol.api_v1.Ic1User;
import com.io7m.icatiro.server.internal.command_exec.IcCommandExecutionResult;
import com.io7m.icatiro.server.internal.command_exec.IcCommandExecutorType;

import java.util.Objects;

/**
 * Ic1CmdUserSelf
 */

public final class Ic1CmdUserSelf
  implements IcCommandExecutorType<
  Ic1CommandContext, Ic1CommandUserSelf, Ic1ResponseType>
{
  /**
   * Ic1CmdUserSelf
   */

  public Ic1CmdUserSelf()
  {

  }

  @Override
  public IcCommandExecutionResult<Ic1ResponseType> execute(
    final Ic1CommandContext context,
    final Ic1CommandUserSelf command)
    throws Exception
  {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(command, "command");

    final var transaction =
      context.transaction();
    final var users =
      transaction.queries(IcDatabaseUsersQueriesType.class);

    final var userId = context.user().id();
    transaction.userIdSet(userId);
    final var user = users.userGetRequire(userId);

    return new IcCommandExecutionResult<>(
      200,
      new Ic1ResponseUserSelf(context.requestId(), Ic1User.ofUser(user))
    );
  }
}
