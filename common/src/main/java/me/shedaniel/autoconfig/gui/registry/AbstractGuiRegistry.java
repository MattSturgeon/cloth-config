package me.shedaniel.autoconfig.gui.registry;

import me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess;

public abstract class AbstractGuiRegistry implements GuiRegistryAccess {
    private GuiLookupTable lookup;
    
    protected AbstractGuiRegistry() {
        lookup = new GuiLookupTable();
    }
    
    @Override
    public GuiLookupTable getLookupTable() {
        return lookup;
    }
    
    @Override
    public void setLookupTable(GuiLookupTable lookupTable) {
        this.lookup = lookupTable;
    }
}
