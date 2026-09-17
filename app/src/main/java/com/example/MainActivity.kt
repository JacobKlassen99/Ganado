package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.FichaGanadoScreen
import com.example.ui.screens.GanadoListScreen
import com.example.ui.screens.NuevoGanadoScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GanadoViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RegistroGanadoApp()
                }
            }
        }
    }
}

@Composable
fun RegistroGanadoApp(
    viewModel: GanadoViewModel = viewModel()
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "ganado_list"
    ) {
        composable("ganado_list") {
            GanadoListScreen(
                viewModel = viewModel,
                onNavigateToDetail = { numeroGanado ->
                    navController.navigate("ficha/$numeroGanado")
                },
                onNavigateToNuevo = {
                    navController.navigate("nuevo_ganado")
                }
            )
        }

        composable("nuevo_ganado") {
            NuevoGanadoScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onGanadoSaved = { numeroGanado ->
                    // Navegar directamente a la ficha del nuevo animal creado
                    navController.popBackStack()
                    navController.navigate("ficha/$numeroGanado")
                }
            )
        }

        composable(
            route = "ficha/{numeroGanado}",
            arguments = listOf(
                navArgument("numeroGanado") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val numeroGanado = backStackEntry.arguments?.getString("numeroGanado") ?: ""
            FichaGanadoScreen(
                numeroGanado = numeroGanado,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onOpenAnimalProfile = { targetNumero ->
                    navController.navigate("ficha/$targetNumero")
                }
            )
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
