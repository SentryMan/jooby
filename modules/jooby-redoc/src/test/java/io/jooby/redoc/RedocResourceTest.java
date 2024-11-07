/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.redoc;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

public class RedocResourceTest {
  @Test
  public void shouldCheckIndexPage() throws IOException {
    String index = asset("index.html");
    assertTrue(index.contains("${redocPath}"), index);
    assertTrue(index.contains("${openAPIPath}"), index);
  }

  @Test
  public void shouldCheckBundle() throws IOException {
    String index = asset("redoc.standalone.js");
    assertNotNull(index);
  }

  private String asset(String resource) throws IOException {
    return IOUtils.toString(
        requireNonNull(getClass().getResource("/redoc/" + resource)), StandardCharsets.UTF_8);
  }
}
