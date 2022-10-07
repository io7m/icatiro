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

import com.io7m.icatiro.database.api.IcDatabaseTransactionType;
import com.io7m.icatiro.error_codes.IcErrorCode;
import com.io7m.icatiro.protocol.tickets.IcTResponseError;
import com.io7m.icatiro.protocol.tickets.IcTResponseType;
import com.io7m.icatiro.server.internal.IcServerClock;
import com.io7m.icatiro.server.internal.IcServerStrings;
import com.io7m.icatiro.server.internal.IcUserSession;
import com.io7m.icatiro.server.internal.command_exec.IcCommandContext;
import com.io7m.icatiro.services.api.IcServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

import static com.io7m.icatiro.server.internal.IcRequests.requestUserAgent;
import static com.io7m.icatiro.server.internal.IcServerRequestDecoration.requestIdFor;

/**
 * The command context for user API commands.
 */

public final class IcTCommandContext
  extends IcCommandContext<IcTResponseType>
{
  /**
   * The context for execution of a command (or set of commands in a
   * transaction).
   *
   * @param inServices      The service directory
   * @param inStrings       The string resources
   * @param inRequestId     The request ID
   * @param inTransaction   The transaction
   * @param inClock         The clock
   * @param inSession       The user session
   * @param remoteHost      The remote host
   * @param remoteUserAgent The remote user agent
   */

  public IcTCommandContext(
    final IcServiceDirectoryType inServices,
    final IcServerStrings inStrings,
    final UUID inRequestId,
    final IcDatabaseTransactionType inTransaction,
    final IcServerClock inClock,
    final IcUserSession inSession,
    final String remoteHost,
    final String remoteUserAgent)
  {
    super(
      inServices,
      inStrings,
      inRequestId,
      inTransaction,
      inClock,
      inSession,
      remoteHost,
      remoteUserAgent
    );
  }

  /**
   * Create a new command context from the given objects.
   *
   * @param services    The service directory
   * @param transaction The database transaction
   * @param request     The request
   * @param userSession The user session
   *
   * @return A context
   */

  public static IcTCommandContext create(
    final IcServiceDirectoryType services,
    final IcDatabaseTransactionType transaction,
    final HttpServletRequest request,
    final IcUserSession userSession)
  {
    return new IcTCommandContext(
      services,
      services.requireService(IcServerStrings.class),
      requestIdFor(request),
      transaction,
      services.requireService(IcServerClock.class),
      userSession,
      request.getRemoteHost(),
      requestUserAgent(request)
    );
  }

  @Override
  protected IcTResponseError error(
    final UUID id,
    final IcErrorCode errorCode,
    final String message)
  {
    return new IcTResponseError(id, errorCode.id(), message);
  }
}
