package me.shedaniel.autoconfig.requirements.definition;

import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.gui.registry.GuiLookupTable;
import me.shedaniel.autoconfig.requirements.HandlerLookupTable;
import me.shedaniel.autoconfig.requirements.builder.BooleanRequirementBuilder;
import me.shedaniel.autoconfig.requirements.builder.EnumRequirementBuilder;
import me.shedaniel.autoconfig.requirements.builder.StaticRequirementBuilder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.Requirement;
import me.shedaniel.clothconfig2.api.ValueHolder;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

record RequirementDefinitionEntry(
        Reference[] refs,
        String[] conditions,
        Pattern[] regexConditions
) {
    
    static RequirementDefinitionEntry from(Class<?> base, ConfigEntry.Requirement.EnableIf annotation) {
        return from(base, annotation.value(), annotation.condition(), annotation.regexCondition());
    }
    
    static RequirementDefinitionEntry from(Class<?> base, ConfigEntry.Requirement.DisplayIf annotation) {
        return from(base, annotation.value(), annotation.condition(), annotation.regexCondition());
    }
    
    private static RequirementDefinitionEntry from(Class<?> base, String[] references, String[] conditions, String[] regexConditions) {
        Reference[] refs = Arrays.stream(references)
                .map(ref -> Reference.parse(base, ref))
                .toArray(Reference[]::new);
        
        Pattern[] patterns;
        try {
            patterns = Arrays.stream(regexConditions)
                    .map(Pattern::compile)
                    .toArray(Pattern[]::new);
        } catch (PatternSyntaxException e) {
            throw new RuntimeException("Invalid regex in requirement conditions (in %s)".formatted(base.getCanonicalName()), e);
        }
        
        return new RequirementDefinitionEntry(refs, conditions, patterns);
    }
    
    public Requirement build(GuiLookupTable guis, HandlerLookupTable handlers) {
        Requirement[] requirements = Arrays.stream(this.refs())
                .map(reference -> {
                    // First, check if the reference points to a custom handler
                    Requirement handler = handlers.getHandler(reference);
                    if (handler != null) {
                        // Use the handler
                        return handler;
                    }
                    
                    // Otherwise, assume it's a reference to a Config Entry
                    AbstractConfigListEntry gui = guis.getGui(reference);
                    if (gui == null) {
                        throw new IllegalStateException("Reference \"%s\" does not match any Config Entry or Requirement Handler..."
                                .formatted(reference.original()));
                    }
                    
                    // Build a static handler matching the Config Entry's value to the defined conditions
                    StaticRequirementBuilder<?> builder = builder(gui);
                    if (builder == null) {
                        throw new IllegalStateException("Unsupported Config Entry type `%s`".formatted(gui.getClass().getSimpleName()));
                    }
                    
                    return builder.build();
                    
                })
                .toArray(Requirement[]::new);
        
        return Requirement.any(requirements);
    }
    
    @SuppressWarnings("unchecked")
    private @Nullable StaticRequirementBuilder<?> builder(ValueHolder<?> gui) {
        Class<?> type = gui.getType();
        if (Boolean.class.equals(type)) {
            return new BooleanRequirementBuilder((ValueHolder<Boolean>) gui, conditions(), regexConditions());
        }
        if (Enum.class.isAssignableFrom(type)) {
            return new EnumRequirementBuilder<>((ValueHolder<Enum<?>>) gui, conditions(), regexConditions());
        }
        // TODO String
        // TODO Generic _string compare_ via toString _or_valueOf??
        return null;
    }
}
