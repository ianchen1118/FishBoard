package com.example.androidtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.androidtest.ui.theme.AndroidTestTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AndroidTestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FishBoardApp()
                }
            }
        }
    }
}

enum class Screen {
    Home,
    SessionSetup,
    RapidScan,
    Records,
    RecordDetail,
    Calibration,
    Export
}

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
    val notes: String?,
    val photoFilename: String,
    val photoRelativePath: String,
    val photoUri: String?
)

@Composable
fun FishBoardApp() {
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var currentSession by remember { mutableStateOf<ScanSession?>(null) }
    var selectedRecordId by remember { mutableStateOf<String?>(null) }
    var nextSessionNumber by remember { mutableStateOf(1) }
    val fishRecords = remember { mutableStateListOf<FishRecord>() }

    when (currentScreen) {
        Screen.Home -> HomeScreen(
            currentSession = currentSession,
            recordCount = currentSession?.let { session ->
                fishRecords.count { it.sessionId == session.sessionId }
            } ?: 0,
            onRapidScanClick = {
                currentScreen = if (currentSession == null) {
                    Screen.SessionSetup
                } else {
                    Screen.RapidScan
                }
            },
            onNewSessionClick = { currentScreen = Screen.SessionSetup },
            onRecordsClick = { currentScreen = Screen.Records },
            onCalibrationClick = { currentScreen = Screen.Calibration },
            onExportClick = { currentScreen = Screen.Export }
        )

        Screen.SessionSetup -> SessionSetupScreen(
            nextSessionNumber = nextSessionNumber,
            onStartSession = { locationCode, deviceCode ->
                currentSession = createScanSession(
                    locationCode = locationCode,
                    deviceCode = deviceCode,
                    sessionNumber = nextSessionNumber
                )
                nextSessionNumber += 1
                currentScreen = Screen.RapidScan
            },
            onBackClick = { currentScreen = Screen.Home }
        )

        Screen.RapidScan -> RapidScanScreen(
            session = currentSession,
            records = currentSession?.let { session ->
                fishRecords.filter { it.sessionId == session.sessionId }
            }.orEmpty(),
            onSimulateScan = {
                currentSession?.let { session ->
                    val nextFishNumber = fishRecords.count { it.sessionId == session.sessionId } + 1
                    fishRecords.add(createFakeFishRecord(session, nextFishNumber))
                }
            },
            onReviewRecordsClick = { currentScreen = Screen.Records },
            onBackClick = { currentScreen = Screen.Home }
        )

        Screen.Records -> RecordsScreen(
            records = fishRecords,
            onRecordClick = { record ->
                selectedRecordId = record.internalId
                currentScreen = Screen.RecordDetail
            },
            onBackClick = { currentScreen = Screen.Home }
        )

        Screen.RecordDetail -> RecordDetailScreen(
            record = fishRecords.firstOrNull { it.internalId == selectedRecordId },
            onSave = { updatedRecord ->
                val index = fishRecords.indexOfFirst { it.internalId == updatedRecord.internalId }
                if (index >= 0) {
                    fishRecords[index] = updatedRecord
                }
                currentScreen = Screen.Records
            },
            onBackClick = { currentScreen = Screen.Records }
        )

        Screen.Calibration -> CalibrationScreen(
            onBackClick = { currentScreen = Screen.Home }
        )

        Screen.Export -> ExportScreen(
            records = fishRecords,
            onBackClick = { currentScreen = Screen.Home }
        )
    }
}

