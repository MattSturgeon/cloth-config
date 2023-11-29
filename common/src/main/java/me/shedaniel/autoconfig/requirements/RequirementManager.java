package me.shedaniel.autoconfig.requirements;

import me.shedaniel.autoconfig.gui.registry.GuiLookupTable;
import me.shedaniel.autoconfig.requirements.definition.RequirementDefinition;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.Requirement;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;

import static me.shedaniel.autoconfig.requirements.definition.RequirementDefinition.Action.DISPLAY;
import static me.shedaniel.autoconfig.requirements.definition.RequirementDefinition.Action.ENABLE;

@ApiStatus.Internal
public class RequirementManager {
    
    private final GuiLookupTable guiLookupTable;
    private final Map<AbstractConfigListEntry, Set<RequirementDefinition>> requirements = new HashMap<>();
    
    public RequirementManager(GuiLookupTable guiLookupTable) {
        this.guiLookupTable = guiLookupTable;
    }
    
    /**
     * Register config entries whose defining field may have requirement annotations declared.
     */
    public void registerRequirements(Collection<? extends AbstractConfigListEntry> guis, Field field) {
        // If this field has requirements defined, add them to the requirements registry
        List<RequirementDefinition> requirements = RequirementDefinition.from(field);
        
        if (!requirements.isEmpty()) {
            // Try to select a relatively efficient initial capacity,
            // considering the entry may have additional requirements defined elsewhere.
            int size = requirements.size();
            int initialCapacity = size + Math.min(size, 32);
            guis.forEach(gui -> this.requirements
                    .computeIfAbsent(gui, e -> new HashSet<>(initialCapacity))
                    .addAll(requirements));
        }
    }
    
    /**
     * Build and apply all registered {@link Requirement requirements}.
     */
    public void build() {
        requirements.forEach((gui, definitions) -> buildGuiRequirements(gui, definitions).apply());
    }
    
    private GuiRequirements buildGuiRequirements(AbstractConfigListEntry gui, Collection<RequirementDefinition> definitions) {
        Requirement[] enable = definitions.stream()
                .filter(definition -> Objects.equals(definition.action(), ENABLE))
                .map(definition -> definition.build(guiLookupTable))
                .toArray(Requirement[]::new);
        
        Requirement[] display = definitions.stream()
                .filter(definition -> Objects.equals(definition.action(), DISPLAY))
                .map(definition -> definition.build(guiLookupTable))
                .toArray(Requirement[]::new);
        
        return new GuiRequirements(gui, merge(enable), merge(display));
    }
    
    /**
     * Combine zero or more {@link Requirement requirements}, using {@link Requirement#all(Requirement...) Requirement#all()} if necessary.
     */
    public static @Nullable Requirement merge(Requirement... requirements) {
        if (requirements.length < 1) {
            return null;
        }
        if (requirements.length < 2) {
            return requirements[0];
        }
        return Requirement.all(requirements);
    }
    
    /**
     * Encapsulates {@link #enable} and {@link #display} requirements, along with the
     * {@link AbstractConfigListEntry Config Entry GUI} that they should control.
     */
    private record GuiRequirements(AbstractConfigListEntry gui, @Nullable Requirement enable, @Nullable Requirement display) {
        /**
         * Applies {@link #enable} and {@link #display} requirements to the {@link #gui Config Entry Gui}.
         */
        private void apply() {
            Optional.ofNullable(this.enable())
                    .ifPresent(this.gui()::setRequirement);
            
            Optional.ofNullable(this.display())
                    .ifPresent(this.gui()::setDisplayRequirement);
        }
    }
}

