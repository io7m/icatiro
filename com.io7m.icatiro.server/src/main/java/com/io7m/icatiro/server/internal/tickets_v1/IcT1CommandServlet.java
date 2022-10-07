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

package com.io7m.icatiro.server.internal.tickets_v1;

import com.io7m.icatiro.database.api.IcDatabaseException;
import com.io7m.icatiro.database.api.IcDatabaseTransactionType;
import com.io7m.icatiro.database.api.IcDatabaseType;
import com.io7m.icatiro.protocol.IcProtocolException;
import com.io7m.icatiro.protocol.tickets.IcTCommandType;
import com.io7m.icatiro.protocol.tickets.IcTResponseError;
import com.io7m.icatiro.protocol.tickets.IcTResponseType;
import com.io7m.icatiro.protocol.tickets.cb.IcT1Messages;
import com.io7m.icatiro.server.internal.IcHTTPErrorStatusException;
import com.io7m.icatiro.server.internal.IcRequestLimits;
import com.io7m.icatiro.server.internal.command_exec.IcCommandExecutionFailure;
import com.io7m.icatiro.server.internal.tickets.IcTCommandContext;
import com.io7m.icatiro.server.internal.tickets.IcTCommandExecutor;
import com.io7m.icatiro.services.api.IcServiceDirectoryType;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

import static com.io7m.icatiro.database.api.IcDatabaseRole.ICATIRO;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.icatiro.server.internal.IcServerRequestDecoration.requestIdFor;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;

/**
 * A servlet for executing a single command.
 */

public final class IcT1CommandServlet extends IcT1AuthenticatedServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IcT1CommandServlet.class);

  private final IcDatabaseType database;
  private final IcRequestLimits limits;
  private final IcT1Messages messages;
  private final IcTCommandExecutor executor;
  private final IcServiceDirectoryType services;

  /**
   * A servlet for executing a single command.
   *
   * @param inServices The service directory
   */

  public IcT1CommandServlet(
    final IcServiceDirectoryType inServices)
  {
    super(inServices);

    this.services =
      Objects.requireNonNull(inServices, "inServices");
    this.database =
      inServices.requireService(IcDatabaseType.class);
    this.limits =
      inServices.requireService(IcRequestLimits.class);
    this.messages =
      inServices.requireService(IcT1Messages.class);
    this.executor =
      new IcTCommandExecutor();
  }

  @Override
  protected Logger logger()
  {
    return LOG;
  }

  @Override
  protected void serviceAuthenticated(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final HttpSession session)
    throws Exception
  {
    final var requestId =
      requestIdFor(request);

    try (var input = this.limits.boundedMaximumInput(request, 1048576)) {
      final var data = input.readAllBytes();
      final var message = this.messages.parse(data);
      if (message instanceof IcTCommandType<?> command) {
        this.executeCommand(request, servletResponse, command);
        return;
      }
    } catch (final IcProtocolException e) {
      throw new IcHTTPErrorStatusException(
        BAD_REQUEST_400,
        PROTOCOL_ERROR,
        e.getMessage(),
        e
      );
    }

    throw new IcHTTPErrorStatusException(
      BAD_REQUEST_400,
      PROTOCOL_ERROR,
      this.strings().format("expectedCommand", "IcU1CommandType")
    );
  }

  private void executeCommand(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final IcTCommandType<?> command)
    throws IcDatabaseException, IOException, InterruptedException
  {
    try (var connection = this.database.openConnection(ICATIRO)) {
      try (var transaction = connection.openTransaction()) {
        this.executeCommandInTransaction(
          request,
          servletResponse,
          command,
          transaction
        );
      }
    }
  }

  private void executeCommandInTransaction(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final IcTCommandType<?> command,
    final IcDatabaseTransactionType transaction)
    throws IOException, IcDatabaseException, InterruptedException
  {
    final var context =
      IcTCommandContext.create(
        this.services,
        transaction,
        request,
        this.userSession()
      );

    final var sends =
      this.sends();

    try {
      final IcTResponseType result = this.executor.execute(context, command);
      sends.send(servletResponse, 200, result);
      if (result instanceof IcTResponseError error) {
        Span.current().setAttribute("icatiro.errorCode", error.errorCode());
      } else {
        transaction.commit();
      }
    } catch (final IcCommandExecutionFailure e) {
      Span.current().setAttribute("icatiro.errorCode", e.errorCode().id());
      sends.send(
        servletResponse,
        e.httpStatusCode(),
        new IcTResponseError(
          e.requestId(),
          e.errorCode().id(),
          e.getMessage()
        ));
    }
  }
}
