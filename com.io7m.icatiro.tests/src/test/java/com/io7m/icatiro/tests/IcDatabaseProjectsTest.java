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

import com.io7m.icatiro.database.api.IcDatabaseException;
import com.io7m.icatiro.database.api.IcDatabaseProjectsQueriesType;
import com.io7m.icatiro.database.api.IcDatabaseTicketsQueriesType;
import com.io7m.icatiro.model.IcProjectID;
import com.io7m.icatiro.model.IcProjectShortName;
import com.io7m.icatiro.model.IcProjectTitle;
import com.io7m.icatiro.model.IcTicketCreation;
import com.io7m.icatiro.model.IcTicketTitle;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;

import static com.io7m.icatiro.database.api.IcDatabaseRole.ICATIRO;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.PROJECT_DUPLICATE;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.PROJECT_NONEXISTENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class IcDatabaseProjectsTest extends IcWithDatabaseContract
{
  /**
   * Project titles must be unique.
   *
   * @throws Exception On errors
   */

  @Test
  public void testProjectCreateUniqueTitle()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var user =
      this.databaseCreateUserInitial("someone", "12345678");
    final var transaction =
      this.transactionOf(ICATIRO);

    transaction.userIdSet(user);

    final var projects =
      transaction.queries(IcDatabaseProjectsQueriesType.class);

    final var p0 =
      projects.projectCreate(
        new IcProjectTitle("TITLE"),
        new IcProjectShortName("SOME")
      );

    final var ex =
      assertThrows(IcDatabaseException.class, () -> {
        projects.projectCreate(
          new IcProjectTitle("TITLE"),
          new IcProjectShortName("ELSE")
        );
      });

    assertEquals(PROJECT_DUPLICATE, ex.errorCode());
  }

  /**
   * Project short names must be unique.
   *
   * @throws Exception On errors
   */

  @Test
  public void testProjectCreateUniqueShortName()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var user =
      this.databaseCreateUserInitial("someone", "12345678");
    final var transaction =
      this.transactionOf(ICATIRO);

    transaction.userIdSet(user);

    final var projects =
      transaction.queries(IcDatabaseProjectsQueriesType.class);

    final var p0 =
      projects.projectCreate(
        new IcProjectTitle("OTHER"),
        new IcProjectShortName("SOME")
      );

    final var ex =
      assertThrows(IcDatabaseException.class, () -> {
        projects.projectCreate(
          new IcProjectTitle("TITLE"),
          new IcProjectShortName("SOME")
        );
      });

    assertEquals(PROJECT_DUPLICATE, ex.errorCode());
  }
}
