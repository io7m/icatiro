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

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.TableLike;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.List;

final class IcDatabaseGenericPaginatedQueries
{
  private IcDatabaseGenericPaginatedQueries()
  {

  }

  public static List<IcDatabasePageDefinition> createPageDefinitions(
    final DSLContext context,
    final TableLike<?> table,
    final List<Field<?>> fields,
    final int pageSize)
  {
    /*
     * An object is on a page boundary if the row number is exactly
     * divisible by the page size. This can be calculated on the
     * database side with a window function.
     */

    final var pageBoundaryExpression =
      DSL.rowNumber()
        .over(DSL.orderBy(fields))
        .modulo(DSL.inline(pageSize));

    final var casePageBoundary =
      DSL.case_(pageBoundaryExpression)
        .when(DSL.inline(0), DSL.inline(true))
        .else_(DSL.inline(false))
        .as("is_page_boundary");

    final var innerSelects = new ArrayList<>(fields);
    innerSelects.add(casePageBoundary);

    final var innerPageBoundaries =
      context.select(innerSelects)
        .from(table)
        .orderBy(fields)
        .asTable("inner");

    final ArrayList<Field<?>> innerFields = new ArrayList<>();
    for (final var field : fields) {
      innerFields.add(innerPageBoundaries.field(field));
    }

    /*
     * Use a window function to calculate page numbers. Select records
     * but only return those rows where is_page_boundary is true.
     */

    final var pageNumberExpression =
      DSL.rowNumber()
        .over(DSL.orderBy(innerFields))
        .plus(DSL.inline(1))
        .as("page_number");

    final ArrayList<Field<?>> outerSelects = new ArrayList<>(innerFields);
    outerSelects.add(pageNumberExpression);

    final var outerPageBoundaries =
      context.select(outerSelects)
        .from(innerPageBoundaries)
        .where(DSL.condition(innerPageBoundaries.field("is_page_boundary").isTrue()));

    final var pages =
      new ArrayList<IcDatabasePageDefinition>();
    final var result =
      outerPageBoundaries.fetch();

    pages.add(new IcDatabasePageDefinition(List.of(), 0));
    for (final var record : result) {
      final var values = new ArrayList<>(fields.size());
      for (final var field : fields) {
        values.add(record.get(field));
      }
      pages.add(
        new IcDatabasePageDefinition(
          values,
          record.<Integer>getValue("page_number", Integer.class).intValue()
        )
      );
    }
    return pages;
  }
}
