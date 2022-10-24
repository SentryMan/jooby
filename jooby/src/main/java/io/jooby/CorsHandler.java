/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handle preflight and simple CORS requests. CORS options are set via: {@link Cors}.
 *
 * @author edgar
 * @since 2.0.4
 * @see Cors
 */
public class CorsHandler implements Route.Decorator {

  private static final String ORIGIN = "Origin";

  private static final String ANY_ORIGIN = "*";

  private static final String AC_REQUEST_METHOD = "Access-Control-Request-Method";

  private static final String AC_REQUEST_HEADERS = "Access-Control-Request-Headers";

  private static final String AC_MAX_AGE = "Access-Control-Max-Age";

  private static final String AC_EXPOSE_HEADERS = "Access-Control-Expose-Headers";

  private static final String AC_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

  private static final String AC_ALLOW_HEADERS = "Access-Control-Allow-Headers";

  private static final String AC_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

  private static final String AC_ALLOW_METHODS = "Access-Control-Allow-Methods";

  private final Cors options;

  private static final Logger log = LoggerFactory.getLogger(CorsHandler.class);

  /**
   * Creates a new {@link CorsHandler}.
   *
   * @param options Cors options, or empty for using default options.
   */
  public CorsHandler(@Nonnull final Cors options) {
    this.options = options;
  }

  /**
   * Creates a new {@link CorsHandler} with default options.
   */
  public CorsHandler() {
    this(new Cors());
  }

  @Nonnull @Override public Route.Handler apply(@Nonnull Route.Handler next) {
    return ctx -> {
      String origin = ctx.header("Origin").valueOrNull();
      if (origin != null) {
        if (!options.allowOrigin(origin)) {
          log.debug("denied origin: {}", origin);
          return ctx.send(StatusCode.FORBIDDEN);
        }
        log.debug("allowed origin: {}", origin);
        if (ctx.isPreflight()) {
          log.debug("handling preflight for: {}", origin);
          if (preflight(ctx, options, origin)) {
            return ctx;
          } else {
            log.debug("preflight for {} {} with origin: {} failed", ctx.header(AC_REQUEST_METHOD),
                ctx.getRequestURL(), origin);
            return ctx.send(StatusCode.FORBIDDEN);
          }
        } else {
          // OPTIONS?
          if (ctx.getMethod().equalsIgnoreCase(Router.OPTIONS)) {
            // handle normal OPTIONS
            String allow = Router.METHODS.stream()
                .flatMap(method -> allowMethod(ctx, method))
                .collect(Collectors.joining(","));
            ctx.setResponseHeader("Allow", allow);
            return ctx.send(StatusCode.OK);
          } else {
            log.debug("handling simple cors for: {}", origin);
            ctx.setResetHeadersOnError(false);
            simple(ctx, options, origin);
          }
        }
      }
      return next.apply(ctx);
    };
  }

  private Stream<String> allowMethod(Context ctx, String method) {
    String existingMethod = ctx.getMethod();
    try {
      ctx.setMethod(method);
      Router.Match match = ctx.getRouter().match(ctx);
      return match.matches() ? Stream.of(method) : Stream.empty();
    } finally {
      ctx.setMethod(existingMethod);
    }
  }

  private static void simple(final Context ctx, final Cors options, final String origin) {
    if ("null".equals(origin)) {
      ctx.setResponseHeader(AC_ALLOW_ORIGIN, ANY_ORIGIN);
    } else {
      ctx.setResponseHeader(AC_ALLOW_ORIGIN, origin);
      if (!options.anyHeader()) {
        ctx.setResponseHeader("Vary", ORIGIN);
      }
      if (options.getUseCredentials()) {
        ctx.setResponseHeader(AC_ALLOW_CREDENTIALS, true);
      }
      if (!options.getExposedHeaders().isEmpty()) {
        ctx.setResponseHeader(AC_EXPOSE_HEADERS,
            options.getExposedHeaders().stream().collect(Collectors.joining()));
      }
    }
  }

  @Nonnull @Override public void setRoute(@Nonnull Route route) {
    route.setHttpOptions(true);
  }

  private boolean preflight(final Context ctx, final Cors options, final String origin) {
    /*
      Allowed method
     */
    boolean allowMethod = ctx.header(AC_REQUEST_METHOD).toOptional()
        .map(options::allowMethod)
        .orElse(false);
    if (!allowMethod) {
      return false;
    }

    /*
      Allowed headers
     */
    List<String> headers = ctx.header(AC_REQUEST_HEADERS).toOptional().map(header ->
        Arrays.asList(header.split("\\s*,\\s*"))
    ).orElse(Collections.emptyList());
    if (!options.allowHeaders(headers)) {
      return false;
    }

    /*
      Allowed methods
     */
    ctx.setResponseHeader(AC_ALLOW_METHODS,
        options.getMethods().stream().collect(Collectors.joining(",")));

    List<String> allowedHeaders = options.anyHeader() ? headers : options.getHeaders();
    ctx.setResponseHeader(AC_ALLOW_HEADERS,
        allowedHeaders.stream().collect(Collectors.joining(",")));

    /*
      Allow credentials
     */
    if (options.getUseCredentials()) {
      ctx.setResponseHeader(AC_ALLOW_CREDENTIALS, true);
    }

    long maxAge = options.getMaxAge().getSeconds();
    if (maxAge > 0) {
      ctx.setResponseHeader(AC_MAX_AGE, maxAge);
    }

    ctx.setResponseHeader(AC_ALLOW_ORIGIN, origin);

    if (!options.anyOrigin()) {
      ctx.setResponseHeader("Vary", ORIGIN);
    }

    ctx.send(StatusCode.OK);
    return true;
  }
}
