package me.shedaniel.autoconfig.gui.registry.api;

import me.shedaniel.autoconfig.gui.registry.GuiLookupTable;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface GuiLookupAccess {
    GuiLookupTable getLookupTable();
    void setLookupTable(GuiLookupTable lookupTable);
}
