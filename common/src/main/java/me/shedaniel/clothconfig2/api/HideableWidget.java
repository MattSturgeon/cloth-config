package me.shedaniel.clothconfig2.api;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public interface HideableWidget {
    
    /**
     * Checks whether this config entry gui is shown on screen.
     * 
     * <p>Requirements are checked independently (once per tick). This method simply reads the result of the latest
     * check, making it extremely cheap to run.
     * 
     * @return whether to display the config entry
     * @see DisableableWidget#isEnabled()
     * @see TickableWidget#tick()
     */
    @ApiStatus.Experimental
    boolean isDisplayed();
    
    @ApiStatus.Experimental
    void setDisplayRequirement(Requirement requirement);
    
    @ApiStatus.Experimental
    Requirement getDisplayRequirement();
}
