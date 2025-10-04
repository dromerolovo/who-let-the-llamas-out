package com.llamas

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.llamas.managers.LocationManager
import com.llamas.ui.theme.LlamasTheme
import com.llamas.viewmodels.LlamasHandlerViewModel
import com.llamas.viewmodels.LocationViewModel
import com.llamas.viewmodels.SignalRViewModel
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
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

    val createdLlamas = remember { mutableStateListOf<Int>() }

    var shadowCounter by remember { mutableStateOf(0) }

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

//        MapEffect(createdLlamas.toList()) { mapView ->
//            mapView.mapboxMap.addOnMapClickListener { point ->
//
//                val screen = mapView.mapboxMap.pixelForCoordinate(point)
//
//                mapView.mapboxMap.queryRenderedFeatures(
//                    RenderedQueryGeometry(screen),
//                    RenderedQueryOptions(listOf("model-layer"), null)
//                ) { expected ->
//                    expected.value?.let { renderedQueryResults ->
//                        renderedQueryResults.firstOrNull()?.let { renderedResult ->
//                            val feature = renderedResult.feature
//                            val id = feature.getStringProperty("id")
//                            val name = feature.getStringProperty("name")
//
//                            Toast.makeText(context, "Clicked model: $name (id: $id)", Toast.LENGTH_SHORT).show()
//                        }
//                    }
//
//                    expected.error?.let { error ->
//                        Log.e("Mapbox", "Query error: ${error.message}")
//                    }
//                }
//
//                true
//            }
//        }

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
    }
}
