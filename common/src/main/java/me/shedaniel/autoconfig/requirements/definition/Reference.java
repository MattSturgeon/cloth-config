package me.shedaniel.autoconfig.requirements.definition;

import org.jetbrains.annotations.ApiStatus;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @param basePackage
 * @param classRef
 * @param memberRef
 * @param index Index of the Gui Config Entry to use. Negative indexes are subtracted from the total number of entries. Defaults to -1.
 */
@ApiStatus.Internal
public record Reference(
        String basePackage,
        String classRef,
        String memberRef,
        int index,
        @Deprecated String original
) {
    // Define a regex with named groups to parse references
    private static final String CLASS = "class";
    private static final String MEMBER = "member";
    private static final String INDEX = "index";
    private static final Pattern REF_PATTERN = Pattern.compile("^"
                                                               + "(?:(?<"+CLASS+">.*)[.#])?" // FIXME should we restrict to _one of_ `.` and `#` instead of either or?
                                                               + "(?<"+MEMBER+">[^.]+?)" // Use reluctant qualifier, so we can have an optional index after
                                                               + "(?:\\[(?<"+INDEX+">[0-9]+)])?"
                                                               + "$");
    
    public static Reference parse(Class<?> base, String reference) {
        Matcher m = REF_PATTERN.matcher(reference);
        String clsRef;
        String mbrRef;
        String pkg;
        int index;
        
        // Validate and parse, wrapping any errors in a RuntimeException
        try {
            if (!m.matches()) {
                throw new RuntimeException("Invalid format");
                
            }
            
            pkg = base.getPackageName();
            
            clsRef = m.group(CLASS);
            mbrRef = m.group(MEMBER);
            
            String indexStr = m.group(INDEX);
            index = indexStr == null ? -1 : Integer.parseInt(indexStr, 10);
            
            if (clsRef == null || clsRef.isEmpty()) {
                // FIXME as we typically check package-relative first, it may be more efficient to strip the packageName from the canonical name...
                clsRef = base.getCanonicalName();
            } else {
                throwIfInvalid(clsRef);
            }
            
            throwIfInvalid(mbrRef);
        } catch (Throwable t) {
            throw new RuntimeException("Unable to parse reference \"%s\" on `%s`".formatted(reference, base.getSimpleName()), t);
        }
        
        return new Reference(pkg, clsRef, mbrRef, index, reference);
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
}
