/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hibernate;

import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Allow to customize a Session before opening it.
 *
 * @author edgar
 * @since 2.5.1
 */
public interface StatelessSessionProvider {
  /**
   * Creates a new session.
   *
   * @param builder Session builder.
   * @return A new session.
   */
  @NonNull StatelessSession newSession(@NonNull StatelessSessionBuilder builder);
}
