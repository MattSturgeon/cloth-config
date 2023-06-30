package me.shedaniel.clothconfig2.api;

import org.jetbrains.annotations.Nullable;

public interface NullableValueHolder<T> {
    @Nullable T getValue();
}
