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


package com.io7m.icatiro.server.internal.command_exec;

import com.io7m.icatiro.database.api.IcDatabaseTransactionType;
import com.io7m.icatiro.error_codes.IcErrorCode;
import com.io7m.icatiro.protocol.api.IcProtocolMessageType;
import com.io7m.icatiro.server.internal.IcServerClock;
import com.io7m.icatiro.server.internal.IcServerStrings;
import com.io7m.icatiro.services.api.IcServiceDirectoryType;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * The context for execution of a command (or set of commands in a
 * transaction).
 *
 * @param <E> The type of error messages
 */

public abstract class IcCommandContext<E extends IcProtocolMessageType>
{
  private final IcServiceDirectoryType services;
  private final UUID requestId;
  private final IcDatabaseTransactionType transaction;
  private final IcServerClock clock;
  private final IcServerStrings strings;

  /**
   * The context for execution of a command (or set of commands in a
   * transaction).
   *
   * @param inServices    The service directory
   * @param inStrings     The string resources
   * @param inRequestId   The request ID
   * @param inClock       The clock
   * @param inTransaction The transaction
   */

  public IcCommandContext(
    final IcServiceDirectoryType inServices,
    final IcServerStrings inStrings,
    final UUID inRequestId,
    final IcDatabaseTransactionType inTransaction,
    final IcServerClock inClock)
  {
    this.services =
      Objects.requireNonNull(inServices, "services");
    this.requestId =
      Objects.requireNonNull(inRequestId, "requestId");
    this.transaction =
      Objects.requireNonNull(inTransaction, "transaction");
    this.clock =
      Objects.requireNonNull(inClock, "clock");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
  }

  /**
   * @return The service directory used during execution
   */

  public IcServiceDirectoryType services()
  {
    return this.services;
  }

  /**
   * @return The ID of the incoming request
   */

  public UUID requestId()
  {
    return this.requestId;
  }

  /**
   * @return The database transaction
   */

  public IcDatabaseTransactionType transaction()
  {
    return this.transaction;
  }

  /**
   * @return The current time
   */

  public OffsetDateTime now()
  {
    return this.clock.now();
  }

  /**
   * Produce an execution result indicating an error, with a formatted error
   * message.
   *
   * @param statusCode The HTTP status code
   * @param errorCode  The error code
   * @param messageId  The string resource message ID
   * @param args       The string resource format arguments
   *
   * @return An execution result
   */

  public IcCommandExecutionResult<E> resultErrorFormatted(
    final int statusCode,
    final IcErrorCode errorCode,
    final String messageId,
    final Object... args)
  {
    return this.resultError(
      statusCode,
      errorCode,
      this.strings.format(messageId, args)
    );
  }

  /**
   * Produce an execution result indicating an error, with a string constant
   * message.
   *
   * @param statusCode The HTTP status code
   * @param errorCode  The error code
   * @param message    The string message
   *
   * @return An execution result
   */

  public IcCommandExecutionResult<E> resultError(
    final int statusCode,
    final IcErrorCode errorCode,
    final String message)
  {
    return new IcCommandExecutionResult<>(
      statusCode,
      this.error(this.requestId, errorCode, message)
    );
  }

  protected abstract E error(
    UUID id,
    IcErrorCode errorCode,
    String message
  );
}
