package me.shedaniel.autoconfig.gui.registry;

import me.shedaniel.autoconfig.requirements.RequirementManager;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface RequirementManagerAccess {
    /**
     * Internal method to access the {@link RequirementManager}.
     */
    RequirementManager getRequirementManager();
    /**
     * Internal method to define the {@link RequirementManager} to be used.
     */
    void setRequirementManager(RequirementManager manager);
}
