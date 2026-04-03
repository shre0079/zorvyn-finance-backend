package com.shreyash.zorvyn.finance_dashboard_backend.dtos;


/**
 * Payload for POST /api/transactions.
 * Validation rules (per specification):
 *  - amount  : positive, non-null, max 13 digits before the decimal point.
 *  - type    : must be a valid TransactionType enum value.
 *  - category: must be a valid TransactionCategory enum value.
 *  - date    : valid, not in the future (future-date check handled in service).
 *  - description: optional, max 500 characters.
 */


public class CreateTransactionRequest {
}
