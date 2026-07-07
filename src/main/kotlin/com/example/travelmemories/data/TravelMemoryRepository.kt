package com.example.travelmemories.data

interface TravelMemoryRepository {
    fun getAll(): List<TravelMemory>
    fun getById(id: Long): TravelMemory?
    fun save(memory: TravelMemory): Long
    fun delete(id: Long)
}
