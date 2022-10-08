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
import com.io7m.icatiro.database.api.IcDatabaseProjectsQueriesType;
import com.io7m.icatiro.model.IcProject;
import com.io7m.icatiro.model.IcProjectID;
import com.io7m.icatiro.model.IcProjectShortName;
import com.io7m.icatiro.model.IcProjectTitle;
import org.jooq.exception.DataAccessException;

import java.util.Objects;

import static com.io7m.icatiro.database.postgres.internal.IcDatabaseExceptions.handleDatabaseException;
import static com.io7m.icatiro.database.postgres.internal.Tables.AUDIT;
import static com.io7m.icatiro.database.postgres.internal.tables.Projects.PROJECTS;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.PROJECT_DUPLICATE;

final class IcDatabaseProjectsQueries
  extends IcBaseQueries
  implements IcDatabaseProjectsQueriesType
{
  protected IcDatabaseProjectsQueries(
    final IcDatabaseTransaction inTransaction)
  {
    super(inTransaction);
  }

  @Override
  public IcProject projectCreate(
    final IcProjectTitle title,
    final IcProjectShortName shortName)
    throws IcDatabaseException
  {
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(shortName, "shortName");

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var userId = transaction.userId();

    try {
      {
        final var p =
          context.fetchOne(PROJECTS, PROJECTS.NAME_DISPLAY.eq(title.value()));
        if (p != null) {
          throw new IcDatabaseException(
            "A project with title '%s' already exists."
              .formatted(title.value()),
            PROJECT_DUPLICATE
          );
        }
      }

      {
        final var p =
          context.fetchOne(PROJECTS, PROJECTS.NAME_SHORT.eq(shortName.value()));
        if (p != null) {
          throw new IcDatabaseException(
            "A project with short name '%s' already exists."
              .formatted(shortName.value()),
            PROJECT_DUPLICATE
          );
        }
      }

      final var newRecord = context.newRecord(PROJECTS);
      newRecord.setNameDisplay(title.value());
      newRecord.setNameShort(shortName.value());
      newRecord.store();

      final var newId =
        newRecord.getId().longValue();

      context.insertInto(AUDIT)
        .set(AUDIT.USER_ID, userId)
        .set(AUDIT.TIME, this.currentTime())
        .set(AUDIT.MESSAGE, Long.toUnsignedString(newId))
        .set(AUDIT.TYPE, "PROJECT_CREATED")
        .execute();

      return new IcProject(
        new IcProjectID(newId),
        title,
        shortName
      );
    } catch (final DataAccessException e) {
      throw handleDatabaseException(transaction, e);
    }
  }
}
