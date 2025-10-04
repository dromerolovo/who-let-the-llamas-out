package com.llamas

import android.os.Bundle
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.llamas.ui.theme.LlamasTheme
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.BooleanValue
import com.mapbox.maps.extension.compose.style.standard.LightPresetValue
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.standard.StandardStyleConfigurationState

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            var menuExpanded by remember { mutableStateOf(false) }

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
                    MainEntryPoint(innerPadding)
                }
            }
        }
    }
}

@Composable
fun MainEntryPoint(innerPadding : PaddingValues) {
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            zoom(18.0)
            center(Point.fromLngLat(-123.149015, 49.294082))
            pitch(70.0)
            bearing(30.0)
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
    ) {}
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LlamasTheme {
        Greeting("Android")
    }
}