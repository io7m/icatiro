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

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * A comment on a ticket.
 *
 * @param ticket           The ticket ID
 * @param time             The time the comment was created
 * @param owner            The ticket owner
 * @param commentId        The comment ID
 * @param commentRepliedTo The comment being replied to
 * @param text             The text
 */

public record IcTicketComment(
  IcTicketID ticket,
  OffsetDateTime time,
  UUID owner,
  long commentId,
  OptionalLong commentRepliedTo,
  String text)
{
  /**
   * A comment on a ticket.
   *
   * @param ticket           The ticket ID
   * @param time             The time the comment was created
   * @param owner            The ticket owner
   * @param commentId        The comment ID
   * @param commentRepliedTo The comment being replied to
   * @param text             The text
   */

  public IcTicketComment
  {
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(text, "text");
    Objects.requireNonNull(ticket, "ticket");
    Objects.requireNonNull(time, "time");
    Objects.requireNonNull(commentRepliedTo, "commentRepliedTo");
  }
}
