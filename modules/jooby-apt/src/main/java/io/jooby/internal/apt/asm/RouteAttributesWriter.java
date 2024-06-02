/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt.asm;

import static java.util.Collections.singletonList;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.DCONST_0;
import static org.objectweb.asm.Opcodes.DCONST_1;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FCONST_0;
import static org.objectweb.asm.Opcodes.FCONST_1;
import static org.objectweb.asm.Opcodes.FCONST_2;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.ICONST_4;
import static org.objectweb.asm.Opcodes.ICONST_5;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.LCONST_0;
import static org.objectweb.asm.Opcodes.LCONST_1;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;
import javax.lang.model.util.Types;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.jooby.apt.JoobyProcessor;
import io.jooby.internal.apt.MethodDescriptor;
import io.jooby.internal.apt.Primitives;
import io.jooby.internal.apt.TypeDefinition;

public class RouteAttributesWriter {

  private static class EnumValue {
    private String type;

    private String value;

    public EnumValue(String type, String value) {
      this.type = type;
      this.value = value;
    }
  }

  private static final Predicate<String> HTTP_ANNOTATION =
      it ->
          (it.startsWith("io.jooby.annotation")
                  && !it.contains("io.jooby.annotation.Transactional"))
              || it.startsWith("jakarta.ws.rs")
              || it.startsWith("javax.ws.rs");

  private static final Predicate<String> NULL_ANNOTATION =
      it ->
          it.endsWith("NonNull")
              || it.endsWith("NotNull")
              || it.endsWith("Nonnull")
              || it.endsWith("Nullable");

  private static final Predicate<String> KOTLIN_ANNOTATION = it -> it.equals("kotlin.Metadata");

  private static final Predicate<String> ATTR_FILTER =
      HTTP_ANNOTATION.or(NULL_ANNOTATION).or(KOTLIN_ANNOTATION);

  private final Elements elements;

  private final Types types;

  private final String moduleInternalName;

  private final ClassWriter writer;

  private final MethodVisitor visitor;

  private final String[] userAttrFilter;

  public RouteAttributesWriter(
      Elements elements,
      Types types,
      ClassWriter writer,
      String moduleInternalName,
      MethodVisitor visitor,
      String[] userAttrFilter) {
    this.elements = elements;
    this.types = types;
    this.writer = writer;
    this.moduleInternalName = moduleInternalName;
    this.visitor = visitor;
    this.userAttrFilter = userAttrFilter;
  }

