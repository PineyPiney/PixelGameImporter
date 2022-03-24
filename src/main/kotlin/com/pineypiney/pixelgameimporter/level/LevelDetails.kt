package com.pineypiney.pixelgameimporter.level

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class LevelDetails(val worldName: String, val fileName: String, val width: Float, val created: LocalDateTime, val edited: LocalDateTime) {

    companion object{
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy")
    }
}