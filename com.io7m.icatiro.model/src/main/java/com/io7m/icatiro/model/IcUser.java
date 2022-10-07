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

package com.io7m.icatiro.model;

import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Information for a single user.
 *
 * @param id          The user's ID
 * @param name        The user's name
 * @param emails      The user's emails
 * @param permissions The user's permissions
 */

public record IcUser(
  UUID id,
  IdName name,
  List<IdEmail> emails,
  IcPermissionSet permissions)
{
  /**
   * Information for a single user.
   *
   * @param id          The user's ID
   * @param name        The user's name
   * @param emails      The user's emails
   * @param permissions The user's permissions
   */

  public IcUser
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(emails, "emails");
    Objects.requireNonNull(permissions, "permissions");
  }
}
