package io.github.audipz.datachange.framework.model;

/**
 * Defines transaction boundaries for ChangeSet execution.
 */
public enum TransactionMode {
    CHANGESET,
    PER_CHANGE,
    CUSTOM
}

