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
import com.io7m.icatiro.database.api.IcDatabaseTicketSearchType;
import com.io7m.icatiro.database.api.IcDatabaseTicketsQueriesType;
import com.io7m.icatiro.database.postgres.internal.tables.records.ProjectsRecord;
import com.io7m.icatiro.model.IcPage;
import com.io7m.icatiro.model.IcProjectID;
import com.io7m.icatiro.model.IcProjectShortName;
import com.io7m.icatiro.model.IcProjectTitle;
import com.io7m.icatiro.model.IcProjectUniqueIdentifierType;
import com.io7m.icatiro.model.IcTicket;
import com.io7m.icatiro.model.IcTicketColumnOrdering;
import com.io7m.icatiro.model.IcTicketComment;
import com.io7m.icatiro.model.IcTicketCommentCreation;
import com.io7m.icatiro.model.IcTicketCreation;
import com.io7m.icatiro.model.IcTicketID;
import com.io7m.icatiro.model.IcTicketSearch;
import com.io7m.icatiro.model.IcTicketSummary;
import com.io7m.icatiro.model.IcTicketTitle;
import com.io7m.idstore.model.IdName;
import com.io7m.jqpage.core.JQKeysetRandomAccessPageDefinition;
import com.io7m.jqpage.core.JQKeysetRandomAccessPagination;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.TableOnConditionStep;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

