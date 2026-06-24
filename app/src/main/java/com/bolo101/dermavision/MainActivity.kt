package com.bolo101.dermavision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bolo101.dermavision.ui.theme.screens.CameraScreen
import com.bolo101.dermavision.ui.theme.screens.HomeScreen
import com.bolo101.dermavision.ui.theme.screens.ResultScreen
import com.bolo101.dermavision.ui.theme.DermaVisionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DermaVisionTheme {
                DermaVisionApp()
            }
        }
    }
}

@Composable
fun DermaVisionApp() {
    // navController = le GPS de l'app, il sait où on est et où aller
    val navController = rememberNavController()

    // NavHost = la carte de toutes les destinations possibles
    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            HomeScreen(
                onStartAnalysis = { navController.navigate("camera") }
            )
        }

        composable("camera") {
            CameraScreen(
                onPhotoTaken = { navController.navigate("result") },
                onBack = { navController.popBackStack() }
            )
        }

        composable("result") {
            ResultScreen(
                onNewAnalysis = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}