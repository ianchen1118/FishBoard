package com.example.androidtest

import java.util.UUID

fun createScanSession(
    locationCode: String,
    deviceCode: String,
    sessionNumber: Int,
    startedAtMillis: Long = System.currentTimeMillis()
): ScanSession {
    val dateCode = startedAtMillis.formatDateCode()
    val cleanLocationCode = locationCode.ifBlank { "FIELD" }.uppercase().filterSessionCode()
    val cleanDeviceCode = deviceCode.ifBlank { "D01" }.uppercase().filterSessionCode()
    val sessionPart = "S${sessionNumber.toString().padStart(3, '0')}"

    return ScanSession(
        sessionId = "$dateCode-$cleanLocationCode-$cleanDeviceCode-$sessionPart",
        dateCode = dateCode,
        locationCode = cleanLocationCode,
        deviceCode = cleanDeviceCode,
        sessionNumber = sessionNumber,
        startedAtMillis = startedAtMillis
    )
}

fun createFakeFishRecord(
    session: ScanSession,
    fishNumber: Int
): FishRecord {
    val fakeSpecies = listOf("Unknown", "Bluefish", "Striped Bass", "Sea Bass")
    val species = fakeSpecies[(fishNumber - 1) % fakeSpecies.size]
    val confidence = if (species == "Unknown") null else 0.70 + (fishNumber % 4) * 0.06
    val fishPart = "F${fishNumber.toString().padStart(6, '0')}"
    val photoFilename = "${session.sessionId}-$fishPart.jpg"
    val photoRelativePath = "images/${session.sessionId}/$photoFilename"

    return FishRecord(
        internalId = UUID.randomUUID().toString(),
        sessionId = session.sessionId,
        fishNumber = fishNumber,
        displayFishId = "${session.sessionId}-$fishPart",
        timestampMillis = System.currentTimeMillis(),
        species = species,
        speciesConfidence = confidence,
        lengthMm = 180 + (fishNumber * 17) % 220,
        lengthConfidence = 0.80 + (fishNumber % 3) * 0.04,
        correctedSpecies = null,
        correctedLengthMm = null,
        reviewed = false,
        exportedAtMillis = null,
        notes = null,
        photoFilename = photoFilename,
        photoRelativePath = photoRelativePath,
        photoUri = null
    )
}
