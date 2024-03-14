/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handlebars;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.TemplateEngine;

public class HandlebarsTemplateEngine implements TemplateEngine {

  private final List<String> extensions;
  private final Handlebars handlebars;

  public HandlebarsTemplateEngine(Handlebars handlebars, List<String> extensions) {
    this.handlebars = handlebars;
    this.extensions = Collections.unmodifiableList(extensions);
  }

  @NonNull @Override
  public List<String> extensions() {
    return extensions;
  }

  @Override
  public String render(Context ctx, ModelAndView modelAndView) throws Exception {
    Template template = handlebars.compile(modelAndView.getView());
    Map<String, Object> model = new HashMap<>(ctx.getAttributes());
    model.putAll(modelAndView.getModel());
    return template.apply(model);
  }
}
