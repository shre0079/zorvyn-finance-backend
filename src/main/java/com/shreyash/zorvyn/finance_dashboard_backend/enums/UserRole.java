package com.shreyash.zorvyn.finance_dashboard_backend.enums;

//VIEWER   – read-only access to own transactions and own dashboard.
//ANALYST  – can create, update, and soft-delete own transactions.
//ADMIN    – full access: manage users, view/edit/delete any transaction

public enum UserRole {
    VIEWER,
    ANALYST,
    ADMIN
}