@Composable
fun HomeScreen(
    currentSession: ScanSession?,
    recordCount: Int,
    onRapidScanClick: () -> Unit,
    onNewSessionClick: () -> Unit,
    onRecordsClick: () -> Unit,
    onCalibrationClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "FishBoard",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Rapid fish scanning prototype",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (currentSession == null) {
                "No active session"
            } else {
                "$recordCount records in active session"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        if (currentSession != null) {
            Spacer(modifier = Modifier.height(12.dp))

            InfoCard {
                Text(
                    text = "Active Session",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(currentSession.sessionId)
                Text("Location: ${currentSession.locationCode}")
                Text("Device: ${currentSession.deviceCode}")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        PrimaryMenuButton(
            text = "Start Rapid Scan",
            onClick = onRapidScanClick
        )

        PrimaryMenuButton(
            text = "Start New Session",
            onClick = onNewSessionClick
        )

        PrimaryMenuButton(
            text = "Review Records",
            onClick = onRecordsClick
        )

        PrimaryMenuButton(
            text = "Calibration",
            onClick = onCalibrationClick
        )

        PrimaryMenuButton(
            text = "Export Data",
            onClick = onExportClick
        )
    }
}

@Composable
fun SessionSetupScreen(
    nextSessionNumber: Int,
    onStartSession: (locationCode: String, deviceCode: String) -> Unit,
    onBackClick: () -> Unit
) {
    var locationCode by remember { mutableStateOf("FIELD") }
    var deviceCode by remember { mutableStateOf("D01") }

    ScreenContainer {
        Text(
            text = "New Session",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Session S${nextSessionNumber.toString().padStart(3, '0')} will group the fish scanned in one field run.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = locationCode,
            onValueChange = { locationCode = it.uppercase().filterSessionCode() },
            label = { Text("Location code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            value = deviceCode,
            onValueChange = { deviceCode = it.uppercase().filterSessionCode() },
            label = { Text("Device code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        InfoCard {
            val previewSession = createScanSession(
                locationCode = locationCode,
                deviceCode = deviceCode,
                sessionNumber = nextSessionNumber
            )
            Text(
                text = "Preview ID",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(previewSession.sessionId)
            Text("${previewSession.sessionId}-F000001")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onStartSession(
                    locationCode.ifBlank { "FIELD" },
                    deviceCode.ifBlank { "D01" }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Session")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

@Composable
fun RapidScanScreen(
    session: ScanSession?,
    records: List<FishRecord>,
    onSimulateScan: () -> Unit,
    onReviewRecordsClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val lastRecord = records.lastOrNull()
    val status = if (session == null) {
        "Start a session before scanning."
    } else if (lastRecord == null) {
        "Ready"
    } else {
        "Saved ${lastRecord.displayFishId}. Ready for next fish."
    }

    ScreenContainer {
        Text(
            text = "Rapid Scan",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        InfoCard {
            Text(
                text = "Active Session",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(session?.sessionId ?: "No active session")
        }

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard {
            Text(
                text = "Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard {
            Text(
                text = "Last Record",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (lastRecord == null) {
                Text(
                    text = "No fish scanned yet.",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                RecordSummary(record = lastRecord)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSimulateScan,
            enabled = session != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Simulate Fish Scan")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onReviewRecordsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Review Records")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

@Composable
fun RecordsScreen(
    records: List<FishRecord>,
    onRecordClick: (FishRecord) -> Unit,
    onBackClick: () -> Unit
) {
    ScreenContainer {
        Text(
            text = "Review Records",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${records.size} total records",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (records.isEmpty()) {
            InfoCard {
                Text(
                    text = "No records yet. Start Rapid Scan and simulate a fish scan first.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            records.asReversed().forEach { record ->
                InfoCard {
                    RecordSummary(record = record)

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { onRecordClick(record) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Record")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

@Composable
fun RecordDetailScreen(
    record: FishRecord?,
    onSave: (FishRecord) -> Unit,
    onBackClick: () -> Unit
) {
    if (record == null) {
        PlaceholderWorkflowScreen(
            title = "Record Not Found",
            body = "The selected record is no longer available.",
            detail = "Go back to Review Records and choose another record.",
            onBackClick = onBackClick
        )
        return
    }

    var speciesText by remember(record.internalId) {
        mutableStateOf(record.correctedSpecies ?: record.species)
    }
    var lengthText by remember(record.internalId) {
        mutableStateOf((record.correctedLengthMm ?: record.lengthMm).toString())
    }
    var notesText by remember(record.internalId) {
        mutableStateOf(record.notes.orEmpty())
    }

    ScreenContainer {
        Text(
            text = "Record Detail",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard {
            Text(
                text = record.displayFishId,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("Session: ${record.sessionId}")
            Text("Captured: ${record.formattedTimestamp()}")
            Text("Internal ID: ${record.internalId}")
            Text("Photo: ${record.photoRelativePath}")
        }

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard {
            Text(
                text = "AI / Placeholder Prediction",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("Predicted species: ${record.species}${record.speciesConfidence.asPercentSuffix()}")
            Text("Predicted length: ${record.lengthMm} mm${record.lengthConfidence.asPercentSuffix()}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = speciesText,
            onValueChange = { speciesText = it },
            label = { Text("Corrected species") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            value = lengthText,
            onValueChange = { lengthText = it.filter { char -> char.isDigit() } },
            label = { Text("Corrected length in mm") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            value = notesText,
            onValueChange = { notesText = it },
            label = { Text("Notes") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onSave(
                    record.copy(
                        correctedSpecies = speciesText.ifBlank { null },
                        correctedLengthMm = lengthText.toIntOrNull(),
                        reviewed = true,
                        notes = notesText.ifBlank { null }
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Review")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

@Composable
fun CalibrationScreen(
    onBackClick: () -> Unit
) {
    PlaceholderWorkflowScreen(
        title = "Calibration",
        body = "This will set the pixel-to-millimeter ratio for length measurement.",
        detail = "First version: placeholder only. Next versions can add manual mm-per-pixel input or head/tail reference point selection.",
        onBackClick = onBackClick
    )
}

@Composable
fun ExportScreen(
    records: List<FishRecord>,
    onBackClick: () -> Unit
) {
    val reviewedCount = records.count { it.reviewed }
    val csvPreview = remember(records) {
        records.toFishBoardCsv()
    }

    ScreenContainer {
        Text(
            text = "Export Data",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard {
            Text(
                text = "Export Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("${records.size} total records")
            Text("$reviewedCount reviewed records")
            Text("${records.size - reviewedCount} records still need review")
        }

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard {
            Text(
                text = "CSV Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (records.isEmpty()) {
                Text("No records yet. Scan fish before exporting.")
            } else {
                Text(
                    text = csvPreview,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard {
            Text(
                text = "Package Layout Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = records.toExportPackagePreview(),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard {
            Text(
                text = "Next Export Step",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("Future versions will write this CSV and matching images into a ZIP package.")
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

@Composable
fun PlaceholderWorkflowScreen(
    title: String,
    body: String,
    detail: String,
    onBackClick: () -> Unit
) {
    ScreenContainer {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        InfoCard {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

@Composable
fun RecordSummary(record: FishRecord) {
    Text(
        text = record.displayFishId,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text("Species: ${record.species}${record.speciesConfidence.asPercentSuffix()}")
    Text("Length: ${record.lengthMm} mm${record.lengthConfidence.asPercentSuffix()}")
    if (record.correctedSpecies != null || record.correctedLengthMm != null) {
        Text("Corrected species: ${record.correctedSpecies ?: record.species}")
        Text("Corrected length: ${record.correctedLengthMm ?: record.lengthMm} mm")
    }
    Text("Session: ${record.sessionId}")
    Text("Fish number: ${record.fishNumber}")
    Text("Photo: ${record.photoFilename}")
    Text("Captured: ${record.formattedTimestamp()}")
    Text("Reviewed: ${if (record.reviewed) "Yes" else "No"}")
}

@Composable
fun PrimaryMenuButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Text(text)
    }
}

@Composable
fun ScreenContainer(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.Start,
        content = content
    )
}

@Composable
fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

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
        notes = null,
        photoFilename = photoFilename,
        photoRelativePath = photoRelativePath,
        photoUri = null
    )
}

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
