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

import com.io7m.icatiro.model.IcPage;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.function.Function;

final class IcDatabaseGenericPagination
{
  private IcDatabaseGenericPagination()
  {

  }

  static <T> IcPage<T> paginate(
    final DSLContext context,
    final Select<?> query,
    final Field<?>[] sort,
    final long limit,
    final long offset,
    final Function<Record, T> fromRecord)
  {
    final var pageQuery =
      paginateInner(context, query, sort, limit, offset);

    final var results =
      pageQuery.fetch();
    final var items =
      new ArrayList<T>();

    int pageIndex = 0;
    int pageCount = 0;
    Long pageFirstOffset = null;

    for (final var record : results) {
      if (pageFirstOffset == null) {
        pageFirstOffset =
          record.get("pageItemIndex", Long.class);
        final var pageItemsTotal =
          record.get("pageItemsTotal", Double.class)
            .doubleValue();
        pageCount = (int) Math.ceil(pageItemsTotal / (double) limit);
      }
      pageIndex =
        record.get("pageIndexCurrent", Integer.class)
          .intValue();
      items.add(fromRecord.apply(record));
    }

    if (pageFirstOffset == null) {
      pageFirstOffset = Long.valueOf(0L);
    }

    return new IcPage<>(
      items,
      pageIndex,
      pageCount,
      pageFirstOffset.longValue()
    );
  }

  static Select<?> paginateInner(
    final DSLContext context,
    final Select<?> query,
    final Field<?>[] sort,
    final long limit,
    final long offset)
  {
    final var u =
      query.asTable("u");

    final var pageItemsTotal =
      DSL.count().over()
        .as("pageItemsTotal");

    final var pageItemIndex =
      DSL.rowNumber().over().orderBy(u.fields(sort))
        .as("pageItemIndex");

    final var t =
      context.select(u.asterisk())
        .select(pageItemsTotal, pageItemIndex)
        .from(u)
        .orderBy(u.fields(sort))
        .limit(Long.valueOf(limit))
        .offset(Long.valueOf(offset));

    final var pageSize =
      DSL.count().over()
        .as("pageSize");

    final var pageIndexLast =
      DSL.field(DSL.max(t.field(pageItemIndex)).over().eq(t.field(pageItemsTotal)))
        .as("pageIndexLast");

    final var pageIndexCurrent =
      t.field(pageItemIndex)
        .minus(DSL.inline(1))
        .div(Long.valueOf(limit))
        .plus(DSL.inline(1))
        .as("pageIndexCurrent");

    return context.select(t.fields(query.getSelect().toArray(Field[]::new)))
      .select(
        pageSize,
        pageIndexLast,
        t.field(pageItemsTotal),
        t.field(pageItemIndex),
        pageIndexCurrent)
      .from(t)
      .orderBy(t.fields(sort));
  }
}
