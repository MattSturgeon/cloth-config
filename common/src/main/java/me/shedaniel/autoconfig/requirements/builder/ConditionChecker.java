package me.shedaniel.autoconfig.requirements.builder;

public interface ConditionChecker<T, C> {
    boolean check(T value, C condition);
}
