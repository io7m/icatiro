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

package com.io7m.icatiro.database.postgres.internal;

import com.io7m.icatiro.database.api.IcDatabaseAuditQueriesType;
import com.io7m.icatiro.database.api.IcDatabaseException;
import com.io7m.icatiro.database.postgres.internal.tables.records.AuditRecord;
import com.io7m.icatiro.model.IcAuditEvent;
import com.io7m.icatiro.model.IcSubsetMatch;
import org.jooq.exception.DataAccessException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.io7m.icatiro.database.postgres.internal.IcDatabaseExceptions.handleDatabaseException;
import static com.io7m.icatiro.database.postgres.internal.Tables.AUDIT;

final class IcDatabaseAuditQueries
  extends IcBaseQueries
  implements IcDatabaseAuditQueriesType
{
  IcDatabaseAuditQueries(
    final IcDatabaseTransaction inTransaction)
  {
    super(inTransaction);
  }

  private static IcAuditEvent toAuditEvent(
    final AuditRecord record)
  {
    return new IcAuditEvent(
      record.getId().longValue(),
      record.getUserId(),
      record.getTime(),
      record.getType(),
      record.getMessage()
    );
  }

  @Override
  public List<IcAuditEvent> auditEvents(
    final OffsetDateTime fromInclusive,
    final OffsetDateTime toInclusive,
    final IcSubsetMatch<String> owner,
    final IcSubsetMatch<String> type,
    final IcSubsetMatch<String> message)
    throws IcDatabaseException
  {
    Objects.requireNonNull(fromInclusive, "fromInclusive");
    Objects.requireNonNull(toInclusive, "toInclusive");
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(message, "message");

    try (var transaction = this.transaction()) {
      final var context = transaction.createContext();
      try {
        var selection =
          context.selectFrom(AUDIT)
            .where(AUDIT.TIME.ge(fromInclusive))
            .and(AUDIT.TIME.le(toInclusive));

        final var ownerInclude = owner.include();
        if (!ownerInclude.isEmpty()) {
          final var q = "%%%s%%".formatted(ownerInclude);
          selection = selection.and(AUDIT.USER_ID.likeIgnoreCase(q));
        }
        final var ownerExclude = owner.exclude();
        if (!ownerExclude.isEmpty()) {
          final var q = "%%%s%%".formatted(ownerExclude);
          selection = selection.andNot(AUDIT.USER_ID.likeIgnoreCase(q));
        }

        final var typeInclude = type.include();
        if (!typeInclude.isEmpty()) {
          final var q = "%%%s%%".formatted(typeInclude);
          selection = selection.and(AUDIT.TYPE.likeIgnoreCase(q));
        }
        final var typeExclude = type.exclude();
        if (!typeExclude.isEmpty()) {
          final var q = "%%%s%%".formatted(typeExclude);
          selection = selection.andNot(AUDIT.TYPE.likeIgnoreCase(q));
        }

        final var messageInclude = message.include();
        if (!messageInclude.isEmpty()) {
          final var q = "%%%s%%".formatted(messageInclude);
          selection = selection.and(AUDIT.MESSAGE.likeIgnoreCase(q));
        }
        final var messageExclude = message.exclude();
        if (!messageExclude.isEmpty()) {
          final var q = "%%%s%%".formatted(messageExclude);
          selection = selection.andNot(AUDIT.MESSAGE.likeIgnoreCase(q));
        }

        return selection.stream()
          .map(IcDatabaseAuditQueries::toAuditEvent)
          .toList();
      } catch (final DataAccessException e) {
        throw handleDatabaseException(transaction, e);
      }
    }
  }

  @Override
  public void auditPut(
    final UUID userId,
    final OffsetDateTime time,
    final String type,
    final String message,
    final boolean confidential)
    throws IcDatabaseException
  {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(time, "time");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(message, "message");

    final var context =
      this.transaction().createContext();

    try {
      context.insertInto(AUDIT)
        .set(AUDIT.TIME, time)
        .set(AUDIT.TYPE, type)
        .set(AUDIT.USER_ID, userId)
        .set(AUDIT.MESSAGE, message)
        .set(AUDIT.CONFIDENTIAL, Boolean.valueOf(confidential))
        .execute();
    } catch (final DataAccessException e) {
      throw handleDatabaseException(this.transaction(), e);
    }
  }
}