  public void process(ExecutableElement method, BiConsumer<String, Object[]> log) {
    Map<String, Object> attributes = annotationMap(method);
    for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
      String name = attribute.getKey();
      Object value = attribute.getValue();
      log.accept("  %s: %s", new Object[] {name, value});

      visitor.visitVarInsn(ALOAD, 2);
      visitor.visitLdcInsn(name);

      annotationValue(writer, visitor, value);

      visitor.visitMethodInsn(
          INVOKEVIRTUAL,
          MethodDescriptor.Route.attribute().getDeclaringType().getInternalName(),
          MethodDescriptor.Route.attribute().getName(),
          MethodDescriptor.Route.attribute().getDescriptor(),
          false);
      visitor.visitInsn(POP);
    }
  }

  private Map<String, Object> annotationMap(ExecutableElement method) {
    // class
    Map<String, Object> attributes =
        annotationMap(method.getEnclosingElement().getAnnotationMirrors(), null);
    // method
    attributes.putAll(annotationMap(method.getAnnotationMirrors(), null));
    return attributes;
  }

  private Map<String, Object> annotationMap(
      List<? extends AnnotationMirror> annotations, String root) {
    Map<String, Object> result = new HashMap<>();
    for (AnnotationMirror annotation : annotations) {
      Element elem = annotation.getAnnotationType().asElement();
      Retention retention = elem.getAnnotation(Retention.class);
      RetentionPolicy retentionPolicy =
          retention == null ? RetentionPolicy.CLASS : retention.value();
      String type = annotation.getAnnotationType().toString();
      if (
      // ignore annotations not available at runtime
      retentionPolicy != RetentionPolicy.RUNTIME
          // ignore core, jars annotations
          || ATTR_FILTER.test(type)
          // ignore user specified annotations
          || Arrays.stream(userAttrFilter).anyMatch(type::startsWith)) {

        continue;
      }
      String prefix =
          root == null
              ? annotation.getAnnotationType().asElement().getSimpleName().toString()
              : root;
      // Set all values and then override with present values (fix for JDK 11+)
      toMap(annotation.getElementValues(), prefix).forEach(result::put);
      toMap(elements.getElementValuesWithDefaults(annotation), prefix).forEach(result::putIfAbsent);
    }
    return result;
  }

  private Map<String, Object> toMap(
      Map<? extends ExecutableElement, ? extends AnnotationValue> values, String prefix) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> attribute :
        values.entrySet()) {
      Object value = annotationValue(attribute.getValue());
      if (value != null) {
        String method = attribute.getKey().getSimpleName().toString();
        String name = method.equals("value") ? prefix : prefix + "." + method;
        // Found value is override on JDK 11 with default annotation value, we trust that spe
        result.putIfAbsent(name, value);
      }
    }
    return result;
  }

  private Object annotationValue(AnnotationValue annotationValue) {
    try {
      return annotationValue.accept(
          new SimpleAnnotationValueVisitor8<Object, Void>() {
            @Override
            protected Object defaultAction(Object value, Void unused) {
              return value;
            }

            @Override
            public Object visitAnnotation(AnnotationMirror mirror, Void unused) {
              Map<String, Object> annotation = annotationMap(singletonList(mirror), null);
              return annotation.isEmpty() ? null : annotation;
            }

            @Override
            public Object visitEnumConstant(VariableElement enumeration, Void unused) {
              TypeMirror typeMirror = enumeration.asType();
              Element element = types.asElement(typeMirror);
              Name binaryName = elements.getBinaryName((TypeElement) element);
              return new EnumValue(binaryName.toString(), enumeration.toString());
            }

            @Override
            public Object visitArray(List<? extends AnnotationValue> values, Void unused) {
              if (values.size() > 0) {
                List<Object> result = new ArrayList<>();
                for (AnnotationValue it : values) {
                  result.add(annotationValue(it));
                }
                return result;
              }
              return null;
            }
          },
          null);
    } catch (UnsupportedOperationException x) {
      // See https://github.com/jooby-project/jooby/issues/2417
      return null;
    }
  }

  private void annotationValue(ClassWriter writer, MethodVisitor visitor, Object value) {
    try {
      if (value instanceof Map) {
        String newMap = annotationMapValue(writer, (Map) value);
        visitor.visitMethodInsn(
            INVOKESTATIC, moduleInternalName, newMap, "()Ljava/util/Map;", false);
      } else if (value instanceof List) {
        List values = (List) value;
        String componentType =
            values.get(0) instanceof EnumValue
                ? ((EnumValue) values.get(0)).type
                : values.get(0).getClass().getName();
        if (values.size() > 0) {
          ArrayWriter.write(
              visitor, componentType, values, v -> annotationValue(writer, visitor, v));
          Method asList = Arrays.class.getDeclaredMethod("asList", Object[].class);
          visitor.visitMethodInsn(
              INVOKESTATIC,
              Type.getInternalName(asList.getDeclaringClass()),
              asList.getName(),
              Type.getMethodDescriptor(asList),
              false);
        } else {
          Method emptyList = Collections.class.getDeclaredMethod("emptyList");
          visitor.visitMethodInsn(
              INVOKESTATIC,
              Type.getInternalName(emptyList.getDeclaringClass()),
              emptyList.getName(),
              Type.getMethodDescriptor(emptyList),
              false);
        }
      } else {
        annotationSingleValue(visitor, value);
      }
    } catch (NoSuchMethodException cause) {
      throw JoobyProcessor.propagate(cause);
    }
  }

  private String annotationMapValue(ClassWriter writer, Map<String, Object> map) {
    String methodName = "newMap" + Long.toHexString(UUID.randomUUID().getMostSignificantBits());
    MethodVisitor methodVisitor =
        writer.visitMethod(
            ACC_PRIVATE | ACC_STATIC,
            methodName,
            "()Ljava/util/Map;",
            "()Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;",
            null);
    methodVisitor.visitCode();
    methodVisitor.visitTypeInsn(NEW, "java/util/HashMap");
    methodVisitor.visitInsn(DUP);
    methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
    methodVisitor.visitVarInsn(ASTORE, 0);
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      methodVisitor.visitVarInsn(ALOAD, 0);
      methodVisitor.visitLdcInsn(entry.getKey());
      annotationValue(writer, methodVisitor, entry.getValue());
      methodVisitor.visitMethodInsn(
          Opcodes.INVOKEINTERFACE,
          "java/util/Map",
          "put",
          "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
          true);
      methodVisitor.visitInsn(POP);
    }
    methodVisitor.visitVarInsn(ALOAD, 0);
    methodVisitor.visitInsn(Opcodes.ARETURN);
    methodVisitor.visitMaxs(0, 0);
    methodVisitor.visitEnd();
    return methodName;
  }

  private void annotationSingleValue(MethodVisitor visitor, Object value) {
    try {
      if (value instanceof String) {
        visitor.visitLdcInsn(value);
      } else if (value instanceof Boolean) {
        annotationBoolean(visitor, (Boolean) value, false, ICONST_0, ICONST_1);
      } else if (value instanceof Character) {
        annotationCharacter(
            visitor,
            (Character) value,
            false,
            ICONST_0,
            ICONST_1,
            ICONST_2,
            ICONST_3,
            ICONST_4,
            ICONST_5);
      } else if (value instanceof Short) {
        annotationNumber(
            visitor,
            (Number) value,
            true,
            ICONST_0,
            ICONST_1,
            ICONST_2,
            ICONST_3,
            ICONST_4,
            ICONST_5);
      } else if (value instanceof Integer) {
        annotationNumber(
            visitor,
            (Number) value,
            true,
            ICONST_0,
            ICONST_1,
            ICONST_2,
            ICONST_3,
            ICONST_4,
            ICONST_5);
      } else if (value instanceof Long) {
        annotationNumber(visitor, (Number) value, false, LCONST_0, LCONST_1);
      } else if (value instanceof Float) {
        annotationNumber(visitor, (Number) value, false, FCONST_0, FCONST_1, FCONST_2);
      } else if (value instanceof Double) {
        annotationNumber(visitor, (Number) value, false, DCONST_0, DCONST_1);
      } else if (value instanceof TypeMirror) {
        TypeDefinition typeDef = new TypeDefinition(types, (TypeMirror) value);
        if (typeDef.isPrimitive()) {
          Method wrapper = Primitives.wrapper(typeDef);
          visitor.visitFieldInsn(
              GETSTATIC,
              Type.getInternalName(wrapper.getDeclaringClass()),
              "TYPE",
              "Ljava/lang/Class;");
        } else {
          visitor.visitLdcInsn(typeDef.toJvmType());
        }
      } else if (value instanceof EnumValue) {
        EnumValue enumValue = (EnumValue) value;
        Type type = Type.getObjectType(enumValue.type.replace(".", "/"));
        visitor.visitFieldInsn(
            GETSTATIC, type.getInternalName(), enumValue.value, type.getDescriptor());
      }

      Method wrapper = Primitives.wrapper(value.getClass());
      if (wrapper != null) {
        visitor.visitMethodInsn(
            INVOKESTATIC,
            Type.getInternalName(wrapper.getDeclaringClass()),
            wrapper.getName(),
            Type.getMethodDescriptor(wrapper),
            false);
      }
    } catch (NoSuchMethodException cause) {
      throw JoobyProcessor.propagate(cause);
    }
  }

  private void annotationBoolean(
      MethodVisitor visitor, Boolean value, boolean checkRange, Integer... constants) {
    annotationPrimitive(visitor, value, checkRange, b -> b.booleanValue() ? 1 : 0, constants);
  }

  private void annotationCharacter(
      MethodVisitor visitor, Character value, boolean checkRange, Integer... constants) {
    annotationPrimitive(visitor, value, checkRange, c -> (int) c.charValue(), constants);
  }

  private void annotationNumber(
      MethodVisitor visitor, Number value, boolean checkRange, Integer... constants) {
    annotationPrimitive(visitor, value, checkRange, Number::intValue, constants);
  }

  private <T> void annotationPrimitive(
      MethodVisitor visitor,
      T value,
      boolean checkRange,
      Function<T, Integer> intMapper,
      Integer... constants) {
    int v = intMapper.apply(value).intValue();
    if (v >= 0 && v <= constants.length) {
      visitor.visitInsn(constants[v].intValue());
    } else {
      if (checkRange) {
        if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
          visitor.visitIntInsn(Opcodes.BIPUSH, v);
        } else if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
          visitor.visitIntInsn(Opcodes.SIPUSH, v);
        } else {
          visitor.visitLdcInsn(value);
        }
      } else {
        visitor.visitLdcInsn(value);
      }
    }
  }
}
