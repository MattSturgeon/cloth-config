package me.shedaniel.autoconfig.requirements.definition;

import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.gui.registry.GuiLookupTable;
import me.shedaniel.autoconfig.requirements.HandlerLookupTable;
import me.shedaniel.autoconfig.requirements.builder.RequirementBuilder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.Requirement;
import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@ApiStatus.Internal
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
                    return RequirementBuilder
                            .builder(gui, conditions(), regexConditions())
                            .build();
                })
                .toArray(Requirement[]::new);
        
        return Requirement.any(requirements);
    }
}
