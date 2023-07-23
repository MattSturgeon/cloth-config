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

package me.shedaniel.clothconfig2.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public interface ValueHolder<T> {
    /**
     * Get the value held by this Value Holder.
     * 
     * <p>Depending on the implementation, this method may or may not be {@link Nullable}.
     * 
     * @return the current value.
     */
    T getValue();
    
    /**
     * Get the held value's class, as returned by {@link #getValue()}.
     * 
     * @return the {@link Class} describing the held value.
     */
    @ApiStatus.Internal
    @SuppressWarnings("unchecked")
    default Class<T> getType() {
        T value = getValue();
        if (value != null) {
            // FIXME this doesn't work reliably for nullable values
            return (Class<T>) value.getClass();
        }
        
        Class<T> type;
        try {
            // FIXME this doesn't work when the final implementation is still generic.
            //       (Object.class is always returned)
            Method valueMethod = this.getClass().getMethod("getValue");
            type = (Class<T>) valueMethod.getReturnType();
        } catch (NoSuchMethodException | ClassCastException e) {
            throw new IllegalStateException(e);
        }
        return type;
    }
}
