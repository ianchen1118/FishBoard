package com.example.androidtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.androidtest.ui.theme.AndroidTestTheme

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
    Scan,
    Database,
    Export
}

@Composable
fun FishBoardApp() {
    var currentScreen by remember { mutableStateOf(Screen.Home) }

    when (currentScreen) {
        Screen.Home -> HomeScreen(
            onStartScanClick = {
                currentScreen = Screen.Scan
            },
            onDatabaseClick = {
                currentScreen = Screen.Database
            },
            onExportClick = {
                currentScreen = Screen.Export
            }
        )

        Screen.Scan -> ScanScreen(
            onBackClick = {
                currentScreen = Screen.Home
            }
        )

        Screen.Database -> DatabaseScreen(
            onBackClick = {
                currentScreen = Screen.Home
            }
        )

        Screen.Export -> ExportScreen(
            onBackClick = {
                currentScreen = Screen.Home
            }
        )
    }
}

@Composable
fun HomeScreen(
    onStartScanClick: () -> Unit,
    onDatabaseClick: () -> Unit,
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
            text = "Fish Board",
            style = MaterialTheme.typography.headlineLarge
        )

        Button(
            onClick = onStartScanClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
        ) {
            Text("Start Scan")
        }

        Button(
            onClick = onDatabaseClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text("Database")
        }

        Button(
            onClick = onExportClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text("Export")
        }
    }
}

@Composable
fun ScanScreen(
    onBackClick: () -> Unit
) {
    BasicScreenLayout(
        title = "Scan Screen",
        description = "This will become the camera screen later.",
        onBackClick = onBackClick
    )
}

@Composable
fun DatabaseScreen(
    onBackClick: () -> Unit
) {
    BasicScreenLayout(
        title = "Database Screen",
        description = "This will show saved fish records later.",
        onBackClick = onBackClick
    )
}

@Composable
fun ExportScreen(
    onBackClick: () -> Unit
) {
    BasicScreenLayout(
        title = "Export Screen",
        description = "This will export CSV and images later.",
        onBackClick = onBackClick
    )
}

@Composable
fun BasicScreenLayout(
    title: String,
    description: String,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}