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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.shadow
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
import android.app.Activity
import android.content.IntentSender
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.maps.android.compose.GoogleMap
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.CameraUpdateFactory
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

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
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
   // Se inicializan las variables a utilizar en el programa
    var montoCompraStr by remember { mutableStateOf("") }
    var resultInfo by remember { mutableStateOf("") }
    var currentUserLocation by remember { mutableStateOf<LatLng?>(null) }
    var isLocating by remember { mutableStateOf(false) }
    var pendingIntentSender by remember { mutableStateOf<IntentSenderRequest?>(null) }

    fun obtenerUbicacion() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(context)
        val task = client.checkLocationSettings(builder.build())

        isLocating = true
        task.addOnSuccessListener {
            val cancellationTokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                isLocating = false
                if (location != null) {
                    currentUserLocation = LatLng(location.latitude, location.longitude)
                } else {
                    scope.launch { snackbarHostState.showSnackbar("No se pudo obtener la ubicación") }
                }
            }.addOnFailureListener {
                isLocating = false
                scope.launch { snackbarHostState.showSnackbar("Error: ${it.message}") }
            }
        }

        task.addOnFailureListener { exception ->
            isLocating = false
            if (exception is ResolvableApiException) {
                try {
                    pendingIntentSender = IntentSenderRequest.Builder(exception.resolution.intentSender).build()
                } catch (_: IntentSender.SendIntentException) { }
            } else {
                scope.launch { snackbarHostState.showSnackbar("Asegúrese de tener el GPS habilitado") }
            }
        }
    }

    val settingResultRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            obtenerUbicacion()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("El GPS es necesario para calcular el envío")
            }
        }
    }

    LaunchedEffect(pendingIntentSender) {
        pendingIntentSender?.let {
            settingResultRequest.launch(it)
            pendingIntentSender = null
        }
    }
    // Se utilizará una ubicación arbitraria para probar la funcionalidad
    val latCentro = -41.53594865890991
    val lonCentro = -73.07477927290162
    val centroDistribucion = LatLng(latCentro, lonCentro)

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
            obtenerUbicacion()
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
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Calculo de Despacho",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(
                    centroDistribucion,
                    12f
                )
            }

            LaunchedEffect(currentUserLocation) {
                currentUserLocation?.let { userLoc ->
                    val bounds = LatLngBounds.builder()
                        .include(centroDistribucion)
                        .include(userLoc)
                        .build()
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(bounds, 100),
                        1000
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = hasPermission),
                    uiSettings = MapUiSettings(myLocationButtonEnabled = false)
                ) {
                    // Marcador Centro de Distribución
                    Marker(
                        state = rememberMarkerState(position = centroDistribucion),
                        title = "Centro de Distribución",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )

                    currentUserLocation?.let {
                        // Círculo azul para ubicación actual
                        Circle(
                            center = it,
                            radius = 100.0, // Radio en metros
                            fillColor = Color(0x330000FF),
                            strokeColor = Color.Blue,
                            strokeWidth = 2f
                        )
                    }
                }

                // Se incorpora también un botón para centrar la ubicación
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp)
                        .size(40.dp)
                        .shadow(elevation = 4.dp, shape = CircleShape),
                    shape = CircleShape,
                    color = Color.White
                ) {
                    IconButton(
                        onClick = {
                            obtenerUbicacion()
                        },
                        enabled = !isLocating
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Mi ubicación",
                                tint = Color(0xFF666666)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = montoCompraStr,
                onValueChange = { montoCompraStr = it },
                label = { Text("Total Compra") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                            scope.launch {
                                snackbarHostState.showSnackbar("Cálculo realizado con éxito")
                            }
                        } catch (e: Exception) {
                            resultInfo = "Error: ${e.message}"
                            scope.launch {
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            }
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Ingrese un monto válido y active el GPS")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Calcular Tarifa", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = resultInfo,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
