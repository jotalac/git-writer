package dev.jotalac

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.jotalac.core.di.initFileKitAndroid
import dev.jotalac.core.di.initKoin
import dev.jotalac.core.di.initKoinAndroid

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        initFileKitAndroid(this)

        setContent {
            App()
        }

    }
}

@Preview()
@Composable
fun AppAndroidPreview() {
    App()
}