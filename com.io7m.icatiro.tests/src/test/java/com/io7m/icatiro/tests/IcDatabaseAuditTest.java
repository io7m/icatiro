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

import com.io7m.icatiro.database.api.IcDatabaseAuditQueriesType;
import com.io7m.icatiro.database.api.IcDatabaseUsersQueriesType;
import com.io7m.icatiro.model.IcAuditSearchParameters;
import com.io7m.icatiro.model.IcPermissionSet;
import com.io7m.icatiro.model.IcTimeRange;
import com.io7m.icatiro.model.IcUser;
import com.io7m.idstore.model.IdName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public final class IcDatabaseAuditTest extends IcWithDatabaseContract
{
  @Test
  public void testAuditSearch()
    throws Exception
  {
    this.withTransaction(transaction -> {
      final var q =
        transaction.queries(IcDatabaseAuditQueriesType.class);
      final var u =
        transaction.queries(IcDatabaseUsersQueriesType.class);

      final var uid = UUID.randomUUID();
      u.userPut(new IcUser(
        uid,
        new IdName("x"),
        List.of(),
        IcPermissionSet.empty()
      ));

      for (int index = 0; index < 1000; ++index) {
        q.auditPut(uid, OffsetDateTime.now(), "T", Integer.toUnsignedString(index));
      }

      q.auditEvents(new IcAuditSearchParameters(
        IcTimeRange.largest(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        100
      ), OptionalLong.empty());
      return null;
    });
  }
}
