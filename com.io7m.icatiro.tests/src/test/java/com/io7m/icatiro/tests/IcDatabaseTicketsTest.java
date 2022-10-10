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


package com.io7m.icatiro.tests;

import com.io7m.icatiro.database.api.IcDatabaseProjectsQueriesType;
import com.io7m.icatiro.database.api.IcDatabaseTicketsQueriesType;
import com.io7m.icatiro.database.api.IcDatabaseUsersQueriesType;
import com.io7m.icatiro.model.IcPermission;
import com.io7m.icatiro.model.IcPermissionGlobal;
import com.io7m.icatiro.model.IcPermissionSet;
import com.io7m.icatiro.model.IcProjectShortName;
import com.io7m.icatiro.model.IcProjectTitle;
import com.io7m.icatiro.model.IcTicketColumn;
import com.io7m.icatiro.model.IcTicketColumnOrdering;
import com.io7m.icatiro.model.IcTicketCreation;
import com.io7m.icatiro.model.IcTicketSearch;
import com.io7m.icatiro.model.IcTicketSummary;
import com.io7m.icatiro.model.IcTicketTitle;
import com.io7m.icatiro.model.IcTimeRange;
import com.io7m.icatiro.model.IcUser;
import com.io7m.idstore.model.IdName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class IcDatabaseTicketsTest extends IcWithDatabaseContract
{
  /**
   * Basic ticket searches work.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTicketSearch()
    throws Exception
  {
    this.withTransaction(transaction -> {
      final var u =
        transaction.queries(IcDatabaseUsersQueriesType.class);
      final var p =
        transaction.queries(IcDatabaseProjectsQueriesType.class);
      final var t =
        transaction.queries(IcDatabaseTicketsQueriesType.class);

      final var uid = UUID.randomUUID();
      u.userPut(new IcUser(
        uid,
        new IdName("x"),
        List.of(),
        IcPermissionSet.of(
          List.of(
            new IcPermissionGlobal(IcPermission.TICKET_READ),
            new IcPermissionGlobal(IcPermission.TICKET_CREATE)
          )
        )
      ));

      transaction.userIdSet(uid);

      final var project =
        p.projectCreate(
          new IcProjectTitle("Project"),
          new IcProjectShortName("PROJECT")
        );

      for (int index = 0; index < 1000; ++index) {
        t.ticketCreate(
          new IcTicketCreation(
            project.id(),
            new IcTicketTitle("Ticket %d".formatted(index)),
            "Ticket description %d".formatted(index)
          )
        );
      }

      transaction.commit();

      final var parameters =
        new IcTicketSearch(
          IcTimeRange.largest(),
          IcTimeRange.largest(),
          new IcTicketColumnOrdering(IcTicketColumn.BY_ID, true),
          300,
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
        );

      final var search =
        t.ticketSearch(parameters);

      {
        final var page = search.pageCurrent(t);
        assertEquals(1, page.pageIndex());
        assertEquals(4, page.pageCount());
        assertEquals(0L, page.pageFirstOffset());
        assertEquals(300, page.items().size());
        checkTickets(0L, page.items());
      }

      {
        final var page = search.pageNext(t);
        assertEquals(2, page.pageIndex());
        assertEquals(4, page.pageCount());
        assertEquals(300L, page.pageFirstOffset());
        assertEquals(300, page.items().size());
        checkTickets(300L, page.items());
      }

      {
        final var page = search.pageNext(t);
        assertEquals(3, page.pageIndex());
        assertEquals(4, page.pageCount());
        assertEquals(600L, page.pageFirstOffset());
        assertEquals(300, page.items().size());
        checkTickets(600L, page.items());
      }

      {
        final var page = search.pageNext(t);
        assertEquals(4, page.pageIndex());
        assertEquals(4, page.pageCount());
        assertEquals(900L, page.pageFirstOffset());
        assertEquals(100, page.items().size());
        checkTickets(900L, page.items());
      }

      return null;
    });
  }

  /**
   * Basic ticket fulltext title searches work.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTicketTitleSearch()
    throws Exception
  {
    this.withTransaction(transaction -> {
      final var u =
        transaction.queries(IcDatabaseUsersQueriesType.class);
      final var p =
        transaction.queries(IcDatabaseProjectsQueriesType.class);
      final var t =
        transaction.queries(IcDatabaseTicketsQueriesType.class);

      final var uid = UUID.randomUUID();
      u.userPut(new IcUser(
        uid,
        new IdName("x"),
        List.of(),
        IcPermissionSet.of(
          List.of(
            new IcPermissionGlobal(IcPermission.TICKET_READ),
            new IcPermissionGlobal(IcPermission.TICKET_CREATE)
          )
        )
      ));

      transaction.userIdSet(uid);

      final var project =
        p.projectCreate(
          new IcProjectTitle("Project"),
          new IcProjectShortName("PROJECT")
        );

      for (int index = 0; index < 1000; ++index) {
        if (index % 2 == 0) {
          t.ticketCreate(
            new IcTicketCreation(
              project.id(),
              new IcTicketTitle("The quick brown fox jumped over the lazy dogs."),
              "Ticket description %d".formatted(index)
            )
          );
        } else {
          t.ticketCreate(
            new IcTicketCreation(
              project.id(),
              new IcTicketTitle("Ticket %d".formatted(index)),
              "Ticket description %d".formatted(index)
            )
          );
        }
      }

      transaction.commit();

      final var parameters =
        new IcTicketSearch(
          IcTimeRange.largest(),
          IcTimeRange.largest(),
          new IcTicketColumnOrdering(IcTicketColumn.BY_ID, true),
          300,
          Optional.of("fox"),
          Optional.empty(),
          Optional.empty()
        );

      final var search =
        t.ticketSearch(parameters);

      {
        final var page = search.pageCurrent(t);
        assertEquals(1, page.pageIndex());
        assertEquals(2, page.pageCount());
        assertEquals(0L, page.pageFirstOffset());
        assertEquals(300, page.items().size());
        checkTicketsFulltextTitle(page.items());
      }

      {
        final var page = search.pageNext(t);
        assertEquals(2, page.pageIndex());
        assertEquals(2, page.pageCount());
        assertEquals(300L, page.pageFirstOffset());
        assertEquals(200, page.items().size());
        checkTicketsFulltextTitle(page.items());
      }

      return null;
    });
  }

  /**
   * Basic ticket fulltext description searches work.
   *
   * @throws Exception On errors
   */

  @Test
  public void testTicketDescriptionSearch()
    throws Exception
  {
    this.withTransaction(transaction -> {
      final var u =
        transaction.queries(IcDatabaseUsersQueriesType.class);
      final var p =
        transaction.queries(IcDatabaseProjectsQueriesType.class);
      final var t =
        transaction.queries(IcDatabaseTicketsQueriesType.class);

      final var uid = UUID.randomUUID();
      u.userPut(new IcUser(
        uid,
        new IdName("x"),
        List.of(),
        IcPermissionSet.of(
          List.of(
            new IcPermissionGlobal(IcPermission.TICKET_READ),
            new IcPermissionGlobal(IcPermission.TICKET_CREATE)
          )
        )
      ));

      transaction.userIdSet(uid);

      final var project =
        p.projectCreate(
          new IcProjectTitle("Project"),
          new IcProjectShortName("PROJECT")
        );

      for (int index = 0; index < 1000; ++index) {
        if (index % 2 == 0) {
          t.ticketCreate(
            new IcTicketCreation(
              project.id(),
              new IcTicketTitle("Ticket %d".formatted(index)),
              "The quick brown fox jumped over the lazy dogs."
            )
          );
        } else {
          t.ticketCreate(
            new IcTicketCreation(
              project.id(),
              new IcTicketTitle("Ticket %d".formatted(index)),
              "Ticket description %d".formatted(index)
            )
          );
        }
      }

      transaction.commit();

      final var parameters =
        new IcTicketSearch(
          IcTimeRange.largest(),
          IcTimeRange.largest(),
          new IcTicketColumnOrdering(IcTicketColumn.BY_ID, true),
          300,
          Optional.empty(),
          Optional.of("fox"),
          Optional.empty()
        );

      final var search =
        t.ticketSearch(parameters);

      {
        final var page = search.pageCurrent(t);
        assertEquals(1, page.pageIndex());
        assertEquals(2, page.pageCount());
        assertEquals(0L, page.pageFirstOffset());
        assertEquals(300, page.items().size());
        checkTicketsFulltextDescription(0L, page.items());
      }

      {
        final var page = search.pageNext(t);
        assertEquals(2, page.pageIndex());
        assertEquals(2, page.pageCount());
        assertEquals(300L, page.pageFirstOffset());
        assertEquals(200, page.items().size());
        checkTicketsFulltextDescription(600L, page.items());
      }

      return null;
    });
  }

  private static void checkTickets(
    final long offset,
    final List<IcTicketSummary> items)
  {
    for (int index = 0; index < items.size(); ++index) {
      final var item = items.get(index);
      assertEquals(
        "Ticket %d".formatted(offset + index),
        item.ticketTitle().value()
      );
    }
  }

  private static void checkTicketsFulltextDescription(
    final long offset,
    final List<IcTicketSummary> items)
  {
    for (int index = 0; index < items.size(); ++index) {
      final var item = items.get(index);
      assertEquals(
        "Ticket %d".formatted(offset + (index * 2)),
        item.ticketTitle().value()
      );
    }
  }

  private static void checkTicketsFulltextTitle(
    final List<IcTicketSummary> items)
  {
    for (int index = 0; index < items.size(); ++index) {
      final var item = items.get(index);
      assertEquals(
        "The quick brown fox jumped over the lazy dogs.",
        item.ticketTitle().value()
      );
    }
  }
}
