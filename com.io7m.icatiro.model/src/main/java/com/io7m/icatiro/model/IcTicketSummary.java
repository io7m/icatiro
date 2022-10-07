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
import java.util.Objects;
import java.util.UUID;

/**
 * The short summary of a ticket.
 *
 * @param projectShortName The project short name
 * @param projectTitle     The project title
 * @param reporter         The ID of the reporter
 * @param reporterName     The name of the reporter
 * @param ticketId         The ticket ID
 * @param ticketTitle      The ticket title
 * @param timeCreated      The creation time for the ticket
 * @param timeUpdated      The last updated time for the ticket
 */

public record IcTicketSummary(
  IcProjectTitle projectTitle,
  IcProjectShortName projectShortName,
  IcTicketID ticketId,
  IcTicketTitle ticketTitle,
  OffsetDateTime timeCreated,
  OffsetDateTime timeUpdated,
  UUID reporter,
  IdName reporterName)
{
  /**
   * The short summary of a ticket.
   *
   * @param projectShortName The project short name
   * @param projectTitle     The project title
   * @param reporter         The ID of the reporter
   * @param reporterName     The name of the reporter
   * @param ticketId         The ticket ID
   * @param ticketTitle      The ticket title
   * @param timeCreated      The creation time for the ticket
   * @param timeUpdated      The last updated time for the ticket
   */

  public IcTicketSummary
  {
    Objects.requireNonNull(projectShortName, "projectShortName");
    Objects.requireNonNull(projectTitle, "projectTitle");
    Objects.requireNonNull(reporter, "reporter");
    Objects.requireNonNull(reporterName, "reporterName");
    Objects.requireNonNull(ticketId, "ticketId");
    Objects.requireNonNull(ticketTitle, "ticketTitle");
    Objects.requireNonNull(timeCreated, "timeCreated");
    Objects.requireNonNull(timeUpdated, "timeUpdated");
  }

  /**
   * @return The ticket short name
   */

  public String ticketShortName()
  {
    return "%s-%s".formatted(this.projectShortName, this.ticketId);
  }
}
