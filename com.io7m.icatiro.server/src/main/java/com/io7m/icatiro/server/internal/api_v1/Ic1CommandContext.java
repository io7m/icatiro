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

import com.io7m.icatiro.database.api.IcDatabaseTransactionType;
import com.io7m.icatiro.error_codes.IcErrorCode;
import com.io7m.icatiro.model.IcUser;
import com.io7m.icatiro.protocol.api_v1.Ic1ResponseError;
import com.io7m.icatiro.protocol.api_v1.Ic1ResponseType;
import com.io7m.icatiro.server.internal.IcServerClock;
import com.io7m.icatiro.server.internal.IcServerStrings;
import com.io7m.icatiro.server.internal.command_exec.IcCommandContext;
import com.io7m.icatiro.services.api.IcServiceDirectoryType;

import java.util.Objects;
import java.util.UUID;

/**
 * The command context for public API commands.
 */

public final class Ic1CommandContext extends IcCommandContext<Ic1ResponseType>
{
  private final IcUser user;

  /**
   * @return The user executing the command.
   */

  public IcUser user()
  {
    return this.user;
  }

  /**
   * The context for execution of a command (or set of commands in a
   * transaction).
   *
   * @param inServices    The service directory
   * @param inStrings     The string resources
   * @param inRequestId   The request ID
   * @param inTransaction The transaction
   * @param inClock       The clock
   * @param inUser        The user executing the command
   */

  public Ic1CommandContext(
    final IcServiceDirectoryType inServices,
    final IcServerStrings inStrings,
    final UUID inRequestId,
    final IcDatabaseTransactionType inTransaction,
    final IcServerClock inClock,
    final IcUser inUser)
  {
    super(inServices, inStrings, inRequestId, inTransaction, inClock);
    this.user = Objects.requireNonNull(inUser, "inUser");
  }

  @Override
  protected Ic1ResponseError error(
    final UUID id,
    final IcErrorCode errorCode,
    final String message)
  {
    return new Ic1ResponseError(id, errorCode.id(), message);
  }
}
