package com.gonalv.apprepartos6

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.gonalv.apprepartos6.ui.theme.AppRepartoS6Theme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
//import com.google.firebase.auth.FirebaseAuth

class MenuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        enableEdgeToEdge()
        setContent {
            AppRepartoS6Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LocationViewer(
                        fusedLocationClient = fusedLocationClient,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

//Se define una data class para almacenar la informacion geografica del usuario
data class UbicacionUsuario(
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun LocationViewer(fusedLocationClient: FusedLocationProviderClient, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var locationInfo by remember { mutableStateOf("Ubicación desconocida") }
    var hasPermission by remember {
        mutableStateOf(
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!hasPermission) {
            Toast.makeText(context, "Permiso denegado por el usuario", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Información de Ubicación:")
        Text(text = locationInfo, modifier = Modifier.padding(16.dp))

        Button(onClick = {
            if (hasPermission) {
                obtenerUbicacion(context, fusedLocationClient) { location ->
                    locationInfo = "Lat: ${location.latitude}, Lon: ${location.longitude}"
                    val usuario = FirebaseAuth.getInstance().currentUser
                    if (usuario != null) {
                        guardarGPS(userId = usuario.uid, lat = location.latitude, lon = location.longitude)
                    } else {
                        Toast.makeText(context, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }) {
            Text(if (hasPermission) "Obtener Ubicacion" else "Conceder Permisos")
        }
    }
}

fun obtenerUbicacion(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    onLocationReceived: (Location) -> Unit
) {
    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        Toast.makeText(context, "No se ha concedido permiso de ubicación", Toast.LENGTH_SHORT).show()
        return
    }

    val cancellationTokenSource = CancellationTokenSource()
    fusedLocationClient.getCurrentLocation(
        Priority.PRIORITY_HIGH_ACCURACY,
        cancellationTokenSource.token
    ).addOnSuccessListener { location: Location? ->
        if (location != null) {
            onLocationReceived(location)
        } else {
            Toast.makeText(context, "No se pudo obtener la ubicación. Asegúrese de que el GPS esté encendido.", Toast.LENGTH_SHORT).show()
        }
    }.addOnFailureListener { exception ->
        Toast.makeText(context, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
    }
}

fun guardarGPS(userId: String, lat: Double, lon: Double) {
    val database = FirebaseDatabase.getInstance(
        "https://login-semana-6-f42a1-default-rtdb.firebaseio.com/"
    ).reference
    val ubicacion = UbicacionUsuario(lat, lon)

    database.child("ubicaciones").child(userId).setValue(ubicacion).addOnSuccessListener {
        println("Ubicación almacenada exitosamente")
    }
        .addOnFailureListener {
            println("Error al guardar ubicación: ${it.message}")
        }
}


