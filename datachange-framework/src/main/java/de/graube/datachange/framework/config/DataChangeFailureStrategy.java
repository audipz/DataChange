package de.graube.datachange.framework.config;

/**
 * Defines how startup execution reacts to one failing ChangeSet.
 */
public enum DataChangeFailureStrategy {
    FAIL_FAST,
    CONTINUE
}

