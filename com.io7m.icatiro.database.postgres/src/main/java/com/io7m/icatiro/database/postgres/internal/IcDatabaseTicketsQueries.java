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

import com.io7m.icatiro.database.api.IcDatabaseException;
import com.io7m.icatiro.database.api.IcDatabaseTicketsQueriesType;
import com.io7m.icatiro.database.postgres.internal.tables.records.ProjectsRecord;
import com.io7m.icatiro.model.IcProjectID;
import com.io7m.icatiro.model.IcProjectShortName;
import com.io7m.icatiro.model.IcProjectTitle;
import com.io7m.icatiro.model.IcProjectUniqueIdentifierType;
import com.io7m.icatiro.model.IcTicketCreation;
import com.io7m.icatiro.model.IcTicketID;
import com.io7m.icatiro.model.IcTicketOrdering;
import com.io7m.icatiro.model.IcTicketSummary;
import com.io7m.icatiro.model.IcTicketTitle;
import com.io7m.icatiro.model.IcTimeRange;
import com.io7m.icatiro.model.IcUserDisplayName;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.jooq.SelectForUpdateStep;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.icatiro.database.postgres.internal.IcDatabaseExceptions.handleDatabaseException;
import static com.io7m.icatiro.database.postgres.internal.IcDatabaseUsersQueries.USER_DOES_NOT_EXIST;
import static com.io7m.icatiro.database.postgres.internal.Tables.AUDIT;
import static com.io7m.icatiro.database.postgres.internal.Tables.TICKETS;
import static com.io7m.icatiro.database.postgres.internal.Tables.USERS;
import static com.io7m.icatiro.database.postgres.internal.tables.Projects.PROJECTS;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.PROJECT_NONEXISTENT;
import static com.io7m.icatiro.model.IcPermission.TICKET_READ;
import static java.lang.Long.toUnsignedString;
import static java.lang.Long.valueOf;

