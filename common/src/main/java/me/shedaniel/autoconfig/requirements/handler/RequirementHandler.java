package me.shedaniel.autoconfig.requirements.handler;

import me.shedaniel.autoconfig.gui.registry.GuiLookupTable;
import me.shedaniel.autoconfig.requirements.definition.RequirementDefinitionEntry;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface RequirementHandler {
    
    boolean run();
    
    static RequirementHandler from(RequirementDefinitionEntry definition, GuiLookupTable guiLookupTable) {
        return RequirementHandlerFactory.fromDefinition(definition, guiLookupTable);
    }
}
