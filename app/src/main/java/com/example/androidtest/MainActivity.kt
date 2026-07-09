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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.androidtest.ui.theme.AndroidTestTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = FishBoardRepository(
            FishBoardDatabase.getDatabase(this).fishBoardDao()
        )

        setContent {
            AndroidTestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FishBoardApp(repository = repository)
                }
            }
        }
    }
}

@Composable
fun FishBoardApp(repository: FishBoardRepository) {
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var currentSession by remember { mutableStateOf<ScanSession?>(null) }
    var selectedRecordId by remember { mutableStateOf<String?>(null) }
    var sessions by remember { mutableStateOf<List<ScanSession>>(emptyList()) }
    var fishRecords by remember { mutableStateOf<List<FishRecord>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val nextSessionNumber = (sessions.maxOfOrNull { it.sessionNumber } ?: 0) + 1

    LaunchedEffect(repository) {
        repository.sessions.collect { sessions = it }
    }

    LaunchedEffect(repository) {
        repository.records.collect { fishRecords = it }
    }

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
                val session = createScanSession(
                    locationCode = locationCode,
                    deviceCode = deviceCode,
                    sessionNumber = nextSessionNumber
                )
                currentSession = session
                coroutineScope.launch {
                    repository.saveSession(session)
                }
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
                    val record = createFakeFishRecord(session, nextFishNumber)
                    coroutineScope.launch {
                        repository.addRecord(record)
                    }
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
                coroutineScope.launch {
                    repository.updateRecord(updatedRecord)
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
            onMarkRecordsExported = { exportedRecords, exportedAtMillis ->
                coroutineScope.launch {
                    repository.markRecordsExported(exportedRecords, exportedAtMillis)
                }
            },
            onDeleteExportedRecords = {
                coroutineScope.launch {
                    repository.deleteExportedRecords()
                }
            },
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
    var showTechnicalDetails by remember(record.internalId) {
        mutableStateOf(false)
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
            Text("Photo: Linked")
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

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { showTechnicalDetails = !showTechnicalDetails },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (showTechnicalDetails) "Hide Technical Details" else "Show Technical Details")
        }

        if (showTechnicalDetails) {
            Spacer(modifier = Modifier.height(12.dp))

            InfoCard {
                Text(
                    text = "Technical Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("Internal ID: ${record.internalId}")
                Text("Photo filename: ${record.photoFilename}")
                Text("Photo relative path: ${record.photoRelativePath}")
            }
        }

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
    onMarkRecordsExported: (List<FishRecord>, Long) -> Unit,
    onDeleteExportedRecords: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val newRecordCount = records.count { it.exportedAtMillis == null }
    val exportedRecordCount = records.count { it.exportedAtMillis != null }
    var exportOnlyNewRecords by remember { mutableStateOf(true) }
    var showTechnicalPreview by remember { mutableStateOf(false) }
    var confirmDeleteExported by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf<ExportPackageResult?>(null) }
    var exportError by remember { mutableStateOf<String?>(null) }
    val recordsToExport = if (exportOnlyNewRecords) {
        records.filter { it.exportedAtMillis == null }
    } else {
        records
    }
    val reviewedCount = recordsToExport.count { it.reviewed }
    val csvPreview = remember(recordsToExport) {
        recordsToExport.toFishBoardCsv()
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
            Text("$newRecordCount new records")
            Text("$exportedRecordCount already exported")
        }

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard {
            Text(
                text = "Export Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(if (exportOnlyNewRecords) "Exporting new records only" else "Exporting all records")

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { exportOnlyNewRecords = !exportOnlyNewRecords },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (exportOnlyNewRecords) "Switch to Export All Records" else "Switch to New Records Only")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard {
            Text(
                text = "Export Package",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (recordsToExport.isEmpty()) {
                Text("No records are ready for this export.")
            } else {
                Text("Ready to package records and linked fish photos.")
                Text("${recordsToExport.size} CSV rows")
                Text("${recordsToExport.size} linked photo files")
                Text("$reviewedCount reviewed records")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val result = runCatching {
                    context.exportFishBoardRecords(recordsToExport)
                }
                exportResult = result.getOrNull()
                exportError = result.exceptionOrNull()?.message
                if (result.isSuccess) {
                    onMarkRecordsExported(recordsToExport, System.currentTimeMillis())
                }
            },
            enabled = recordsToExport.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Export Package")
        }

        if (exportResult != null || exportError != null) {
            Spacer(modifier = Modifier.height(12.dp))

            InfoCard {
                Text(
                    text = if (exportResult != null) "Export Created" else "Export Failed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val result = exportResult
                if (result != null) {
                    Text(result.file.name)
                    Text("${result.recordCount} records exported")
                    Text("${result.imageCount} photos included")
                    if (result.missingImageCount > 0) {
                        Text("${result.missingImageCount} photos not available yet")
                    }
                } else {
                    Text(exportError ?: "Unknown export error")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val result = exportResult
        if (result != null) {
            Button(
                onClick = { context.shareExportPackage(result.file) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Share Export Package")
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        InfoCard {
            Text(
                text = "Data Management",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("$exportedRecordCount records marked exported")

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    if (confirmDeleteExported) {
                        onDeleteExportedRecords()
                        confirmDeleteExported = false
                        exportResult = null
                        exportError = null
                    } else {
                        confirmDeleteExported = true
                    }
                },
                enabled = exportedRecordCount > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (confirmDeleteExported) "Confirm Delete Exported Records" else "Delete Exported Records")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { showTechnicalPreview = !showTechnicalPreview },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (showTechnicalPreview) "Hide Technical Preview" else "Show Technical Preview")
        }

        if (showTechnicalPreview) {
            Spacer(modifier = Modifier.height(12.dp))

            InfoCard {
                Text(
                    text = "CSV Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (recordsToExport.isEmpty()) {
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
                    text = recordsToExport.toExportPackagePreview(),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            val result = exportResult
            if (result != null) {
                Spacer(modifier = Modifier.height(12.dp))

                InfoCard {
                    Text(
                        text = "Saved File",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(result.file.absolutePath)
                }
            }
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
    Text("Photo: Linked")
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
