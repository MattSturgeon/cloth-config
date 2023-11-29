package me.shedaniel.autoconfig.requirements.definition;

import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Quantifier;
import me.shedaniel.autoconfig.gui.registry.GuiLookupTable;
import me.shedaniel.clothconfig2.api.Requirement;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ApiStatus.Internal
public record RequirementDefinition(
        Quantifier qty,
        Action action,
        RequirementDefinitionEntry[] children
) {
    
    public RequirementDefinition(Class<?> base, ConfigEntry.Requirements.EnableIf annotation) {
        this(
                Quantifier.ALL,
                Action.ENABLE,
                new RequirementDefinitionEntry[] { RequirementDefinitionEntry.from(base, annotation) });
    }
    
    public RequirementDefinition(Class<?> base, ConfigEntry.Requirements.DisplayIf annotation) {
        this(
                Quantifier.ALL,
                Action.DISPLAY,
                new RequirementDefinitionEntry[] { RequirementDefinitionEntry.from(base, annotation) });
    }
    
    public RequirementDefinition(Class<?> base, ConfigEntry.Requirements.EnableIfGroup annotation) {
        this(
                annotation.quantifier(),
                Action.ENABLE,
                Arrays.stream(annotation.value())
                        .map(refs -> RequirementDefinitionEntry.from(base, refs))
                        .toArray(RequirementDefinitionEntry[]::new));
    }
    
    public RequirementDefinition(Class<?> base, ConfigEntry.Requirements.DisplayIfGroup annotation) {
        this(
                annotation.quantifier(),
                Action.DISPLAY,
                Arrays.stream(annotation.value())
                        .map(refs -> RequirementDefinitionEntry.from(base, refs))
                        .toArray(RequirementDefinitionEntry[]::new));
    }
    
    public static List<RequirementDefinition> from(Field field) {
        List<RequirementDefinition> list = new ArrayList<>(4);
        
        Class<?> base = field.getDeclaringClass();
        var enableIf = field.getDeclaredAnnotation(ConfigEntry.Requirements.EnableIf.class);
        var displayIf = field.getDeclaredAnnotation(ConfigEntry.Requirements.DisplayIf.class);
        var enableIfGroup = field.getDeclaredAnnotation(ConfigEntry.Requirements.EnableIfGroup.class);
        var displayIfGroup = field.getDeclaredAnnotation(ConfigEntry.Requirements.DisplayIfGroup.class);
        
        if (enableIf != null) {
            list.add(new RequirementDefinition(base, enableIf));
        }
        
        if (displayIf != null) {
            list.add(new RequirementDefinition(base, displayIf));
        }
        
        if (enableIfGroup != null) {
            list.add(new RequirementDefinition(base, enableIfGroup));
        }
        
        if (displayIfGroup != null) {
            list.add(new RequirementDefinition(base, displayIfGroup));
        }
        
        return list;
    }
    
    public Requirement build(GuiLookupTable guis) {
        Requirement[] requirements = Arrays.stream(this.children())
                .map(child -> child.build(guis))
                .toArray(Requirement[]::new);
        
        // If there's no children, something weird is going on
        if (requirements.length < 1) {
            throw new IllegalStateException("Trying to build requirement without any children...");
        }
        
        // If there's only one child, try to avoid wrapping it in another requirement
        if (requirements.length == 1 && qty() != Quantifier.NONE) {
            return requirements[0];
        }
        
        return switch (this.qty()) {
            case ALL -> Requirement.all(requirements);
            case ANY -> Requirement.any(requirements);
            case NONE -> Requirement.none(requirements);
            case ONE -> Requirement.one(requirements);
        };
    }
    
    public enum Action {
        ENABLE, DISPLAY
    }
}
