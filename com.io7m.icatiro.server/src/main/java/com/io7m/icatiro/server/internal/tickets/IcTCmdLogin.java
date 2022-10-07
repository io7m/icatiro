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

import com.io7m.icatiro.protocol.tickets.IcTCommandLogin;
import com.io7m.icatiro.protocol.tickets.IcTResponseType;
import com.io7m.icatiro.server.internal.command_exec.IcCommandExecutionFailure;
import com.io7m.icatiro.server.internal.command_exec.IcCommandExecutorType;

import java.util.Objects;

import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.PROTOCOL_ERROR;

/**
 * {@code IcTCommandLogin}
 */

public final class IcTCmdLogin
  implements IcCommandExecutorType<
  IcTCommandContext, IcTCommandLogin, IcTResponseType>
{
  /**
   * {@code IcTCommandLogin}
   */

  public IcTCmdLogin()
  {

  }

  @Override
  public IcTResponseType execute(
    final IcTCommandContext context,
    final IcTCommandLogin command)
    throws IcCommandExecutionFailure
  {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(command, "command");

    throw context.failFormatted(
      400,
      PROTOCOL_ERROR,
      "commandNotHere"
    );
  }
}
