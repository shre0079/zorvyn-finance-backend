package com.shreyash.zorvyn.finance_dashboard_backend.enums;

/**
 * Whether money is flowing in (INCOME) or out (EXPENSE).
 * Case-insensitive parsing is handled in request DTOs via
 * a custom Jackson deserializer / @JsonCreator.
 */

public enum TransactionType {
    INCOME,
    EXPENSE
}
