/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;

import io.jooby.run.JoobyRunOptions;

public class RunMojoTest {

  @Test
  public void ensureConfigurationOptions() {
    Stream.of(JoobyRunOptions.class.getDeclaredFields())
        .filter(
            field ->
                !field.getName().equals("projectName")
                    && !field.getName().equals("basedir")
                    && !Modifier.isStatic(field.getModifiers()))
        .forEach(
            field -> {
              try {
                Field target = FieldUtils.getField(RunMojo.class, field.getName(), true);
                assertEquals(field.getGenericType(), target.getGenericType(), field.toString());
              } catch (Exception x) {
                throw new IllegalStateException(x);
              }
            });
  }
}
