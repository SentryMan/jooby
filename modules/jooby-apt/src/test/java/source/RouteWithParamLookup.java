/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package source;

import static io.jooby.ParamSource.COOKIE;
import static io.jooby.ParamSource.FLASH;
import static io.jooby.ParamSource.HEADER;
import static io.jooby.ParamSource.PATH;
import static io.jooby.ParamSource.QUERY;
import static io.jooby.ParamSource.SESSION;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.jooby.Context;
import io.jooby.annotation.GET;
import io.jooby.annotation.Param;

public class RouteWithParamLookup {

  @GET("/lookup/no-sources")
  public String lookupNoSources(Context ctx, @Param({}) int myParam) {
    return String.valueOf(myParam);
  }

  @GET("/lookup/source-num-1")
  public String lookupSourceNum1(@Nullable @Param({PATH}) String myParam) {
    return String.valueOf(myParam);
  }

  @GET("/lookup/source-num-2")
  public String lookupSourceNum2(@Nullable @Param({PATH, HEADER}) String myParam) {
    return String.valueOf(myParam);
  }

  @GET("/lookup/source-num-3")
  public String lookupSourceNum3(@Nullable @Param({PATH, HEADER, COOKIE}) String myParam) {
    return String.valueOf(myParam);
  }

  @GET("/lookup/source-num-4")
  public String lookupSourceNum4(@Nullable @Param({PATH, HEADER, COOKIE, FLASH}) String myParam) {
    return String.valueOf(myParam);
  }

  @GET("/lookup/source-num-5")
  public String lookupSourceNum5(
      @Nullable @Param({PATH, HEADER, COOKIE, FLASH, SESSION}) String myParam) {
    return String.valueOf(myParam);
  }

  @GET("/lookup/source-num-6")
  public String lookupSourceNum6(
      @Nullable @Param({PATH, HEADER, COOKIE, FLASH, SESSION, QUERY}) String myParam) {
    return String.valueOf(myParam);
  }

  @GET("/lookup/source-num-6plus")
  public String lookupSourceNum6plus(
      @Nullable @Param({PATH, HEADER, COOKIE, FLASH, SESSION, QUERY}) String myParam) {
    return String.valueOf(myParam);
  }

  @GET("/lookup/query-path/{myParam}")
  public String lookupQueryPath(@Param({QUERY, PATH}) int myParam) {
    return String.valueOf(myParam);
  }

  @GET("/lookup/path-query/{myParam}")
  public String lookupPathQuery(@Param({PATH, QUERY}) int myParam) {
    return String.valueOf(myParam);
  }

  @GET("/lookup/missing")
  public String lookupMissing(@Param({PATH, QUERY}) String myParam) {
    return String.valueOf(myParam);
  }
}
