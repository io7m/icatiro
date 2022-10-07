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

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The set of permissions held by a user.
 */

public final class IcPermissionSet
{
  private static final IcPermissionSet EMPTY =
    new IcPermissionSet(
      Set.of(),
      Set.of(),
      Set.of()
    );

  private final Set<IcPermissionGlobal> globals;
  private final Set<IcPermissionProjectwide> projectwides;
  private final Set<IcPermissionTicketwide> ticketwides;

  private IcPermissionSet(
    final Set<IcPermissionGlobal> inGlobals,
    final Set<IcPermissionProjectwide> inProjectWides,
    final Set<IcPermissionTicketwide> inTicketWides)
  {
    this.globals =
      Objects.requireNonNull(inGlobals, "globals");
    this.projectwides =
      Objects.requireNonNull(inProjectWides, "projectwides");
    this.ticketwides =
      Objects.requireNonNull(inTicketWides, "ticketwides");
  }

  /**
   * @return A new mutable permission set builder
   */

  public static Builder builder()
  {
    return new Builder();
  }

  /**
   * @return The empty permission set
   */

  public static IcPermissionSet empty()
  {
    return EMPTY;
  }

  /**
   * @param permissions The permissions
   *
   * @return A permission set based on the given collection
   */

  public static IcPermissionSet of(
    final Collection<IcPermissionScopedType> permissions)
  {
    Objects.requireNonNull(permissions, "permissions");

    final var builder = builder();
    for (final var p : permissions) {
      builder.add(p);
    }
    return builder.build();
  }

  /**
   * @return The permission set as a stream
   */

  public Stream<IcPermissionScopedType> stream()
  {
    return Stream.concat(
      Stream.concat(
        this.globals.stream().sorted(),
        this.projectwides.stream().sorted()
      ),
      this.ticketwides.stream().sorted()
    );
  }

  /**
   * @param object     An object that is access controlled
   * @param permission A permission
   *
   * @return {@code true} if the set implies the given permission for the given
   * object
   */

  public boolean implies(
    final IcAccessControlledType object,
    final IcPermission permission)
  {
    Objects.requireNonNull(object, "object");
    Objects.requireNonNull(permission, "permission");

    if (this.globals.contains(new IcPermissionGlobal(permission))) {
      return true;
    }

    if (object instanceof IcProjectID project) {
      return this.projectwides.contains(
        new IcPermissionProjectwide(project, permission)
      );
    }

    if (object instanceof IcTicketID ticket) {
      if (this.projectwides.contains(
        new IcPermissionProjectwide(ticket.project(), permission))) {
        return true;
      }
      return this.ticketwides.contains(
        new IcPermissionTicketwide(ticket, permission));
    }

    throw new IllegalStateException(
      "Unrecognized access controlled object: %s".formatted(object)
    );
  }

  /**
   * @param permission A permission
   *
   * @return {@code true} if the set implies the given permission
   */

  public boolean impliesScoped(
    final IcPermissionScopedType permission)
  {
    Objects.requireNonNull(permission, "permission");

    if (this.globals.contains(new IcPermissionGlobal(permission.permission()))) {
      return true;
    }

    if (permission instanceof IcPermissionProjectwide projectwide) {
      return this.projectwides.contains(projectwide);
    }

    if (permission instanceof IcPermissionTicketwide ticketwide) {
      if (this.projectwides.contains(
        new IcPermissionProjectwide(
          ticketwide.ticketId().project(),
          ticketwide.permission()))) {
        return true;
      }
      return this.ticketwides.contains(ticketwide);
    }

    return false;
  }

  @Override
  public boolean equals(
    final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || !this.getClass().equals(o.getClass())) {
      return false;
    }
    final IcPermissionSet that = (IcPermissionSet) o;
    return this.globals.equals(that.globals)
           && this.projectwides.equals(that.projectwides)
           && this.ticketwides.equals(that.ticketwides);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(this.globals, this.projectwides, this.ticketwides);
  }

  @Override
  public String toString()
  {
    return this.stream()
      .toList()
      .toString();
  }

  /**
   * @return This permission set as a builder
   */

  public Builder toBuilder()
  {
    final var builder = new Builder();
    this.stream().forEach(builder::add);
    return builder;
  }

  /**
   * A mutable permission set builder.
   */

  public static final class Builder
  {
    private final HashSet<IcPermissionGlobal> globals;
    private final HashSet<IcPermissionProjectwide> projectwides;
    private final HashSet<IcPermissionTicketwide> ticketwides;

    private Builder()
    {
      this.globals =
        new HashSet<>();
      this.projectwides =
        new HashSet<>();
      this.ticketwides =
        new HashSet<>();
    }

    /**
     * Add a new global permission.
     *
     * @param global The permission
     *
     * @return this
     */

    public Builder addGlobal(
      final IcPermissionGlobal global)
    {
      this.globals.add(
        Objects.requireNonNull(global, "global"));
      return this;
    }

    /**
     * Add a new projectwide permission.
     *
     * @param projectwide The permission
     *
     * @return this
     */

    public Builder addProjectwide(
      final IcPermissionProjectwide projectwide)
    {
      this.projectwides.add(
        Objects.requireNonNull(projectwide, "projectwide"));
      return this;
    }

    /**
     * Add a new ticketwide permission.
     *
     * @param ticketwide The permission
     *
     * @return this
     */

    public Builder addTicketwide(
      final IcPermissionTicketwide ticketwide)
    {
      this.ticketwides.add(
        Objects.requireNonNull(ticketwide, "ticketwide"));
      return this;
    }

    /**
     * Add a new permission.
     *
     * @param permission The permission
     *
     * @return this
     */

    public Builder add(
      final IcPermissionScopedType permission)
    {
      Objects.requireNonNull(permission, "permission");

      if (permission instanceof IcPermissionGlobal global) {
        return this.addGlobal(global);
      }
      if (permission instanceof IcPermissionProjectwide projectwide) {
        return this.addProjectwide(projectwide);
      }
      if (permission instanceof IcPermissionTicketwide ticketwide) {
        return this.addTicketwide(ticketwide);
      }
      throw new IllegalStateException(
        "Unrecognized permission type: %s".formatted(permission)
      );
    }

    /**
     * @return A permission set
     */

    public IcPermissionSet build()
    {
      return new IcPermissionSet(
        Set.copyOf(this.globals),
        Set.copyOf(this.projectwides),
        Set.copyOf(this.ticketwides)
      );
    }
  }
}
