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
import com.io7m.icatiro.protocol.tickets.IcTCommandPermissionGrant;
import com.io7m.icatiro.protocol.tickets.IcTCommandProjectCreate;
import com.io7m.icatiro.protocol.tickets.IcTCommandTicketCommentCreate;
import com.io7m.icatiro.protocol.tickets.IcTCommandTicketCreate;
import com.io7m.icatiro.protocol.tickets.IcTCommandTicketGet;
import com.io7m.icatiro.protocol.tickets.IcTCommandTicketSearchBegin;
import com.io7m.icatiro.protocol.tickets.IcTCommandTicketSearchNext;
import com.io7m.icatiro.protocol.tickets.IcTCommandTicketSearchPrevious;
import com.io7m.icatiro.protocol.tickets.IcTCommandType;
import com.io7m.icatiro.protocol.tickets.IcTResponseType;
import com.io7m.icatiro.server.internal.command_exec.IcCommandExecutionFailure;
import com.io7m.icatiro.server.internal.command_exec.IcCommandExecutorType;

import java.io.IOException;

/**
 * A command executor for Tickets commands.
 */

public final class IcTCommandExecutor
  implements IcCommandExecutorType<
  IcTCommandContext,
  IcTCommandType<? extends IcTResponseType>,
  IcTResponseType>
{
  /**
   * A command executor for Tickets commands.
   */

  public IcTCommandExecutor()
  {

  }

  private static IcTResponseType executeCommand(
    final IcTCommandContext context,
    final IcTCommandType<? extends IcTResponseType> command)
    throws IcCommandExecutionFailure, IOException, InterruptedException
  {
    if (command instanceof IcTCommandLogin c) {
      return new IcTCmdLogin().execute(context, c);
    }
    if (command instanceof IcTCommandTicketSearchBegin c) {
      return new IcTCmdTicketSearchBegin().execute(context, c);
    }
    if (command instanceof IcTCommandTicketSearchNext c) {
      return new IcTCmdTicketSearchNext().execute(context, c);
    }
    if (command instanceof IcTCommandTicketSearchPrevious c) {
      return new IcTCmdTicketSearchPrevious().execute(context, c);
    }
    if (command instanceof IcTCommandProjectCreate c) {
      return new IcTCmdProjectCreate().execute(context, c);
    }
    if (command instanceof IcTCommandTicketCreate c) {
      return new IcTCmdTicketCreate().execute(context, c);
    }
    if (command instanceof IcTCommandPermissionGrant c) {
      return new IcTCmdPermissionGrant().execute(context, c);
    }
    if (command instanceof IcTCommandTicketCommentCreate c) {
      return new IcTCmdTicketCommentCreate().execute(context, c);
    }
    if (command instanceof IcTCommandTicketGet c) {
      return new IcTCmdTicketGet().execute(context, c);
    }

    throw new IllegalStateException();
  }

  @Override
  public IcTResponseType execute(
    final IcTCommandContext context,
    final IcTCommandType<? extends IcTResponseType> command)
    throws IcCommandExecutionFailure, IOException, InterruptedException
  {
    final var span =
      context.tracer()
        .spanBuilder(command.getClass().getSimpleName())
        .startSpan();

    try (var ignored = span.makeCurrent()) {
      return executeCommand(context, command);
    } catch (final Throwable e) {
      span.recordException(e);
      throw e;
    } finally {
      span.end();
    }
  }
}
