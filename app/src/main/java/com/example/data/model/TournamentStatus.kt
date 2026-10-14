package com.example.data.model

enum class TournamentStatus(val displayName: String) {
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    REFUNDED("Refunded");

    companion object {
        fun fromString(value: String): TournamentStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: COMPLETED
        }
    }
}
