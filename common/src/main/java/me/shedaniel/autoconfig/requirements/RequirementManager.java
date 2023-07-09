package me.shedaniel.autoconfig.requirements;

import joptsimple.internal.Strings;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.annotation.RequirementHandler;
import me.shedaniel.autoconfig.example.ExampleConfig;
import me.shedaniel.autoconfig.gui.registry.GuiLookupTable;
import me.shedaniel.autoconfig.requirements.definition.Reference;
import me.shedaniel.autoconfig.requirements.definition.RequirementDefinition;
import me.shedaniel.clothconfig2.api.AbstractConfigEntry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.Requirement;
import me.shedaniel.clothconfig2.api.ValueHolder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

@ApiStatus.Internal
public class RequirementManager {
    
    private final GuiLookupTable guis;
    private final Collection<Class<?>> handlerClasses = new HashSet<>();
    // FIXME AbstractConfigEntry vs AbstractConfigListEntry (vs a superclass or interface...)
    private final Map<AbstractConfigEntry, Set<RequirementDefinition>> declaredRequirements = new HashMap<>();
    
    public RequirementManager(GuiLookupTable guiLookupTable) {
        this.guis = guiLookupTable;
    }
    
    /**
     * Register config entries whose defining field may have requirement annotations declared
     * 
     * @param guis
     * @param field
     */
    public void registerRequirements(Collection<? extends AbstractConfigEntry> guis, Field field) {
        
        // If this field has requirements defined, add them to the map
        List<RequirementDefinition> requirements = RequirementDefinition.from(field);
        
        if (!requirements.isEmpty()) {
            // Try to select a relatively efficient initial capacity,
            // considering the entry may have additional requirements defined elsewhere.
            int size = requirements.size();
            int initialCapacity = size + Math.min(size, 32);
            for (AbstractConfigEntry gui : guis) {
                declaredRequirements
                        .computeIfAbsent(gui, e -> new HashSet<>(initialCapacity))
                        .addAll(requirements);
            }
        }
    }
    
    /**
     * Register a class which may contain custom requirement handlers
     * 
     * @param classes zero or more classes which contain requirement handler methods
     */
    public void registerHandlerClass(Class<?> ...classes) {
        Collections.addAll(handlerClasses, classes);
    }
    
    public void buildCustomRequirement() {
        HandlerLookupTable handlers = HandlerLookupTable.fromClasses(handlerClasses, this::buildCustomRequirement);
        declaredRequirements.forEach(((gui, definitions) ->
                buildRequirements(gui, definitions, guis, handlers)
                        .apply()));
        
        // DEBUGGING print everything registered
        // TODO REMOVE
        System.out.println("Registered fields");
        guis.getTable().forEach((key, list) -> list.forEach(gui -> System.out.printf("%s: %s%n", key, gui.toString())));
        
        System.out.printf("%n%nRegistered fields with dependencies%n");
        declaredRequirements.forEach((gui, list) -> 
                list.forEach(def -> System.out.printf("%s: %s%n", gui.toString(), def.action())));
        
        System.out.printf("%n%nCustom Requirement methods:%n");
        handlers.getHandlers().forEach((key, req) -> System.out.printf("%s: %s%n", key, req.toString()));
        
        // Test out get methods
        // TODO REMOVE
        System.out.printf("%n%n");
        try {
            Requirement handler1 = handlers.getHandler("me.shedaniel.autoconfig.example.ExampleConfig.ModuleC.Handlers#coolToggleIsEnabled");
            boolean result = handler1.check();
            System.out.printf("Got handler1 (coolToggleIsEnabled), currently %s%n", result);
        } catch (Throwable t) {
            System.out.printf("Error getting handler1: %s%n", t);
            t.printStackTrace(System.out);
        }
        try {
            Reference ref = Reference.parse(ExampleConfig.ModuleC.class, "ExampleConfig.ModuleC.Handlers.coolToggleMatchesLameToggle");
            Requirement handler2 = handlers.getHandler(ref);
            boolean result = handler2.check();
            System.out.printf("Got handler2 (coolToggleMatchesLameToggle), currently %s%n", result);
        } catch (Throwable t) {
            System.out.printf("Error getting handler2: %s%n", t);
            t.printStackTrace(System.out);
        }
        try {
            Reference ref = Reference.parse(ExampleConfig.ModuleC.Handlers.class, "coolEnumIsGoodOrBetter");
            Requirement handler3 = handlers.getHandler(ref);
            boolean result = handler3.check();
            System.out.printf("handler3 (coolEnumIsGoodOrBetter), currently %s%n", result);
        } catch (Throwable t) {
            System.out.printf("Error checking handler3: %s%n", t);
            t.printStackTrace(System.out);
        }
        try {
            Reference reference = Reference.parse(ExampleConfig.ModuleC.DependencySubCategory.class, "#coolToggle");
            var gui1 = guis.getGui(reference);
            System.out.printf("Got gui1 (coolToggle): %s%n", gui1.toString());
        } catch (Throwable t) {
            System.out.printf("Error getting gui1: %s%n", t.getLocalizedMessage());
            t.printStackTrace(System.out);
        }
        try {
            Reference reference = Reference.parse(ExampleConfig.ModuleC.Handlers.class, "ExampleConfig.ModuleC.DependencySubCategory#coolToggle[0]");
            var gui2 = guis.getGui(reference);
            System.out.printf("Got gui2 (coolToggle): %s%n", gui2.toString());
        } catch (Throwable t) {
            System.out.printf("Error getting gui2: %s%n", t.getLocalizedMessage());
            t.printStackTrace(System.out);
        }
    }
    
