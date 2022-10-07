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
import com.io7m.icatiro.model.IcAuditEvent;
import com.io7m.icatiro.model.IcAuditSearchParameters;
import org.jooq.Condition;
import org.jooq.SelectForUpdateStep;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
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

  @Override
  public List<IcAuditEvent> auditEvents(
    final IcAuditSearchParameters parameters,
    final OptionalLong seek)
    throws IcDatabaseException
  {
    Objects.requireNonNull(parameters, "parameters");
    Objects.requireNonNull(seek, "seek");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan("IcDatabaseAuditQueries.auditEvents");

    try {
      final var baseSelection =
        context.selectFrom(AUDIT);

      /*
       * The events must lie within the given time ranges.
       */

      final var timeCreatedCondition =
        DSL.condition(
          AUDIT.TIME.ge(parameters.timeRange().timeLower())
            .and(AUDIT.TIME.le(parameters.timeRange().timeUpper()))
        );

      /*
       * Search queries might be present.
       */

      Condition searchCondition = DSL.trueCondition();

      final var typeOpt = parameters.type();
      if (typeOpt.isPresent()) {
        final var q = "%%%s%%".formatted(typeOpt.get());
        searchCondition =
          searchCondition.and(DSL.condition(AUDIT.TYPE.likeIgnoreCase(q)));
      }

      final var ownerOpt = parameters.owner();
      if (ownerOpt.isPresent()) {
        final var q = "%%%s%%".formatted(ownerOpt.get());
        searchCondition =
          searchCondition.and(DSL.condition(AUDIT.USER_ID.likeIgnoreCase(q)));
      }

      final var msgOpt = parameters.message();
      if (msgOpt.isPresent()) {
        final var q = "%%%s%%".formatted(msgOpt.get());
        searchCondition =
          searchCondition.and(DSL.condition(AUDIT.MESSAGE.likeIgnoreCase(q)));
      }

      final var allConditions =
        timeCreatedCondition.and(searchCondition);

      final var baseOrdering =
        baseSelection.where(allConditions)
          .orderBy(AUDIT.ID.asc());

      /*
       * If a seek is specified, then seek!
       */

      final SelectForUpdateStep<?> next;
      if (seek.isPresent()) {
        next = baseOrdering.seek(Long.valueOf(seek.getAsLong()))
          .limit(Integer.valueOf(parameters.limit()));
      } else {
        next = baseOrdering.limit(Integer.valueOf(parameters.limit()));
      }

      return next.fetch()
        .map(record -> {
          return new IcAuditEvent(
            record.getValue(AUDIT.ID).longValue(),
            record.getValue(AUDIT.USER_ID),
            record.getValue(AUDIT.TIME),
            record.getValue(AUDIT.TYPE),
            record.getValue(AUDIT.MESSAGE)
          );
        });

    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(this.transaction(), e);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public void auditPut(
    final UUID userIc,
    final OffsetDateTime time,
    final String type,
    final String message)
    throws IcDatabaseException
  {
    Objects.requireNonNull(userIc, "userIc");
    Objects.requireNonNull(time, "time");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(message, "message");

    final var transaction = this.transaction();
    final var context = transaction.createContext();

    final var querySpan =
      transaction.createQuerySpan("IcDatabaseAuditQueries.auditPut");

    try {
      context.insertInto(AUDIT)
        .set(AUDIT.TIME, time)
        .set(AUDIT.TYPE, type)
        .set(AUDIT.USER_ID, userIc)
        .set(AUDIT.MESSAGE, message)
        .execute();
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public long auditCount(
    final IcAuditSearchParameters parameters)
    throws IcDatabaseException
  {
    Objects.requireNonNull(parameters, "parameters");

    final var transaction = this.transaction();
    final var context = transaction.createContext();

    final var querySpan =
      transaction.createQuerySpan("IcDatabaseAuditQueries.auditCount");

    try {
      /*
       * The events must lie within the given time ranges.
       */

      final var timeCreatedCondition =
        DSL.condition(
          AUDIT.TIME.ge(parameters.timeRange().timeLower())
            .and(AUDIT.TIME.le(parameters.timeRange().timeUpper()))
        );

      /*
       * Search queries might be present.
       */

      Condition searchCondition = DSL.trueCondition();

      final var typeOpt = parameters.type();
      if (typeOpt.isPresent()) {
        final var q = "%%%s%%".formatted(typeOpt.get());
        searchCondition =
          searchCondition.and(DSL.condition(AUDIT.TYPE.likeIgnoreCase(q)));
      }

      final var ownerOpt = parameters.owner();
      if (ownerOpt.isPresent()) {
        final var q = "%%%s%%".formatted(ownerOpt.get());
        searchCondition =
          searchCondition.and(DSL.condition(AUDIT.USER_ID.likeIgnoreCase(q)));
      }

      final var msgOpt = parameters.message();
      if (msgOpt.isPresent()) {
        final var q = "%%%s%%".formatted(msgOpt.get());
        searchCondition =
          searchCondition.and(DSL.condition(AUDIT.MESSAGE.likeIgnoreCase(q)));
      }

      final var allConditions =
        timeCreatedCondition.and(searchCondition);

      return ((Integer) context.selectCount()
        .from(AUDIT)
        .where(allConditions)
        .fetchOne()
        .getValue(0))
        .longValue();

    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(this.transaction(), e);
    } finally {
      querySpan.end();
    }
  }
}