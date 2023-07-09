package me.shedaniel.autoconfig.annotation;

import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declare the annotated method to be a custom requirement handler.
 *
 * <p>The requirement can be depended-on by config entries to allow more complex dependency logic to be used.
 */
@ApiStatus.Experimental
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequirementHandler {
    
    /**
     * One or more references to {@link ConfigEntry.Gui Config Entrie GUIs}.
     * The current value of each Config Entry will be passed to the custom handler as arguments.
     */
    String[] value() default {};
}
