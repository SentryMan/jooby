/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i2968;

import io.jooby.annotation.GET;
import io.jooby.annotation.QueryParam;

public class C2968 {
  @GET("/hello")
  public String hello(@QueryParam String name) {
    return name;
  }
}
