package me.shedaniel.autoconfig.requirements;

import me.shedaniel.autoconfig.example.ExampleConfig;
import me.shedaniel.autoconfig.gui.registry.GuiLookupTable;
import me.shedaniel.autoconfig.requirements.builder.HandlerBuilder;
import me.shedaniel.autoconfig.requirements.definition.Reference;
import me.shedaniel.autoconfig.requirements.definition.RequirementDefinition;
import me.shedaniel.clothconfig2.api.Requirement;
import me.shedaniel.clothconfig2.gui.widget.DynamicEntryListWidget;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;

@ApiStatus.Internal
public class RequirementManager {
    
    private final GuiLookupTable guis;
    private final Collection<Class<?>> handlerClasses = new HashSet<>();
    private final Map<DynamicEntryListWidget.Entry, Set<RequirementDefinition>> requirements = new HashMap<>();
    private final HandlerBuilder handlerBuilder;
    
    public RequirementManager(GuiLookupTable guiLookupTable) {
        this.guis = guiLookupTable;
        handlerBuilder = HandlerBuilder.using(guis);
    }
    
    /**
     * Register config entries whose defining field may have requirement annotations declared
     * 
     * @param guis
     * @param field
     */
    public void registerRequirements(Collection<? extends DynamicEntryListWidget.Entry> guis, Field field) {
        
        // If this field has requirements defined, add them to the map
        List<RequirementDefinition> requirements = RequirementDefinition.from(field);
        
        if (!requirements.isEmpty()) {
            // Try to select a relatively efficient initial capacity,
            // considering the entry may have additional requirements defined elsewhere.
            int size = requirements.size();
            int initialCapacity = size + Math.min(size, 32);
            for (DynamicEntryListWidget.Entry gui : guis) {
                this.requirements
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
    
    public void build() {
        HandlerLookupTable handlers = HandlerLookupTable.fromClasses(handlerClasses, handlerBuilder);
        requirements.forEach(((gui, definitions) ->
                buildRequirements(gui, definitions, guis, handlers)
                        .apply()));
        
        // DEBUGGING print everything registered
        // TODO REMOVE
        System.out.println("Registered fields");
        guis.getTable().forEach((key, list) -> list.forEach(gui -> System.out.printf("%s: %s%n", key, gui.toString())));
        
        System.out.printf("%n%nRegistered fields with dependencies%n");
        requirements.forEach((gui, list) -> 
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
    
    private static Requirements buildRequirements(DynamicEntryListWidget.Entry gui, Collection<RequirementDefinition> requirements, GuiLookupTable guis, HandlerLookupTable handlers) {
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
            DynamicEntryListWidget.Entry gui,
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

