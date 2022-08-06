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
import com.io7m.icatiro.database.api.IcDatabaseUsersQueriesType;
import com.io7m.icatiro.model.IcUserDisplayName;
import com.io7m.icatiro.model.IcUserEmail;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.io7m.icatiro.database.api.IcDatabaseRole.ICATIRO;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.USER_DUPLICATE_EMAIL;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.USER_DUPLICATE_ID;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.USER_DUPLICATE_NAME;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.USER_NONEXISTENT;
import static java.time.OffsetDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class IcDatabaseUsersTest extends IcWithDatabaseContract
{
  /**
   * Setting the transaction user to a nonexistent user fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserSetNonexistent()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(ICATIRO);

    final var ex =
      assertThrows(IcDatabaseException.class, () -> {
        transaction.userIdSet(randomUUID());
      });
    assertEquals(USER_NONEXISTENT, ex.errorCode());
  }

  /**
   * Creating a user works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUser()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateUserInitial(
        "admin",
        "12345678"
      );

    final var transaction =
      this.transactionOf(ICATIRO);

    transaction.userIdSet(adminId);

    final var users =
      transaction.queries(IcDatabaseUsersQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      databaseGenerateBadPassword();

    var user =
      users.userCreate(
        reqId,
        new IcUserDisplayName("someone"),
        new IcUserEmail("someone@example.com"),
        now,
        password
      );

    assertEquals("someone", user.name().value());
    assertEquals(reqId, user.id());
    assertEquals("someone@example.com", user.email().value());
    assertEquals(now.toEpochSecond(), user.created().toEpochSecond());
    assertEquals(now.toEpochSecond(), user.lastLoginTime().toEpochSecond());

    user = users.userGet(reqId).orElseThrow();
    assertEquals("someone", user.name().value());
    assertEquals(reqId, user.id());
    assertEquals("someone@example.com", user.email().value());
    assertEquals(now.toEpochSecond(), user.created().toEpochSecond());
    assertEquals(now.toEpochSecond(), user.lastLoginTime().toEpochSecond());

    user = users.userGetForEmail(new IcUserEmail("someone@example.com")).orElseThrow();
    assertEquals("someone", user.name().value());
    assertEquals(reqId, user.id());
    assertEquals("someone@example.com", user.email().value());
    assertEquals(now.toEpochSecond(), user.created().toEpochSecond());
    assertEquals(now.toEpochSecond(), user.lastLoginTime().toEpochSecond());

    user = users.userGetForName(new IcUserDisplayName("someone")).orElseThrow();
    assertEquals("someone", user.name().value());
    assertEquals(reqId, user.id());
    assertEquals("someone@example.com", user.email().value());
    assertEquals(now.toEpochSecond(), user.created().toEpochSecond());
    assertEquals(now.toEpochSecond(), user.lastLoginTime().toEpochSecond());

    checkAuditLog(
      transaction,
      new ExpectedEvent("USER_CREATED", adminId.toString()),
      new ExpectedEvent("USER_CREATED", reqId.toString())
    );
  }

  /**
   * Creating a user with a duplicate ID fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserDuplicateId()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateUserInitial("admin", "12345678");

    final var transaction =
      this.transactionOf(ICATIRO);

    transaction.userIdSet(adminId);

    final var users =
      transaction.queries(IcDatabaseUsersQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      databaseGenerateBadPassword();

    final var user =
      users.userCreate(
        reqId,
        new IcUserDisplayName("someone"),
        new IcUserEmail("someone@example.com"),
        now,
        password
      );

    final var ex =
      assertThrows(IcDatabaseException.class, () -> {
        users.userCreate(
          reqId,
          new IcUserDisplayName("someoneElse"),
          new IcUserEmail("someone2@example.com"),
          now,
          password
        );
      });

    assertEquals(USER_DUPLICATE_ID, ex.errorCode());
  }

  /**
   * Creating a user with a duplicate email fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserDuplicateEmail()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateUserInitial("admin", "12345678");

    final var transaction =
      this.transactionOf(ICATIRO);

    transaction.userIdSet(adminId);

    final var users =
      transaction.queries(IcDatabaseUsersQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      databaseGenerateBadPassword();

    final var user =
      users.userCreate(
        reqId,
        new IcUserDisplayName("someone"),
        new IcUserEmail("someone@example.com"),
        now,
        password
      );

    final var ex =
      assertThrows(IcDatabaseException.class, () -> {
        users.userCreate(
          randomUUID(),
          new IcUserDisplayName("someoneElse"),
          new IcUserEmail("someone@example.com"),
          now,
          password
        );
      });

    assertEquals(USER_DUPLICATE_EMAIL, ex.errorCode());
  }

  /**
   * Creating a user with a duplicate name fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserDuplicateName()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateUserInitial("admin", "12345678");

    final var transaction =
      this.transactionOf(ICATIRO);

    transaction.userIdSet(adminId);

    final var users =
      transaction.queries(IcDatabaseUsersQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      databaseGenerateBadPassword();

    final var user =
      users.userCreate(
        reqId,
        new IcUserDisplayName("someone"),
        new IcUserEmail("someone@example.com"),
        now,
        password
      );

    final var ex =
      assertThrows(IcDatabaseException.class, () -> {
        users.userCreate(
          randomUUID(),
          new IcUserDisplayName("someone"),
          new IcUserEmail("someone2@example.com"),
          now,
          password
        );
      });

    assertEquals(USER_DUPLICATE_NAME, ex.errorCode());
  }

  /**
   * Logging in works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserLogin()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateUserInitial("admin", "12345678");

    final var transaction =
      this.transactionOf(ICATIRO);

    transaction.userIdSet(adminId);

    final var users =
      transaction.queries(IcDatabaseUsersQueriesType.class);

    final var id =
      randomUUID();
    final var now =
      now();

    final var password =
      databaseGenerateBadPassword();

    final var user =
      users.userCreate(
        id,
        new IcUserDisplayName("someone"),
        new IcUserEmail("someone@example.com"),
        now,
        password
      );

    users.userLogin(user.id(), "127.0.0.1");

    checkAuditLog(
      transaction,
      new ExpectedEvent("USER_CREATED", adminId.toString()),
      new ExpectedEvent("USER_CREATED", id.toString()),
      new ExpectedEvent("USER_LOGGED_IN", "127.0.0.1")
    );
  }

  /**
   * Logging in fails for nonexistent users.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUserLoginNonexistent()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(ICATIRO);

    final var users =
      transaction.queries(IcDatabaseUsersQueriesType.class);

    final var ex =
      assertThrows(IcDatabaseException.class, () -> {
        users.userLogin(randomUUID(), "127.0.0.1");
      });
    assertEquals(USER_NONEXISTENT, ex.errorCode());
  }
}
