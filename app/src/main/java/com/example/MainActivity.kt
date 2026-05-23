package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.data.db.AppDatabase
import com.example.data.repository.SmsRepository
import com.example.ui.SmsBomberScreen
import com.example.ui.SmsViewModel
import com.example.ui.SmsViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Local persistence dependencies
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = SmsRepository(database.smsDao())

        // Initialize State Machine with custom factory
        val viewModel: SmsViewModel by viewModels {
            SmsViewModelFactory(repository)
        }

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                SmsBomberScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
