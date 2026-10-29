package com.autotech.android

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.autotech.android.ui.navigation.AutoTechRoot
import com.autotech.android.ui.navigation.PendingOpen
import com.autotech.android.work.ReminderNotifier
import com.autotech.android.ui.theme.AtColors
import com.autotech.android.ui.theme.AutoTechTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleNotification(intent)
        // Ícones escuros nas barras do sistema, combinando com o fundo claro do layout.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            AutoTechTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = AtColors.Background) {
                    AutoTechRoot()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotification(intent)
    }

    /** RF-06: ao tocar na notificação, abre o histórico de manutenções do veículo. */
    private fun handleNotification(intent: Intent?) {
        val id = intent?.getLongExtra(ReminderNotifier.EXTRA_VEHICLE_ID, -1L) ?: -1L
        if (id > 0) {
            PendingOpen.maintenanceVehicleId.value = id
            intent?.removeExtra(ReminderNotifier.EXTRA_VEHICLE_ID)
        }
    }
}
