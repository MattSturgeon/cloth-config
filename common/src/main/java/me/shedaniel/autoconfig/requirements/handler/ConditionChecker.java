package me.shedaniel.autoconfig.requirements.handler;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@FunctionalInterface
public interface ConditionChecker<T, C> {
    boolean check(T value, C condition);
}
