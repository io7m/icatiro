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

package com.io7m.icatiro.protocol.tickets.cb;

import com.io7m.cedarbridge.runtime.api.CBIntegerUnsigned32;
import com.io7m.cedarbridge.runtime.api.CBIntegerUnsigned64;
import com.io7m.cedarbridge.runtime.api.CBIntegerUnsigned8;
import com.io7m.cedarbridge.runtime.api.CBList;
import com.io7m.cedarbridge.runtime.api.CBSerializableType;
import com.io7m.cedarbridge.runtime.api.CBString;
import com.io7m.icatiro.model.IcPage;
import com.io7m.icatiro.model.IcPermission;
import com.io7m.icatiro.model.IcPermissionGlobal;
import com.io7m.icatiro.model.IcPermissionProjectwide;
import com.io7m.icatiro.model.IcPermissionScopedType;
import com.io7m.icatiro.model.IcPermissionSet;
import com.io7m.icatiro.model.IcPermissionTicketwide;
import com.io7m.icatiro.model.IcProject;
import com.io7m.icatiro.model.IcProjectID;
import com.io7m.icatiro.model.IcProjectShortName;
import com.io7m.icatiro.model.IcProjectTitle;
import com.io7m.icatiro.model.IcTicketColumn;
import com.io7m.icatiro.model.IcTicketColumnOrdering;
import com.io7m.icatiro.model.IcTicketCreation;
import com.io7m.icatiro.model.IcTicketID;
import com.io7m.icatiro.model.IcTicketListParameters;
import com.io7m.icatiro.model.IcTicketSummary;
import com.io7m.icatiro.model.IcTicketTitle;
import com.io7m.icatiro.model.IcTimeRange;
import com.io7m.icatiro.model.IcUser;
import com.io7m.icatiro.protocol.IcProtocolException;
import com.io7m.icatiro.protocol.IcProtocolMessageValidatorType;
import com.io7m.icatiro.protocol.tickets.IcTCommandLogin;
import com.io7m.icatiro.protocol.tickets.IcTCommandPermissionGrant;
import com.io7m.icatiro.protocol.tickets.IcTCommandProjectCreate;
import com.io7m.icatiro.protocol.tickets.IcTCommandTicketCreate;
import com.io7m.icatiro.protocol.tickets.IcTCommandTicketSearchBegin;
import com.io7m.icatiro.protocol.tickets.IcTCommandTicketSearchNext;
import com.io7m.icatiro.protocol.tickets.IcTCommandTicketSearchPrevious;
import com.io7m.icatiro.protocol.tickets.IcTCommandType;
import com.io7m.icatiro.protocol.tickets.IcTMessageType;
import com.io7m.icatiro.protocol.tickets.IcTResponseError;
import com.io7m.icatiro.protocol.tickets.IcTResponseLogin;
import com.io7m.icatiro.protocol.tickets.IcTResponsePermissionGrant;
import com.io7m.icatiro.protocol.tickets.IcTResponseProjectCreate;
import com.io7m.icatiro.protocol.tickets.IcTResponseTicketCreate;
import com.io7m.icatiro.protocol.tickets.IcTResponseTicketSearchBegin;
import com.io7m.icatiro.protocol.tickets.IcTResponseTicketSearchNext;
import com.io7m.icatiro.protocol.tickets.IcTResponseTicketSearchPrevious;
import com.io7m.icatiro.protocol.tickets.IcTResponseType;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static com.io7m.cedarbridge.runtime.api.CBBooleanType.fromBoolean;
import static com.io7m.cedarbridge.runtime.api.CBCore.string;
import static com.io7m.cedarbridge.runtime.api.CBCore.unsigned16;
import static com.io7m.cedarbridge.runtime.api.CBCore.unsigned64;
import static com.io7m.icatiro.error_codes.IcStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.icatiro.protocol.tickets.cb.Ic1TicketColumn.ByID;
import static com.io7m.icatiro.protocol.tickets.cb.Ic1TicketColumn.ByTimeCreated;
import static com.io7m.icatiro.protocol.tickets.cb.Ic1TicketColumn.ByTimeUpdated;
import static com.io7m.icatiro.protocol.tickets.cb.Ic1TicketColumn.ByTitle;

/**
 * Functions to translate between the core command set and the Tickets
 * Cedarbridge encoding command set.
 */

