/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Predicate;

import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.i1292.Controller1292_;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue1292 {

  public static class SubApp extends Jooby {
    {
      get("/", ctx -> "subapp");
    }
  }

  @ServerTest
  public void shouldSwitchBaseOnDomain(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.domain(
                  "foo.devd.io",
                  () -> {
                    app.get("/", ctx -> "foo");
                  });

              app.domain(
                  "bar.devd.io",
                  () -> {
                    app.get("/", ctx -> "bar");
                  });

              app.domain(
                  "mvc.devd.io",
                  () -> {
                    app.mvc(new Controller1292_());
                  });

              app.domain("subapp.devd.io", new SubApp());

              app.get("/", ctx -> "app");
            })
        .ready(
            client -> {
              client.header("Host", "foo.devd.io");
              client.get(
                  "/",
                  rsp -> {
                    assertEquals("foo", rsp.body().string());
                  });

              client.header("Host", "bar.devd.io");
              client.get(
                  "/",
                  rsp -> {
                    assertEquals("bar", rsp.body().string());
                  });

              client.header("Host", "mvc.devd.io");
              client.get(
                  "/",
                  rsp -> {
                    assertEquals("mvc", rsp.body().string());
                  });

              client.header("Host", "subapp.devd.io");
              client.get(
                  "/",
                  rsp -> {
                    assertEquals("subapp", rsp.body().string());
                  });

              client.get(
                  "/",
                  rsp -> {
                    assertEquals("app", rsp.body().string());
                  });
            });
  }

  private Predicate<Context> domainIs(String host) {
    return ctx -> ctx.getHost().equals(host);
  }
}
