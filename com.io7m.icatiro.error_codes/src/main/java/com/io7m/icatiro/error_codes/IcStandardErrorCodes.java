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

package com.io7m.icatiro.error_codes;

/**
 * Standard error codes.
 */

public final class IcStandardErrorCodes
{
  private IcStandardErrorCodes()
  {

  }

  /**
   * A client sent a broken message of some kind.
   */

  public static final IcErrorCode PROTOCOL_ERROR =
    new IcErrorCode("error-protocol");

  /**
   * Authenticating a user or admin failed.
   */

  public static final IcErrorCode AUTHENTICATION_ERROR =
    new IcErrorCode("error-authentication");

  /**
   * An internal I/O error.
   */

  public static final IcErrorCode IO_ERROR =
    new IcErrorCode("error-io");

  /**
   * An internal serialization error.
   */

  public static final IcErrorCode SERIALIZATION_ERROR =
    new IcErrorCode("error-serialization");

  /**
   * An error raised by the Trasco database versioning library.
   */

  public static final IcErrorCode TRASCO_ERROR =
    new IcErrorCode("error-trasco");

  /**
   * An internal SQL database error.
   */

  public static final IcErrorCode SQL_ERROR =
    new IcErrorCode("error-sql");

  /**
   * An internal SQL database error relating to database revisioning.
   */

  public static final IcErrorCode SQL_REVISION_ERROR =
    new IcErrorCode("error-sql-revision");

  /**
   * A violation of an SQL foreign key integrity constraint.
   */

  public static final IcErrorCode SQL_ERROR_FOREIGN_KEY =
    new IcErrorCode("error-sql-foreign-key");

  /**
   * A violation of an SQL uniqueness constraint.
   */

  public static final IcErrorCode SQL_ERROR_UNIQUE =
    new IcErrorCode("error-sql-unique");

  /**
   * An attempt was made to use a query class that is unsupported.
   */

  public static final IcErrorCode SQL_ERROR_UNSUPPORTED_QUERY_CLASS =
    new IcErrorCode("error-sql-unsupported-query-class");

  /**
   * A generic "operation not permitted" error.
   */

  public static final IcErrorCode OPERATION_NOT_PERMITTED =
    new IcErrorCode("error-operation-not-permitted");

  /**
   * An action was denied by the security policy.
   */

  public static final IcErrorCode SECURITY_POLICY_DENIED =
    new IcErrorCode("error-security-policy-denied");

  /**
   * The wrong HTTP method was used.
   */

  public static final IcErrorCode HTTP_METHOD_ERROR =
    new IcErrorCode("error-http-method");

  /**
   * An HTTP parameter was required but missing.
   */

  public static final IcErrorCode HTTP_PARAMETER_NONEXISTENT =
    new IcErrorCode("error-http-parameter-nonexistent");

  /**
   * An HTTP parameter had an invalid value.
   */

  public static final IcErrorCode HTTP_PARAMETER_INVALID =
    new IcErrorCode("error-http-parameter-invalid");

  /**
   * An HTTP request exceeded the size limit.
   */

  public static final IcErrorCode HTTP_SIZE_LIMIT =
    new IcErrorCode("error-http-size-limit");


  /**
   * An attempt was made to create a user that already exists.
   */

  public static final IcErrorCode USER_DUPLICATE =
    new IcErrorCode("error-user-duplicate");

  /**
   * An attempt was made to create a user that already exists (ID conflict).
   */

  public static final IcErrorCode USER_DUPLICATE_ID =
    new IcErrorCode("error-user-duplicate-id");

  /**
   * An attempt was made to create a user that already exists (Name conflict).
   */

  public static final IcErrorCode USER_DUPLICATE_NAME =
    new IcErrorCode("error-user-duplicate-name");

  /**
   * An attempt was made to create a user that already exists (Email conflict).
   */

  public static final IcErrorCode USER_DUPLICATE_EMAIL =
    new IcErrorCode("error-user-duplicate-email");

  /**
   * An attempt was made to reference a user that does not exist.
   */

  public static final IcErrorCode USER_NONEXISTENT =
    new IcErrorCode("error-user-nonexistent");

  /**
   * An attempt was made to perform an operation that requires a user.
   */

  public static final IcErrorCode USER_UNSET =
    new IcErrorCode("error-user-unset");

  /**
   * An attempt was made to create an initial user in a database, but a user
   * already existed.
   */

  public static final IcErrorCode USER_NOT_INITIAL =
    new IcErrorCode("error-user-not-initial");

  /**
   * A problem occurred with the format of a password (such as an unsupported
   * password algorithm).
   */

  public static final IcErrorCode PASSWORD_ERROR =
    new IcErrorCode("error-password");

  /**
   * An attempt was made to create a project that already exists.
   */

  public static final IcErrorCode PROJECT_DUPLICATE =
    new IcErrorCode("error-project-duplicate");

  /**
   * An attempt was made to reference a project that does not exist.
   */

  public static final IcErrorCode PROJECT_NONEXISTENT =
    new IcErrorCode("error-project-nonexistent");

}
