package me.shedaniel.autoconfig.gui.registry;

import me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess;
import me.shedaniel.autoconfig.requirements.RequirementManager;

public abstract class AbstractGuiRegistry implements GuiRegistryAccess {
    private RequirementManager manager;
    private GuiLookupTable lookup;
    
    protected AbstractGuiRegistry() {
        lookup = new GuiLookupTable();
        manager = new RequirementManager(lookup);
    }
    
    @Override
    public GuiLookupTable getLookupTable() {
        return lookup;
    }
    @Override
    public void setLookupTable(GuiLookupTable lookupTable) {
        this.lookup = lookupTable;
    }
    
    @Override
    public RequirementManager getRequirementManager() {
        return this.manager;
    }
    
    @Override
    public void setRequirementManager(RequirementManager manager) {
        this.manager = manager;
    }
    
}
