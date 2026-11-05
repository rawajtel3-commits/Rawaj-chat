package com.example.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromCabinType(type: CabinType?): String = type?.name ?: CabinType.WORK.name

    @TypeConverter
    fun toCabinType(value: String?): CabinType = try {
        if (value.isNullOrBlank()) CabinType.WORK else CabinType.valueOf(value)
    } catch (e: Exception) {
        CabinType.WORK
    }

    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority?): String = priority?.name ?: TaskPriority.MEDIUM.name

    @TypeConverter
    fun toTaskPriority(value: String?): TaskPriority = try {
        if (value.isNullOrBlank()) TaskPriority.MEDIUM else TaskPriority.valueOf(value)
    } catch (e: Exception) {
        TaskPriority.MEDIUM
    }

    @TypeConverter
    fun fromTaskStatus(status: TaskStatus?): String = status?.name ?: TaskStatus.TODO.name

    @TypeConverter
    fun toTaskStatus(value: String?): TaskStatus = try {
        if (value.isNullOrBlank()) TaskStatus.TODO else TaskStatus.valueOf(value)
    } catch (e: Exception) {
        TaskStatus.TODO
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String = list?.joinToString(",") ?: ""

    @TypeConverter
    fun toStringList(value: String?): List<String> = if (value.isNullOrBlank()) {
        emptyList()
    } else {
        value.split(",")
    }

    @TypeConverter
    fun fromOutboxType(type: OutboxType?): String = type?.name ?: OutboxType.MESSAGE.name

    @TypeConverter
    fun toOutboxType(value: String?): OutboxType = try {
        if (value.isNullOrBlank()) OutboxType.MESSAGE else OutboxType.valueOf(value)
    } catch (e: Exception) {
        OutboxType.MESSAGE
    }

    @TypeConverter
    fun fromOutboxStatus(status: OutboxStatus?): String = status?.name ?: OutboxStatus.PENDING.name

    @TypeConverter
    fun toOutboxStatus(value: String?): OutboxStatus = try {
        if (value.isNullOrBlank()) OutboxStatus.PENDING else OutboxStatus.valueOf(value)
    } catch (e: Exception) {
        OutboxStatus.PENDING
    }
}


