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

import com.io7m.idstore.model.IdName;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A ticket.
 *
 * @param id           The ticket ID
 * @param title        The title
 * @param timeCreated  The creation time
 * @param timeUpdated  The update time
 * @param reporter     The reporter
 * @param reporterName The reporter name
 * @param description  The description
 * @param comments     The comments
 */

public record IcTicket(
  IcTicketID id,
  IcTicketTitle title,
  OffsetDateTime timeCreated,
  OffsetDateTime timeUpdated,
  UUID reporter,
  IdName reporterName,
  String description,
  List<IcTicketComment> comments)
{
  /**
   * A ticket.
   *
   * @param id           The ticket ID
   * @param title        The title
   * @param timeCreated  The creation time
   * @param timeUpdated  The update time
   * @param reporter     The reporter
   * @param reporterName The reporter name
   * @param description  The description
   * @param comments     The comments
   */

  public IcTicket
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(timeCreated, "timeCreated");
    Objects.requireNonNull(timeUpdated, "timeUpdated");
    Objects.requireNonNull(reporter, "reporter");
    Objects.requireNonNull(reporterName, "reporterName");
    Objects.requireNonNull(description, "description");
    Objects.requireNonNull(comments, "comments");
  }
}
