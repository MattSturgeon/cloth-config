package me.shedaniel.autoconfig.requirements;

import me.shedaniel.autoconfig.annotation.RequirementHandler;
import me.shedaniel.autoconfig.requirements.builder.HandlerBuilder;
import me.shedaniel.autoconfig.requirements.definition.Reference;
import me.shedaniel.clothconfig2.api.Requirement;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@ApiStatus.Internal
public class HandlerLookupTable {
    
    private final Map<String, Requirement> handlers = new HashMap<>();
    
    public static HandlerLookupTable fromClasses(Collection<Class<?>> classes, HandlerBuilder builder) {
        Set<Method> methods = classes.stream()
                .map(Class::getDeclaredMethods)
                .flatMap(Arrays::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        return fromMethods(methods, builder);
        
    }
    public static HandlerLookupTable fromMethods(Collection<Method> methods, HandlerBuilder builder) {
        HandlerLookupTable table = new HandlerLookupTable();
        
        methods.forEach(method -> {
            RequirementHandler definition = method.getDeclaredAnnotation(RequirementHandler.class);
            if (definition == null) {
                return;
            }
            
            Requirement requirement = builder.build(method, definition);
            
            table.register(method, requirement);
        });
        
        return table;
    }
    
    public void register(Method method, Requirement handler) {
        handlers.compute(keyOf(method), (key, value) -> {
            if (value != null) {
                throw new RuntimeException("Error: Multiple methods have the same reference (\"%s\")".formatted(key));
            }
            
            return handler;
        });
    }
    
    public @Nullable Requirement getHandler(Reference reference) {
        // First, try package-relative
        Requirement req = getHandler(relKeyOf(reference));
        if (req != null) {
            return req;
        }
        
        // Then try canonical
        return getHandler(keyOf(reference));
    }
    
    public @Nullable Requirement getHandler(String reference) {
        return handlers.get(reference);
    }
    
    @Deprecated
    public Map<String, Requirement> getHandlers() {
        return handlers;
    }
    
    private static String keyOf(Reference reference) {
        return reference.classRef() + "#" + reference.memberRef();
    }
    
    private static String relKeyOf(Reference reference) {
        return reference.basePackage() + "." + reference.classRef() + "#" + reference.memberRef();
    }
    
    private static String keyOf(Method method) {
        return method.getDeclaringClass().getCanonicalName() + "#" + method.getName();
    }
}
