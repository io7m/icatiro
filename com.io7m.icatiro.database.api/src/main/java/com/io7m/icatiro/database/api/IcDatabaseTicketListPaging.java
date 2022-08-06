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

import com.io7m.icatiro.model.IcTicketListParameters;
import com.io7m.icatiro.model.IcTicketOrdering;
import com.io7m.icatiro.model.IcTicketSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A paging handler for ticket summaries.
 */

public final class IcDatabaseTicketListPaging
  implements IcDatabaseTicketListPagingType
{
  private final UUID user;
  private final IcTicketListParameters parameters;
  private volatile int currentPage;
  private final ConcurrentHashMap<Integer, Page> pages;

  private IcDatabaseTicketListPaging(
    final UUID inUser,
    final IcTicketListParameters inParameters)
  {
    this.user =
      Objects.requireNonNull(inUser, "user");
    this.parameters =
      Objects.requireNonNull(inParameters, "parameters");

    this.currentPage = 0;
    this.pages = new ConcurrentHashMap<Integer, Page>();
    this.pages.put(Integer.valueOf(0), new Page());
  }

  private static final class Page
  {
    private volatile Optional<List<Object>> seek = Optional.empty();

    Page()
    {

    }
  }

  /**
   * A paging handler for ticket summaries.
   *
   * @param user         The user
   * @param inParameters The ticket list parameters
   *
   * @return A paging handler
   */

  public static IcDatabaseTicketListPagingType create(
    final UUID user,
    final IcTicketListParameters inParameters)
  {
    return new IcDatabaseTicketListPaging(user, inParameters);
  }

  @Override
  public IcTicketListParameters pageParameters()
  {
    return this.parameters;
  }

  @Override
  public int pageNumber()
  {
    return this.currentPage;
  }

  @Override
  public boolean pageNextAvailable()
  {
    return this.pages.containsKey(Integer.valueOf(this.currentPage + 1));
  }

  @Override
  public List<IcTicketSummary> pagePrevious(
    final IcDatabaseTicketsQueriesType queries)
    throws IcDatabaseException
  {
    if (this.currentPage > 0) {
      this.currentPage = this.currentPage - 1;
    }
    return this.pageCurrent(queries);
  }

  @Override
  public List<IcTicketSummary> pageNext(
    final IcDatabaseTicketsQueriesType queries)
    throws IcDatabaseException
  {
    final var nextPage = this.currentPage + 1;
    if (this.pages.containsKey(Integer.valueOf(nextPage))) {
      this.currentPage = nextPage;
    }
    return this.pageCurrent(queries);
  }

  @Override
  public List<IcTicketSummary> pageCurrent(
    final IcDatabaseTicketsQueriesType queries)
    throws IcDatabaseException
  {
    final var page =
      this.pages.get(Integer.valueOf(this.currentPage));

    final var results =
      queries.ticketListWithPermissions(
        this.user,
        this.parameters.timeCreatedRange(),
        this.parameters.timeUpdatedRange(),
        this.parameters.ordering(),
        this.parameters.limit(),
        page.seek
      );

    if (results.size() == this.parameters.limit()) {
      final var nextPage =
        this.pages.computeIfAbsent(
          Integer.valueOf(this.currentPage + 1),
          integer -> new Page()
        );

      final var lastSummary = results.get(results.size() - 1);
      nextPage.seek = Optional.of(
        fieldsForSeek(lastSummary, this.parameters.ordering())
      );
    }

    return results;
  }


  private static List<Object> fieldsForSeek(
    final IcTicketSummary summary,
    final IcTicketOrdering ordering)
  {
    final var orderColumns =
      ordering.ordering();
    final var columnCount =
      orderColumns.size();
    final var fields =
      new ArrayList<>(columnCount);

    for (int index = 0; index < columnCount; ++index) {
      final var columnOrdering = orderColumns.get(index);
      fields.add(
        switch (columnOrdering.column()) {
          case BY_ID -> {
            yield Long.valueOf(summary.ticketId().value());
          }
          case BY_TITLE -> {
            yield summary.ticketTitle().value();
          }
          case BY_TIME_CREATED -> {
            yield summary.timeCreated();
          }
          case BY_TIME_UPDATED -> {
            yield summary.timeUpdated();
          }
        });
      Objects.requireNonNull(fields.get(index), "fields.get(index)");
    }

    return List.copyOf(fields);
  }
}
