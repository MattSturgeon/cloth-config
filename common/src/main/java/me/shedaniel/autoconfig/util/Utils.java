/*
 * This file is part of Cloth Config.
 * Copyright (C) 2020 - 2021 shedaniel
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package me.shedaniel.autoconfig.util;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;

import static java.util.stream.Collectors.toMap;

public class Utils {
    private Utils() {
    }
    
    @ExpectPlatform
    public static Path getConfigFolder() {
        throw new AssertionError();
    }
    
    public static <V> V constructUnsafely(Class<V> cls) {
        try {
            Constructor<V> constructor = cls.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static <V> V getUnsafely(Field field, Object obj) {
        if (obj == null)
            return null;
        
        try {
            field.setAccessible(true);
            //noinspection unchecked
            return (V) field.get(obj);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static <V> V getUnsafely(Field field, Object obj, V defaultValue) {
        V ret = getUnsafely(field, obj);
        if (ret == null)
            ret = defaultValue;
        return ret;
    }
    
    public static void setUnsafely(Field field, Object obj, Object newValue) {
        if (obj == null)
            return;
        
        try {
            field.setAccessible(true);
            field.set(obj, newValue);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static <T, K, U> Collector<T, ?, Map<K, U>> toLinkedMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valueMapper
    ) {
        return toMap(
                keyMapper,
                valueMapper,
                (u, v) -> {
                    throw new IllegalStateException(String.format("Duplicate key %s", u));
                },
                LinkedHashMap::new
        );
    }
    
    /**
     * Checks if {@code to} can be assigned from {@code from}, including boxed types and their respective primitives.
     */
    public static boolean typesCompatible(Class<?> from, Class<?> to) {
        if (to.isAssignableFrom(from)) {
            return true;
        }
        if (to.isPrimitive()) {
            return typesCompatible(from, boxPrimitive(to));
        }
        if (from.isPrimitive()) {
            return typesCompatible(boxPrimitive(from), to);
        }
        return false;
    }
    
    /**
     * Converts a primitive class into a boxed class.
     * 
     * <p> If a non-primitive class is used, it is returned as-is.
     */
    public static Class<?> box(Class<?> cls) {
        return cls.isPrimitive() ? boxPrimitive(cls) : cls;
    }
    
    private static Class<?> boxPrimitive(Class<?> primitive) {
        if (primitive == Integer.TYPE) {
            return Integer.class;
        }
        if (primitive == Byte.TYPE) {
            return Byte.class;
        }
        if (primitive == Short.TYPE) {
            return Short.class;
        }
        if (primitive == Long.TYPE) {
            return Long.class;
        }
        if (primitive == Float.TYPE) {
            return Float.class;
        }
        if (primitive == Double.TYPE) {
            return Double.class;
        }
        if (primitive == Boolean.TYPE) {
            return Boolean.class;
        }
        if (primitive == Character.TYPE) {
            return Character.class;
        }
        throw new IllegalStateException("%s#box() was passed a non-primitive class! %s"
                .formatted(Utils.class.getCanonicalName(), primitive.getCanonicalName()));
    }
}
