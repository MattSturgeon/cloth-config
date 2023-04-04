package me.shedaniel.autoconfig.util;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import java.util.regex.Pattern;

public class RelativeRefParser {
    
    private static final char JAVA_MEMBER = '#';
    private static final char JOINER = '.';
    
    private final Class<?> baseClass;
    
    public RelativeRefParser(Class<?> baseClass) {
        this.baseClass = baseClass;
    }
    
    public static String getReference(Field field) {
        return field.getDeclaringClass().getName() + JAVA_MEMBER + field.getName();
    }
    
    public static String getReference(@Nullable Class<?> baseClass, Field field) {
        if (baseClass == null)
            return getReference(field);
        
        Deque<String> classes = new LinkedList<>();
        Class<?> aClass = field.getDeclaringClass();
        while (aClass != baseClass) {
            classes.addFirst(aClass.getSimpleName());
            aClass = Optional.ofNullable(aClass.getDeclaringClass())
                    .orElseThrow(() -> new IllegalArgumentException("\"%s\" does not encapsulate field \"%s\"".formatted(baseClass.getName(), getReference(field))));
        }
    
        return String.join(String.valueOf(JOINER), classes) + JAVA_MEMBER + field.getName();
    }
    
    public Field getField(String reference) {
        return getField(baseClass, reference);
    }
    
    public static Field getField(@Nullable Class<?> baseClass, String reference) {
        if (reference == null || reference.isEmpty())
            return null;
    
        String trimmed = reference.trim();
        int index = trimmed.indexOf(JAVA_MEMBER);
        if (index < 0)
            throw new IllegalArgumentException("Invalid reference: missing '%s' deliminator: \"%s\"".formatted(JAVA_MEMBER, trimmed));
        
        String classReference = trimmed.substring(0, index);
        String fieldName = trimmed.substring(index + 1);
        
        if (fieldName.isEmpty())
            throw new IllegalArgumentException("Invalid reference: missing field name: \"%s\"".formatted(trimmed));
        
        // Try absolute
        try {
            Class<?> absoluteClass = Class.forName(classReference);
            return absoluteClass.getDeclaredField(fieldName);
        } catch (ClassNotFoundException e) {
            // Not absolute
        } catch (NoSuchFieldException e) {
            // Invalid field reference
            throw new IllegalArgumentException(e);
        }
        
        // Try relative
        if (baseClass == null)
            throw new IllegalArgumentException(new NullPointerException("`baseClass` cannot be null for relative reference: \"%s\"".formatted(trimmed)));
    
        // We can we support relative class references starting with the base class name... FIXME is this a good idea?
        // I.e. "BaseClass.FooClass.BarClass#field" is equivalent to "FooClass.BarClass#field".
        //
        // Pattern matches `baseClass`'s simple name - so long as it is at the start of the string,
        // and followed by either `JOINER` or the end of the string.
        String subReference = Pattern.compile("^" + Pattern.quote(baseClass.getSimpleName()) + "(?:" + Pattern.quote(String.valueOf(JOINER)) + "|$)")
                .matcher(classReference)
                .replaceFirst("");
        
        Class<?> referencedClass;
        if (subReference.isEmpty()) {
            // References a sibling member directly
            referencedClass = baseClass;
        } else {
            // References via a sibling class
            try {
                referencedClass = getSubClass(baseClass, subReference);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }
        
        return getFieldInClass(referencedClass, fieldName)
                .orElseThrow(() -> new IllegalArgumentException(new NoSuchFieldException("No field \"%s\" found in \"%s\""
                        .formatted(fieldName, referencedClass.getName()))));
    }
    
    private static Optional<Field> getFieldInClass(Class<?> aClass, String fieldName) {
        return Arrays.stream(aClass.getDeclaredFields())
                .filter(field -> field.getName().equals(fieldName))
                .findAny();
    }
    
    private static Class<?> getSubClass(Class<?> baseClass, String reference) throws ClassNotFoundException {
        int index = reference.indexOf(JOINER);
        String childName;
        String remaining;
        if (index < 0) {
            childName = reference;
            remaining = "";
        } else {
            childName = reference.substring(0, index);
            remaining = reference.substring(index + 1);
        }
    
        Class<?> childClass = getChildClass(baseClass, childName);
        return remaining.isEmpty() ? getSubClass(childClass, remaining) : childClass;
    }
    
    private static Class<?> getChildClass(Class<?> parent, String childName) throws ClassNotFoundException {
        return Arrays.stream(parent.getDeclaredClasses())
                .filter(sibling -> sibling.getSimpleName().equals(childName))
                .findAny()
                .orElseThrow(() -> new ClassNotFoundException("No class \"%s\" found in \"%s\""
                        .formatted(childName, parent.getName())));
    }
}
