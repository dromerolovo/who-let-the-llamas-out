package com.llamas

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.llamas.data.InterpolatedLlama
import com.llamas.managers.LocationManager
import com.llamas.ui.theme.LlamasTheme
import com.llamas.viewmodels.LlamasHandlerViewModel
import com.llamas.viewmodels.LocationViewModel
import com.llamas.viewmodels.SignalRViewModel
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.SourceQueryOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.BooleanValue
import com.mapbox.maps.extension.compose.style.standard.LightPresetValue
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.standard.StandardStyleConfigurationState
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.ModelLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.model.addModel
import com.mapbox.maps.extension.style.model.model
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import java.util.Locale.filter

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            var menuExpanded by remember { mutableStateOf(false) }
            val signalRViewModel = remember { SignalRViewModel() }
            val llamasHandlerViewModel = remember { LlamasHandlerViewModel() }
            val locationViewModel = remember { LocationViewModel() }

            val llamasUpdates = signalRViewModel.llamasUpdates.collectAsState()

            val locationManager : LocationManager = LocationManager(this)
            locationManager.requestLocationPermission()

            locationViewModel.startLocationUpdates(this)

            LaunchedEffect(llamasUpdates.value) {
                Log.d("SignalR", "Received Llamas: ${llamasUpdates.value}")
            }

            LlamasTheme {
                Scaffold(modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            actions = {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.Menu,
                                        contentDescription = "Localized description"
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Game Status") },
                                        onClick = {  }

                                    )
                                }
                            },
                            title = { Text(
                                "Who Let The Llamas Out?",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,

                                ) }

                        )
                    }) { innerPadding ->
                    MainEntryPoint(innerPadding, signalRViewModel, llamasHandlerViewModel, locationViewModel)
                }
            }
        }
    }
}

