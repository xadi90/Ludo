package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.i18n.LocalAppStrings
import com.example.i18n.stringsForLanguage
import com.example.ui.navigation.LudoNavHost
import com.example.ui.theme.LudoTheme
import com.example.ui.viewmodel.LudoViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LudoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val strings = stringsForLanguage(settings.languageCode)

            CompositionLocalProvider(LocalAppStrings provides strings) {
                LudoTheme(darkTheme = settings.darkThemeMode) {
                    LudoNavHost(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.soundManager.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.soundManager.onPause()
    }
}
