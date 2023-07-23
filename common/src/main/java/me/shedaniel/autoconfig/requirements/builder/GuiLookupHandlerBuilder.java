package me.shedaniel.autoconfig.requirements.builder;

import joptsimple.internal.Strings;
import me.shedaniel.autoconfig.annotation.RequirementHandler;
import me.shedaniel.autoconfig.gui.registry.GuiLookupTable;
import me.shedaniel.autoconfig.requirements.definition.Reference;
import me.shedaniel.clothconfig2.api.Requirement;
import me.shedaniel.clothconfig2.api.ValueHolder;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ApiStatus.Internal
class GuiLookupHandlerBuilder implements HandlerBuilder {
    private final GuiLookupTable guiTable;
    
    GuiLookupHandlerBuilder(GuiLookupTable guiTable) {
        this.guiTable = guiTable;
    }
    
    @Override
    public Requirement build(Method method, RequirementHandler definition) {
        validateSignature(method);
        List<? extends ValueHolder<?>> targets = getTargetGuis(method.getDeclaringClass(), definition);
        List<? extends Class<?>> types = targets.stream().map(ValueHolder::getType).toList();
        validateParameterTypes(method, types);
        method.setAccessible(true);
        
        return () -> run(method, targets);
    }
    
    /**
     * Runs the handler method, passing it the current value of each target
     */
    private static boolean run(Method method, List<? extends ValueHolder<?>> targets) {
        Object[] args = targets.stream()
                .map(ValueHolder::getValue)
                .toArray();
        
        try {
            return (Boolean) method.invoke(null, args);
        } catch (Throwable t) {
            throw new RuntimeException("Exception thrown while handling requirement using %s#%s"
                    .formatted(method.getDeclaringClass().getCanonicalName(), method.getName()),
                    t);
        }
    }
    
    /**
     * Gets a list of GUIs referenced in the {@link RequirementHandler} definition.
     */
    private List<? extends ValueHolder<?>> getTargetGuis(Class<?> base, RequirementHandler definition) {
        return Arrays.stream(definition.value())
                .map(reference -> Reference.parse(base, reference))
                .map(this.guiTable::getGui)
                .map(gui -> (ValueHolder<?>) gui)
                .toList();
    }
    
    /**
     * @throws RuntimeException if the method cannot be used as a handler function because of its method signature.
     */
    private static void validateSignature(Method method) {
        Class<?> returnType = method.getReturnType();
        if (!compatibleTypes(returnType, Boolean.TYPE)) {
            throw new RuntimeException(("Unexpected return type on %s#%s: expected `boolean` but found %s\n")
                    .formatted(method.getDeclaringClass().getCanonicalName(), method.getName(), returnType));
        }
        
        if (method.isVarArgs()) {
            throw new RuntimeException(("Unexpected VarArgs on %s#%s: Variable arguments are not supported\n")
                    .formatted(method.getDeclaringClass().getCanonicalName(), method.getName()));
        }
        
        // TODO can we handle instance methods too? How would we find/store the object instance?
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new RuntimeException(("Unexpected instance method %s#%s: Only static methods are supported\n")
                    .formatted(method.getDeclaringClass().getCanonicalName(), method.getName()));
        }
        
        // Ensure no exceptions are declared on the handler
        List<String> exceptions = Arrays.stream(method.getExceptionTypes())
                .map(Class::getCanonicalName)
                .toList();
        if (!exceptions.isEmpty()) {
            throw new RuntimeException("Unexpected exceptions declared on %s#%s\n"
                                               .formatted(method.getDeclaringClass().getCanonicalName(), method.getName())
                                       + Strings.join(exceptions, "\n"));
        }
    }
    
    /**
     * @throws RuntimeException if the method's parameter types are mismatched with the targeted types.
     */
    private static void validateParameterTypes(Method method, List<? extends Class<?>> types) {
        // Validate the method parameters are compatible with the targeted guis
        int targetCount = types.size();
        Class<?>[] parameterTypes = method.getParameterTypes();
        List<String> typeErrors = new ArrayList<>(targetCount);
        
        if (parameterTypes.length != targetCount) {
            typeErrors.add("    Incorrect parameter count, expected %d found %d"
                    .formatted(method.getParameterCount(), targetCount));
        }
        
        for (int i = 0; i < targetCount; i++) {
            Class<?> paramType = parameterTypes[i];
            Class<?> targetType = types.get(i);
            
            if (!compatibleTypes(targetType, paramType)) {
                typeErrors.add("    [param %s]: Expected `%s` found `%s`"
                        .formatted(i, targetType, paramType));
            }
        }
        
        if (!typeErrors.isEmpty()) {
            // TODO custom exception class
            throw new RuntimeException("Invalid parameter types on %s#%s: (%d errors)\n"
                                               .formatted(method.getDeclaringClass().getCanonicalName(), method.getName(), typeErrors.size())
                                       + Strings.join(typeErrors, "\n"));
        }
    }
    
    /**
     * Checks if {@code to} can be assigned from {@code from}, including boxed types and their respective primitives.
     */
    private static boolean compatibleTypes(Class<?> from, Class<?> to) {
        if (to.isAssignableFrom(from)) {
            return true;
        }
        if (to.isPrimitive()) {
            return compatibleTypes(from, box(to));
        }
        if (from.isPrimitive()) {
            return compatibleTypes(box(from), to);
        }
        return false;
    }
    
    /**
     * Converts a primitive class into a boxed class.
     * 
     * @throws IllegalStateException if a non-primitive class is used.
     */
    private static Class<?> box(Class<?> primitive) {
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
                .formatted(GuiLookupHandlerBuilder.class.getCanonicalName(), primitive.getCanonicalName()));
    }
}
