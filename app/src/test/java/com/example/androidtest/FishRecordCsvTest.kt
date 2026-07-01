package com.example.androidtest

import org.junit.Assert.assertTrue
import org.junit.Test

class FishRecordCsvTest {
    @Test
    fun csvExportIncludesReviewCorrectionsAndEscapesNotes() {
        val session = createScanSession(
            locationCode = "PIER01",
            deviceCode = "D03",
            sessionNumber = 1,
            startedAtMillis = 1782921600000L
        )
        val record = createFakeFishRecord(session, 1).copy(
            correctedSpecies = "Bluefish",
            correctedLengthMm = 241,
            reviewed = true,
            notes = "Needs review, has \"mark\""
        )

        val csv = listOf(record).toFishBoardCsv()

        assertTrue(csv.contains("correctedSpecies"))
        assertTrue(csv.contains("Bluefish"))
        assertTrue(csv.contains("241"))
        assertTrue(csv.contains("\"Needs review, has \"\"mark\"\"\""))
    }
}
