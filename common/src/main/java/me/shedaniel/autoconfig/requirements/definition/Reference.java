package me.shedaniel.autoconfig.requirements.definition;

import me.shedaniel.autoconfig.annotation.ConfigEntry;
import org.jetbrains.annotations.ApiStatus;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApiStatus.Internal
public interface Reference {
    // Define a regex with named groups to parse references
    String MEMBER = "member";
    String DESCR = "desrciptos";
    String INDEX = "index";
    Pattern REF_PATTERN = Pattern.compile("^"
                                                               + "#?" // FIXME should we enforce having/not-having a '#' prefix?
                                                               + "(?<" + MEMBER + ">[^.]+?)"
                                                               + "(?<" + DESCR + ">\\([^)]+?\\)\\[?(?:[ZBCSIJFD]|L;)|\\(\\))?"
                                                               + "(?:\\[(?<" + INDEX + ">[0-9]+)])?"
                                                               + "$");
    
    String original();
    
    static Reference parse(Class<?> base, ConfigEntry.Ref reference) {
        Class<?> cls = Objects.equals(ConfigEntry.None.class, reference.cls())
                ? base : reference.cls();
        return parse(cls, reference.value());
    }
    
    static Reference parse(Class<?> base, String reference) {
        Matcher match = REF_PATTERN.matcher(reference);
        String member;
        MethodTypeDesc descriptor;
        int index;
        boolean checkForField;
        
        // Validate and parse, wrapping any errors in a RuntimeException
        try {
            if (!match.matches()) {
                throw new RuntimeException("Invalid format");
            }
            
            member = match.group(MEMBER);
            
            String descStr = match.group(DESCR);
            descriptor = descStr == null || descStr.equals("()")
                    ? null : MethodTypeDesc.ofDescriptor(descStr);
            
            String indexStr = match.group(INDEX);
            index = indexStr == null ? -1 : Integer.parseInt(indexStr, 10);
            
            if (indexStr != null && descStr != null) {
                throw new IllegalArgumentException("index syntax not supported on method refs");
            }
            
            checkForField = indexStr != null || descStr == null;
            
            throwIfInvalid(member);
        } catch (Throwable t) {
            throw new RuntimeException("Unable to parse reference \"%s\" on `%s`".formatted(reference, base.getSimpleName()), t);
        }
        
        // Get the referenced target
        java.lang.reflect.Method method;
        try {
            if (!checkForField) {
                throw new NoSuchFieldException(); // Hacky GOTO catch block
            }
            java.lang.reflect.Field field = base.getDeclaredField(member);
            return new Reference.Field(base, field, index, reference);
        } catch (NoSuchFieldException fieldException) {
            try {
                List<java.lang.reflect.Method> methods = Arrays.stream(base.getDeclaredMethods())
                        .filter(m -> m.getName().equals(member))
                        .filter(m -> descriptor == null || compareTypes(descriptor, m))
                        .toList();
                
                if (methods.isEmpty()) {
                    throw new NoSuchMethodException("TODO");
                }
                if (methods.size() > 1) {
                    throw new IllegalArgumentException("Multiple matches");
                }
                method = methods.get(0);
                
            } catch (NoSuchMethodException methodException) {
                String[] fields = Arrays.stream(base.getDeclaredFields()).map(java.lang.reflect.Field::getName).toArray(String[]::new);
                String[] methods = Arrays.stream(base.getDeclaredMethods()).map(java.lang.reflect.Method::getName).toArray(String[]::new);
                
                throw new IllegalArgumentException("No method or field found for reference \"%s\" on class \"%s\". Fields: %s  Methods %s."
                        .formatted(reference, base.getCanonicalName(), Arrays.toString(fields), Arrays.toString(methods)));
            }
        }

        return new Reference.Method(base, method, reference);
    }
    
    private static boolean compareTypes(MethodTypeDesc desc, java.lang.reflect.Method method) {
        if (!compareTypes(desc.returnType(), method.getReturnType())) {
            return false;
        }
        return compareTypes(desc.parameterArray(), method.getParameterTypes());
    }
    
    private static boolean compareTypes(ClassDesc[] descList, Class<?>[] classList) {
        if (descList.length < 1) {
            return classList.length < 1;
        }
        if (descList.length != classList.length) {
            return false;
        }
        for (int i = 0; i < descList.length; i++) {
            if (!compareTypes(descList[i], classList[i])) {
                return false;
            }
        }
        return true;
    }
    
    private static boolean compareTypes(ClassDesc desc, Class<?> cls) {
        return cls.describeConstable().map(desc::equals).orElse(false);
    }
    
    private static void throwIfInvalid(CharSequence identifier) {
        if (identifier == null) {
            throw new RuntimeException("Invalid identifier: null");
        }
        
        int i = validateIdentifier(identifier);
        
        if (i < -1) {
            // Valid
            return;
        }
        
        if (i < 0) {
            // Empty
            throw new RuntimeException("Invalid identifier: empty string");
        }
        
        throw new RuntimeException("Invalid identifier: illegal char '%c' at index %d"
                .formatted(identifier.charAt(i), i));
    }
    
    /**
     * Checks the given string is a valid java identifier
     * @return -2 if valid, -1 if empty, otherwise the index of the first invalid char
     */
    private static int validateIdentifier(CharSequence identifier) {
        int len = identifier.length();
        if (len < 1) {
            // Empty
            return -1;
        }
        
        int i = 0;
        if (!Character.isJavaIdentifierStart(identifier.charAt(i))) {
            return i;
        }
        
        while (++i < len) {
            char c = identifier.charAt(i);
            if (c != '.' && !Character.isJavaIdentifierPart(c)) {
                return i;
            }
        }
        
        // Valid
        return -2;
    }
    
    /**
     * @param baseClass
     * @param field
     * @param index       Index of the Gui Config Entry to use. Negative indexes are subtracted from the total number of entries. Defaults to -1.
     * @param original
     */
    @ApiStatus.Internal
    record Field(
            Class<?> baseClass,
            java.lang.reflect.Field field,
            int index,
            @Deprecated String original
    ) implements Reference {}
    
    /**
     * @param baseClass
     * @param method
     * @param original
     */
    @ApiStatus.Internal
    record Method(
            Class<?> baseClass,
            java.lang.reflect.Method method,
            @Deprecated String original
    ) implements Reference {}
}
