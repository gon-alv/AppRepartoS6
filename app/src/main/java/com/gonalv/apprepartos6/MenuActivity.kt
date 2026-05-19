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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.gonalv.apprepartos6.ui.theme.AppRepartoS6Theme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.rememberCameraPositionState
//import com.google.firebase.auth.FirebaseAuth

class MenuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        enableEdgeToEdge()
        setContent {
            AppRepartoS6Theme {
                MainMenuScreen(fusedLocationClient = fusedLocationClient)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(fusedLocationClient: FusedLocationProviderClient) {
    val context = LocalContext.current
    var montoCompraStr by remember { mutableStateOf("") }
    var resultInfo by remember { mutableStateOf("") }
    var currentUserLocation by remember { mutableStateOf<LatLng?>(null) }

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
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            obtenerUbicacion(context, fusedLocationClient) { location ->
                currentUserLocation = LatLng(location.latitude, location.longitude)
            }
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("App Reparto S6") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mapa
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(
                    currentUserLocation ?: LatLng(-41.4689, -72.9411),
                    12f
                )
            }

            LaunchedEffect(currentUserLocation) {
                currentUserLocation?.let {
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 15f)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    currentUserLocation?.let {
                        Marker(
                            state = rememberMarkerState(position = it),
                            title = "Tu ubicación"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = montoCompraStr,
                onValueChange = { montoCompraStr = it },
                label = { Text("Total Compra") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val monto = montoCompraStr.toLongOrNull()
                    if (monto != null && currentUserLocation != null) {
                        try {
                            val distancia = calcularDistancia(
                                currentUserLocation!!.latitude,
                                currentUserLocation!!.longitude
                            )
                            val tarifaEnvio = calcularTarifa(monto, distancia)
                            val totalFinal = monto + tarifaEnvio

                            val precioPorKm = if (distancia > 0) tarifaEnvio / distancia else 0.0

                            resultInfo = "Distancia: %.2f km\nTarifa Envío: $%d\nPrecio por Km: $%.2f\nTotal con Despacho: $%d".format(
                                distancia, tarifaEnvio, precioPorKm, totalFinal
                            )

                            // Guardar en Firebase
                            val usuario = FirebaseAuth.getInstance().currentUser
                            usuario?.let {
                                guardarGPS(it.uid, currentUserLocation!!.latitude, currentUserLocation!!.longitude)
                            }
                        } catch (e: Exception) {
                            resultInfo = "Error: ${e.message}"
                        }
                    } else {
                        Toast.makeText(context, "Ingrese un monto válido y active el GPS", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Calcular Tarifa")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = resultInfo)
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
        "https://apprepartopro401-default-rtdb.firebaseio.com/"
    ).reference
    val ubicacion = UbicacionUsuario(lat, lon)

    database.child("ubicacionUsuario").child(userId).setValue(ubicacion).addOnSuccessListener {
        println("Ubicación almacenada exitosamente")
    }
        .addOnFailureListener {
            println("Error al guardar ubicación: ${it.message}")
        }
}

fun calcularTarifa(montoCompra: Long, distanciaKm: Double): Int {
    require(distanciaKm >= 0) { "La distancia no puede ser negativa" }

    return when {
        montoCompra > 50000 -> {
            if (distanciaKm <= 20.0) {
                0
            } else {
                throw IllegalArgumentException("El servicio de despacho gratuito sobre $50.000 solo cubre hasta 20 km.")
            }
        }
        montoCompra in 25000..49999 -> {
            (distanciaKm * 150).toInt()
        }
        else -> {
            (distanciaKm * 300).toInt()
        }
    }
}

fun calcularDistancia(latUsuario: Double, lonUsuario: Double): Double {
    val latCentro = -41.53594865890991
    val lonCentro = -73.07477927290162
    val resultados = FloatArray(1)
    Location.distanceBetween(latUsuario, lonUsuario, latCentro, lonCentro, resultados)
    return (resultados[0] / 1000).toDouble() // Distancia en km
}