@Composable
fun MainEntryPoint
    (
        innerPadding : PaddingValues,
        signalRViewModel: SignalRViewModel,
        llamasHandlerViewModel: LlamasHandlerViewModel,
        locationViewModel: LocationViewModel
    ) {

    val context = LocalContext.current
    val interpolatedLlamas by llamasHandlerViewModel.interpolatedLlamas.collectAsState()
    val llamasUpdates by signalRViewModel.llamasUpdates.collectAsState()
    val locationUpdate by locationViewModel.userLocation.collectAsState()

    val modelsPixelsPositionState = remember { mutableStateMapOf<Int, ScreenCoordinate>() }

    val createdLlamas = remember { mutableStateListOf<Int>() }

    var shadowCounter by remember { mutableStateOf(0) }

    var showDialog by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf<Int?>(null)}

    if(showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .background(Color.White, shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text("Llama #$selectedId", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("You can design any content here.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showDialog = false }) {
                        Text("Close")
                    }
                }
            }
        }
    }

    LaunchedEffect(llamasUpdates) {
        Log.d("SignalRinMain", "Received ${llamasUpdates.size} updates")
        if(llamasUpdates.isNotEmpty()) {
            Log.d("SignalRinMain", "Received ${llamasUpdates.size} updates")
            llamasHandlerViewModel.updateLlamas(llamasUpdates)
        }
    }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            zoom(18.0)
            center(Point.fromLngLat(0.0, 0.0))
            pitch(70.0)
            bearing(30.0)
        }
    }

    LaunchedEffect(locationUpdate) {
        Log.d("Location", "Received location update: $locationUpdate")
        locationUpdate?.let { location ->
            Log.d("Location", "Received location update: $location")
            mapViewportState.setCameraOptions {
                center(location)
                zoom(18.0)
                pitch(60.0)
                bearing(30.0)
            }
        }
    }


    MapboxMap(
        Modifier.fillMaxSize().padding(innerPadding),
        mapViewportState = mapViewportState,
        style = {
            val standardStyleConfigurationState = remember {
                StandardStyleConfigurationState().apply {
                    lightPreset = LightPresetValue.DAY

                    show3dObjects = BooleanValue(true)
                }
            }

            MapboxStandardStyle(
                standardStyleConfigurationState = standardStyleConfigurationState
            )
        }
    ) {

        MapEffect(Unit) { mapView ->
            mapView.mapboxMap.getStyle { style ->
                val modelLlama = model("llama") {
                    uri("asset://Llama.glb")
                }
                style.addModel(modelLlama)
                Log.d("MapUpdate", "Added llama model")
            }
        }
        MapEffect(interpolatedLlamas.keys.toSet()) { mapView ->
            mapView.mapboxMap.getStyle { style ->
                interpolatedLlamas.keys.forEach { id ->
                    if (!createdLlamas.contains(id)) {
                        val sourceId = "llama-source-$id"
                        val layerId = "llama-layer-$id"

                        val obj = interpolatedLlamas[id]!!
                        val point = Point.fromLngLat(obj.currentPosition.x, obj.currentPosition.y)
                        val feature = Feature.fromGeometry(point).apply {
                            addStringProperty("id", id.toString())
                        }
                        val geoJson = FeatureCollection.fromFeature(feature)


                        style.addSource(
                            geoJsonSource(sourceId) {
                                featureCollection(geoJson)
                            }
                        )

                        style.addLayer(
                            ModelLayer(layerId, sourceId).apply {
                                modelId("llama")
                                modelScale(listOf(1.5, 1.5, 1.5))
                                modelRotation(listOf(0.0, 0.0, obj.bearing))
                                modelTranslation(listOf(0.0, 0.0, 0.0))
                            }
                        )


                        createdLlamas.add(id)
                        Log.d("MapUpdate", "Created llama with ID: $id")
                    }
                }

                val llamasToRemove = createdLlamas - interpolatedLlamas.keys.toSet()
                llamasToRemove.forEach { id ->
                    val sourceId = "llama-source-$id"
                    val layerId = "llama-layer-$id"

                    style.removeStyleLayer(layerId)
                    style.removeStyleSource(sourceId)
                    createdLlamas.remove(id)
                    Log.d("MapUpdate", "Removed llama with ID: $id")
                }
            }
        }

        MapEffect(interpolatedLlamas) { mapView ->
            mapView.mapboxMap.getStyle { style ->
                interpolatedLlamas.forEach { (id, llm) ->
                    val sourceId = "llama-source-$id"
                    val source = style.getSourceAs<GeoJsonSource>(sourceId)

                    if (source != null) {


                        val point = Point.fromLngLat(llm.currentPosition.x, llm.currentPosition.y)
                        val feature = Feature.fromGeometry(point)
                        val geoJson = FeatureCollection.fromFeature(feature)
                        source.featureCollection(geoJson)

                        val layerId = "llama-layer-$id"
                        val layer = style.getLayer(layerId) as? ModelLayer



                        layer?.modelRotation(listOf(0.0, 0.0, (llm.bearing + 180) % 360))


                        layer?.modelCastShadows(true)

                        //This solution is pretty terrible and doesn't fix the shadow problem when
                        //adding a 3d model to mapbox map. When the geojson of the model is updated the shadow doesn't follow the model
                        // and if the source or layer gets removed and spawn again a flickering effect is produced. That's why
                        //I'm adding this line, so that every 5 interpolations the shadow is refresh. The flickering effect
                        // is still there but it's much better.
                        if (shadowCounter % 5 == 1) layer?.modelCastShadows(false)
                    }
                }
            }
        }

        MapEffect(interpolatedLlamas) { mapView ->
            interpolatedLlamas.forEach { (id, llm) ->
                val screenPoint = mapView.mapboxMap.pixelForCoordinate(Point.fromLngLat(llm.currentPosition.x, llm.currentPosition.y))
                modelsPixelsPositionState[id] = screenPoint
            }

        }

        MapEffect { mapView ->
            mapView.mapboxMap.addOnMapClickListener { point ->
                val screen = mapView.mapboxMap.pixelForCoordinate(point)
                Log.d("MapClick", "Screen: $screen")
                modelsPixelsPositionState.forEach { (id, screenPoint) ->
                    if(screen.x >= screenPoint.x - 50 && screen.x <= screenPoint.x + 50 && screen.y >= screenPoint.y - 50 && screen.y <= screenPoint.y + 50) {
                        showDialog = true
                        selectedId = id
                        Log.d("MapClick", "Selected llama with ID: $id")
                    }
                }
                true
            }

        }


//        MapEffect { mapView ->
//            mapView.mapboxMap.addOnMapClickListener { point ->
//                val screen = mapView.mapboxMap.pixelForCoordinate(point)
//
//                val llamaLayers = createdLlamas.map { "llama-layer-1" }
//
//                mapView.mapboxMap.queryRenderedFeatures(
//                    RenderedQueryGeometry(screen),
//                    RenderedQueryOptions(llamaLayers, null)
//                ) { result ->
//                    result.value?.forEach { qf ->
//                        println("  - ${qf.queriedFeature.sourceLayer}")
//                    }
//                    result.value?.firstOrNull().let { feature ->
//                        val name = feature?.queriedFeature?.feature?.getStringProperty("id")
//                        val id = feature?.queriedFeature?.feature?.id()
//                        println(name)
//                        println(id)
//                    }
//                }
//
//                true
//            }
//        }
    }
}
