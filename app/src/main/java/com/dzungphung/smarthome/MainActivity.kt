package com.dzungphung.smarthome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.dzungphung.smarthome.client.IotClientManager
import com.dzungphung.smarthome.ui.MainViewModel
import com.dzungphung.smarthome.ui.SmartHomeHomeScreen
import com.dzungphung.smarthome.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var clientManager: IotClientManager
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(clientManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize IotClientManager in Main Process
        clientManager = IotClientManager(applicationContext)
        viewModel.connect()

        setContent {
            MyApplicationTheme {
                SmartHomeHomeScreen(viewModel = viewModel)
            }
        }
    }
}