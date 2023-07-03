package me.shedaniel.clothconfig2.api;

import org.jetbrains.annotations.Nullable;

public interface ValueHolder<T> {
    @Nullable T getValue();
}
