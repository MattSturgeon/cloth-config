/*
 * This file is part of Cloth Config.
 * Copyright (C) 2020 - 2021 shedaniel
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package me.shedaniel.autoconfig.annotation;

import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.*;

public class ConfigEntry {
    
    private ConfigEntry() {
    }
    
    /**
     * Sets the category name of the config entry. Categories are created in order.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Category {
        String value();
    }
    
    /**
     * Applies to int and long fields.
     * Sets the GUI to a slider.
     * In a future version it will enforce bounds at deserialization.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface BoundedDiscrete {
        long min() default 0;
        
        long max();
    }
    
    /**
     * Applies to int fields.
     * Sets the GUI to a color picker.
     * In a future version it will enforce bounds at deserialization.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ColorPicker {
        boolean allowAlpha() default false;
    }

//    /**
//     * Applies to float and double fields.
//     * In a future version it will enforce bounds at deserialization.
//     */
//    @Retention(RetentionPolicy.RUNTIME)
//    @Target(ElementType.FIELD)
//    public @interface BoundedFloating {
//        double min() default 0;
//
//        double max();
//    }
    
    public static class Gui {
        private Gui() {
        }
        
        /**
         * Removes the field from the config GUI.
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        public @interface Excluded {
        }
        
        /**
         * Applies to objects.
         * Adds GUI entries for the field's inner fields at the same level as this field.
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        public @interface TransitiveObject {
        }
        
        /**
         * Applies to objects.
         * Adds GUI entries for the field's inner fields in a collapsible section.
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        public @interface CollapsibleObject {
            boolean startExpanded() default false;
        }
        
        /**
         * Applies a tooltip to list entries that support it, defined in your lang file.
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        public @interface Tooltip {
            int count() default 1;
        }
        
        /**
         * Applies no tooltip to list entries that support it, defined in your lang file.
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        public @interface NoTooltip {
            
        }
        
        /**
         * Applies a section of text right before this entry, defined in your lang file.
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        public @interface PrefixText {
        }
        
        /**
         * Requires restart if the field is modified.
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        public @interface RequiresRestart {
            boolean value() default true;
        }
        
        /**
         * Defines how an enum is handled
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        public @interface EnumHandler {
            EnumDisplayOption option() default EnumDisplayOption.DROPDOWN;
            
            enum EnumDisplayOption {
                DROPDOWN,
                BUTTON
            }
        }
    }
    
    @ApiStatus.Experimental
    public static class Requirement {
        private Requirement() {}
        
        /**
         * This Config Entry is enabled only when this requirement is met.
         *
         * <p>{@link #value()} contains a list of references, usually to other {@link ConfigEntry.Gui Config Entries}, whose
         * values are compared against {@link #condition()} and {@link #regexCondition()}.
         * 
         * <p>If no conditions are provided, {@code boolean} Config Entries will assume the required condition is {@code true},
         * while non-{@code boolean} Config Entries will throw an {@link IllegalArgumentException}.
         *
         * <p>Additionally, {@link #value()} can contain references to {@link RequirementHandler Custom Requirements}, which are
         * evaluated without considering any conditions defined here.
         *
         * <p>This requirement is considered met if any Custom Requirement evaluates to {@code true} <strong>or</strong>
         * any Config Entry requirement matches any of the conditions listed.
         * 
         * @see DisplayIf
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        @Repeatable(EnableIfGroup.class)
        public @interface EnableIf {
            /**
             * One or more references to {@link ConfigEntry.Gui Config Entries} or {@link RequirementHandler Custom Requirements}.
             */
            String[] value();
            
            /**
             * Zero or more strings that satisfy this condition.
             * 
             * <p>Ignored by {@link RequirementHandler Custom Requirements}
             */
            String[] condition() default {};
            
            /**
             * Zero or more {@link java.util.regex.Pattern Regular Expressions} that satisfy this condition.
             * 
             * <p>Will be compiled using {@link java.util.regex.Pattern#compile(String) Pattern.compile()} and a
             * {@link java.util.regex.PatternSyntaxException PatternSyntaxException} thrown if the RegEx is invalid.
             * 
             * <p>Ignored by {@link RequirementHandler Custom Requirements}
             */
            String[] regexCondition() default {};
        }
        
        /**
         * This Config Entry is displayed in the GUI only when this requirement is met.
         * 
         * @see EnableIf
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        @Repeatable(DisplayIfGroup.class)
        public @interface DisplayIf {
            /**
             * @see EnableIf#value() 
             */
            String[] value();
            
            /**
             * @see EnableIf#condition() 
             */
            String[] condition() default {};
            
            /**
             * @see EnableIf#regexCondition() 
             */
            String[] regexCondition() default {};
        }
        
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        public @interface EnableIfGroup {
            
            EnableIf[] value();
            
            Quantity requirement() default Quantity.ALL;
        }
        
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        public @interface DisplayIfGroup {
            
            DisplayIf[] value();
            
            Quantity requirement() default Quantity.ALL;
        }
        
        public enum Quantity {
            ALL,
            ANY,
            NONE,
            ONE
        }
    }
}
