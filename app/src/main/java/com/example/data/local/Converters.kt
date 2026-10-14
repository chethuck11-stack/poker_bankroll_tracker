package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.BankrollTransactionType
import com.example.data.model.TournamentStatus

class Converters {
    @TypeConverter
    fun fromTournamentStatus(status: TournamentStatus?): String {
        return status?.name ?: TournamentStatus.COMPLETED.name
    }

    @TypeConverter
    fun toTournamentStatus(value: String?): TournamentStatus {
        return if (value != null) TournamentStatus.fromString(value) else TournamentStatus.COMPLETED
    }

    @TypeConverter
    fun fromBankrollTransactionType(type: BankrollTransactionType?): String {
        return type?.name ?: BankrollTransactionType.MANUAL_CORRECTION.name
    }

    @TypeConverter
    fun toBankrollTransactionType(value: String?): BankrollTransactionType {
        return if (value != null) BankrollTransactionType.fromString(value) else BankrollTransactionType.MANUAL_CORRECTION
    }
}
