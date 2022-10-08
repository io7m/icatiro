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

import com.io7m.icatiro.model.IcTicketColumnOrdering;
import com.io7m.icatiro.model.IcTicketCreation;
import com.io7m.icatiro.model.IcTicketSummary;
import com.io7m.icatiro.model.IcTimeRange;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The database queries involving tickets.
 */

public non-sealed interface IcDatabaseTicketsQueriesType
  extends IcDatabaseQueriesType
{
  /**
   * Create a new ticket.
   *
   * @param creation The creation information
   *
   * @return The new project
   *
   * @throws IcDatabaseException On errors
   */

  @IcDatabaseRequiresUser
  IcTicketSummary ticketCreate(
    IcTicketCreation creation)
    throws IcDatabaseException;

  /**
   * List tickets observable by the given user according to their permissions.
   *
   * @param user             The user
   * @param timeCreatedRange Only tickets created within this time range will be
   *                         included
   * @param timeUpdatedRange Only tickets updated within this time range will be
   *                         included
   * @param ordering         The field by which to order the list of tickets
   * @param limit            The limit on the number of items returned
   * @param seek             The record to which to seek, if any
   *
   * @return The tickets
   *
   * @throws IcDatabaseException On errors
   */

  List<IcTicketSummary> ticketListWithPermissions(
    UUID user,
    IcTimeRange timeCreatedRange,
    IcTimeRange timeUpdatedRange,
    IcTicketColumnOrdering ordering,
    int limit,
    Optional<Object> seek)
    throws IcDatabaseException;

  /**
   * Count tickets observable by the given user according to their permissions.
   *
   * @param user             The user
   * @param timeCreatedRange Only tickets created within this time range will be
   *                         included
   * @param timeUpdatedRange Only tickets updated within this time range will be
   *                         included
   *
   * @return The ticket count
   *
   * @throws IcDatabaseException On errors
   */

  long ticketListCountWithPermissions(
    UUID user,
    IcTimeRange timeCreatedRange,
    IcTimeRange timeUpdatedRange)
    throws IcDatabaseException;
}
