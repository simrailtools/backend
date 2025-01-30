/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024-2025 Pasqual Koschmieder and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package tools.simrail.backend.external.feign;

import feign.Param;
import feign.QueryMapEncoder;
import feign.codec.EncodeException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CustomFieldQueryMapEncoder implements QueryMapEncoder {

  private final Map<Class<?>, List<BiConsumer<Object, Map<String, Object>>>> classToResolverCache =
    new ConcurrentHashMap<>(16, 0.9f, 1);

  private static @Nullable Object resolveFieldValue(@NotNull Field field, @NotNull Object instance) {
    try {
      return field.get(instance);
    } catch (IllegalAccessException exception) {
      throw new EncodeException("Unable to resolve value of field to encode into query map", exception);
    }
  }

  private static @Nullable Param.Expander constructExpanderInstance(@NotNull Class<? extends Param.Expander> type) {
    try {
      if (type != Param.ToStringExpander.class) {
        // specific expander given, construct the expander
        return type.getConstructor().newInstance();
      } else {
        // no specific expander given, do not expand anything
        return null;
      }
    } catch (ReflectiveOperationException exception) {
      throw new EncodeException("Unable to construct instance of custom expander " + type, exception);
    }
  }

  @Override
  public @NotNull Map<String, Object> encode(@NotNull Object object) {
    // get the resolvers for the class and call each of them (if there are any to call)
    var resolvers = this.classToResolverCache.computeIfAbsent(object.getClass(), this::parseResolvers);
    if (!resolvers.isEmpty()) {
      Map<String, Object> result = new HashMap<>();
      for (var resolver : resolvers) {
        resolver.accept(object, result);
      }

      return result;
    } else {
      return Map.of();
    }
  }

  private @NotNull List<BiConsumer<Object, Map<String, Object>>> parseResolvers(@NotNull Class<?> type) {
    List<BiConsumer<Object, Map<String, Object>>> resolvers = new LinkedList<>();

    // walk down the whole tree of classes until we hit the object class
    var current = type;
    do {
      var fields = current.getDeclaredFields();
      for (var field : fields) {
        // ignore static fields
        var modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers)) {
          continue;
        }

        // check if the field is a param
        var paramData = field.getAnnotation(Param.class);
        if (paramData != null) {
          field.setAccessible(true);

          // get the parameter name, only use the given name if it's not empty
          var suppliedName = paramData.value();
          var paramName = suppliedName.trim().isEmpty() ? field.getName() : suppliedName;

          // make a single instance of the expander type
          var expander = constructExpanderInstance(paramData.expander());

          // register the param, continue after
          resolvers.add((typeInstance, targetMap) -> {
            var fieldValue = resolveFieldValue(field, typeInstance);
            if (fieldValue != null) {
              var expandedValue = expander == null ? fieldValue : expander.expand(fieldValue);
              targetMap.put(paramName, expandedValue);
            }
          });
          continue;
        }

        // check if the parameter is a query map itself
        var queryMapData = field.getAnnotation(FlattenQueryMap.class);
        if (queryMapData != null) {
          field.setAccessible(true);
          resolvers.add((typeInstance, targetMap) -> {
            var fieldValue = resolveFieldValue(field, typeInstance);
            if (fieldValue != null) {
              var encodeResult = this.encode(fieldValue);
              targetMap.putAll(encodeResult);
            }
          });
        }
      }
    } while ((current = current.getSuperclass()) != Object.class);

    return resolvers;
  }
}
