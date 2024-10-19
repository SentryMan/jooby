/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.ModelAndView;
import io.jooby.freemarker.FreemarkerModule;
import io.jooby.handlebars.HandlebarsModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.pebble.PebbleModule;
import io.jooby.thymeleaf.ThymeleafModule;

public class TemplateEngineTest {
  @ServerTest
  public void templateEngines(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(new ThymeleafModule());
              app.install(new HandlebarsModule());
              app.install(new FreemarkerModule());
              app.install(new PebbleModule());

              app.get("/1", ctx -> ModelAndView.map("index.hbs").put("name", "Handlebars"));
              app.get("/2", ctx -> ModelAndView.map("index.ftl").put("name", "Freemarker"));
              app.get("/3", ctx -> ModelAndView.map("index.html").put("name", "Thymeleaf"));
              app.get("/4", ctx -> ModelAndView.map("index.pebble").put("name", "Pebble"));
            })
        .ready(
            client -> {
              client.get(
                  "/1",
                  rsp -> {
                    assertEquals("Hello Handlebars!", rsp.body().string().trim());
                  });
              client.get(
                  "/2",
                  rsp -> {
                    assertEquals("Hello Freemarker!", rsp.body().string().trim());
                  });
              client.get(
                  "/3",
                  rsp -> {
                    assertEquals(
                        "<!DOCTYPE html>\n"
                            + "<html>\n"
                            + "<body>\n"
                            + "<p>\n"
                            + "  Hello <span>Thymeleaf</span>\n"
                            + "</p>\n"
                            + "</body>\n"
                            + "</html>",
                        rsp.body().string().replace("\r", "").trim());
                  });
              client.get(
                  "/4",
                  rsp -> {
                    assertEquals("Hello Pebble!", rsp.body().string().trim());
                  });
            });
  }

  @ServerTest
  public void thymeleafShouldReadFromFileSystem(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(
                  new ThymeleafModule(runner.resolvePath("src", "test", "resources", "views")));

              app.get("/", ctx -> ModelAndView.map("index.html").put("name", "Thymeleaf"));
            })
        .ready(
            client -> {
              client.get(
                  "/",
                  rsp -> {
                    assertEquals(
                        "<!DOCTYPE html>\n"
                            + "<html>\n"
                            + "<body>\n"
                            + "<p>\n"
                            + "  Hello <span>Thymeleaf</span>\n"
                            + "</p>\n"
                            + "</body>\n"
                            + "</html>",
                        rsp.body().string().replace("\r", "").trim());
                  });
            });
  }

  @ServerTest
  public void handlebarsShouldReadFromFileSystem(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(
                  new HandlebarsModule(runner.resolvePath("src", "test", "resources", "views")));

              app.get("/", ctx -> ModelAndView.map("index.hbs").put("name", "Handlebars"));
            })
        .ready(
            client -> {
              client.get(
                  "/",
                  rsp -> {
                    assertEquals("Hello Handlebars!", rsp.body().string().trim());
                  });
            });
  }

  @ServerTest
  public void freemarkerShouldReadFromFileSystem(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(
                  new FreemarkerModule(runner.resolvePath("src", "test", "resources", "views")));

              app.get("/", ctx -> ModelAndView.map("index.ftl").put("name", "Freemarker"));
            })
        .ready(
            client -> {
              client.get(
                  "/",
                  rsp -> {
                    assertEquals("Hello Freemarker!", rsp.body().string().trim());
                  });
            });
  }
}
