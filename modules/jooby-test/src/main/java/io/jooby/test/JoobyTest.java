/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.jooby.ExecutionMode;
import io.jooby.Jooby;

/**
 * Startup Jooby applications using JUnit extension mechanism.
 *
 * <p>When this annotation is set at class level, a single jooby application is started:
 *
 * <pre>{@code
 * &#64;JoobyTest(MyApp.class)
 * public class MyTest {
 *
 *   &#64;Test
 *   public void test() {
 *     Use your favorite HTTP client and call Jooby.
 *   }
 * }
 * }</pre>
 *
 * When this annotation is set at method level, a jooby application is started per test/method:
 *
 * <pre>{@code
 * public class MyTest {
 *
 *   &#64;JoobyTest(MyApp.class)
 *   public void test() {
 *     Use your favorite HTTP client and call Jooby.
 *   }
 *
 *   &#64;JoobyTest(MyApp.class)
 *   public void anotherTest() {
 *     Use your favorite HTTP client and call Jooby.
 *   }
 * }
 * }</pre>
 *
 * Server path and port can be injected as instance fields or method parameters:
 *
 * <pre>{@code
 * &#64;JoobyTest(MyApp.class)
 * public MyTest {
 *
 *   public String serverPath;
 *
 *   public void test() {
 *     Use your favorite HTTP client and call Jooby.
 *   }
 * }
 * }</pre>
 *
 * <pre>{@code
 * public MyTest {
 *
 *   &#64;JoobyTest(MyApp.class)
 *   public void test(String serverPath) {
 *     Use your favorite HTTP client and call Jooby.
 *   }
 * }
 * }</pre>
 *
 * @author edgar
 * @since 2.0.0
 */
@Target({TYPE, METHOD})
@Retention(RUNTIME)
@Test
@ExtendWith(JoobyExtension.class)
public @interface JoobyTest {
  /**
   * Application class. Required.
   *
   * @return Application class.
   */
  Class<? extends Jooby> value();

  /**
   * Factory class (owner of {@link #factoryMethod()}. Use for create an application
   * programmatically.
   *
   * @return Factory class or fallback for current test class.
   */
  Class<?> factoryClass() default Object.class;

  /**
   * Name of factory method to create an application programmatically.
   *
   * @return Name of factory method to create an application programmatically.
   */
  String factoryMethod() default "";

  /**
   * Application environment, default is <code>test</code>.
   *
   * @return Application environment, default is <code>test</code>.
   */
  String environment() default "test";

  /**
   * Server port. The default port is <code>8911</code>. Use <code>0</code> for random port.
   *
   * @return Server port. The default port is <code>8911</code>.
   */
  int port() default -1;

  /**
   * Application execution mode. Default is {@link ExecutionMode#DEFAULT}.
   *
   * @return Application execution mode. Default is {@link ExecutionMode#DEFAULT}.
   */
  ExecutionMode executionMode() default ExecutionMode.DEFAULT;
}
