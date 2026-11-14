package com.pleos.familyjourney

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pleos.familyjourney.navigation.AppNavGraph
import com.pleos.familyjourney.ui.FamilyJourneyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as FamilyJourneyApp
        setContent {
            FamilyJourneyTheme {
                AppNavGraph(app)
            }
        }
    }
}