import static com.io7m.icatiro.database.postgres.internal.IcDatabaseExceptions.handleDatabaseException;
import static com.io7m.icatiro.database.postgres.internal.Tables.AUDIT;
import static com.io7m.icatiro.database.postgres.internal.Tables.TICKETS;
import static com.io7m.icatiro.database.postgres.internal.Tables.TICKET_COMMENTS;
import static com.io7m.icatiro.database.postgres.internal.Tables.USERS;
import static com.io7m.icatiro.database.postgres.internal.tables.Projects.PROJECTS;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.PROJECT_NONEXISTENT;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.TICKET_COMMENT_NONEXISTENT;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.TICKET_NONEXISTENT;
import static com.io7m.icatiro.model.IcPermission.TICKET_READ;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DB_STATEMENT;
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

  private static IcTicketSummary mapTicketWithPermissions(
    final org.jooq.Record record)
  {
    return new IcTicketSummary(
      new IcProjectTitle(record.get(PROJECTS.NAME_DISPLAY)),
      new IcProjectShortName(record.get(PROJECTS.NAME_SHORT)),
      new IcTicketID(
        new IcProjectID(record.get(TICKETS.PROJECT)),
        record.get(TICKETS.ID)
      ),
      new IcTicketTitle(record.get(TICKETS.TITLE)),
      record.get(TICKETS.TIME_CREATED),
      record.get(TICKETS.TIME_UPDATED),
      record.get(TICKETS.REPORTER),
      new IdName(record.get(USERS.NAME))
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
    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var userId =
      transaction.userId();
    final var querySpan =
      transaction.createQuerySpan(
        "IdDatabaseTicketsQueries.ticketCreate");

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
      newTicket.setDescription(creation.description());
      newTicket.store();

      final var newId =
        newTicket.getId().longValue();

      context.insertInto(AUDIT)
        .set(AUDIT.USER_ID, userId)
        .set(AUDIT.TIME, this.currentTime())
        .set(AUDIT.MESSAGE, toUnsignedString(newId))
        .set(AUDIT.TYPE, "TICKET_CREATED")
        .execute();

      return new IcTicketSummary(
        new IcProjectTitle(project.getNameDisplay()),
        new IcProjectShortName(project.getNameShort()),
        new IcTicketID(new IcProjectID(project.getId().longValue()), newId),
        new IcTicketTitle(newTicket.getTitle()),
        timeNow,
        timeNow,
        userId,
        new IdName(user.getName())
      );
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public IcTicketComment ticketCommentCreate(
    final IcTicketCommentCreation creation)
    throws IcDatabaseException
  {
    Objects.requireNonNull(creation, "creation");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var userId =
      transaction.userId();
    final var querySpan =
      transaction.createQuerySpan(
        "IdDatabaseTicketsQueries.ticketCommentCreate");

    try {
      final var ticketId = creation.ticket();
      if (!checkTicketExists(context, ticketId)) {
        throw new IcDatabaseException(
          "No such ticket %s".formatted(ticketId),
          TICKET_NONEXISTENT
        );
      }

      final var timeNow =
        this.currentTime();
      final var inReplyTo =
        creation.commentRepliedTo();

      /*
       * If a comment is replying to a comment, then check that the comment
       * is actually on this ticket.
       */

      if (inReplyTo.isPresent()) {
        final var replyComment =
          valueOf(inReplyTo.getAsLong());
        final var existingTicketOpt =
          context.selectFrom(TICKET_COMMENTS)
            .where(TICKET_COMMENTS.ID.eq(replyComment))
            .fetchOptional();

        if (existingTicketOpt.isEmpty()) {
          throw new IcDatabaseException(
            "No such ticket comment %s".formatted(replyComment),
            TICKET_COMMENT_NONEXISTENT
          );
        }

        final var existingTicket = existingTicketOpt.get();
        if (existingTicket.getTicketId().longValue() != ticketId.value()) {
          throw new IcDatabaseException(
            "No such ticket comment %s".formatted(replyComment),
            TICKET_COMMENT_NONEXISTENT
          );
        }
      }

      final var newComment = context.newRecord(TICKET_COMMENTS);
      newComment.set(TICKET_COMMENTS.TICKET_ID, valueOf(ticketId.value()));
      newComment.set(TICKET_COMMENTS.TIME, timeNow);
      newComment.set(TICKET_COMMENTS.TEXT, creation.text());

      if (inReplyTo.isPresent()) {
        final var replyComment = valueOf(inReplyTo.getAsLong());
        newComment.set(
          TICKET_COMMENTS.TICKET_REPLIED_TO,
          replyComment
        );
      }

      newComment.set(TICKET_COMMENTS.OWNER, userId);
      newComment.store();

      final var newId =
        newComment.getId().longValue();

      context.insertInto(AUDIT)
        .set(AUDIT.USER_ID, userId)
        .set(AUDIT.TIME, this.currentTime())
        .set(AUDIT.MESSAGE, toUnsignedString(newId))
        .set(AUDIT.TYPE, "TICKET_COMMENT_CREATED")
        .execute();

      return new IcTicketComment(
        ticketId,
        timeNow,
        userId,
        newId,
        inReplyTo,
        creation.text()
      );
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public Optional<IcTicket> ticketGet(
    final IcTicketID id)
    throws IcDatabaseException
  {
    Objects.requireNonNull(id, "id");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "IdDatabaseTicketsQueries.ticketGet");

    try {
      final var baseTable =
        TICKETS
          .join(PROJECTS).on(PROJECTS.ID.eq(TICKETS.PROJECT))
          .join(USERS).on(USERS.ID.eq(TICKETS.REPORTER));

      final var ticketQuery =
        context.selectFrom(baseTable)
          .where(TICKETS.ID.eq(valueOf(id.value())));

      final var ticketRecordOpt = ticketQuery.fetchOptional();
      if (ticketRecordOpt.isEmpty()) {
        return Optional.empty();
      }

      final var ticketRecord =
        ticketRecordOpt.get();

      final var selectComments =
        context.selectFrom(TICKET_COMMENTS)
          .where(TICKET_COMMENTS.TICKET_ID.eq(valueOf(id.value())))
          .orderBy(TICKET_COMMENTS.TIME.asc());

      final var comments =
        selectComments.fetch()
          .map(r -> {
            return new IcTicketComment(
              id,
              r.getTime(),
              r.getOwner(),
              r.getId().longValue(),
              toOptionalLong(r.getTicketRepliedTo()),
              r.getText()
            );
          });

      return Optional.of(
        new IcTicket(
          id,
          new IcTicketTitle(ticketRecord.get(TICKETS.TITLE)),
          ticketRecord.get(TICKETS.TIME_CREATED),
          ticketRecord.get(TICKETS.TIME_UPDATED),
          ticketRecord.get(TICKETS.REPORTER),
          new IdName(ticketRecord.get(USERS.NAME)),
          ticketRecord.get(TICKETS.DESCRIPTION),
          comments
        )
      );
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e);
    } finally {
      querySpan.end();
    }
  }

  private static OptionalLong toOptionalLong(
    final Long id)
  {
    if (id == null) {
      return OptionalLong.empty();
    }
    return OptionalLong.of(id.longValue());
  }

  @Override
  public IcTicket ticketGetRequire(
    final IcTicketID id)
    throws IcDatabaseException
  {
    return this.ticketGet(id)
      .orElseThrow(() -> {
        return new IcDatabaseException(
          "No such ticket %s".formatted(id),
          TICKET_NONEXISTENT
        );
      });
  }

  @Override
  public boolean ticketExists(
    final IcTicketID id)
    throws IcDatabaseException
  {
    Objects.requireNonNull(id, "id");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "IdDatabaseTicketsQueries.ticketExists");

    try {
      return checkTicketExists(context, id);
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e);
    } finally {
      querySpan.end();
    }
  }

  private static boolean checkTicketExists(
    final DSLContext context,
    final IcTicketID id)
  {
    return context.fetchExists(
      TICKETS.where(
        DSL.condition(TICKETS.PROJECT.eq(valueOf(id.project().value())))
          .and(DSL.condition(TICKETS.ID.eq(valueOf(id.value()))))
      )
    );
  }

  @Override
  public IcDatabaseTicketSearchType ticketSearch(
    final IcTicketSearch parameters)
    throws IcDatabaseException
  {
    Objects.requireNonNull(parameters, "parameters");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "IdDatabaseTicketsQueries.ticketSearch.create");

    try {
      final var observer =
        transaction.userId();

      final var baseTable =
        TICKETS
          .join(PROJECTS).on(PROJECTS.ID.eq(TICKETS.PROJECT))
          .join(USERS).on(USERS.ID.eq(TICKETS.REPORTER));

      /*
       * The permission condition is responsible for filtering out
       * tickets with which the user has no permission to read.
       */

      final var permissionCondition =
        DSL.condition(
          "permission_is_allowed(?, TICKETS.PROJECT, TICKETS.ID, ?)",
          observer,
          Integer.valueOf(TICKET_READ.value())
        );

      /*
       * The tickets must lie within the given time ranges.
       */

      final var timeCreatedRange = parameters.timeCreatedRange();
      final var timeCreatedCondition =
        DSL.condition(
          TICKETS.TIME_CREATED.ge(timeCreatedRange.timeLower())
            .and(TICKETS.TIME_CREATED.le(timeCreatedRange.timeUpper()))
        );

      final var timeUpdatedRange = parameters.timeUpdatedRange();
      final var timeUpdatedCondition =
        DSL.condition(
          TICKETS.TIME_UPDATED.ge(timeUpdatedRange.timeLower())
            .and(TICKETS.TIME_UPDATED.le(timeUpdatedRange.timeUpper()))
        );

      Condition whereCondition = permissionCondition;
      whereCondition = whereCondition.and(timeCreatedCondition);
      whereCondition = whereCondition.and(timeUpdatedCondition);

      /*
       * Filter by reporter if requested.
       */

      final var reporterOpt = parameters.reporter();
      if (reporterOpt.isPresent()) {
        whereCondition =
          whereCondition.and(TICKETS.REPORTER.eq(reporterOpt.get()));
      }

      /*
       * Do fulltext title and description searches if requested.
       */

      final var titleOpt = parameters.titleSearch();
      if (titleOpt.isPresent()) {
        whereCondition =
          whereCondition.and(
            DSL.condition(
              "? @@ to_tsquery('english', ?)",
              TICKETS.TITLE,
              titleOpt.get()
            )
          );
      }

      final var descriptionOpt = parameters.descriptionSearch();
      if (descriptionOpt.isPresent()) {
        whereCondition =
          whereCondition.and(
            DSL.condition(
              "? @@ to_tsquery('english', ?)",
              TICKETS.DESCRIPTION,
              descriptionOpt.get()
            )
          );
      }

      final var query =
        baseTable.where(whereCondition);

      final var pages =
        JQKeysetRandomAccessPagination.createPageDefinitions(
          context,
          query,
          List.of(orderField(parameters.ordering())),
          Integer.toUnsignedLong(parameters.limit()),
          statement -> {
            querySpan.setAttribute(DB_STATEMENT, statement.toString());
          }
        );

      return new TicketSearch(
        baseTable,
        whereCondition,
        pages
      );
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e);
    } finally {
      querySpan.end();
    }
  }

  private static final class TicketSearch
    extends IcAbstractSearch<IcDatabaseTicketsQueries, IcDatabaseTicketsQueriesType, IcTicketSummary>
    implements IcDatabaseTicketSearchType
  {
    private final TableOnConditionStep<Record> baseTable;
    private final Condition whereCondition;

    TicketSearch(
      final TableOnConditionStep<Record> inBaseTable,
      final Condition inWhereCondition,
      final List<JQKeysetRandomAccessPageDefinition> pages)
    {
      super(pages);

      this.baseTable =
        Objects.requireNonNull(inBaseTable, "baseTable");
      this.whereCondition =
        Objects.requireNonNull(inWhereCondition, "whereCondition");
    }

    @Override
    protected IcPage<IcTicketSummary> page(
      final IcDatabaseTicketsQueries queries,
      final JQKeysetRandomAccessPageDefinition page)
      throws IcDatabaseException
    {
      final var transaction =
        queries.transaction();
      final var context =
        transaction.createContext();

      final var querySpan =
        transaction.createQuerySpan(
          "IdDatabaseTicketsQueries.ticketSearch.page");

      try {
        final var query =
          context.selectFrom(this.baseTable)
            .where(this.whereCondition)
            .orderBy(page.orderBy());

        final var seek = page.seek();
        final Select<?> select;
        if (seek.length != 0) {
          select = query.seek(seek).limit(valueOf(page.limit()));
        } else {
          select = query.limit(valueOf(page.limit()));
        }

        querySpan.setAttribute(DB_STATEMENT, select.toString());

        final var items =
          select.fetch()
            .map(IcDatabaseTicketsQueries::mapTicketWithPermissions);

        return new IcPage<>(
          items,
          (int) page.index(),
          this.pageCount(),
          page.firstOffset()
        );
      } catch (final DataAccessException e) {
        querySpan.recordException(e);
        throw handleDatabaseException(transaction, e);
      } finally {
        querySpan.end();
      }
    }
  }

  private static Field<?> orderField(
    final IcTicketColumnOrdering ordering)
  {
    return switch (ordering.column()) {
      case BY_ID -> TICKETS.ID;
      case BY_TITLE -> TICKETS.TITLE;
      case BY_TIME_CREATED -> TICKETS.TIME_CREATED;
      case BY_TIME_UPDATED -> TICKETS.TIME_UPDATED;
    };
  }
}
