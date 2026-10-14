package com.example.data.model

enum class TournamentFormat(val displayName: String) {
    MTT("MTT"),
    SNG("SNG"),
    PKO("PKO / Bounty"),
    SATELLITE("Satellite"),
    OTHER("Other");

    companion object {
        fun fromString(value: String): TournamentFormat {
            return entries.firstOrNull { 
                it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) 
            } ?: MTT
        }
    }
}
