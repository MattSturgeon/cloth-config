package me.shedaniel.autoconfig.requirements.definition;

import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Requirement.Quantity;
import me.shedaniel.autoconfig.gui.registry.GuiLookupTable;
import me.shedaniel.autoconfig.requirements.HandlerLookupTable;
import me.shedaniel.clothconfig2.api.Requirement;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ApiStatus.Internal
public record RequirementDefinition(
        Quantity qty,
        Action action,
        RequirementDefinitionMember[] children
) {
    
    public RequirementDefinition(Class<?> base, ConfigEntry.Requirement.EnableIf annotation) {
        this(
                Quantity.ALL,
                Action.ENABLE,
                new RequirementDefinitionMember[] { RequirementDefinitionMember.from(base, annotation) });
    }
    
    public RequirementDefinition(Class<?> base, ConfigEntry.Requirement.DisplayIf annotation) {
        this(
                Quantity.ALL,
                Action.DISPLAY,
                new RequirementDefinitionMember[] { RequirementDefinitionMember.from(base, annotation) });
    }
    
    public RequirementDefinition(Class<?> base, ConfigEntry.Requirement.EnableIfGroup annotation) {
        this(
                annotation.requirement(),
                Action.ENABLE,
                Arrays.stream(annotation.value())
                        .map(refs -> RequirementDefinitionMember.from(base, refs))
                        .toArray(RequirementDefinitionMember[]::new));
    }
    
    public RequirementDefinition(Class<?> base, ConfigEntry.Requirement.DisplayIfGroup annotation) {
        this(
                annotation.requirement(),
                Action.DISPLAY,
                Arrays.stream(annotation.value())
                        .map(refs -> RequirementDefinitionMember.from(base, refs))
                        .toArray(RequirementDefinitionMember[]::new));
    }
    
    public static List<RequirementDefinition> from(Field field) {
        List<RequirementDefinition> list = new ArrayList<>(4);
        
        Class<?> base = field.getDeclaringClass();
        var enableIf = field.getDeclaredAnnotation(ConfigEntry.Requirement.EnableIf.class);
        var displayIf = field.getDeclaredAnnotation(ConfigEntry.Requirement.DisplayIf.class);
        var enableIfGroup = field.getDeclaredAnnotation(ConfigEntry.Requirement.EnableIfGroup.class);
        var displayIfGroup = field.getDeclaredAnnotation(ConfigEntry.Requirement.DisplayIfGroup.class);
        
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
    
    public Requirement build(GuiLookupTable guis, HandlerLookupTable handlers) {
        Requirement[] requirements = Arrays.stream(this.children())
                .map(child -> child.build(guis, handlers))
                .toArray(Requirement[]::new);
        
        // If there's no children, something weird is going on
        if (requirements.length < 1) {
            throw new IllegalStateException("Trying to build requirement without any children...");
        }
        
        // If there's only one child, try to avoid wrapping it in another requirement
        if (requirements.length == 1 && qty() != Quantity.NONE) {
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
        ENABLE, DISPLAY;
    }
}
