package com.example.travelmemories.data

data class TravelMemory(
    val id: Long = 0,
    val type: MemoryType,
    val title: String,
    val country: String,
    val visitDate: String,
    val rating: Int,
    val note: String,
    val photoUri: String? = null,
)