final class IcDatabaseTicketsQueries
  extends IcBaseQueries
  implements IcDatabaseTicketsQueriesType
{
  protected IcDatabaseTicketsQueries(
    final IcDatabaseTransaction inTransaction)
  {
    super(inTransaction);
  }

  private static Collection<? extends OrderField<?>> orderFields(
    final IcTicketOrdering ordering)
  {
    final var columns = ordering.ordering();
    final var fields = new ArrayList<OrderField<?>>(columns.size());
    for (final var columnOrder : columns) {
      fields.add(
        switch (columnOrder.column()) {
          case BY_ID -> {
            if (columnOrder.ascending()) {
              yield TICKETS.ID.asc();
            }
            yield TICKETS.ID.desc();
          }
          case BY_TIME_CREATED -> {
            if (columnOrder.ascending()) {
              yield TICKETS.TIME_CREATED.asc();
            }
            yield TICKETS.TIME_CREATED.desc();
          }
          case BY_TIME_UPDATED -> {
            if (columnOrder.ascending()) {
              yield TICKETS.TIME_UPDATED.asc();
            }
            yield TICKETS.TIME_UPDATED.desc();
          }
          case BY_TITLE -> {
            if (columnOrder.ascending()) {
              yield TICKETS.TITLE.asc();
            }
            yield TICKETS.TITLE.desc();
          }
        });
    }
    return List.copyOf(fields);
  }

  private static IcTicketSummary mapTicketWithPermissions(
    final org.jooq.Record record)
  {
    return new IcTicketSummary(
      new IcProjectID(record.get(TICKETS.PROJECT)),
      new IcProjectTitle(record.get(PROJECTS.NAME_DISPLAY)),
      new IcProjectShortName(record.get(PROJECTS.NAME_SHORT)),
      new IcTicketID(record.get(TICKETS.ID)),
      new IcTicketTitle(record.get(TICKETS.TITLE)),
      record.get(TICKETS.TIME_CREATED),
      record.get(TICKETS.TIME_UPDATED),
      record.get(TICKETS.REPORTER),
      new IcUserDisplayName(record.get(USERS.NAME))
    );
  }

  private static ProjectsRecord findProject(
    final DSLContext context,
    final IcProjectUniqueIdentifierType project)
    throws IcDatabaseException
  {
    if (project instanceof IcProjectID id) {
      return context.fetchOptional(
          PROJECTS,
          PROJECTS.ID.eq(valueOf(id.value())))
        .orElseThrow(() -> {
          return new IcDatabaseException(
            "No such project with ID %s".formatted(id),
            PROJECT_NONEXISTENT
          );
        });
    }
    if (project instanceof IcProjectShortName name) {
      return context.fetchOptional(
          PROJECTS,
          PROJECTS.NAME_SHORT.eq(name.value()))
        .orElseThrow(() -> {
          return new IcDatabaseException(
            "No such project with short name %s".formatted(name),
            PROJECT_NONEXISTENT
          );
        });
    }

    throw new IllegalStateException();
  }

  @Override
  public IcTicketSummary ticketCreate(
    final IcTicketCreation creation)
    throws IcDatabaseException
  {
    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var userId = transaction.userId();

    try {
      final var user =
        context.fetchOne(USERS, USERS.ID.eq(userId));
      final var timeNow =
        this.currentTime();
      final var project =
        findProject(context, creation.project());

      final var newTicket = context.newRecord(TICKETS);
      newTicket.setTimeCreated(timeNow);
      newTicket.setTimeUpdated(timeNow);
      newTicket.setProject(project.getId());
      newTicket.setReporter(userId);
      newTicket.setTitle(creation.title().value());
      newTicket.store();

      final var newId =
        newTicket.getId().longValue();

      context.insertInto(AUDIT)
        .set(AUDIT.USER_ID, userId)
        .set(AUDIT.TIME, this.currentTime())
        .set(AUDIT.MESSAGE, toUnsignedString(newId))
        .set(AUDIT.TYPE, "TICKET_CREATED")
        .set(AUDIT.CONFIDENTIAL, Boolean.FALSE)
        .execute();

      return new IcTicketSummary(
        new IcProjectID(project.getId().longValue()),
        new IcProjectTitle(project.getNameDisplay()),
        new IcProjectShortName(project.getNameShort()),
        new IcTicketID(newId),
        new IcTicketTitle(newTicket.getTitle()),
        timeNow,
        timeNow,
        userId,
        new IcUserDisplayName(user.getName())
      );
    } catch (final DataAccessException e) {
      throw handleDatabaseException(transaction, e);
    }
  }

  @Override
  public List<IcTicketSummary> ticketListWithPermissions(
    final UUID userId,
    final IcTimeRange timeCreatedRange,
    final IcTimeRange timeUpdatedRange,
    final IcTicketOrdering ordering,
    final int limit,
    final Optional<List<Object>> seek)
    throws IcDatabaseException
  {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(timeCreatedRange, "timeCreatedRange");
    Objects.requireNonNull(timeUpdatedRange, "timeUpdatedRange");
    Objects.requireNonNull(ordering, "ordering");
    Objects.requireNonNull(seek, "seek");

    final var transaction = this.transaction();
    final var context = transaction.createContext();

    try {
      final var user =
        context.fetchOptional(USERS, USERS.ID.eq(userId))
          .orElseThrow(USER_DOES_NOT_EXIST);

      final var baseSelection =
        context.select(
          TICKETS.ID,
          TICKETS.PROJECT,
          TICKETS.TIME_CREATED,
          TICKETS.TIME_UPDATED,
          TICKETS.REPORTER,
          TICKETS.TITLE,
          USERS.NAME,
          PROJECTS.NAME_DISPLAY,
          PROJECTS.NAME_SHORT
        ).from(
          TICKETS
            .join(PROJECTS).on(PROJECTS.ID.eq(TICKETS.PROJECT))
            .join(USERS).on(USERS.ID.eq(TICKETS.REPORTER))
        );

      /*
       * The permission condition is responsible for filtering out
       * tickets with which the user has no permission to read.
       */

      final var permissionCondition =
        DSL.condition(
          "permission_is_allowed(?, TICKETS.PROJECT, TICKETS.ID, ?)",
          userId,
          Integer.valueOf(TICKET_READ.value())
        );

      /*
       * The tickets must lie within the given time ranges.
       */

      final var timeCreatedCondition =
        DSL.condition(
          TICKETS.TIME_CREATED.ge(timeCreatedRange.timeLower())
            .and(TICKETS.TIME_CREATED.le(timeCreatedRange.timeUpper()))
        );

      final var timeUpdatedCondition =
        DSL.condition(
          TICKETS.TIME_UPDATED.ge(timeCreatedRange.timeLower())
            .and(TICKETS.TIME_UPDATED.le(timeCreatedRange.timeUpper()))
        );

      final var allConditions =
        permissionCondition
          .and(timeCreatedCondition)
          .and(timeUpdatedCondition);

      final var baseOrdering =
        baseSelection.where(allConditions)
          .orderBy(orderFields(ordering));

      /*
       * If a seek is specified, then seek!
       */

      final SelectForUpdateStep<?> next;
      if (seek.isPresent()) {
        final var page = seek.get();
        final var fields = page.toArray();
        next = baseOrdering.seek(fields).limit(Integer.valueOf(limit));
      } else {
        next = baseOrdering.limit(Integer.valueOf(limit));
      }

      return next.fetch()
        .map(IcDatabaseTicketsQueries::mapTicketWithPermissions);
    } catch (final DataAccessException e) {
      throw handleDatabaseException(transaction, e);
    }
  }
}
