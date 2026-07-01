package com.example.androidtest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipFile

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
        assertTrue(csv.contains("photoFilename"))
        assertTrue(csv.contains("photoRelativePath"))
        assertTrue(csv.contains(record.photoFilename))
        assertTrue(csv.contains(record.photoRelativePath))
        assertTrue(csv.contains("\"Needs review, has \"\"mark\"\"\""))
    }

    @Test
    fun exportPackagePreviewGroupsImagesBySession() {
        val session = createScanSession(
            locationCode = "PIER01",
            deviceCode = "D03",
            sessionNumber = 1,
            startedAtMillis = 1782921600000L
        )
        val records = listOf(
            createFakeFishRecord(session, 1),
            createFakeFishRecord(session, 2)
        )

        val preview = records.toExportPackagePreview()

        assertTrue(preview.contains("records.csv"))
        assertTrue(preview.contains("images/"))
        assertTrue(preview.contains(session.sessionId))
        assertTrue(preview.contains(records[0].photoFilename))
        assertTrue(preview.contains(records[1].photoFilename))
    }

    @Test
    fun exportZipIncludesCsvAndAvailablePhotos() {
        val tempDir = Files.createTempDirectory("fishboard-export-test").toFile()
        val imageFile = File(tempDir, "source-image.jpg")
        val exportFile = File(tempDir, "export.zip")

        try {
            imageFile.writeBytes(byteArrayOf(1, 2, 3, 4))

            val session = createScanSession(
                locationCode = "PIER01",
                deviceCode = "D03",
                sessionNumber = 1,
                startedAtMillis = 1782921600000L
            )
            val record = createFakeFishRecord(session, 1).copy(
                photoUri = imageFile.absolutePath
            )

            val result = writeFishBoardExportZip(
                records = listOf(record),
                outputFile = exportFile
            )

            assertEquals(1, result.recordCount)
            assertEquals(1, result.imageCount)
            assertEquals(0, result.missingImageCount)

            ZipFile(exportFile).use { zip ->
                assertNotNull(zip.getEntry("records.csv"))
                assertNotNull(zip.getEntry(record.photoRelativePath))
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun exportZipIncludesMissingImageManifestWhenPhotosAreUnavailable() {
        val tempDir = Files.createTempDirectory("fishboard-export-test").toFile()
        val exportFile = File(tempDir, "export.zip")

        try {
            val session = createScanSession(
                locationCode = "PIER01",
                deviceCode = "D03",
                sessionNumber = 1,
                startedAtMillis = 1782921600000L
            )
            val record = createFakeFishRecord(session, 1)

            val result = writeFishBoardExportZip(
                records = listOf(record),
                outputFile = exportFile
            )

            assertEquals(1, result.recordCount)
            assertEquals(0, result.imageCount)
            assertEquals(1, result.missingImageCount)

            ZipFile(exportFile).use { zip ->
                assertNotNull(zip.getEntry("records.csv"))
                assertNotNull(zip.getEntry("missing_images.csv"))
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