public final class IcT1Validation
  implements IcProtocolMessageValidatorType<IcTMessageType, ProtocolTicketsv1Type>
{
  /**
   * Functions to translate between the core command set and the Tickets
   * Cedarbridge encoding command set.
   */

  public IcT1Validation()
  {

  }


  @Override
  public ProtocolTicketsv1Type convertToWire(
    final IcTMessageType message)
    throws IcProtocolException
  {
    try {
      if (message instanceof IcTCommandType<?> c) {
        return toWireCommand(c);
      }
      if (message instanceof IcTResponseType r) {
        return toWireResponse(r);
      }
    } catch (final Exception e) {
      throw new IcProtocolException(PROTOCOL_ERROR, e.getMessage());
    }

    throw new IcProtocolException(
      PROTOCOL_ERROR,
      "Unrecognized message: %s".formatted(message)
    );
  }

  private static ProtocolTicketsv1Type toWireResponse(
    final IcTResponseType r)
    throws IcProtocolException
  {
    if (r instanceof IcTResponseLogin cc) {
      return toWireResponseLogin(cc);
    }
    if (r instanceof IcTResponseTicketSearchBegin cc) {
      return toWireResponseTicketSearchBegin(cc);
    }
    if (r instanceof IcTResponseTicketSearchNext cc) {
      return toWireResponseTicketSearchNext(cc);
    }
    if (r instanceof IcTResponseTicketSearchPrevious cc) {
      return toWireResponseTicketSearchPrevious(cc);
    }
    if (r instanceof IcTResponseProjectCreate cc) {
      return toWireResponseProjectCreate(cc);
    }
    if (r instanceof IcTResponseError cc) {
      return toWireResponseError(cc);
    }
    if (r instanceof IcTResponseTicketCreate cc) {
      return toWireResponseTicketCreate(cc);
    }
    if (r instanceof IcTResponsePermissionGrant cc) {
      return toWireResponsePermissionGrant(cc);
    }

    throw new IcProtocolException(
      PROTOCOL_ERROR,
      "Unrecognized message: %s".formatted(r)
    );
  }

  private static ProtocolTicketsv1Type toWireResponsePermissionGrant(
    final IcTResponsePermissionGrant cc)
  {
    return new Ic1ResponsePermissionGrant(
      toWireUUID(cc.requestId())
    );
  }

  private static ProtocolTicketsv1Type toWireResponseTicketCreate(
    final IcTResponseTicketCreate cc)
  {
    return new Ic1ResponseTicketCreate(
      toWireUUID(cc.requestId()),
      toWireTicketSummary(cc.ticket())
    );
  }

  private static ProtocolTicketsv1Type toWireResponseProjectCreate(
    final IcTResponseProjectCreate cc)
  {
    return new Ic1ResponseProjectCreate(
      toWireUUID(cc.requestId()),
      toWireProject(cc.project())
    );
  }

  private static Ic1Project toWireProject(
    final IcProject project)
  {
    return new Ic1Project(
      unsigned64(project.id().value()),
      string(project.shortName().value()),
      string(project.title().value())
    );
  }

  private static ProtocolTicketsv1Type toWireResponseError(
    final IcTResponseError cc)
  {
    return new Ic1ResponseError(
      toWireUUID(cc.requestId()),
      string(cc.errorCode()),
      string(cc.message())
    );
  }

  private static <A, B extends CBSerializableType> Ic1Page<B> toWirePage(
    final IcPage<A> page,
    final Function<A, B> f)
  {
    return new Ic1Page<>(
      new CBList<>(page.items().stream().map(f).toList()),
      new CBIntegerUnsigned32(Integer.toUnsignedLong(page.pageIndex())),
      new CBIntegerUnsigned32(Integer.toUnsignedLong(page.pageCount())),
      new CBIntegerUnsigned64(page.pageFirstOffset())
    );
  }

  private static ProtocolTicketsv1Type toWireResponseTicketSearchBegin(
    final IcTResponseTicketSearchBegin cc)
  {
    return new Ic1ResponseTicketSearchBegin(
      toWireUUID(cc.requestId()),
      toWirePage(cc.tickets(), IcT1Validation::toWireTicketSummary)
    );
  }

  private static ProtocolTicketsv1Type toWireResponseTicketSearchNext(
    final IcTResponseTicketSearchNext cc)
  {
    return new Ic1ResponseTicketSearchNext(
      toWireUUID(cc.requestId()),
      toWirePage(cc.tickets(), IcT1Validation::toWireTicketSummary)
    );
  }

  private static ProtocolTicketsv1Type toWireResponseTicketSearchPrevious(
    final IcTResponseTicketSearchPrevious cc)
  {
    return new Ic1ResponseTicketSearchPrevious(
      toWireUUID(cc.requestId()),
      toWirePage(cc.tickets(), IcT1Validation::toWireTicketSummary)
    );
  }

  private static Ic1TicketSummary toWireTicketSummary(
    final IcTicketSummary summary)
  {
    return new Ic1TicketSummary(
      string(summary.projectTitle().value()),
      string(summary.projectShortName().value()),
      toWireTicketId(summary.ticketId()),
      string(summary.ticketTitle().value()),
      toWireTimestamp(summary.timeCreated()),
      toWireTimestamp(summary.timeUpdated()),
      toWireUUID(summary.reporter()),
      string(summary.reporterName().value())
    );
  }

  private static ProtocolTicketsv1Type toWireResponseLogin(
    final IcTResponseLogin cc)
  {
    return new Ic1ResponseLogin(
      toWireUUID(cc.requestId()),
      toWireUser(cc.user())
    );
  }

  private static Ic1User toWireUser(
    final IcUser user)
  {
    return new Ic1User(
      toWireUUID(user.id()),
      string(user.name().value()),
      new CBList<>(
        user.emails()
          .stream()
          .map(IdEmail::value)
          .map(CBString::new)
          .toList()
      ),
      new CBList<>(
        user.permissions()
          .stream()
          .map(IcT1Validation::toWirePermissionScoped)
          .toList()
      )
    );
  }

  private static Ic1PermissionScoped toWirePermissionScoped(
    final IcPermissionScopedType scoped)
  {
    if (scoped instanceof IcPermissionGlobal global) {
      return new Ic1PermissionScoped.Global(
        toWirePermission(global.permission())
      );
    }

    if (scoped instanceof IcPermissionProjectwide projectwide) {
      return new Ic1PermissionScoped.Projectwide(
        unsigned64(projectwide.projectId().value()),
        toWirePermission(projectwide.permission())
      );
    }

    if (scoped instanceof IcPermissionTicketwide ticketwide) {
      return new Ic1PermissionScoped.Ticketwide(
        toWireTicketId(ticketwide.ticketId()),
        toWirePermission(ticketwide.permission())
      );
    }

    throw new IllegalStateException(
      "Unrecognized scoped permission: %s".formatted(scoped)
    );
  }

  private static Ic1TicketID toWireTicketId(
    final IcTicketID ticketId)
  {
    return new Ic1TicketID(
      unsigned64(ticketId.project().value()),
      unsigned64(ticketId.value())
    );
  }

  private static Ic1Permission toWirePermission(
    final IcPermission permission)
  {
    return switch (permission) {
      case TICKET_READ -> new Ic1Permission.TicketRead();
      case TICKET_WRITE -> new Ic1Permission.TicketWrite();
      case TICKET_CREATE -> new Ic1Permission.TicketCreate();
      case PROJECT_CREATE -> new Ic1Permission.ProjectCreate();
    };
  }

  private static Ic1UUID toWireUUID(
    final UUID id)
  {
    return new Ic1UUID(
      unsigned64(id.getMostSignificantBits()),
      unsigned64(id.getLeastSignificantBits())
    );
  }

  private static ProtocolTicketsv1Type toWireCommand(
    final IcTCommandType<?> c)
    throws IcProtocolException
  {
    if (c instanceof IcTCommandLogin cc) {
      return toWireCommandLogin(cc);
    }
    if (c instanceof IcTCommandTicketSearchBegin cc) {
      return toWireCommandTicketSearchBegin(cc);
    }
    if (c instanceof IcTCommandTicketSearchNext cc) {
      return toWireCommandTicketSearchNext(cc);
    }
    if (c instanceof IcTCommandTicketSearchPrevious cc) {
      return toWireCommandTicketSearchPrevious(cc);
    }
    if (c instanceof IcTCommandProjectCreate cc) {
      return toWireCommandProjectCreate(cc);
    }
    if (c instanceof IcTCommandTicketCreate cc) {
      return toWireCommandTicketCreate(cc);
    }
    if (c instanceof IcTCommandPermissionGrant cc) {
      return toWireCommandPermissionGrant(cc);
    }

    throw new IcProtocolException(
      PROTOCOL_ERROR,
      "Unrecognized message: %s".formatted(c)
    );
  }

  private static ProtocolTicketsv1Type toWireCommandPermissionGrant(
    final IcTCommandPermissionGrant cc)
  {
    return new Ic1CommandPermissionGrant(
      toWireUUID(cc.targetUser()),
      toWirePermissionScoped(cc.permission())
    );
  }

  private static ProtocolTicketsv1Type toWireCommandTicketCreate(
    final IcTCommandTicketCreate cc)
  {
    return new Ic1CommandTicketCreate(
      unsigned64(cc.creation().project().value()),
      string(cc.creation().title().value())
    );
  }

  private static ProtocolTicketsv1Type toWireCommandProjectCreate(
    final IcTCommandProjectCreate cc)
  {
    return new Ic1CommandProjectCreate(
      string(cc.shortName().value()),
      string(cc.title().value())
    );
  }

  private static ProtocolTicketsv1Type toWireCommandTicketSearchNext(
    final IcTCommandTicketSearchNext cc)
  {
    return new Ic1CommandTicketSearchNext();
  }

  private static ProtocolTicketsv1Type toWireCommandTicketSearchPrevious(
    final IcTCommandTicketSearchPrevious cc)
  {
    return new Ic1CommandTicketSearchPrevious();
  }

  private static ProtocolTicketsv1Type toWireCommandTicketSearchBegin(
    final IcTCommandTicketSearchBegin cc)
  {
    return new Ic1CommandTicketSearchBegin(
      toWireTicketSearchParameters(cc.parameters())
    );
  }

  private static Ic1TicketSearchParameters toWireTicketSearchParameters(
    final IcTicketListParameters p)
  {
    return new Ic1TicketSearchParameters(
      toWireTimeRange(p.timeCreatedRange()),
      toWireTimeRange(p.timeUpdatedRange()),
      toWireTicketColumnOrdering(p.ordering()),
      unsigned16(p.limit())
    );
  }

  private static CBList<Ic1TicketColumnOrdering> toWireTicketColumnOrderings(
    final List<IcTicketColumnOrdering> ordering)
  {
    return new CBList<>(
      ordering.stream()
        .map(IcT1Validation::toWireTicketColumnOrdering)
        .toList()
    );
  }

  private static Ic1TicketColumnOrdering toWireTicketColumnOrdering(
    final IcTicketColumnOrdering o)
  {
    return new Ic1TicketColumnOrdering(
      toWireTicketColumn(o.column()),
      fromBoolean(o.ascending())
    );
  }

  private static Ic1TicketColumn toWireTicketColumn(
    final IcTicketColumn column)
  {
    return switch (column) {
      case BY_ID -> new ByID();
      case BY_TITLE -> new ByTitle();
      case BY_TIME_CREATED -> new ByTimeCreated();
      case BY_TIME_UPDATED -> new ByTimeUpdated();
    };
  }

  private static Ic1TimeRange toWireTimeRange(
    final IcTimeRange timeRange)
  {
    return new Ic1TimeRange(
      toWireTimestamp(timeRange.timeLower()),
      toWireTimestamp(timeRange.timeUpper())
    );
  }

  private static ProtocolTicketsv1Type toWireCommandLogin(
    final IcTCommandLogin cc)
  {
    return new Ic1CommandLogin(
      string(cc.userName()),
      string(cc.password())
    );
  }

  @Override
  public IcTMessageType convertFromWire(
    final ProtocolTicketsv1Type message)
    throws IcProtocolException
  {
    try {
      if (message instanceof Ic1CommandLogin m) {
        return fromWireCommandLogin(m);
      }
      if (message instanceof Ic1CommandTicketSearchBegin m) {
        return fromWireCommandTicketSearchBegin(m);
      }
      if (message instanceof Ic1CommandTicketSearchNext m) {
        return fromWireCommandTicketSearchNext(m);
      }
      if (message instanceof Ic1CommandTicketSearchPrevious m) {
        return fromWireCommandTicketSearchPrevious(m);
      }
      if (message instanceof Ic1CommandProjectCreate m) {
        return fromWireCommandProjectCreate(m);
      }
      if (message instanceof Ic1CommandTicketCreate m) {
        return fromWireCommandTicketCreate(m);
      }
      if (message instanceof Ic1CommandPermissionGrant m) {
        return fromWireCommandPermissionGrant(m);
      }

      if (message instanceof Ic1ResponseLogin m) {
        return fromWireResponseLogin(m);
      }
      if (message instanceof Ic1ResponseTicketSearchBegin m) {
        return fromWireResponseTicketSearchBegin(m);
      }
      if (message instanceof Ic1ResponseTicketSearchNext m) {
        return fromWireResponseTicketSearchNext(m);
      }
      if (message instanceof Ic1ResponseTicketSearchPrevious m) {
        return fromWireResponseTicketSearchPrevious(m);
      }
      if (message instanceof Ic1ResponseProjectCreate m) {
        return fromWireResponseProjectCreate(m);
      }
      if (message instanceof Ic1ResponseError m) {
        return fromWireResponseError(m);
      }
      if (message instanceof Ic1ResponseTicketCreate m) {
        return fromWireResponseTicketCreate(m);
      }
      if (message instanceof Ic1ResponsePermissionGrant m) {
        return fromWireResponsePermissionGrant(m);
      }

    } catch (final Exception e) {
      throw new IcProtocolException(PROTOCOL_ERROR, e.getMessage());
    }

    throw new IcProtocolException(
      PROTOCOL_ERROR,
      "Unrecognized message: %s".formatted(message)
    );
  }

  private static IcTMessageType fromWireResponsePermissionGrant(
    final Ic1ResponsePermissionGrant m)
  {
    return new IcTResponsePermissionGrant(fromWireUUID(m.fieldRequestId()));
  }

  private static IcTMessageType fromWireCommandPermissionGrant(
    final Ic1CommandPermissionGrant m)
  {
    return new IcTCommandPermissionGrant(
      fromWireUUID(m.fieldTargetUser()),
      fromWirePermissionScoped(m.fieldPermission())
    );
  }

  private static IcTMessageType fromWireResponseTicketCreate(
    final Ic1ResponseTicketCreate m)
  {
    return new IcTResponseTicketCreate(
      fromWireUUID(m.fieldRequestId()),
      fromWireTicketSummary(m.fieldTicket())
    );
  }

  private static IcTMessageType fromWireCommandTicketCreate(
    final Ic1CommandTicketCreate m)
  {
    return new IcTCommandTicketCreate(
      new IcTicketCreation(
        new IcProjectID(m.fieldProject().value()),
        new IcTicketTitle(m.fieldTitle().value())
      )
    );
  }

  private static IcTMessageType fromWireResponseProjectCreate(
    final Ic1ResponseProjectCreate m)
  {
    return new IcTResponseProjectCreate(
      fromWireUUID(m.fieldRequestId()),
      fromWireProject(m.fieldProject())
    );
  }

  private static IcProject fromWireProject(
    final Ic1Project p)
  {
    return new IcProject(
      new IcProjectID(p.fieldId().value()),
      new IcProjectTitle(p.fieldTitle().value()),
      new IcProjectShortName(p.fieldShortName().value())
    );
  }

  private static IcTMessageType fromWireCommandProjectCreate(
    final Ic1CommandProjectCreate m)
  {
    return new IcTCommandProjectCreate(
      new IcProjectShortName(m.fieldShortName().value()),
      new IcProjectTitle(m.fieldTitle().value())
    );
  }

  private static UUID fromWireUUID(
    final Ic1UUID uuid)
  {
    return new UUID(
      uuid.fieldMsb().value(),
      uuid.fieldLsb().value()
    );
  }

  private static <A extends CBSerializableType, B> IcPage<B> fromWirePage(
    final Ic1Page<A> page,
    final Function<A, B> f)
  {
    return new IcPage<>(
      page.fieldItems().values().stream().map(f).toList(),
      (int) page.fieldPageIndex().value(),
      (int) page.fieldPageCount().value(),
      page.fieldPageFirstOffset().value()
    );
  }

  private static IcTicketSummary fromWireTicketSummary(
    final Ic1TicketSummary m)
  {
    return new IcTicketSummary(
      new IcProjectTitle(m.fieldProjectTitle().value()),
      new IcProjectShortName(m.fieldProjectShortName().value()),
      fromWireTicketId(m.fieldTicketId()),
      new IcTicketTitle(m.fieldTicketTitle().value()),
      fromWireTimestamp(m.fieldTimeCreated()),
      fromWireTimestamp(m.fieldTimeUpdated()),
      fromWireUUID(m.fieldReporter()),
      new IdName(m.fieldReporterName().value())
    );
  }

  private static IcTicketID fromWireTicketId(
    final Ic1TicketID ticketID)
  {
    return new IcTicketID(
      new IcProjectID(ticketID.fieldProject().value()),
      ticketID.fieldTicket().value()
    );
  }

  private static IcTMessageType fromWireResponseLogin(
    final Ic1ResponseLogin m)
    throws IcProtocolException
  {
    return new IcTResponseLogin(
      fromWireUUID(m.fieldRequestId()),
      fromWireUser(m.fieldUser())
    );
  }

  private static IcUser fromWireUser(
    final Ic1User user)
    throws IcProtocolException
  {
    return new IcUser(
      fromWireUUID(user.fieldId()),
      new IdName(user.fieldIdName().value()),
      fromWireEmails(user.fieldEmails()),
      fromWirePermissionSet(user.fieldPermissions())
    );
  }

  private static IcPermissionSet fromWirePermissionSet(
    final CBList<Ic1PermissionScoped> permissions)
  {
    final var builder = IcPermissionSet.builder();
    final var results =
      permissions.values()
        .stream()
        .map(IcT1Validation::fromWirePermissionScoped)
        .toList();

    for (final var result : results) {
      builder.add(result);
    }
    return builder.build();
  }

  private static IcPermissionScopedType fromWirePermissionScoped(
    final Ic1PermissionScoped p)
  {
    if (p instanceof Ic1PermissionScoped.Global global) {
      return new IcPermissionGlobal(
        fromWirePermission(global.fieldPermission())
      );
    }

    if (p instanceof Ic1PermissionScoped.Projectwide projectwide) {
      return new IcPermissionProjectwide(
        new IcProjectID(projectwide.fieldProject().value()),
        fromWirePermission(projectwide.fieldPermission())
      );
    }

    if (p instanceof Ic1PermissionScoped.Ticketwide ticketwide) {
      return new IcPermissionTicketwide(
        fromWireTicketId(ticketwide.fieldTicket()),
        fromWirePermission(ticketwide.fieldPermission())
      );
    }

    throw new IllegalStateException(
      "Unrecognized scoped permission: %s".formatted(p)
    );
  }

  private static IcPermission fromWirePermission(
    final Ic1Permission p)
  {
    if (p instanceof Ic1Permission.TicketRead) {
      return IcPermission.TICKET_READ;
    }
    if (p instanceof Ic1Permission.ProjectCreate) {
      return IcPermission.PROJECT_CREATE;
    }
    if (p instanceof Ic1Permission.TicketWrite) {
      return IcPermission.TICKET_WRITE;
    }
    if (p instanceof Ic1Permission.TicketCreate) {
      return IcPermission.TICKET_CREATE;
    }

    throw new IllegalStateException(
      "Unrecognized permission: %s".formatted(p)
    );
  }

  private static List<IdEmail> fromWireEmails(
    final CBList<CBString> fieldEmails)
    throws IcProtocolException
  {
    final var es = fieldEmails.values();
    if (es.isEmpty()) {
      throw new IcProtocolException(PROTOCOL_ERROR, "Emails list is empty!");
    }

    final var emails = new ArrayList<>(fieldEmails.values());
    return emails.stream().map(CBString::value).map(IdEmail::new).toList();
  }

  private static IcTMessageType fromWireResponseError(
    final Ic1ResponseError m)
  {
    return new IcTResponseError(
      fromWireUUID(m.fieldRequestId()),
      m.fieldErrorCode().value(),
      m.fieldMessage().value()
    );
  }

  private static IcTMessageType fromWireResponseTicketSearchNext(
    final Ic1ResponseTicketSearchNext m)
  {
    return new IcTResponseTicketSearchNext(
      fromWireUUID(m.fieldRequestId()),
      fromWirePage(m.fieldPage(), IcT1Validation::fromWireTicketSummary)
    );
  }

  private static IcTMessageType fromWireResponseTicketSearchPrevious(
    final Ic1ResponseTicketSearchPrevious m)
  {
    return new IcTResponseTicketSearchPrevious(
      fromWireUUID(m.fieldRequestId()),
      fromWirePage(m.fieldPage(), IcT1Validation::fromWireTicketSummary)
    );
  }

  private static IcTMessageType fromWireResponseTicketSearchBegin(
    final Ic1ResponseTicketSearchBegin m)
  {
    return new IcTResponseTicketSearchBegin(
      fromWireUUID(m.fieldRequestId()),
      fromWirePage(m.fieldPage(), IcT1Validation::fromWireTicketSummary)
    );
  }

  private static IcTMessageType fromWireCommandLogin(
    final Ic1CommandLogin login)
  {
    return new IcTCommandLogin(
      login.fieldUserName().value(),
      login.fieldPassword().value()
    );
  }

  private static IcTMessageType fromWireCommandTicketSearchNext(
    final Ic1CommandTicketSearchNext m)
  {
    return new IcTCommandTicketSearchNext();
  }

  private static IcTMessageType fromWireCommandTicketSearchPrevious(
    final Ic1CommandTicketSearchPrevious m)
  {
    return new IcTCommandTicketSearchPrevious();
  }

  private static IcTMessageType fromWireCommandTicketSearchBegin(
    final Ic1CommandTicketSearchBegin m)
  {
    return new IcTCommandTicketSearchBegin(
      fromWireTicketListParameters(m.fieldParameters())
    );
  }

  private static IcTicketListParameters fromWireTicketListParameters(
    final Ic1TicketSearchParameters p)
  {
    return new IcTicketListParameters(
      fromWireTimeRange(p.fieldTimeCreatedRange()),
      fromWireTimeRange(p.fieldTimeUpdatedRange()),
      fromWireColumnOrdering(p.fieldOrdering()),
      p.fieldLimit().value()
    );
  }

  private static IcTimeRange fromWireTimeRange(
    final Ic1TimeRange range)
  {
    return new IcTimeRange(
      fromWireTimestamp(range.fieldLower()),
      fromWireTimestamp(range.fieldUpper())
    );
  }

  private static Ic1TimestampUTC toWireTimestamp(
    final OffsetDateTime t)
  {
    return new Ic1TimestampUTC(
      new CBIntegerUnsigned32(Integer.toUnsignedLong(t.getYear())),
      new CBIntegerUnsigned8(t.getMonthValue()),
      new CBIntegerUnsigned8(t.getDayOfMonth()),
      new CBIntegerUnsigned8(t.getHour()),
      new CBIntegerUnsigned8(t.getMinute()),
      new CBIntegerUnsigned8(t.getSecond()),
      new CBIntegerUnsigned32(Integer.toUnsignedLong(t.getNano() / 1000))
    );
  }

  private static OffsetDateTime fromWireTimestamp(
    final Ic1TimestampUTC t)
  {
    return OffsetDateTime.of(
      (int) (t.fieldYear().value() & 0xffffffffL),
      t.fieldMonth().value(),
      t.fieldDay().value(),
      t.fieldHour().value(),
      t.fieldMinute().value(),
      t.fieldSecond().value(),
      (int) (t.fieldMillisecond().value() * 1000L),
      ZoneOffset.UTC
    );
  }

  private static List<IcTicketColumnOrdering> fromWireColumnOrderings(
    final CBList<Ic1TicketColumnOrdering> c)
  {
    return c.values()
      .stream()
      .map(IcT1Validation::fromWireColumnOrdering)
      .toList();
  }

  private static IcTicketColumnOrdering fromWireColumnOrdering(
    final Ic1TicketColumnOrdering o)
  {
    return new IcTicketColumnOrdering(
      fromWireTicketColumn(o.fieldColumn()),
      o.fieldAscending().asBoolean()
    );
  }

  private static IcTicketColumn fromWireTicketColumn(
    final Ic1TicketColumn c)
  {
    if (c instanceof ByID) {
      return IcTicketColumn.BY_ID;
    } else if (c instanceof ByTimeCreated) {
      return IcTicketColumn.BY_TIME_CREATED;
    } else if (c instanceof ByTimeUpdated) {
      return IcTicketColumn.BY_TIME_UPDATED;
    } else if (c instanceof ByTitle) {
      return IcTicketColumn.BY_TITLE;
    }

    throw new IllegalArgumentException(
      "Unrecognized user column: %s".formatted(c)
    );
  }
}
