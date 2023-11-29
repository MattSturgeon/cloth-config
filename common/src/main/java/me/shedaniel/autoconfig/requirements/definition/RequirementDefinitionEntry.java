package me.shedaniel.autoconfig.requirements.definition;

import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.gui.registry.GuiLookupTable;
import me.shedaniel.autoconfig.requirements.handler.RequirementHandler;
import me.shedaniel.clothconfig2.api.Requirement;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;
import java.util.List;

@ApiStatus.Internal
public interface RequirementDefinitionEntry {
    
    default Requirement build(GuiLookupTable guis) {
        RequirementHandler handler = RequirementHandler.from(this, guis);
        return handler::run;
    }
    
    static RequirementDefinitionEntry from(Class<?> base, ConfigEntry.Requirements.EnableIf annotation) {
        return from(base, annotation.value(), annotation.conditions(), annotation.refArgs(), annotation.staticArgs());
    }
    
    static RequirementDefinitionEntry from(Class<?> base, ConfigEntry.Requirements.DisplayIf annotation) {
        return from(base, annotation.value(), annotation.conditions(), annotation.refArgs(), annotation.staticArgs());
    }
    
    static RequirementDefinitionEntry from(Class<?> base, ConfigEntry.Ref target, String[] conditions, ConfigEntry.Ref[] refArgs, String[] staticArgs) {
        Reference parsedTargetRef = Reference.parse(base, target);
        
        if (parsedTargetRef instanceof Reference.Method methodReference) {
            if (conditions.length > 0) {
                LogManager.getLogger().warn("ConfigEntry Requirement targeting method does not support 'conditions'. Use 'staticArgs' or 'refArgs' instead.");
            }
            
            List<Reference> parsedRefArgs = Arrays.stream(refArgs)
                    .map(ref -> Reference.parse(base, ref))
                    .toList();
            
            List<Reference.Field> fieldRefArgs = parsedRefArgs.stream()
                    .filter(Reference.Field.class::isInstance)
                    .map(Reference.Field.class::cast)
                    .toList();
            
            if (parsedRefArgs.size() != fieldRefArgs.size()) {
                int diff = parsedRefArgs.size() - fieldRefArgs.size();
                LogManager.getLogger().warn("ConfigEntry Requirement has {} refArgs that do not reference ConfigEntry Fields.", diff);
            }
            
            return new RequirementDefinitionEntry.Method(methodReference, fieldRefArgs.toArray(Reference.Field[]::new), staticArgs);
        }
        
        if (parsedTargetRef instanceof Reference.Field fieldReference) {
            if (refArgs.length > 0) {
                LogManager.getLogger().warn("ConfigEntry Requirement targeting Field does not support 'refArgs'. Use 'conditions' instead.");
            }
            if (staticArgs.length > 0) {
                LogManager.getLogger().warn("ConfigEntry Requirement targeting Field does not support 'staticArgs'. Use 'conditions' instead.");
            }
            
            return new RequirementDefinitionEntry.Field(fieldReference, conditions);
        }
        
        throw new IllegalStateException("Unsupported implementation of %s \"%s\"".formatted(Reference.class.getSimpleName(), parsedTargetRef.getClass().getSimpleName()));
    }
    
    record Method(Reference.Method target, Reference.Field[] refArgs, String[] staticArgs) implements RequirementDefinitionEntry {}
    record Field(Reference.Field target, String[] conditions) implements RequirementDefinitionEntry {}
}
