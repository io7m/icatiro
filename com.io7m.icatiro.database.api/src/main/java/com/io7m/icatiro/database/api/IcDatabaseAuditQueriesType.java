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

package com.io7m.icatiro.database.api;

import com.io7m.icatiro.model.IcAuditEvent;
import com.io7m.icatiro.model.IcSubsetMatch;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The database queries involving the audit log.
 */

public non-sealed interface IcDatabaseAuditQueriesType
  extends IcDatabaseQueriesType
{
  /**
   * Retrieve all audit events from the database between the given (inclusive)
   * times.
   *
   * @param fromInclusive The inclusive lower bound on the event times
   * @param toInclusive   The inclusive upper bound on the event times
   * @param message       The subset of messages to include
   * @param type          The subset of types to include
   * @param owner         The subset of owners to include
   *
   * @return A series of audit events, sorted by time
   *
   * @throws IcDatabaseException On errors
   */

  List<IcAuditEvent> auditEvents(
    OffsetDateTime fromInclusive,
    OffsetDateTime toInclusive,
    IcSubsetMatch<String> owner,
    IcSubsetMatch<String> type,
    IcSubsetMatch<String> message)
    throws IcDatabaseException;

  /**
   * Create an audit event.
   *
   * @param userId       The user ID of the event
   * @param time         The event time
   * @param type         The event type
   * @param message      The event message
   * @param confidential The event confidentiality
   *
   * @throws IcDatabaseException On errors
   */

  void auditPut(
    UUID userId,
    OffsetDateTime time,
    String type,
    String message,
    boolean confidential)
    throws IcDatabaseException;
}
