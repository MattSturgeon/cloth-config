package me.shedaniel.clothconfig2.api;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public interface DisableableWidget {
    
    /**
     * Checks whether this config entry gui is enabled.
     * 
     * <p>Requirements are checked independently (once per tick). This method simply reads the result of the latest
     * check, making it extremely cheap to run.
     * 
     * <p>If {@link HideableWidget#isDisplayed()} is false, this will also be false.
     * 
     * @return whether the config entry is enabled
     * @see HideableWidget#isDisplayed()
     * @see TickableWidget#tick()
     */
    @ApiStatus.Experimental
    boolean isEnabled();
    
    @ApiStatus.Experimental
    void setRequirement(Requirement requirement);
    
    @ApiStatus.Experimental
    Requirement getRequirement();
    
}