    // TODO extract to a builder class
    private Requirement buildCustomRequirement(Method method, RequirementHandler definition) {
        
        // Validate method signature
        Class<?> returnType = method.getReturnType();
        if (returnType != boolean.class && returnType != Boolean.class) {
            throw new RuntimeException(("Unexpected return type on %s#%s: expected %s but found %s\n")
                    .formatted(method.getDeclaringClass().getCanonicalName(), method.getName(), Boolean.class, returnType));
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
        
        // Get a list of targeted guis (their values will be the method parameters)
        int targetCount = definition.value().length;
        List<AbstractConfigListEntry> targets = Arrays.stream(definition.value())
                .map(reference -> Reference.parse(method.getDeclaringClass(), reference))
                .map(this.guis::getGui)
                .toList();
        
        
        // Validate the method parameters are compatible with the targeted guis
        List<String> typeErrors = new ArrayList<>(targetCount);
        
        if (method.getParameterCount() != targetCount) {
            typeErrors.add("    Incorrect parameter count, expected %d found %d"
                    .formatted(method.getParameterCount(), targetCount));
        }
        
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0, paramCount = parameterTypes.length; i < paramCount; i++) {
            Class<?> paramType = parameterTypes[i];
            Class<?> targetType = targets.get(i).getType();
            
            if (!targetType.isAssignableFrom(paramType)) {
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
        
        // Build and return an actual Requirement function which will invoke the handler method
        method.setAccessible(true);
        return () -> {
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
        };
    }
    
    /**
     * Return true if a supported dependency annotation is <em>present</em> on the provided field.
     *
     * @param field the field to check
     * @return whether the provided field has dependency annotations present
     */
    public static boolean hasRequirementAnnotation(Field field) {
        return field.isAnnotationPresent(ConfigEntry.Requirement.EnableIf.class)
               || field.isAnnotationPresent(ConfigEntry.Requirement.EnableIfGroup.class)
               || field.isAnnotationPresent(ConfigEntry.Requirement.DisplayIf.class)
               || field.isAnnotationPresent(ConfigEntry.Requirement.DisplayIfGroup.class);
    }
    
    private static Requirements buildRequirements(AbstractConfigEntry gui, Collection<RequirementDefinition> requirements, GuiLookupTable guis, HandlerLookupTable handlers) {
        Set<Requirement> enables = new HashSet<>(requirements.size());
        Set<Requirement> displays = new HashSet<>(requirements.size());
        
        // TODO consider if it is worth it to do optimization passes,
        //    e.g. we could try to combine similar RequirementGroups into
        //    one definition before building the definitions...
        for (RequirementDefinition definition : requirements) {
            // Build the requirement and add it to the relevant set
            Requirement requirement = definition.build(guis, handlers);
            switch (definition.action()) {
                case ENABLE -> enables.add(requirement);
                case DISPLAY -> displays.add(requirement);
            }
        }
        
        return new Requirements(gui, combine(enables), combine(displays));
    }
    
    /**
     * Combine multiple {@link Requirement requirements} into one using {@link Requirement#all(Requirement...)}.
     */
    private static @Nullable Requirement combine(Collection<Requirement> requirements) {
        if (requirements.size() < 2) {
            return requirements.stream().findAny().orElse(null);
        }
        
        return Requirement.all(requirements.toArray(Requirement[]::new));
    }
    
    /**
     * Encapsulates each supported type of requirement along with the Config Entry GUI that they should control.
     */
    private record Requirements(
            AbstractConfigEntry gui,
            @Nullable Requirement enable,
            @Nullable Requirement display
    ) {
        /**
         * Applies {@link #enable} and {@link #display} requirements to the {@link #gui Config Entry Gui}.
         */
        private void apply() {
            this.gui().setRequirement(this.enable());
            this.gui().setDisplayRequirement(this.display());
        }
    }
}

