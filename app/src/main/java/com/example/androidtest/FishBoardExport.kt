package com.example.androidtest

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun FishRecord.formattedTimestamp(): String {
    return timestampMillis.formatTimestamp()
}

fun Long.formatDateCode(): String {
    return SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(this))
}

fun Long.formatTimestamp(): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(this))
}

fun Long.formatExportDate(): String {
    return SimpleDateFormat("yyyy_MM_dd", Locale.US).format(Date(this))
}

fun Long.formatExportDateTime(): String {
    return SimpleDateFormat("yyyy_MM_dd_HHmmss", Locale.US).format(Date(this))
}

fun List<FishRecord>.toFishBoardCsv(): String {
    val header = listOf(
        "internalId",
        "sessionId",
        "displayFishId",
        "fishNumber",
        "timestamp",
        "predictedSpecies",
        "speciesConfidence",
        "correctedSpecies",
        "predictedLengthMm",
        "lengthConfidence",
        "correctedLengthMm",
        "reviewed",
        "exportedAt",
        "photoFilename",
        "photoRelativePath",
        "notes"
    )

    val rows = map { record ->
        listOf(
            record.internalId,
            record.sessionId,
            record.displayFishId,
            record.fishNumber.toString(),
            record.formattedTimestamp(),
            record.species,
            record.speciesConfidence.asCsvNumber(),
            record.correctedSpecies.orEmpty(),
            record.lengthMm.toString(),
            record.lengthConfidence.asCsvNumber(),
            record.correctedLengthMm?.toString().orEmpty(),
            record.reviewed.toString(),
            record.exportedAtMillis?.formatTimestamp().orEmpty(),
            record.photoFilename,
            record.photoRelativePath,
            record.notes.orEmpty()
        )
    }

    return (listOf(header) + rows)
        .joinToString(separator = "\n") { row ->
            row.joinToString(separator = ",") { value -> value.toCsvCell() }
        }
}

fun List<FishRecord>.toExportPackagePreview(): String {
    if (isEmpty()) {
        return "FishBoardExport_YYYY_MM_DD.zip\n|-- records.csv\n|-- images/"
    }

    val imageLines = groupBy { it.sessionId }.flatMap { (sessionId, sessionRecords) ->
        listOf("|   |-- $sessionId/") + sessionRecords.map { record ->
            "|       |-- ${record.photoFilename}"
        }
    }

    return (listOf(
        "FishBoardExport_${first().timestampMillis.formatExportDate()}.zip",
        "|-- records.csv",
        "|-- images/"
    ) + imageLines).joinToString("\n")
}

fun Context.exportFishBoardRecords(records: List<FishRecord>): ExportPackageResult {
    val documentsDirectory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: filesDir
    val exportDirectory = File(documentsDirectory, "exports")
    val exportFile = File(
        exportDirectory,
        "FishBoardExport_${System.currentTimeMillis().formatExportDateTime()}.zip"
    )

    return writeFishBoardExportZip(
        records = records,
        outputFile = exportFile
    )
}

fun Context.shareExportPackage(file: File) {
    val uri = FileProvider.getUriForFile(
        this,
        "${packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(intent, "Share FishBoard Export"))
}

fun writeFishBoardExportZip(
    records: List<FishRecord>,
    outputFile: File
): ExportPackageResult {
    outputFile.parentFile?.mkdirs()

    var imageCount = 0
    val missingImageRecords = mutableListOf<FishRecord>()

    ZipOutputStream(FileOutputStream(outputFile)).use { zip ->
        zip.writeTextEntry(
            entryName = "records.csv",
            text = records.toFishBoardCsv()
        )

        records.forEach { record ->
            val imageFile = record.photoUri?.let { File(it) }

            if (imageFile != null && imageFile.isFile) {
                zip.writeFileEntry(
                    entryName = record.photoRelativePath,
                    file = imageFile
                )
                imageCount += 1
            } else {
                missingImageRecords.add(record)
            }
        }

        if (missingImageRecords.isNotEmpty()) {
            zip.writeTextEntry(
                entryName = "missing_images.csv",
                text = missingImageRecords.toMissingImagesCsv()
            )
        }
    }

    return ExportPackageResult(
        file = outputFile,
        recordCount = records.size,
        imageCount = imageCount,
        missingImageCount = missingImageRecords.size
    )
}

fun List<FishRecord>.toMissingImagesCsv(): String {
    val header = listOf(
        "displayFishId",
        "photoFilename",
        "photoRelativePath",
        "reason"
    )
    val rows = map { record ->
        listOf(
            record.displayFishId,
            record.photoFilename,
            record.photoRelativePath,
            "Photo file not available in this prototype"
        )
    }

    return (listOf(header) + rows)
        .joinToString(separator = "\n") { row ->
            row.joinToString(separator = ",") { value -> value.toCsvCell() }
        }
}

fun ZipOutputStream.writeTextEntry(
    entryName: String,
    text: String
) {
    putNextEntry(ZipEntry(entryName))
    write(text.toByteArray(Charsets.UTF_8))
    closeEntry()
}

fun ZipOutputStream.writeFileEntry(
    entryName: String,
    file: File
) {
    putNextEntry(ZipEntry(entryName))
    FileInputStream(file).use { input ->
        input.copyTo(this)
    }
    closeEntry()
}

fun Double?.asPercentSuffix(): String {
    return this?.let { " (${(it * 100).toInt()}%)" } ?: ""
}

fun Double?.asCsvNumber(): String {
    return this?.toString().orEmpty()
}

fun String.toCsvCell(): String {
    val escaped = replace("\"", "\"\"")
    val needsQuotes = any { it == ',' || it == '"' || it == '\n' || it == '\r' }
    return if (needsQuotes) {
        "\"$escaped\""
    } else {
        escaped
    }
}

fun String.filterSessionCode(): String {
    return filter { it.isLetterOrDigit() }.take(12)
}
