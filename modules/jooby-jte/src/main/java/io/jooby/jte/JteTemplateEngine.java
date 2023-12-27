/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jte;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.jooby.Context;
import io.jooby.ModelAndView;

class JteTemplateEngine implements io.jooby.TemplateEngine {
  private final TemplateEngine jte;
  private final List<String> extensions;

  public JteTemplateEngine(TemplateEngine jte) {
    this.jte = jte;
    this.extensions = List.of(".jte", ".kte");
  }

  @NonNull @Override
  public List<String> extensions() {
    return extensions;
  }

  @Override
  public String render(Context ctx, ModelAndView modelAndView) {
    var output = new StringOutput();
    var attributes = ctx.getAttributes();
    Map<String, Object> model;
    if (attributes.isEmpty()) {
      model = modelAndView.getModel();
    } else {
      model = new HashMap<>();
      model.putAll(attributes);
      model.putAll(modelAndView.getModel());
    }
    jte.render(modelAndView.getView(), model, output);
    return output.toString();
  }
}
