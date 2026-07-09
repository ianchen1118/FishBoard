package com.example.androidtest

import java.io.File

data class ScanSession(
    val sessionId: String,
    val dateCode: String,
    val locationCode: String,
    val deviceCode: String,
    val sessionNumber: Int,
    val startedAtMillis: Long
)

data class FishRecord(
    val internalId: String,
    val sessionId: String,
    val fishNumber: Int,
    val displayFishId: String,
    val timestampMillis: Long,
    val species: String,
    val speciesConfidence: Double?,
    val lengthMm: Int,
    val lengthConfidence: Double?,
    val correctedSpecies: String?,
    val correctedLengthMm: Int?,
    val reviewed: Boolean,
    val exportedAtMillis: Long?,
    val notes: String?,
    val photoFilename: String,
    val photoRelativePath: String,
    val photoUri: String?
)

data class ExportPackageResult(
    val file: File,
    val recordCount: Int,
    val imageCount: Int,
    val missingImageCount: Int
)

enum class Screen {
    Home,
    SessionSetup,
    RapidScan,
    Records,
    RecordDetail,
    Calibration,
    Export
}
