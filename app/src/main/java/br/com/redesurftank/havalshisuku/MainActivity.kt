@file:Suppress("KotlinConstantConditions")

package br.com.redesurftank.havalshisuku

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import androidx.core.content.FileProvider
import androidx.core.content.edit
import br.com.redesurftank.App
import br.com.redesurftank.havalshisuku.listeners.IDataChanged
import br.com.redesurftank.havalshisuku.managers.*
import br.com.redesurftank.havalshisuku.models.*
import br.com.redesurftank.havalshisuku.services.*
import br.com.redesurftank.havalshisuku.ui.components.*
import br.com.redesurftank.havalshisuku.ui.theme.*
import br.com.redesurftank.havalshisuku.utils.*
import coil.compose.AsyncImage
import coil.request.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min
import kotlinx.coroutines.*
import org.json.JSONArray

const val TAG = "HavalShisuku"

class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                enableEdgeToEdge()
                setContent {
                        HavalShisukuTheme {
                                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                                        MainScreen(modifier = Modifier.padding(innerPadding))
                                }
                        }
                }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
        val prefs =
                App.getDeviceProtectedContext()
                        .getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
        val advancedUse = prefs.getBoolean(SharedPreferencesKeys.ADVANCE_USE.key, false)

        val menuItems = buildList {
                add(DrawerMenuItem("空调仪表", Icons.Default.SmartDisplay, "screens"))
                add(DrawerMenuItem("设置", Icons.Default.Settings, "settings"))
                add(DrawerMenuItem("实时数值", Icons.Default.DeveloperMode, "values"))
                add(DrawerMenuItem("安装应用", Icons.Default.ShoppingCart, "apps"))
                add(DrawerMenuItem("功能中心", Icons.Default.Apps, "features"))
                add(DrawerMenuItem("关于", Icons.Default.Info, "info"))
                if (advancedUse) {
                        add(DrawerMenuItem("Frida 调试", Icons.Default.Build, "frida"))
                }
        }

        var selectedItem by remember { mutableStateOf(0) }

        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

        // Check if width is 1920px (with some tolerance)
        val isFullWidth = screenWidthPx >= 1918f && screenWidthPx <= 1922f
        val startPadding = if (isFullWidth) with(density) { 100.toDp() } else 0.dp

        Row(
                modifier =
                        modifier.fillMaxSize()
                                .padding(start = startPadding)
                                .background(AppColors.Background)
        ) {
                // Fixed Side Menu
                Surface(
                        modifier = Modifier.width(AppDimensions.MenuWidth).fillMaxHeight(),
                        color = Color(0xFF13151A),
                        shadowElevation = 4.dp
                ) {
                        Column(modifier = Modifier.fillMaxHeight()) {
                                menuItems.forEachIndexed { index, item ->
                                        val animatedWidth by
                                                animateFloatAsState(
                                                        targetValue =
                                                                if (selectedItem == index) 1f
                                                                else 0f,
                                                        animationSpec =
                                                                tween(
                                                                        durationMillis = 200,
                                                                        easing = FastOutSlowInEasing
                                                                ),
                                                        label = "backgroundWidth"
                                                )

                                        val borderAlpha by
                                                animateFloatAsState(
                                                        targetValue =
                                                                if (selectedItem == index) 1f
                                                                else 0f,
                                                        animationSpec =
                                                                tween(
                                                                        durationMillis = 0,
                                                                        delayMillis = 0
                                                                ),
                                                        label = "borderAlpha"
                                                )

                                        Box(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .height(
                                                                        AppDimensions.MenuItemHeight
                                                                )
                                                                .clickable { selectedItem = index },
                                                contentAlignment = Alignment.CenterStart
                                        ) {
                                                // Animated background
                                                Box(
                                                        modifier =
                                                                Modifier.fillMaxWidth(animatedWidth)
                                                                        .fillMaxHeight()
                                                                        .background(
                                                                                Brush.horizontalGradient(
                                                                                        colors =
                                                                                                listOf(
                                                                                                        Color(
                                                                                                                0xFF152031
                                                                                                        ),
                                                                                                        Color(
                                                                                                                0xFF13151A
                                                                                                        )
                                                                                                )
                                                                                )
                                                                        )
                                                                        .drawBehind {
                                                                                drawLine(
                                                                                        color =
                                                                                                Color(
                                                                                                                0xFF0B84FF
                                                                                                        )
                                                                                                        .copy(
                                                                                                                alpha =
                                                                                                                        borderAlpha
                                                                                                        ),
                                                                                        start =
                                                                                                Offset(
                                                                                                        0f,
                                                                                                        0f
                                                                                                ),
                                                                                        end =
                                                                                                Offset(
                                                                                                        0f,
                                                                                                        size.height
                                                                                                ),
                                                                                        strokeWidth =
                                                                                                10.dp.toPx()
                                                                                )
                                                                        }
                                                )
                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Start,
                                                        modifier =
                                                                Modifier.padding(horizontal = 20.dp)
                                                ) {
                                                        Icon(
                                                                imageVector = item.icon,
                                                                contentDescription = item.title,
                                                                tint =
                                                                        if (selectedItem == index)
                                                                                AppColors
                                                                                        .MenuSelectedIcon
                                                                        else
                                                                                AppColors
                                                                                        .MenuUnselectedIcon,
                                                                modifier =
                                                                        Modifier.size(
                                                                                AppDimensions
                                                                                        .IconSize
                                                                        )
                                                        )
                                                        Spacer(modifier = Modifier.width(14.dp))
                                                        Text(
                                                                item.title,
                                                                color =
                                                                        if (selectedItem == index)
                                                                                AppColors
                                                                                        .TextPrimary
                                                                        else
                                                                                AppColors
                                                                                        .MenuUnselectedText,
                                                                fontSize = 20.sp,
                                                                fontWeight =
                                                                        if (selectedItem == index)
                                                                                FontWeight.Medium
                                                                        else FontWeight.Normal
                                                        )
                                                }
                                        }
                                }
                        }
                }

                // Main Content
                Column(
                        modifier =
                                Modifier.weight(1f).fillMaxHeight().background(AppColors.Background)
                ) {
                        // Content Area
                        ContentArea {
                                when (menuItems.getOrNull(selectedItem)?.route) {
                                        "settings" -> BasicSettingsTab()
                                        "screens" -> TelasTab()
                                        "values" -> CurrentValuesTab()
                                        "apps" -> InstallAppsTab()
                                        "features" -> FeaturesHubScreen()
                                        "info" -> InformacoesTab()
                                        "frida" -> FridaHooksTab()
                                        else -> BasicSettingsTab()
                                }
                        }
                }
        }
}

data class DrawerMenuItem(
        val title: String,
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicSettingsTab() {
        val context = LocalContext.current
        val prefs =
                App.getDeviceProtectedContext()
                        .getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
        var isAdvancedUse by remember {
                mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.ADVANCE_USE.key, false))
        }
        var selfInstallationCheck by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.SELF_INSTALLATION_INTEGRITY_CHECK.key,
                                false
                        )
                )
        }
        var bypassSelfInstallationCheck by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.BYPASS_SELF_INSTALLATION_INTEGRITY_CHECK.key,
                                false
                        )
                )
        }
        var disableMonitoring by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.DISABLE_MONITORING.key, false)
                )
        }
        var disableAvas by remember {
                mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.DISABLE_AVAS.key, false))
        }
        var disableAvmCarStopped by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.DISABLE_AVM_CAR_STOPPED.key, false)
                )
        }
        var closeWindowOnPowerOff by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.CLOSE_WINDOW_ON_POWER_OFF.key, false)
                )
        }
        var closeWindowOnFoldMirror by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.CLOSE_WINDOW_ON_FOLD_MIRROR.key,
                                false
                        )
                )
        }
        var closeSunroofOnPowerOff by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.CLOSE_SUNROOF_ON_POWER_OFF.key,
                                false
                        )
                )
        }
        var closeSunroofOnFoldMirror by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.CLOSE_SUNROOF_ON_FOLD_MIRROR.key,
                                false
                        )
                )
        }
        var closeSunroofSunShadeOnCloseSunroof by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.CLOSE_SUNROOF_SUN_SHADE_ON_CLOSE_SUNROOF.key,
                                false
                        )
                )
        }
        var setStartupVolume by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.SET_STARTUP_VOLUME.key, false)
                )
        }
        var volume by remember {
                mutableIntStateOf(prefs.getInt(SharedPreferencesKeys.STARTUP_VOLUME.key, 1))
        }
        var closeWindowsOnSpeed by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.CLOSE_WINDOWS_ON_SPEED.key, false)
                )
        }
        var closeSunroofOnSpeed by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.CLOSE_SUNROOF_ON_SPEED.key, false)
                )
        }
        var speedThreshold by remember {
                mutableFloatStateOf(prefs.getFloat(SharedPreferencesKeys.SPEED_THRESHOLD.key, 15f))
        }
        var closeSunroofSpeedThreshold by remember {
                mutableFloatStateOf(
                        prefs.getFloat(SharedPreferencesKeys.SUNROOF_SPEED_THRESHOLD.key, 15f)
                )
        }
        var enableMaxAcOnUnlock by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.ENABLE_MAX_AC_ON_UNLOCK.key, false)
                )
        }
        var maxAcOnUnlockThreshold by remember {
                mutableFloatStateOf(
                        prefs.getFloat(SharedPreferencesKeys.MAX_AC_ON_UNLOCK_THRESHOLD.key, 34f)
                )
        }
        var maxAcTargetTemp by remember {
                mutableFloatStateOf(
                        prefs.getFloat(SharedPreferencesKeys.MAX_AC_TARGET_TEMP.key, 28f)
                )
        }
        var maxAcTimeout by remember {
                mutableIntStateOf(prefs.getInt(SharedPreferencesKeys.MAX_AC_TIMEOUT.key, 0))
        }
        var enableAutoBrightness by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.ENABLE_AUTO_BRIGHTNESS.key, false)
                )
        }
        var nightStartHour by remember {
                mutableIntStateOf(prefs.getInt(SharedPreferencesKeys.NIGHT_START_HOUR.key, 20))
        }
        var nightStartMinute by remember {
                mutableIntStateOf(prefs.getInt(SharedPreferencesKeys.NIGHT_START_MINUTE.key, 0))
        }
        var nightEndHour by remember {
                mutableIntStateOf(prefs.getInt(SharedPreferencesKeys.NIGHT_END_HOUR.key, 6))
        }
        var nightEndMinute by remember {
                mutableIntStateOf(prefs.getInt(SharedPreferencesKeys.NIGHT_END_MINUTE.key, 0))
        }
        var disableBluetoothOnPowerOff by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.DISABLE_BLUETOOTH_ON_POWER_OFF.key,
                                false
                        )
                )
        }
        var disableHotspotOnPowerOff by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.DISABLE_HOTSPOT_ON_POWER_OFF.key,
                                false
                        )
                )
        }
        var nightBrightnessLevel by remember {
                mutableIntStateOf(
                        prefs.getInt(SharedPreferencesKeys.AUTO_BRIGHTNESS_LEVEL_NIGHT.key, 1)
                )
        }
        var dayBrightnessLevel by remember {
                mutableIntStateOf(
                        prefs.getInt(SharedPreferencesKeys.AUTO_BRIGHTNESS_LEVEL_DAY.key, 10)
                )
        }
        var enableSeatVentilationOnAcOn by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.ENABLE_SEAT_VENTILATION_ON_AC_ON.key,
                                false
                        )
                )
        }
        var enableCustomSteeringWheelButtons by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.ENABLE_STEERING_WHEEL_CUSTOM_BUTTONS.key,
                                false
                        )
                )
        }
        var disableNativeNavigation by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.DISABLE_NATIVE_NAVIGATION.key, false)
                )
        }
        var disableNativeVoice by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.DISABLE_NATIVE_VOICE.key, false)
                )
        }
        var disableNativeWeather by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.DISABLE_NATIVE_WEATHER.key, false)
                )
        }
        var enablePersistentBottomBar by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.PERSISTENT_BOTTOM_BAR.key, false)
                )
        }
        var autoHideEnabled by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.BOTTOM_BAR_AUTO_HIDE.key, false)
                )
        }
        var showStartPicker by remember { mutableStateOf(false) }
        var showEndPicker by remember { mutableStateOf(false) }
        var enableSpeedAdjustment by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.ENABLE_SPEED_ADJUSTMENT.key, false)
                )
        }
        var speedAdjustmentOffset by remember {
                mutableFloatStateOf(
                        prefs.getFloat(SharedPreferencesKeys.SPEED_ADJUSTMENT_OFFSET.key, 0f)
                )
        }

        var enableOpenSunroofCurtainOnStart by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.ENABLE_OPEN_SUNROOF_CURTAIN_ON_START.key,
                                false
                        )
                )
        }
        var curtainStartHour by remember {
                mutableIntStateOf(
                        prefs.getInt(SharedPreferencesKeys.OPEN_SUNROOF_CURTAIN_START_HOUR.key, 18)
                )
        }
        var curtainStartMinute by remember {
                mutableIntStateOf(
                        prefs.getInt(SharedPreferencesKeys.OPEN_SUNROOF_CURTAIN_START_MINUTE.key, 0)
                )
        }
        var curtainEndHour by remember {
                mutableIntStateOf(
                        prefs.getInt(SharedPreferencesKeys.OPEN_SUNROOF_CURTAIN_END_HOUR.key, 9)
                )
        }
        var curtainEndMinute by remember {
                mutableIntStateOf(
                        prefs.getInt(SharedPreferencesKeys.OPEN_SUNROOF_CURTAIN_END_MINUTE.key, 0)
                )
        }
        var openSunroofCurtainMaxTemp by remember {
                mutableFloatStateOf(
                        prefs.getFloat(SharedPreferencesKeys.OPEN_SUNROOF_CURTAIN_MAX_TEMP.key, -1f)
                )
        }

        val settingsList = mutableListOf<SettingItem>()

        if (isAdvancedUse && !selfInstallationCheck) {
                settingsList.add(
                        SettingItem(
                                title = "跳过完整性校验",
                                description =
                                        SharedPreferencesKeys
                                                .BYPASS_SELF_INSTALLATION_INTEGRITY_CHECK
                                                .description,
                                checked = bypassSelfInstallationCheck,
                                onCheckedChange = {
                                        bypassSelfInstallationCheck = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .BYPASS_SELF_INSTALLATION_INTEGRITY_CHECK
                                                                .key,
                                                        it
                                                )
                                        }
                                }
                        )
                )
        }

        settingsList.addAll(
                listOfNotNull(
                        SettingItem(
                                title = "禁用原厂导航",
                                description =
                                        "禁用后台常驻的原厂 Neusoft 导航（占用内存/CPU），可逆。",
                                checked = disableNativeNavigation,
                                onCheckedChange = {
                                        disableNativeNavigation = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .DISABLE_NATIVE_NAVIGATION
                                                                .key,
                                                        it
                                                )
                                        }
                                        Thread {
                                                        ServiceManager.getInstance()
                                                                .ensureDebloatedSystemApps()
                                                }
                                                .start()
                                }
                        ),
                        SettingItem(
                                title = "禁用原厂语音助手",
                                description =
                                        "禁用后台常驻的原厂 iFlyTek 语音助手（占用内存/CPU），可逆。",
                                checked = disableNativeVoice,
                                onCheckedChange = {
                                        disableNativeVoice = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys.DISABLE_NATIVE_VOICE
                                                                .key,
                                                        it
                                                )
                                        }
                                        Thread {
                                                        ServiceManager.getInstance()
                                                                .ensureDebloatedSystemApps()
                                                }
                                                .start()
                                }
                        ),
                        SettingItem(
                                title = "禁用原厂天气预报",
                                description =
                                        "禁用后台常驻的原厂天气预报服务，可逆。",
                                checked = disableNativeWeather,
                                onCheckedChange = {
                                        disableNativeWeather = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys.DISABLE_NATIVE_WEATHER
                                                                .key,
                                                        it
                                                )
                                        }
                                        Thread {
                                                        ServiceManager.getInstance()
                                                                .ensureDebloatedSystemApps()
                                                }
                                                .start()
                                }
                        ),
                        SettingItem(
                                title = "熄火时关闭车窗",
                                description =
                                        "发动机熄火时自动关闭车窗",
                                checked = closeWindowOnPowerOff,
                                onCheckedChange = {
                                        closeWindowOnPowerOff = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .CLOSE_WINDOW_ON_POWER_OFF
                                                                .key,
                                                        it
                                                )
                                        }
                                }
                        ),
                        SettingItem(
                                title = "收起后视镜时关闭车窗",
                                description =
                                        "收起后视镜时同步关闭车窗",
                                checked = closeWindowOnFoldMirror,
                                onCheckedChange = {
                                        closeWindowOnFoldMirror = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .CLOSE_WINDOW_ON_FOLD_MIRROR
                                                                .key,
                                                        it
                                                )
                                        }
                                }
                        ),
                        SettingItem(
                                title = "熄火时关闭天窗",
                                description =
                                        SharedPreferencesKeys.CLOSE_SUNROOF_ON_POWER_OFF
                                                .description,
                                checked = closeSunroofOnPowerOff,
                                onCheckedChange = {
                                        closeSunroofOnPowerOff = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .CLOSE_SUNROOF_ON_POWER_OFF
                                                                .key,
                                                        it
                                                )
                                        }
                                }
                        ),
                        SettingItem(
                                title = "收起后视镜时关闭天窗",
                                description =
                                        SharedPreferencesKeys.CLOSE_SUNROOF_ON_FOLD_MIRROR
                                                .description,
                                checked = closeSunroofOnFoldMirror,
                                onCheckedChange = {
                                        closeSunroofOnFoldMirror = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .CLOSE_SUNROOF_ON_FOLD_MIRROR
                                                                .key,
                                                        it
                                                )
                                        }
                                }
                        ),
                        SettingItem(
                                title = "关闭天窗遮阳帘",
                                description =
                                        SharedPreferencesKeys
                                                .CLOSE_SUNROOF_SUN_SHADE_ON_CLOSE_SUNROOF
                                                .description,
                                checked = closeSunroofSunShadeOnCloseSunroof,
                                onCheckedChange = {
                                        closeSunroofSunShadeOnCloseSunroof = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .CLOSE_SUNROOF_SUN_SHADE_ON_CLOSE_SUNROOF
                                                                .key,
                                                        it
                                                )
                                        }
                                }
                        ),
                        SettingItem(
                                title = "高速时自动关窗",
                                description =
                                        SharedPreferencesKeys.CLOSE_WINDOWS_ON_SPEED.description,
                                checked = closeWindowsOnSpeed,
                                onCheckedChange = {
                                        closeWindowsOnSpeed = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys.CLOSE_WINDOWS_ON_SPEED
                                                                .key,
                                                        it
                                                )
                                        }
                                },
                                sliderValue = speedThreshold.toInt(),
                                sliderRange = 10..120,
                                sliderStep = 1,
                                onSliderChange = { newSpeed ->
                                        speedThreshold = newSpeed.toFloat()
                                        prefs.edit {
                                                putFloat(
                                                        SharedPreferencesKeys.SPEED_THRESHOLD.key,
                                                        newSpeed.toFloat()
                                                )
                                        }
                                },
                                sliderLabel = "速度阈值：$speedThreshold km/h"
                        ),
                        SettingItem(
                                title = "高速时自动关天窗",
                                description =
                                        SharedPreferencesKeys.CLOSE_SUNROOF_ON_SPEED.description,
                                checked = closeSunroofOnSpeed,
                                onCheckedChange = {
                                        closeSunroofOnSpeed = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys.CLOSE_SUNROOF_ON_SPEED
                                                                .key,
                                                        it
                                                )
                                        }
                                },
                                sliderValue = closeSunroofSpeedThreshold.toInt(),
                                sliderRange = 10..120,
                                sliderStep = 1,
                                onSliderChange = { newSpeed ->
                                        closeSunroofSpeedThreshold = newSpeed.toFloat()
                                        prefs.edit {
                                                putFloat(
                                                        SharedPreferencesKeys
                                                                .SUNROOF_SPEED_THRESHOLD
                                                                .key,
                                                        newSpeed.toFloat()
                                                )
                                        }
                                },
                                sliderLabel =
                                        "速度阈值：${closeSunroofSpeedThreshold.toInt()} km/h"
                        ),
                        SettingItem(
                                title = "车内温度过高时自动开启 Max AC",
                                description =
                                        SharedPreferencesKeys.ENABLE_MAX_AC_ON_UNLOCK.description,
                                checked = enableMaxAcOnUnlock,
                                onCheckedChange = {
                                        enableMaxAcOnUnlock = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .ENABLE_MAX_AC_ON_UNLOCK
                                                                .key,
                                                        it
                                                )
                                        }
                                },
                                sliderValue = maxAcOnUnlockThreshold.toInt(),
                                sliderRange = 20..38,
                                sliderStep = 1,
                                onSliderChange = { newTemp ->
                                        maxAcOnUnlockThreshold = newTemp.toFloat()
                                        prefs.edit {
                                                putFloat(
                                                        SharedPreferencesKeys
                                                                .MAX_AC_ON_UNLOCK_THRESHOLD
                                                                .key,
                                                        newTemp.toFloat()
                                                )
                                        }
                                },
                                sliderLabel =
                                        "触发温度：${maxAcOnUnlockThreshold.toInt()}°C",
                                customContent =
                                        if (enableMaxAcOnUnlock) {
                                                {
                                                        val timeOptions =
                                                                mapOf(
                                                                        0 to "不限时",
                                                                        1 to "1 分钟",
                                                                        3 to "3 分钟",
                                                                        5 to "5 分钟"
                                                                )
                                                        var expanded by remember {
                                                                mutableStateOf(false)
                                                        }

                                                        Column(
                                                                verticalArrangement =
                                                                        Arrangement.spacedBy(16.dp)
                                                        ) {
                                                                Column {
                                                                        Text(
                                                                                text =
                                                                                        "目标温度：${maxAcTargetTemp.toInt()}°C",
                                                                                fontSize = 14.sp,
                                                                                color = Color.White
                                                                        )
                                                                        Slider(
                                                                                value =
                                                                                        maxAcTargetTemp,
                                                                                onValueChange = {
                                                                                        newTemp ->
                                                                                        maxAcTargetTemp =
                                                                                                newTemp
                                                                                        prefs.edit {
                                                                                                putFloat(
                                                                                                        SharedPreferencesKeys
                                                                                                                .MAX_AC_TARGET_TEMP
                                                                                                                .key,
                                                                                                        newTemp
                                                                                                )
                                                                                        }
                                                                                },
                                                                                valueRange =
                                                                                        18f..34f,
                                                                                steps = 15,
                                                                                colors =
                                                                                        SliderDefaults
                                                                                                .colors(
                                                                                                        thumbColor =
                                                                                                                AppColors
                                                                                                                        .Primary,
                                                                                                        activeTrackColor =
                                                                                                                AppColors
                                                                                                                        .Primary,
                                                                                                        inactiveTrackColor =
                                                                                                                Color(
                                                                                                                        0xFF2C3139
                                                                                                                ),
                                                                                                        activeTickColor =
                                                                                                                Color.Transparent,
                                                                                                        inactiveTickColor =
                                                                                                                Color.Transparent
                                                                                                )
                                                                        )
                                                                }
                                                                Column {
                                                                        Text(
                                                                                text =
                                                                                        SharedPreferencesKeys
                                                                                                .MAX_AC_TIMEOUT
                                                                                                .description,
                                                                                fontSize = 14.sp,
                                                                                color = Color.White
                                                                        )
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.height(
                                                                                                8.dp
                                                                                        )
                                                                        )
                                                                        Box {
                                                                                Text(
                                                                                        text =
                                                                                                timeOptions[
                                                                                                        maxAcTimeout]
                                                                                                        ?: "不限时",
                                                                                        color =
                                                                                                Color(
                                                                                                        0xFF4A9EFF
                                                                                                ),
                                                                                        fontSize =
                                                                                                16.sp,
                                                                                        modifier =
                                                                                                Modifier.background(
                                                                                                                Color(
                                                                                                                        0xFF2A2F37
                                                                                                                ),
                                                                                                                RoundedCornerShape(
                                                                                                                        4.dp
                                                                                                                )
                                                                                                        )
                                                                                                        .padding(
                                                                                                                horizontal =
                                                                                                                        12.dp,
                                                                                                                vertical =
                                                                                                                        8.dp
                                                                                                        )
                                                                                                        .clickable {
                                                                                                                expanded =
                                                                                                                        true
                                                                                                        }
                                                                                )
                                                                                DropdownMenu(
                                                                                        expanded =
                                                                                                expanded,
                                                                                        onDismissRequest = {
                                                                                                expanded =
                                                                                                        false
                                                                                        },
                                                                                        modifier =
                                                                                                Modifier.background(
                                                                                                        Color(
                                                                                                                0xFF2A2F37
                                                                                                        )
                                                                                                )
                                                                                ) {
                                                                                        timeOptions
                                                                                                .forEach {
                                                                                                        (
                                                                                                                value,
                                                                                                                label)
                                                                                                        ->
                                                                                                        DropdownMenuItem(
                                                                                                                text = {
                                                                                                                        Text(
                                                                                                                                label,
                                                                                                                                color =
                                                                                                                                        Color.White
                                                                                                                        )
                                                                                                                },
                                                                                                                onClick = {
                                                                                                                        maxAcTimeout =
                                                                                                                                value
                                                                                                                        prefs
                                                                                                                                .edit {
                                                                                                                                        putInt(
                                                                                                                                                SharedPreferencesKeys
                                                                                                                                                        .MAX_AC_TIMEOUT
                                                                                                                                                        .key,
                                                                                                                                                value
                                                                                                                                        )
                                                                                                                                }
                                                                                                                        expanded =
                                                                                                                                false
                                                                                                                }
                                                                                                        )
                                                                                                }
                                                                                }
                                                                        }
                                                                }
                                                        }
                                                }
                                        } else null
                        ),
                        SettingItem(
                                title =
                                        SharedPreferencesKeys.ENABLE_OPEN_SUNROOF_CURTAIN_ON_START
                                                .description,
                                description =
                                        "车辆通电时自动打开天窗遮阳帘",
                                checked = enableOpenSunroofCurtainOnStart,
                                onCheckedChange = { checked ->
                                        enableOpenSunroofCurtainOnStart = checked
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .ENABLE_OPEN_SUNROOF_CURTAIN_ON_START
                                                                .key,
                                                        checked
                                                )
                                        }
                                },
                                customContent =
                                        if (enableOpenSunroofCurtainOnStart) {
                                                {
                                                        var showCurtainStartPicker by remember {
                                                                mutableStateOf(false)
                                                        }
                                                        var showCurtainEndPicker by remember {
                                                                mutableStateOf(false)
                                                        }
                                                        var expandedTemp by remember {
                                                                mutableStateOf(false)
                                                        }

                                                        val tempOptions =
                                                                mapOf(
                                                                        -1f to "已禁用",
                                                                        26f to "26°C",
                                                                        28f to "28°C",
                                                                        30f to "30°C",
                                                                        32f to "32°C",
                                                                        34f to "34°C",
                                                                        36f to "36°C"
                                                                )

                                                        if (showCurtainStartPicker) {
                                                                val timeSetListener =
                                                                        TimePickerDialog
                                                                                .OnTimeSetListener {
                                                                                        _,
                                                                                        hour,
                                                                                        minute ->
                                                                                        curtainStartHour =
                                                                                                hour
                                                                                        curtainStartMinute =
                                                                                                minute
                                                                                        prefs.edit {
                                                                                                putInt(
                                                                                                        SharedPreferencesKeys
                                                                                                                .OPEN_SUNROOF_CURTAIN_START_HOUR
                                                                                                                .key,
                                                                                                        hour
                                                                                                )
                                                                                                putInt(
                                                                                                        SharedPreferencesKeys
                                                                                                                .OPEN_SUNROOF_CURTAIN_START_MINUTE
                                                                                                                .key,
                                                                                                        minute
                                                                                                )
                                                                                        }
                                                                                        showCurtainStartPicker =
                                                                                                false
                                                                                }
                                                                TimePickerDialog(
                                                                                LocalContext
                                                                                        .current,
                                                                                timeSetListener,
                                                                                curtainStartHour,
                                                                                curtainStartMinute,
                                                                                true
                                                                        )
                                                                        .show()
                                                        }

                                                        if (showCurtainEndPicker) {
                                                                val timeSetListener =
                                                                        TimePickerDialog
                                                                                .OnTimeSetListener {
                                                                                        _,
                                                                                        hour,
                                                                                        minute ->
                                                                                        curtainEndHour =
                                                                                                hour
                                                                                        curtainEndMinute =
                                                                                                minute
                                                                                        prefs.edit {
                                                                                                putInt(
                                                                                                        SharedPreferencesKeys
                                                                                                                .OPEN_SUNROOF_CURTAIN_END_HOUR
                                                                                                                .key,
                                                                                                        hour
                                                                                                )
                                                                                                putInt(
                                                                                                        SharedPreferencesKeys
                                                                                                                .OPEN_SUNROOF_CURTAIN_END_MINUTE
                                                                                                                .key,
                                                                                                        minute
                                                                                                )
                                                                                        }
                                                                                        showCurtainEndPicker =
                                                                                                false
                                                                                }
                                                                TimePickerDialog(
                                                                                LocalContext
                                                                                        .current,
                                                                                timeSetListener,
                                                                                curtainEndHour,
                                                                                curtainEndMinute,
                                                                                true
                                                                        )
                                                                        .show()
                                                        }

                                                        Column(
                                                                verticalArrangement =
                                                                        Arrangement.spacedBy(12.dp)
                                                        ) {
                                                                HorizontalDivider(
                                                                        color = Color(0xFF3A3F47),
                                                                        thickness = 1.dp
                                                                )
                                                                Row(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth(),
                                                                        horizontalArrangement =
                                                                                Arrangement
                                                                                        .SpaceEvenly
                                                                ) {
                                                                        Box(
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                        1f
                                                                                                )
                                                                                                .clickable {
                                                                                                        showCurtainStartPicker =
                                                                                                                true
                                                                                                }
                                                                                                .background(
                                                                                                        Color(
                                                                                                                0xFF2A2F37
                                                                                                        ),
                                                                                                        RoundedCornerShape(
                                                                                                                8.dp
                                                                                                        )
                                                                                                )
                                                                                                .padding(
                                                                                                        16.dp
                                                                                                ),
                                                                                contentAlignment =
                                                                                        Alignment
                                                                                                .Center
                                                                        ) {
                                                                                Column(
                                                                                        horizontalAlignment =
                                                                                                Alignment
                                                                                                        .CenterHorizontally
                                                                                ) {
                                                                                        Text(
                                                                                                "开始",
                                                                                                color =
                                                                                                        Color.White,
                                                                                                fontSize =
                                                                                                        14.sp
                                                                                        )
                                                                                        Spacer(
                                                                                                modifier =
                                                                                                        Modifier.height(
                                                                                                                4.dp
                                                                                                        )
                                                                                        )
                                                                                        Text(
                                                                                                "${String.format("%02d", curtainStartHour)}:${String.format("%02d", curtainStartMinute)}",
                                                                                                color =
                                                                                                        Color(
                                                                                                                0xFF4A9EFF
                                                                                                        ),
                                                                                                fontSize =
                                                                                                        18.sp,
                                                                                                fontWeight =
                                                                                                        FontWeight
                                                                                                                .Medium
                                                                                        )
                                                                                }
                                                                        }
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.width(
                                                                                                12.dp
                                                                                        )
                                                                        )
                                                                        Box(
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                        1f
                                                                                                )
                                                                                                .clickable {
                                                                                                        showCurtainEndPicker =
                                                                                                                true
                                                                                                }
                                                                                                .background(
                                                                                                        Color(
                                                                                                                0xFF2A2F37
                                                                                                        ),
                                                                                                        RoundedCornerShape(
                                                                                                                8.dp
                                                                                                        )
                                                                                                )
                                                                                                .padding(
                                                                                                        16.dp
                                                                                                ),
                                                                                contentAlignment =
                                                                                        Alignment
                                                                                                .Center
                                                                        ) {
                                                                                Column(
                                                                                        horizontalAlignment =
                                                                                                Alignment
                                                                                                        .CenterHorizontally
                                                                                ) {
                                                                                        Text(
                                                                                                "结束",
                                                                                                color =
                                                                                                        Color.White,
                                                                                                fontSize =
                                                                                                        14.sp
                                                                                        )
                                                                                        Spacer(
                                                                                                modifier =
                                                                                                        Modifier.height(
                                                                                                                4.dp
                                                                                                        )
                                                                                        )
                                                                                        Text(
                                                                                                "${String.format("%02d", curtainEndHour)}:${String.format("%02d", curtainEndMinute)}",
                                                                                                color =
                                                                                                        Color(
                                                                                                                0xFF4A9EFF
                                                                                                        ),
                                                                                                fontSize =
                                                                                                        18.sp,
                                                                                                fontWeight =
                                                                                                        FontWeight
                                                                                                                .Medium
                                                                                        )
                                                                                }
                                                                        }
                                                                }

                                                                Row(
                                                                        verticalAlignment =
                                                                                Alignment
                                                                                        .CenterVertically
                                                                ) {
                                                                        Text(
                                                                                "最高温度：",
                                                                                color = Color.White,
                                                                                fontSize = 14.sp,
                                                                                modifier =
                                                                                        Modifier.padding(
                                                                                                end =
                                                                                                        8.dp
                                                                                        )
                                                                        )
                                                                        Box {
                                                                                Text(
                                                                                        text =
                                                                                                tempOptions[
                                                                                                        openSunroofCurtainMaxTemp]
                                                                                                        ?: "已禁用",
                                                                                        color =
                                                                                                Color(
                                                                                                        0xFF4A9EFF
                                                                                                ),
                                                                                        fontSize =
                                                                                                16.sp,
                                                                                        modifier =
                                                                                                Modifier.background(
                                                                                                                Color(
                                                                                                                        0xFF2A2F37
                                                                                                                ),
                                                                                                                RoundedCornerShape(
                                                                                                                        4.dp
                                                                                                                )
                                                                                                        )
                                                                                                        .padding(
                                                                                                                horizontal =
                                                                                                                        12.dp,
                                                                                                                vertical =
                                                                                                                        8.dp
                                                                                                        )
                                                                                                        .clickable {
                                                                                                                expandedTemp =
                                                                                                                        true
                                                                                                        }
                                                                                )
                                                                                DropdownMenu(
                                                                                        expanded =
                                                                                                expandedTemp,
                                                                                        onDismissRequest = {
                                                                                                expandedTemp =
                                                                                                        false
                                                                                        },
                                                                                        modifier =
                                                                                                Modifier.background(
                                                                                                        Color(
                                                                                                                0xFF2A2F37
                                                                                                        )
                                                                                                )
                                                                                ) {
                                                                                        tempOptions
                                                                                                .forEach {
                                                                                                        (
                                                                                                                value,
                                                                                                                label)
                                                                                                        ->
                                                                                                        DropdownMenuItem(
                                                                                                                text = {
                                                                                                                        Text(
                                                                                                                                label,
                                                                                                                                color =
                                                                                                                                        Color.White
                                                                                                                        )
                                                                                                                },
                                                                                                                onClick = {
                                                                                                                        openSunroofCurtainMaxTemp =
                                                                                                                                value
                                                                                                                        prefs
                                                                                                                                .edit {
                                                                                                                                        putFloat(
                                                                                                                                                SharedPreferencesKeys
                                                                                                                                                        .OPEN_SUNROOF_CURTAIN_MAX_TEMP
                                                                                                                                                        .key,
                                                                                                                                                value
                                                                                                                                        )
                                                                                                                                }
                                                                                                                        expandedTemp =
                                                                                                                                false
                                                                                                                }
                                                                                                        )
                                                                                                }
                                                                                }
                                                                        }
                                                                }
                                                        }
                                                }
                                        } else null
                        ),
                        SettingItem(
                                title = "保持禁用分心驾驶监控",
                                description = "关闭行车途中的分心提醒",
                                checked = disableMonitoring,
                                onCheckedChange = {
                                        disableMonitoring = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys.DISABLE_MONITORING
                                                                .key,
                                                        it
                                                )
                                        }
                                        ServiceManager.getInstance().setMonitoringEnabled(!it)
                                }
                        ),
                        SettingItem(
                                title = "启用底部快捷栏",
                                description =
                                        "创建固定底部快捷栏，包含空调与常用功能入口",
                                checked = enablePersistentBottomBar,
                                onCheckedChange = { checked ->
                                        if (checked && !Settings.canDrawOverlays(context)) {
                                                // Request overlay permission
                                                val intent =
                                                        Intent(
                                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                                Uri.parse(
                                                                        "package:${context.packageName}"
                                                                )
                                                        )
                                                context.startActivity(intent)
                                                android.widget.Toast.makeText(
                                                                context,
                                                                "请为底部快捷栏开启悬浮窗权限",
                                                                android.widget.Toast.LENGTH_LONG
                                                        )
                                                        .show()
                                                return@SettingItem
                                        }

                                        enablePersistentBottomBar = checked
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys.PERSISTENT_BOTTOM_BAR
                                                                .key,
                                                        checked
                                                )
                                        }
                                        val serviceIntent =
                                                Intent(
                                                        context,
                                                        br.com.redesurftank.havalshisuku.services
                                                                        .BottomBarService::class
                                                                .java
                                                )
                                        if (checked) {
                                                context.startService(serviceIntent)
                                                Thread {
                                                                br.com.redesurftank.havalshisuku
                                                                        .utils.ShizukuUtils
                                                                        .runCommandAndGetOutput(
                                                                                arrayOf(
                                                                                        "sh",
                                                                                        "-c",
                                                                                        "wm size reset"
                                                                                )
                                                                        )
                                                                val overscan =
                                                                        prefs.getInt(
                                                                                SharedPreferencesKeys
                                                                                        .PERSISTENT_BOTTOM_BAR_OVERSCAN
                                                                                        .key,
                                                                                60
                                                                        )
                                                                br.com.redesurftank.havalshisuku
                                                                        .utils.ShizukuUtils
                                                                        .runCommandAndGetOutput(
                                                                                arrayOf(
                                                                                        "sh",
                                                                                        "-c",
                                                                                        "wm overscan 0,0,0,$overscan"
                                                                                )
                                                                        )
                                                        }
                                                        .start()
                                        } else {
                                                context.stopService(serviceIntent)
                                                Thread {
                                                                br.com.redesurftank.havalshisuku
                                                                        .utils.ShizukuUtils
                                                                        .runCommandAndGetOutput(
                                                                                arrayOf(
                                                                                        "sh",
                                                                                        "-c",
                                                                                        "wm overscan 0,0,0,0"
                                                                                )
                                                                        )
                                                        }
                                                        .start()
                                        }
                                },
                                customContent =
                                        if (enablePersistentBottomBar) {
                                                {
                                                        Column(
                                                                modifier =
                                                                        Modifier.padding(top = 8.dp)
                                                        ) {
                                                                HorizontalDivider(
                                                                        color = Color(0xFF1D2430),
                                                                        thickness = 1.dp
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        12.dp
                                                                                )
                                                                )

                                                                // Auto-hide row
                                                                Row(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth(),
                                                                        horizontalArrangement =
                                                                                Arrangement
                                                                                        .SpaceBetween,
                                                                        verticalAlignment =
                                                                                Alignment
                                                                                        .CenterVertically
                                                                ) {
                                                                        Column(
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                1f
                                                                                        )
                                                                        ) {
                                                                                Text(
                                                                                        "自动隐藏底栏",
                                                                                        color =
                                                                                                Color.White,
                                                                                        fontSize =
                                                                                                16.sp
                                                                                )
                                                                                Text(
                                                                                        "30 秒无操作后隐藏",
                                                                                        color =
                                                                                                Color.Gray,
                                                                                        fontSize =
                                                                                                12.sp
                                                                                )
                                                                        }
                                                                        Switch(
                                                                                checked =
                                                                                        autoHideEnabled,
                                                                                onCheckedChange = {
                                                                                        autoHideEnabled =
                                                                                                it
                                                                                        prefs.edit()
                                                                                                .putBoolean(
                                                                                                        SharedPreferencesKeys
                                                                                                                .BOTTOM_BAR_AUTO_HIDE
                                                                                                                .key,
                                                                                                        it
                                                                                                )
                                                                                                .apply()
                                                                                        BottomBarState
                                                                                                .autoHideEnabled =
                                                                                                it
                                                                                },
                                                                                modifier =
                                                                                        Modifier.scale(
                                                                                                0.9f
                                                                                        ),
                                                                                colors =
                                                                                        SwitchDefaults
                                                                                                .colors(
                                                                                                        checkedThumbColor =
                                                                                                                br.com
                                                                                                                        .redesurftank
                                                                                                                        .havalshisuku
                                                                                                                        .ui
                                                                                                                        .components
                                                                                                                        .AppColors
                                                                                                                        .TextPrimary,
                                                                                                        checkedTrackColor =
                                                                                                                br.com
                                                                                                                        .redesurftank
                                                                                                                        .havalshisuku
                                                                                                                        .ui
                                                                                                                        .components
                                                                                                                        .AppColors
                                                                                                                        .Primary,
                                                                                                        uncheckedThumbColor =
                                                                                                                br.com
                                                                                                                        .redesurftank
                                                                                                                        .havalshisuku
                                                                                                                        .ui
                                                                                                                        .components
                                                                                                                        .AppColors
                                                                                                                        .TextSecondary,
                                                                                                        uncheckedTrackColor =
                                                                                                                br.com
                                                                                                                        .redesurftank
                                                                                                                        .havalshisuku
                                                                                                                        .ui
                                                                                                                        .components
                                                                                                                        .AppColors
                                                                                                                        .ButtonSecondary,
                                                                                                        uncheckedBorderColor =
                                                                                                                Color.Transparent,
                                                                                                        checkedBorderColor =
                                                                                                                Color.Transparent
                                                                                                )
                                                                        )
                                                                }

                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        12.dp
                                                                                )
                                                                )

                                                        }
                                                }
                                        } else null
                        ),
                        SettingItem(
                                title = "禁用 AVAS",
                                description = "低速车辆警示音系统",
                                checked = disableAvas,
                                onCheckedChange = {
                                        disableAvas = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys.DISABLE_AVAS.key,
                                                        it
                                                )
                                        }
                                        ServiceManager.getInstance().setAvasEnabled(!it)
                                }
                        ),
                        SettingItem(
                                title = "车辆静止时关闭 AVM",
                                description =
                                        "车辆静止时自动关闭 360° 全景影像",
                                checked = disableAvmCarStopped,
                                onCheckedChange = {
                                        disableAvmCarStopped = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .DISABLE_AVM_CAR_STOPPED
                                                                .key,
                                                        it
                                                )
                                        }
                                }
                        ),
                        SettingItem(
                                title = "开启空调时联动驾驶员座椅通风",
                                description =
                                        SharedPreferencesKeys.ENABLE_SEAT_VENTILATION_ON_AC_ON
                                                .description,
                                checked = enableSeatVentilationOnAcOn,
                                onCheckedChange = {
                                        enableSeatVentilationOnAcOn = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .ENABLE_SEAT_VENTILATION_ON_AC_ON
                                                                .key,
                                                        it
                                                )
                                        }
                                }
                        ),
                        SettingItem(
                                title = "熄火后关闭蓝牙",
                                description =
                                        SharedPreferencesKeys.DISABLE_BLUETOOTH_ON_POWER_OFF
                                                .description,
                                checked = disableBluetoothOnPowerOff,
                                onCheckedChange = {
                                        disableBluetoothOnPowerOff = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .DISABLE_BLUETOOTH_ON_POWER_OFF
                                                                .key,
                                                        it
                                                )
                                        }
                                }
                        ),
                        SettingItem(
                                title = "熄火后关闭热点",
                                description =
                                        SharedPreferencesKeys.DISABLE_HOTSPOT_ON_POWER_OFF
                                                .description,
                                checked = disableHotspotOnPowerOff,
                                onCheckedChange = {
                                        disableHotspotOnPowerOff = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .DISABLE_HOTSPOT_ON_POWER_OFF
                                                                .key,
                                                        it
                                                )
                                        }
                                }
                        ),
                        SettingItem(
                                title = "启用方向盘自定义按键",
                                description =
                                        SharedPreferencesKeys.ENABLE_STEERING_WHEEL_CUSTOM_BUTTONS
                                                .description,
                                checked = enableCustomSteeringWheelButtons,
                                onCheckedChange = {
                                        enableCustomSteeringWheelButtons = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .ENABLE_STEERING_WHEEL_CUSTOM_BUTTONS
                                                                .key,
                                                        it
                                                )
                                        }
                                        ServiceManager.getInstance()
                                                .ensureSteeringWheelButtonIntegration()
                                },
                                customContent =
                                        if (enableCustomSteeringWheelButtons) {
                                                {
                                                        Column(
                                                                verticalArrangement =
                                                                        Arrangement.spacedBy(12.dp)
                                                        ) {
                                                                HorizontalDivider(
                                                                        color = Color(0xFF3A3F47),
                                                                        thickness = 1.dp
                                                                )

                                                                Text(
                                                                        "Botão 1",
                                                                        color = Color.White,
                                                                        fontSize = 16.sp
                                                                )
                                                                SteeringActionPicker(
                                                                        prefs = prefs,
                                                                        label = "短按",
                                                                        actionPref = SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_1_ACTION,
                                                                        packagePref = SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_1,
                                                                        climatePref = SharedPreferencesKeys.STEERING_WHEEL_CLIMATE_COMMAND_BUTTON_1
                                                                )
                                                                SteeringActionPicker(
                                                                        prefs = prefs,
                                                                        label = "双击",
                                                                        actionPref = SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_1_ACTION_DOUBLE,
                                                                        packagePref = SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_1_DOUBLE,
                                                                        climatePref = SharedPreferencesKeys.STEERING_WHEEL_CLIMATE_COMMAND_BUTTON_1
                                                                )
                                                                SteeringActionPicker(
                                                                        prefs = prefs,
                                                                        label = "长按",
                                                                        actionPref = SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_1_ACTION_LONG,
                                                                        packagePref = SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_1_LONG,
                                                                        climatePref = SharedPreferencesKeys.STEERING_WHEEL_CLIMATE_COMMAND_BUTTON_1
                                                                )

                                                                HorizontalDivider(
                                                                        color = Color(0xFF3A3F47),
                                                                        thickness = 1.dp
                                                                )

                                                                Text(
                                                                        "Botão 2",
                                                                        color = Color.White,
                                                                        fontSize = 16.sp
                                                                )
                                                                SteeringActionPicker(
                                                                        prefs = prefs,
                                                                        label = "短按",
                                                                        actionPref = SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_2_ACTION,
                                                                        packagePref = SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_2,
                                                                        climatePref = SharedPreferencesKeys.STEERING_WHEEL_CLIMATE_COMMAND_BUTTON_2
                                                                )
                                                                SteeringActionPicker(
                                                                        prefs = prefs,
                                                                        label = "双击",
                                                                        actionPref = SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_2_ACTION_DOUBLE,
                                                                        packagePref = SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_2_DOUBLE,
                                                                        climatePref = SharedPreferencesKeys.STEERING_WHEEL_CLIMATE_COMMAND_BUTTON_2
                                                                )
                                                                SteeringActionPicker(
                                                                        prefs = prefs,
                                                                        label = "长按",
                                                                        actionPref = SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_2_ACTION_LONG,
                                                                        packagePref = SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_2_LONG,
                                                                        climatePref = SharedPreferencesKeys.STEERING_WHEEL_CLIMATE_COMMAND_BUTTON_2
                                                                )
                                                        }
                                                }
                                        } else null
                        ),
                        SettingItem(
                                title = "自动调节亮度",
                                description = "根据白天/夜晚自动调节屏幕亮度",
                                checked = enableAutoBrightness,
                                onCheckedChange = {
                                        enableAutoBrightness = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys.ENABLE_AUTO_BRIGHTNESS
                                                                .key,
                                                        it
                                                )
                                        }
                                        AutoBrightnessManager.getInstance().setEnabled(it)
                                },
                                customContent =
                                        if (enableAutoBrightness) {
                                                {
                                                        Column(
                                                                verticalArrangement =
                                                                        Arrangement.spacedBy(12.dp)
                                                        ) {
                                                                HorizontalDivider(
                                                                        color = Color(0xFF3A3F47),
                                                                        thickness = 1.dp
                                                                )

                                                                Row(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth(),
                                                                        horizontalArrangement =
                                                                                Arrangement
                                                                                        .SpaceEvenly
                                                                ) {
                                                                        // Início da noite
                                                                        Box(
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                        1f
                                                                                                )
                                                                                                .clickable {
                                                                                                        showStartPicker =
                                                                                                                true
                                                                                                }
                                                                                                .background(
                                                                                                        Color(
                                                                                                                0xFF2A2F37
                                                                                                        ),
                                                                                                        RoundedCornerShape(
                                                                                                                8.dp
                                                                                                        )
                                                                                                )
                                                                                                .padding(
                                                                                                        16.dp
                                                                                                ),
                                                                                contentAlignment =
                                                                                        Alignment
                                                                                                .Center
                                                                        ) {
                                                                                Column(
                                                                                        horizontalAlignment =
                                                                                                Alignment
                                                                                                        .CenterHorizontally
                                                                                ) {
                                                                                        Text(
                                                                                                "夜晚开始",
                                                                                                color =
                                                                                                        Color.White,
                                                                                                fontSize =
                                                                                                        14.sp
                                                                                        )
                                                                                        Spacer(
                                                                                                modifier =
                                                                                                        Modifier.height(
                                                                                                                4.dp
                                                                                                        )
                                                                                        )
                                                                                        Text(
                                                                                                "${String.format("%02d", nightStartHour)}:${String.format("%02d", nightStartMinute)}",
                                                                                                color =
                                                                                                        Color(
                                                                                                                0xFF4A9EFF
                                                                                                        ),
                                                                                                fontSize =
                                                                                                        18.sp,
                                                                                                fontWeight =
                                                                                                        FontWeight
                                                                                                                .Medium
                                                                                        )
                                                                                }
                                                                        }

                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.width(
                                                                                                12.dp
                                                                                        )
                                                                        )

                                                                        // Fim da noite
                                                                        Box(
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                        1f
                                                                                                )
                                                                                                .clickable {
                                                                                                        showEndPicker =
                                                                                                                true
                                                                                                }
                                                                                                .background(
                                                                                                        Color(
                                                                                                                0xFF2A2F37
                                                                                                        ),
                                                                                                        RoundedCornerShape(
                                                                                                                8.dp
                                                                                                        )
                                                                                                )
                                                                                                .padding(
                                                                                                        16.dp
                                                                                                ),
                                                                                contentAlignment =
                                                                                        Alignment
                                                                                                .Center
                                                                        ) {
                                                                                Column(
                                                                                        horizontalAlignment =
                                                                                                Alignment
                                                                                                        .CenterHorizontally
                                                                                ) {
                                                                                        Text(
                                                                                                "夜晚结束",
                                                                                                color =
                                                                                                        Color.White,
                                                                                                fontSize =
                                                                                                        14.sp
                                                                                        )
                                                                                        Spacer(
                                                                                                modifier =
                                                                                                        Modifier.height(
                                                                                                                4.dp
                                                                                                        )
                                                                                        )
                                                                                        Text(
                                                                                                "${String.format("%02d", nightEndHour)}:${String.format("%02d", nightEndMinute)}",
                                                                                                color =
                                                                                                        Color(
                                                                                                                0xFF4A9EFF
                                                                                                        ),
                                                                                                fontSize =
                                                                                                        18.sp,
                                                                                                fontWeight =
                                                                                                        FontWeight
                                                                                                                .Medium
                                                                                        )
                                                                                }
                                                                        }
                                                                }

                                                                // Slider para nível de brilho
                                                                // diurno
                                                                Column {
                                                                        Text(
                                                                                "白天亮度：$dayBrightnessLevel",
                                                                                color = Color.White,
                                                                                fontSize = 14.sp
                                                                        )
                                                                        Slider(
                                                                                value =
                                                                                        dayBrightnessLevel
                                                                                                .toFloat(),
                                                                                onValueChange = {
                                                                                        newValue ->
                                                                                        dayBrightnessLevel =
                                                                                                newValue.toInt()
                                                                                        prefs.edit {
                                                                                                putInt(
                                                                                                        SharedPreferencesKeys
                                                                                                                .AUTO_BRIGHTNESS_LEVEL_DAY
                                                                                                                .key,
                                                                                                        dayBrightnessLevel
                                                                                                )
                                                                                        }
                                                                                },
                                                                                valueRange =
                                                                                        1f..10f,
                                                                                steps = 9,
                                                                                colors =
                                                                                        SliderDefaults
                                                                                                .colors(
                                                                                                        thumbColor =
                                                                                                                AppColors
                                                                                                                        .Primary,
                                                                                                        activeTrackColor =
                                                                                                                AppColors
                                                                                                                        .Primary,
                                                                                                        inactiveTrackColor =
                                                                                                                Color(
                                                                                                                        0xFF2C3139
                                                                                                                ),
                                                                                                        activeTickColor =
                                                                                                                Color.Transparent,
                                                                                                        inactiveTickColor =
                                                                                                                Color.Transparent,
                                                                                                        disabledThumbColor =
                                                                                                                AppColors
                                                                                                                        .Primary,
                                                                                                        disabledActiveTrackColor =
                                                                                                                AppColors
                                                                                                                        .Primary,
                                                                                                        disabledInactiveTrackColor =
                                                                                                                Color(
                                                                                                                        0xFF2C3139
                                                                                                                )
                                                                                                )
                                                                        )
                                                                }

                                                                // Slider para nível de brilho
                                                                // noturno
                                                                Column {
                                                                        Text(
                                                                                "夜晚亮度：$nightBrightnessLevel",
                                                                                color = Color.White,
                                                                                fontSize = 14.sp
                                                                        )
                                                                        Slider(
                                                                                value =
                                                                                        nightBrightnessLevel
                                                                                                .toFloat(),
                                                                                onValueChange = {
                                                                                        newValue ->
                                                                                        nightBrightnessLevel =
                                                                                                newValue.toInt()
                                                                                        prefs.edit {
                                                                                                putInt(
                                                                                                        SharedPreferencesKeys
                                                                                                                .AUTO_BRIGHTNESS_LEVEL_NIGHT
                                                                                                                .key,
                                                                                                        nightBrightnessLevel
                                                                                                )
                                                                                        }
                                                                                },
                                                                                valueRange =
                                                                                        1f..10f,
                                                                                steps = 9,
                                                                                colors =
                                                                                        SliderDefaults
                                                                                                .colors(
                                                                                                        thumbColor =
                                                                                                                AppColors
                                                                                                                        .Primary,
                                                                                                        activeTrackColor =
                                                                                                                AppColors
                                                                                                                        .Primary,
                                                                                                        inactiveTrackColor =
                                                                                                                Color(
                                                                                                                        0xFF2C3139
                                                                                                                ),
                                                                                                        activeTickColor =
                                                                                                                Color.Transparent,
                                                                                                        inactiveTickColor =
                                                                                                                Color.Transparent,
                                                                                                        disabledThumbColor =
                                                                                                                AppColors
                                                                                                                        .Primary,
                                                                                                        disabledActiveTrackColor =
                                                                                                                AppColors
                                                                                                                        .Primary,
                                                                                                        disabledInactiveTrackColor =
                                                                                                                Color(
                                                                                                                        0xFF2C3139
                                                                                                                )
                                                                                                )
                                                                        )
                                                                }
                                                        }
                                                }
                                        } else null
                        ),
                        SettingItem(
                                title = "设置开机音量",
                                description = SharedPreferencesKeys.SET_STARTUP_VOLUME.description,
                                checked = setStartupVolume,
                                onCheckedChange = {
                                        setStartupVolume = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys.SET_STARTUP_VOLUME
                                                                .key,
                                                        it
                                                )
                                        }
                                },
                                sliderValue = volume,
                                sliderRange = 0..40,
                                onSliderChange = { newVolume ->
                                        volume = newVolume
                                        prefs.edit {
                                                putInt(
                                                        SharedPreferencesKeys.STARTUP_VOLUME.key,
                                                        newVolume
                                                )
                                        }
                                },
                                sliderLabel = "音量：$volume"
                        ),
                        SettingItem(
                                title = "车速校准",
                                description =
                                        "校准虚拟仪表上显示的车速",
                                checked = enableSpeedAdjustment,
                                onCheckedChange = {
                                        enableSpeedAdjustment = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .ENABLE_SPEED_ADJUSTMENT
                                                                .key,
                                                        it
                                                )
                                        }
                                },
                                sliderValue = speedAdjustmentOffset.toInt(),
                                sliderRange = -50..50,
                                sliderStep = 1,
                                onSliderChange = { newValue ->
                                        speedAdjustmentOffset = newValue.toFloat()
                                        prefs.edit {
                                                putFloat(
                                                        SharedPreferencesKeys
                                                                .SPEED_ADJUSTMENT_OFFSET
                                                                .key,
                                                        newValue.toFloat()
                                                )
                                        }
                                },
                                sliderLabel =
                                        "Ajuste: ${if (speedAdjustmentOffset > 0) "+" else ""}${speedAdjustmentOffset.toInt()}%"
                        )
                )
        )

        TwoColumnSettingsLayout(settingsList = settingsList)

        if (showStartPicker) {
                LaunchedEffect(Unit) {
                        val dialog =
                                TimePickerDialog(
                                        context,
                                        { _, h, m ->
                                                nightStartHour = h
                                                nightStartMinute = m
                                                prefs.edit {
                                                        putInt(
                                                                SharedPreferencesKeys
                                                                        .NIGHT_START_HOUR
                                                                        .key,
                                                                h
                                                        )
                                                        putInt(
                                                                SharedPreferencesKeys
                                                                        .NIGHT_START_MINUTE
                                                                        .key,
                                                                m
                                                        )
                                                }
                                                AutoBrightnessManager.getInstance().updateSchedule()
                                        },
                                        nightStartHour,
                                        nightStartMinute,
                                        true
                                )
                        dialog.setOnDismissListener { showStartPicker = false }
                        dialog.show()
                }
        }
        if (showEndPicker) {
                LaunchedEffect(Unit) {
                        val dialog =
                                TimePickerDialog(
                                        context,
                                        { _, h, m ->
                                                nightEndHour = h
                                                nightEndMinute = m
                                                prefs.edit {
                                                        putInt(
                                                                SharedPreferencesKeys.NIGHT_END_HOUR
                                                                        .key,
                                                                h
                                                        )
                                                        putInt(
                                                                SharedPreferencesKeys
                                                                        .NIGHT_END_MINUTE
                                                                        .key,
                                                                m
                                                        )
                                                }
                                                AutoBrightnessManager.getInstance().updateSchedule()
                                        },
                                        nightEndHour,
                                        nightEndMinute,
                                        true
                                )
                        dialog.setOnDismissListener { showEndPicker = false }
                        dialog.show()
                }
        }

        }

// Seletor reutilizável de ação do volante (curto/duplo/longo): dropdown de ação + campo de pacote
// quando a ação é "abrir app" + dropdown de comando do A/C quando a ação é "comando do ar".
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SteeringActionPicker(
        prefs: SharedPreferences,
        label: String,
        actionPref: SharedPreferencesKeys,
        packagePref: SharedPreferencesKeys,
        climatePref: SharedPreferencesKeys
) {
        var action by remember {
                mutableStateOf(
                        prefs.getString(actionPref.key, SteeringWheelCustomActionType.DEFAULT.key)
                                ?: SteeringWheelCustomActionType.DEFAULT.key
                )
        }
        var appPackage by remember { mutableStateOf(prefs.getString(packagePref.key, "") ?: "") }
        var climate by remember {
                mutableStateOf(
                        prefs.getString(
                                climatePref.key,
                                SteeringWheelClimateCommandType.TOGGLE_AC.key
                        )
                                ?: SteeringWheelClimateCommandType.TOGGLE_AC.key
                )
        }
        var expanded by remember { mutableStateOf(false) }
        var climateExpanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                TextField(
                        value =
                                SteeringWheelCustomActionType.entries
                                        .find { it.key == action }
                                        ?.description
                                        ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(label) },
                        trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        SteeringWheelCustomActionType.entries.forEach { type ->
                                DropdownMenuItem(
                                        text = { Text(type.description) },
                                        onClick = {
                                                action = type.key
                                                prefs.edit { putString(actionPref.key, type.key) }
                                                expanded = false
                                                ServiceManager.getInstance()
                                                        .ensureSteeringWheelButtonIntegration()
                                        }
                                )
                        }
                }
        }
        if (action == SteeringWheelCustomActionType.OPEN_APP.key) {
                TextField(
                        value = appPackage,
                        onValueChange = { newPkg ->
                                appPackage = newPkg
                                prefs.edit { putString(packagePref.key, newPkg) }
                        },
                        label = { Text("应用包名") },
                        colors =
                                TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF2A2F37),
                                        unfocusedContainerColor = Color(0xFF2A2F37),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color(0xFFB0B8C4),
                                        focusedIndicatorColor = Color(0xFF4A9EFF),
                                        unfocusedIndicatorColor = Color(0xFF3A3F47)
                                )
                )
        }
        if (action == SteeringWheelCustomActionType.CLIMATE_COMMAND.key) {
                ExposedDropdownMenuBox(
                        expanded = climateExpanded,
                        onExpandedChange = { climateExpanded = !climateExpanded }
                ) {
                        TextField(
                                value =
                                        SteeringWheelClimateCommandType.entries
                                                .find { it.key == climate }
                                                ?.description
                                                ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("空调指令") },
                                trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                                expanded = climateExpanded
                                        )
                                },
                                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                                expanded = climateExpanded,
                                onDismissRequest = { climateExpanded = false }
                        ) {
                                SteeringWheelClimateCommandType.entries.forEach { type ->
                                        DropdownMenuItem(
                                                text = { Text(type.description) },
                                                onClick = {
                                                        climate = type.key
                                                        prefs.edit {
                                                                putString(climatePref.key, type.key)
                                                        }
                                                        climateExpanded = false
                                                }
                                        )
                                }
                        }
                }
        }
}

@Composable
fun FridaHooksTab() {
        val prefs =
                App.getDeviceProtectedContext()
                        .getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
        var enableFridaHooks by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.ENABLE_FRIDA_HOOKS.key, false)
                )
        }
        var enableFridaHookSystemServer by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.ENABLE_FRIDA_HOOK_SYSTEM_SERVER.key,
                                false
                        )
                )
        }
        var showFridaDialog by remember { mutableStateOf(false) }
        var showManualDialog by remember { mutableStateOf(false) }
        LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                item {
                        SettingCard(
                                title = "启用 Frida 钩子",
                                description = SharedPreferencesKeys.ENABLE_FRIDA_HOOKS.description,
                                checked = enableFridaHooks,
                                onCheckedChange = { newValue ->
                                        if (!newValue) {
                                                enableFridaHooks = false
                                                prefs.edit {
                                                        putBoolean(
                                                                SharedPreferencesKeys
                                                                        .ENABLE_FRIDA_HOOKS
                                                                        .key,
                                                                false
                                                        )
                                                }
                                        } else {
                                                showFridaDialog = true
                                        }
                                }
                        )
                }
                item {
                        SettingCard(
                                title = "钩住 System Server",
                                description =
                                        SharedPreferencesKeys.ENABLE_FRIDA_HOOK_SYSTEM_SERVER
                                                .description,
                                checked = enableFridaHookSystemServer,
                                onCheckedChange = { newValue ->
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .ENABLE_FRIDA_HOOK_SYSTEM_SERVER
                                                                .key,
                                                        newValue
                                                )
                                        }
                                        enableFridaHookSystemServer = newValue
                                        if (newValue) FridaUtils.injectSystemServer()
                                }
                        )
                }
                item {
                        Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF13151A))
                        ) {
                                Button(
                                        onClick = { showManualDialog = true },
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFF4A9EFF)
                                                )
                                ) { Text("手动注入代码", color = Color.White) }
                        }
                }
        }
        if (showFridaDialog) {
                AlertDialog(
                        onDismissRequest = { showFridaDialog = false },
                        title = { Text("确认") },
                        text = {
                                Text(
                                        "启用 Frida 脚本属于实验功能，可能造成系统不稳定，请自行承担风险。如果不了解该功能，建议保持关闭。"
                                )
                        },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                showFridaDialog = false
                                                enableFridaHooks = true
                                                prefs.edit {
                                                        putBoolean(
                                                                SharedPreferencesKeys
                                                                        .ENABLE_FRIDA_HOOKS
                                                                        .key,
                                                                true
                                                        )
                                                }
                                                ServiceManager.getInstance().initializeFrida()
                                        }
                                ) { Text("启用") }
                        },
                        dismissButton = {
                                TextButton(onClick = { showFridaDialog = false }) {
                                        Text("取消")
                                }
                        }
                )
        }
        if (showManualDialog) {
                AlertDialog(
                        onDismissRequest = { showManualDialog = false },
                        title = { Text("手动钩子") },
                        text = {
                                val manuals =
                                        FridaUtils.ScriptProcess.entries.filter {
                                                it.injectMode == FridaUtils.InjectMode.MANUAL
                                        }
                                LazyColumn {
                                        items(manuals) { script ->
                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Text(script.process)
                                                        Spacer(Modifier.width(8.dp))
                                                        Button(
                                                                onClick = {
                                                                        FridaUtils.injectScript(
                                                                                script,
                                                                                false
                                                                        )
                                                                }
                                                        ) { Text("注入") }
                                                }
                                        }
                                }
                        },
                        confirmButton = {
                                TextButton(onClick = { showManualDialog = false }) {
                                        Text("关闭")
                                }
                        }
                )
        }
}

data class RevisionEntry(val km: Int, val date: Long)

fun getRevisionHistory(prefs: SharedPreferences): List<RevisionEntry> {
        val json = prefs.getString(SharedPreferencesKeys.INSTRUMENT_REVISION_HISTORY.key, "[]")
        return try {
                val type = object : TypeToken<List<RevisionEntry>>() {}.type
                Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
                Log.e("RevisionHistory", "Error parsing history: ${e.message}")
                emptyList()
        }
}

fun saveRevisionHistory(prefs: SharedPreferences, history: List<RevisionEntry>) {
        val json = Gson().toJson(history)
        prefs.edit { putString(SharedPreferencesKeys.INSTRUMENT_REVISION_HISTORY.key, json) }
}

@Composable
fun ThemeCard(
        theme: ThemeMetadata,
        isDownloaded: Boolean,
        isSelected: Boolean,
        hasUpdate: Boolean = false,
        isDownloading: Boolean,
        onAction: () -> Unit,
        onUpdate: () -> Unit = {},
        onDelete: (() -> Unit)? = null
) {
        val borderColor = if (isSelected) Color(0xFF4CAF50) else Color(0xFF1D2430)
        val borderThickness = if (isSelected) 2.dp else 1.dp

        Card(
                modifier =
                        Modifier.fillMaxWidth()
                                .clickable { onAction() }
                                .border(borderThickness, borderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13151A)),
                shape = RoundedCornerShape(12.dp)
        ) {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        // Thumbnail
                        Card(
                                modifier =
                                        Modifier.width(160.dp)
                                                .height(60.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2228))
                        ) {
                                val context = LocalContext.current
                                val model = remember(theme.thumbnailUrl) {
                                        if (theme.thumbnailUrl.isNotEmpty() && !theme.thumbnailUrl.startsWith("http") && !theme.thumbnailUrl.startsWith("/")) {
                                                context.resources.getIdentifier(theme.thumbnailUrl, "drawable", context.packageName).let { if (it != 0) it else theme.thumbnailUrl }
                                        } else {
                                                theme.thumbnailUrl
                                        }
                                }

                                AsyncImage(
                                        model = model,
                                                contentDescription = theme.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                placeholder =
                                                        painterResource(
                                                                android.R.drawable.ic_menu_gallery
                                                        ),
                                                error =
                                                        painterResource(
                                                                android.R
                                                                        .drawable
                                                                        .ic_menu_report_image
                                                        )
                                )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Info
                        Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                                text = theme.name,
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                        )
                                        if (isDownloaded && theme.name != "Default") {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                        color =
                                                                Color(0xFF4CAF50)
                                                                        .copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(4.dp)
                                                ) {
                                                        Text(
                                                                "已安装",
                                                                color = Color(0xFF4CAF50),
                                                                fontSize = 10.sp,
                                                                modifier =
                                                                        Modifier.padding(
                                                                                horizontal = 6.dp,
                                                                                vertical = 2.dp
                                                                        ),
                                                                fontWeight = FontWeight.Bold
                                                        )
                                                }
                                        }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                        text = theme.description,
                                        color = Color(0xFFB0B8C4),
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                                imageVector = Icons.Default.Style,
                                                contentDescription = null,
                                                tint = Color(0xFF4A9EFF),
                                                modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                                text = "v${theme.version}",
                                                color = Color(0xFF4A9EFF),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Action
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
                                if (isDownloading) {
                                        CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = AppColors.Primary,
                                                strokeWidth = 2.dp
                                        )
                                } else if (hasUpdate) {
                                        IconButton(onClick = onUpdate) {
                                                Icon(
                                                        imageVector = Icons.Default.SystemUpdate,
                                                        contentDescription = "更新",
                                                        tint = Color(0xFF4A9EFF),
                                                        modifier = Modifier.size(24.dp)
                                                )
                                        }
                                } else if (isSelected) {
                                        Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "已选择",
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(28.dp)
                                        )
                                } else if (!isDownloaded) {
                                        Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "下载",
                                                tint = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(24.dp)
                                        )
                                }
                        }

                        if (isDownloaded && onDelete != null && theme.name != "Default") {
                                IconButton(onClick = onDelete) {
                                        Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "删除",
                                                tint = Color.Red.copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp)
                                        )
                                }
                        }
                }
        }
}

@Composable
fun TelasTab() {

        val context = LocalContext.current
        val prefs =
                App.getDeviceProtectedContext()
                        .getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
        val scope = rememberCoroutineScope()

        // Base properties
        var enableProjector by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.ENABLE_INSTRUMENT_PROJECTOR.key,
                                false
                        )
                )
        }
        var enableOdometerAndRevision by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.ENABLE_INSTRUMENT_ODOMETER_AND_REVISION.key,
                                true
                        )
                )
        }
        var enableCustomIntegration by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.ENABLE_INSTRUMENT_CUSTOM_MEDIA_INTEGRATION
                                        .key,
                                false
                        )
                )
        }
        var enableMask by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.ENABLE_VIRTUAL_CLUSTER.key, false)
                )
        }
        var enableCustomMenu by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.ENABLE_CUSTOM_MENU.key, false)
                )
        }
        var allClusterFunctionsEnabled by remember {
                mutableStateOf(enableProjector || enableCustomIntegration || enableCustomMenu)
        }
        var clusterFuelDisplayUnit by remember {
                mutableStateOf(
                        prefs.getString(
                                SharedPreferencesKeys.CLUSTER_FUEL_DISPLAY_UNIT.key,
                                "liters"
                        ) ?: "liters"
                )
        }

        // Revision History States
        var revisionHistory by remember { mutableStateOf(getRevisionHistory(prefs)) }
        var showRegisterDialog by remember { mutableStateOf(false) }
        var expandedHistory by remember { mutableStateOf(false) }
        var tempKm by remember { mutableStateOf("") }
        var tempDate by remember { mutableLongStateOf(0L) }
        var showDatePickerForRegister by remember { mutableStateOf(false) }

        // Virtual Cluster States
        var selectedTheme by remember {
                mutableStateOf(
                        prefs.getString(SharedPreferencesKeys.VIRTUAL_CLUSTER_THEME.key, "Default")
                                ?: "Default"
                )
        }
        var alwaysUseThemeDimensions by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.ALWAYS_USE_THEME_DIMENSIONS.key,
                                true
                        )
                )
        }
        var defaultApp by remember {
                mutableStateOf(
                        prefs.getString(SharedPreferencesKeys.DEFAULT_DISPLAY_APP_PACKAGE.key, "")
                                ?: ""
                )
        }
        var appExpanded by remember { mutableStateOf(false) }
        var themeExpanded by remember { mutableStateOf(false) }
        var configs by remember {
                mutableStateOf(
                        br.com.redesurftank.havalshisuku.managers.DisplayAppLauncher.getAllConfigs()
                )
        }

        // GitHub Themes States
        var githubThemes by remember { mutableStateOf<List<ThemeMetadata>>(emptyList()) }
        var localThemes by remember {
                mutableStateOf(ThemeManager.getInstance(context).getLocalThemes())
        }
        var isFetchingThemes by remember { mutableStateOf(false) }
        var downloadingThemeName by remember { mutableStateOf<String?>(null) }
        var isThemesExpanded by remember { mutableStateOf(true) }

        // Date formatter
        val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

        // Auto-calculate next revision
        val latestRevision = revisionHistory.maxByOrNull { it.km }
        val nextKm = latestRevision?.let { it.km + 12000 } ?: 0
        val nextDate =
                latestRevision?.let {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = it.date
                        cal.add(Calendar.YEAR, 1)
                        cal.timeInMillis
                }
                        ?: 0L

        // Sync calculated revision to prefs for display in projector
        LaunchedEffect(nextKm, nextDate) {
                prefs.edit {
                        putInt(SharedPreferencesKeys.INSTRUMENT_REVISION_KM.key, nextKm)
                        putLong(SharedPreferencesKeys.INSTRUMENT_REVISION_NEXT_DATE.key, nextDate)
                }
        }

        // Periodic app config update
        LaunchedEffect(Unit) {
                while (true) {
                        configs =
                                br.com.redesurftank.havalshisuku.managers.DisplayAppLauncher
                                        .getAllConfigs()
                        kotlinx.coroutines.delay(5000)
                }
        }

        // Refresh local themes on start just in case, and fetch from GitHub
        LaunchedEffect(Unit) {
                localThemes = ThemeManager.getInstance(context).getLocalThemes()
                if (githubThemes.isEmpty()) {
                        isFetchingThemes = true
                        try {
                                githubThemes =
                                        ThemeManager.getInstance(context)
                                                .fetchThemesFromGithub(ThemeManager.THEME_REPO_URL)
                        } catch (e: Exception) {
                                Log.e("TelasTab", "Error fetching themes", e)
                        } finally {
                                isFetchingThemes = false
                        }
                }
        }

        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                // MASTER TOGGLE CARD - Consolidates Projector, Media Integration and Custom Menu
                StyledCard(modifier = Modifier.padding(horizontal = 8.dp)) {
                        Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                        "启用仪表功能",
                                                        color = Color.White,
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                        "启用数据投影、媒体整合与自定义菜单",
                                                        color = Color(0xFFB0B8C4),
                                                        fontSize = 14.sp
                                                )
                                        }
                                        Switch(
                                                checked = allClusterFunctionsEnabled,
                                                onCheckedChange = {
                                                        allClusterFunctionsEnabled = it
                                                        enableProjector = it
                                                        enableCustomIntegration = it
                                                        enableCustomMenu = it

                                                        prefs.edit {
                                                                putBoolean(
                                                                        SharedPreferencesKeys
                                                                                .ENABLE_INSTRUMENT_PROJECTOR
                                                                                .key,
                                                                        it
                                                                )
                                                                putBoolean(
                                                                        SharedPreferencesKeys
                                                                                .ENABLE_INSTRUMENT_CUSTOM_MEDIA_INTEGRATION
                                                                                .key,
                                                                        it
                                                                )
                                                                putBoolean(
                                                                        SharedPreferencesKeys
                                                                                .ENABLE_CUSTOM_MENU
                                                                                .key,
                                                                        it
                                                                )
                                                        }

                                                        if (!it) {
                                                                enableOdometerAndRevision = false
                                                                prefs.edit {
                                                                        putBoolean(
                                                                                SharedPreferencesKeys
                                                                                        .ENABLE_INSTRUMENT_ODOMETER_AND_REVISION
                                                                                        .key,
                                                                                false
                                                                        )
                                                                }
                                                                enableMask = false
                                                                prefs.edit {
                                                                        putBoolean(
                                                                                SharedPreferencesKeys
                                                                                        .ENABLE_VIRTUAL_CLUSTER
                                                                                        .key,
                                                                                false
                                                                        )
                                                                }
                                                        }

                                                        try {
                                                                ServiceManager.getInstance()
                                                                        .ensureSystemApps()
                                                                if (it && enableCustomIntegration) {
                                                                        ServiceManager.getInstance()
                                                                                .startClusterHeartbeat()
                                                                }
                                                        } catch (e: Exception) {
                                                                Log.e(
                                                                        "TelasTab",
                                                                        "更新仪表功能失败：${e.message}"
                                                                )
                                                        }
                                                },
                                                modifier = Modifier.scale(0.9f),
                                                colors =
                                                        SwitchDefaults.colors(
                                                                checkedThumbColor =
                                                                        br.com.redesurftank
                                                                                .havalshisuku.ui
                                                                                .components
                                                                                .AppColors
                                                                                .TextPrimary,
                                                                checkedTrackColor =
                                                                        br.com.redesurftank
                                                                                .havalshisuku.ui
                                                                                .components
                                                                                .AppColors.Primary,
                                                                uncheckedThumbColor =
                                                                        br.com.redesurftank
                                                                                .havalshisuku.ui
                                                                                .components
                                                                                .AppColors
                                                                                .TextSecondary,
                                                                uncheckedTrackColor =
                                                                        br.com.redesurftank
                                                                                .havalshisuku.ui
                                                                                .components
                                                                                .AppColors
                                                                                .ButtonSecondary,
                                                                uncheckedBorderColor =
                                                                        Color.Transparent,
                                                                checkedBorderColor =
                                                                        Color.Transparent
                                                        )
                                        )
                                }

                                // Image 4 Example
                                val image4 = remember {
                                        try {
                                                // NOTE: This is a hardcoded absolute path that may
                                                // not exist on your
                                                // machine.
                                                // To fix this, add your image to 'res/drawable' and
                                                // use:
                                                // BitmapFactory.decodeResource(context.resources,
                                                // R.drawable.your_image)
                                                BitmapFactory.decodeFile(
                                                                "C:\\Users\\marce\\.gemini\\antigravity\\brain\\6ffae6a8-c34c-41a2-8637-3fbac551d5a1\\media__1773951437085.png"
                                                        )
                                                        ?.asImageBitmap()
                                        } catch (e: Exception) {
                                                null
                                        }
                                }
                                if (image4 != null) {
                                        Image(
                                                bitmap = image4,
                                                contentDescription =
                                                        "仪表功能示例",
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .heightIn(max = 160.dp)
                                                                .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Fit
                                        )
                                }
                        }
                }

                val personalizationAlpha = if (allClusterFunctionsEnabled) 1f else 0.4f
                StyledCard(
                        modifier = Modifier.padding(horizontal = 8.dp).alpha(personalizationAlpha)
                ) {
                        Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                                Text(
                                        "个性化",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                )
                                Text(
                                        "仪表功能启用时使用的视觉设置",
                                        color = Color(0xFFB0B8C4),
                                        fontSize = 14.sp
                                )

                                HorizontalDivider(color = Color(0xFF3A3F47), thickness = 1.dp)

                                Text(
                                        "燃油显示单位",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                )

                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                        listOf("liters" to "升", "percent" to "百分比")
                                                .forEach { (value, label) ->
                                                        Row(
                                                                modifier =
                                                                        Modifier.weight(1f)
                                                                                .clip(
                                                                                        RoundedCornerShape(
                                                                                                12.dp
                                                                                        )
                                                                                )
                                                                                .background(
                                                                                        if (clusterFuelDisplayUnit ==
                                                                                                        value
                                                                                        )
                                                                                                Color(0xFF2563EB)
                                                                                                        .copy(
                                                                                                                alpha =
                                                                                                                        0.25f
                                                                                                        )
                                                                                        else Color(
                                                                                                0xFF1F2430
                                                                                        )
                                                                                )
                                                                                .clickable(
                                                                                        enabled =
                                                                                                allClusterFunctionsEnabled
                                                                                ) {
                                                                                        clusterFuelDisplayUnit =
                                                                                                value
                                                                                        prefs.edit {
                                                                                                putString(
                                                                                                        SharedPreferencesKeys
                                                                                                                .CLUSTER_FUEL_DISPLAY_UNIT
                                                                                                                .key,
                                                                                                        value
                                                                                                )
                                                                                        }
                                                                                        Log.d(
                                                                                                "TelasTab",
                                                                                                "[HavalDev] Cluster fuel display unit set to $value"
                                                                                        )
                                                                                }
                                                                                .padding(
                                                                                        horizontal =
                                                                                                12.dp,
                                                                                        vertical =
                                                                                                10.dp
                                                                                ),
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically
                                                        ) {
                                                                RadioButton(
                                                                        selected =
                                                                                clusterFuelDisplayUnit ==
                                                                                        value,
                                                                        enabled =
                                                                                allClusterFunctionsEnabled,
                                                                        onClick = {
                                                                                clusterFuelDisplayUnit =
                                                                                        value
                                                                                prefs.edit {
                                                                                        putString(
                                                                                                SharedPreferencesKeys
                                                                                                        .CLUSTER_FUEL_DISPLAY_UNIT
                                                                                                        .key,
                                                                                                value
                                                                                        )
                                                                                }
                                                                                Log.d(
                                                                                        "TelasTab",
                                                                                        "[HavalDev] Cluster fuel display unit set to $value"
                                                                                )
                                                                        }
                                                                )
                                                                Text(
                                                                        label,
                                                                        color = Color.White,
                                                                        fontSize = 15.sp,
                                                                        fontWeight =
                                                                                FontWeight.Medium
                                                                )
                                                        }
                                                }
                                }
                        }
                }

                // VIRTUAL CLUSTER CARD
                val clusterAlpha = if (allClusterFunctionsEnabled) 1f else 0.4f
                StyledCard(modifier = Modifier.padding(horizontal = 8.dp).alpha(clusterAlpha)) {
                        Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                        "虚拟仪表（空调控制）",
                                                        color = Color.White,
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                        SharedPreferencesKeys.ENABLE_VIRTUAL_CLUSTER
                                                                .description,
                                                        color = Color(0xFFB0B8C4),
                                                        fontSize = 14.sp
                                                )
                                        }
                                        Switch(
                                                checked = enableMask,
                                                enabled = allClusterFunctionsEnabled,
                                                onCheckedChange = {
                                                        enableMask = it
                                                        prefs.edit {
                                                                putBoolean(
                                                                        SharedPreferencesKeys
                                                                                .ENABLE_VIRTUAL_CLUSTER
                                                                                .key,
                                                                        it
                                                                )
                                                                if (it) {
                                                                        putBoolean(
                                                                                SharedPreferencesKeys
                                                                                        .ALWAYS_USE_THEME_DIMENSIONS
                                                                                        .key,
                                                                                true
                                                                        )
                                                                        alwaysUseThemeDimensions =
                                                                                true
                                                                }
                                                        }
                                                },
                                                modifier = Modifier.scale(0.9f),
                                                colors =
                                                        SwitchDefaults.colors(
                                                                checkedThumbColor =
                                                                        br.com.redesurftank
                                                                                .havalshisuku.ui
                                                                                .components
                                                                                .AppColors
                                                                                .TextPrimary,
                                                                checkedTrackColor =
                                                                        br.com.redesurftank
                                                                                .havalshisuku.ui
                                                                                .components
                                                                                .AppColors.Primary,
                                                                uncheckedThumbColor =
                                                                        br.com.redesurftank
                                                                                .havalshisuku.ui
                                                                                .components
                                                                                .AppColors
                                                                                .TextSecondary,
                                                                uncheckedTrackColor =
                                                                        br.com.redesurftank
                                                                                .havalshisuku.ui
                                                                                .components
                                                                                .AppColors
                                                                                .ButtonSecondary,
                                                                uncheckedBorderColor =
                                                                        Color.Transparent,
                                                                checkedBorderColor =
                                                                        Color.Transparent
                                                        )
                                        )
                                }

                                // Image 5 Example
                                if (enableMask || !allClusterFunctionsEnabled) {
                                        val image5 = remember {
                                                try {
                                                        // NOTE: This is a hardcoded absolute path
                                                        // that may not exist on your
                                                        // machine.
                                                        // To fix this, add your image to
                                                        // 'res/drawable' and use:
                                                        // BitmapFactory.decodeResource(context.resources,
                                                        // R.drawable.your_image)
                                                        BitmapFactory.decodeFile(
                                                                        "C:\\Users\\marce\\.gemini\\antigravity\\brain\\6ffae6a8-c34c-41a2-8637-3fbac551d5a1\\media__1773951531439.png"
                                                                )
                                                                ?.asImageBitmap()
                                                } catch (e: Exception) {
                                                        null
                                                }
                                        }
                                        if (image5 != null) {
                                                Image(
                                                        bitmap = image5,
                                                        contentDescription =
                                                                "虚拟仪表示例",
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .heightIn(max = 140.dp)
                                                                        .clip(
                                                                                RoundedCornerShape(
                                                                                        8.dp
                                                                                )
                                                                        ),
                                                        contentScale = ContentScale.Fit
                                                )
                                        }
                                }

                                if (enableMask && allClusterFunctionsEnabled) {
                                        HorizontalDivider(
                                                color = Color(0xFF3A3F47),
                                                thickness = 1.dp
                                        )

                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {

                                                // Theme Dimensions Override Switch
                                                Row(
                                                        modifier = Modifier.weight(1f),
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Column(
                                                                modifier = Modifier.weight(1f),
                                                                horizontalAlignment =
                                                                        Alignment.Start
                                                        ) {
                                                                Text(
                                                                        "主题尺寸",
                                                                        color = Color.White,
                                                                        fontSize = 14.sp,
                                                                        fontWeight =
                                                                                FontWeight.Medium
                                                                )
                                                                Text(
                                                                        "应用始终使用主题尺寸",
                                                                        color = Color(0xFFB0B8C4),
                                                                        fontSize = 10.sp,
                                                                        lineHeight = 12.sp
                                                                )
                                                        }

                                                        Switch(
                                                                checked = alwaysUseThemeDimensions,
                                                                enabled =
                                                                        enableMask &&
                                                                                allClusterFunctionsEnabled,
                                                                onCheckedChange = {
                                                                        alwaysUseThemeDimensions =
                                                                                it
                                                                        prefs.edit {
                                                                                putBoolean(
                                                                                        SharedPreferencesKeys
                                                                                                .ALWAYS_USE_THEME_DIMENSIONS
                                                                                                .key,
                                                                                        it
                                                                                )
                                                                        }
                                                                },
                                                                modifier = Modifier.scale(0.8f),
                                                                colors =
                                                                        SwitchDefaults.colors(
                                                                                checkedThumbColor =
                                                                                        br.com
                                                                                                .redesurftank
                                                                                                .havalshisuku
                                                                                                .ui
                                                                                                .components
                                                                                                .AppColors
                                                                                                .TextPrimary,
                                                                                checkedTrackColor =
                                                                                        br.com
                                                                                                .redesurftank
                                                                                                .havalshisuku
                                                                                                .ui
                                                                                                .components
                                                                                                .AppColors
                                                                                                .Primary,
                                                                                uncheckedThumbColor =
                                                                                        br.com
                                                                                                .redesurftank
                                                                                                .havalshisuku
                                                                                                .ui
                                                                                                .components
                                                                                                .AppColors
                                                                                                .TextSecondary,
                                                                                uncheckedTrackColor =
                                                                                        br.com
                                                                                                .redesurftank
                                                                                                .havalshisuku
                                                                                                .ui
                                                                                                .components
                                                                                                .AppColors
                                                                                                .ButtonSecondary,
                                                                                uncheckedBorderColor =
                                                                                        Color.Transparent,
                                                                                checkedBorderColor =
                                                                                        Color.Transparent
                                                                        )
                                                        )
                                                }

                                                Spacer(modifier = Modifier.width(16.dp))

                                                // Default App Selection
                                                Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                                "默认应用",
                                                                color = Color.White,
                                                                fontSize = 16.sp,
                                                                fontWeight = FontWeight.Medium
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        val pm = context.packageManager
                                                        val selectedAppName =
                                                                if (defaultApp.isEmpty()) "Nenhum"
                                                                else {
                                                                        try {
                                                                                pm.getApplicationInfo(
                                                                                                defaultApp,
                                                                                                0
                                                                                        )
                                                                                        .let {
                                                                                                pm.getApplicationLabel(
                                                                                                                it
                                                                                                        )
                                                                                                        .toString()
                                                                                        }
                                                                        } catch (_: Exception) {
                                                                                defaultApp
                                                                        }
                                                                }

                                                        Box {
                                                                OutlinedButton(
                                                                        onClick = {
                                                                                appExpanded = true
                                                                        },
                                                                        enabled =
                                                                                allClusterFunctionsEnabled,
                                                                        modifier =
                                                                                Modifier.fillMaxWidth(),
                                                                        colors =
                                                                                ButtonDefaults
                                                                                        .outlinedButtonColors(
                                                                                                contentColor =
                                                                                                        Color.White
                                                                                        ),
                                                                        border =
                                                                                BorderStroke(
                                                                                        1.dp,
                                                                                        Color(
                                                                                                0xFF3A3F47
                                                                                        )
                                                                                ),
                                                                        shape =
                                                                                RoundedCornerShape(
                                                                                        8.dp
                                                                                ),
                                                                        contentPadding =
                                                                                PaddingValues(
                                                                                        horizontal =
                                                                                                12.dp,
                                                                                        vertical =
                                                                                                8.dp
                                                                                )
                                                                ) {
                                                                        Row(
                                                                                modifier =
                                                                                        Modifier.fillMaxWidth(),
                                                                                horizontalArrangement =
                                                                                        Arrangement
                                                                                                .SpaceBetween,
                                                                                verticalAlignment =
                                                                                        Alignment
                                                                                                .CenterVertically
                                                                        ) {
                                                                                Text(
                                                                                        selectedAppName,
                                                                                        fontSize =
                                                                                                14.sp,
                                                                                        maxLines = 1
                                                                                )
                                                                                Icon(
                                                                                        Icons.Default
                                                                                                .Settings,
                                                                                        contentDescription =
                                                                                                null,
                                                                                        modifier =
                                                                                                Modifier.size(
                                                                                                        16.dp
                                                                                                )
                                                                                )
                                                                        }
                                                                }

                                                                DropdownMenu(
                                                                        expanded =
                                                                                appExpanded &&
                                                                                        allClusterFunctionsEnabled,
                                                                        onDismissRequest = {
                                                                                appExpanded = false
                                                                        },
                                                                        modifier =
                                                                                Modifier.background(
                                                                                                Color(
                                                                                                        0xFF1E2228
                                                                                                )
                                                                                        )
                                                                                        .border(
                                                                                                1.dp,
                                                                                                Color(
                                                                                                        0xFF3A3F47
                                                                                                )
                                                                                        )
                                                                ) {
                                                                        DropdownMenuItem(
                                                                                text = {
                                                                                        Text(
                                                                                                "Nenhum",
                                                                                                color =
                                                                                                        Color.White
                                                                                        )
                                                                                },
                                                                                onClick = {
                                                                                        defaultApp =
                                                                                                ""
                                                                                        prefs.edit {
                                                                                                putString(
                                                                                                        SharedPreferencesKeys
                                                                                                                .DEFAULT_DISPLAY_APP_PACKAGE
                                                                                                                .key,
                                                                                                        ""
                                                                                                )
                                                                                        }
                                                                                        appExpanded =
                                                                                                false
                                                                                }
                                                                        )
                                                                        configs.forEach { config ->
                                                                                val name =
                                                                                        try {
                                                                                                pm.getApplicationInfo(
                                                                                                                config.packageName,
                                                                                                                0
                                                                                                        )
                                                                                                        .let {
                                                                                                                pm.getApplicationLabel(
                                                                                                                                it
                                                                                                                        )
                                                                                                                        .toString()
                                                                                                        }
                                                                                        } catch (
                                                                                                _:
                                                                                                        Exception) {
                                                                                                config.packageName
                                                                                        }

                                                                                DropdownMenuItem(
                                                                                        text = {
                                                                                                Text(
                                                                                                        name,
                                                                                                        color =
                                                                                                                Color.White
                                                                                                )
                                                                                        },
                                                                                        onClick = {
                                                                                                defaultApp =
                                                                                                        config.packageName
                                                                                                prefs
                                                                                                        .edit {
                                                                                                                putString(
                                                                                                                        SharedPreferencesKeys
                                                                                                                                .DEFAULT_DISPLAY_APP_PACKAGE
                                                                                                                                .key,
                                                                                                                        config.packageName
                                                                                                                )
                                                                                                        }
                                                                                                appExpanded =
                                                                                                        false
                                                                                        }
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                }
                                        }

                                        HorizontalDivider(
                                                color = Color(0xFF3A3F47),
                                                thickness = 1.dp
                                        )

                                        // GITHUB THEMES SECTION
                                        Column {
                                                Row(
                                                        modifier =
                                                                Modifier.fillMaxWidth().clickable {
                                                                        isThemesExpanded =
                                                                                !isThemesExpanded
                                                                },
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween,
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                        "可用主题",
                                                                        color = Color.White,
                                                                        fontSize = 16.sp,
                                                                        fontWeight =
                                                                                FontWeight.Medium
                                                                )
                                                                Text(
                                                                        "用新主题个性化你的仪表",
                                                                        color = Color(0xFFB0B8C4),
                                                                        fontSize = 14.sp
                                                                )
                                                        }
                                                        Row(
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically
                                                        ) {
                                                                IconButton(
                                                                        onClick = {
                                                                                isFetchingThemes =
                                                                                        true
                                                                                scope.launch {
                                                                                        try {
                                                                                                localThemes =
                                                                                                        ThemeManager
                                                                                                                .getInstance(
                                                                                                                        context
                                                                                                                )
                                                                                                                .getLocalThemes()
                                                                                                githubThemes =
                                                                                                        ThemeManager
                                                                                                                .getInstance(
                                                                                                                        context
                                                                                                                )
                                                                                                                .fetchThemesFromGithub(
                                                                                                                        ThemeManager
                                                                                                                                .THEME_REPO_URL
                                                                                                                )
                                                                                        } catch (
                                                                                                e:
                                                                                                        Exception) {
                                                                                                Log.e(
                                                                                                        "TelasTab",
                                                                                                        "Error refreshing themes",
                                                                                                        e
                                                                                                )
                                                                                        } finally {
                                                                                                isFetchingThemes =
                                                                                                        false
                                                                                        }
                                                                                }
                                                                        }
                                                                ) {
                                                                        Icon(
                                                                                imageVector =
                                                                                        Icons.Default
                                                                                                .Refresh,
                                                                                contentDescription =
                                                                                        "更新",
                                                                                tint =
                                                                                        if (isFetchingThemes
                                                                                        )
                                                                                                AppColors
                                                                                                        .Primary
                                                                                        else
                                                                                                Color.White
                                                                        )
                                                                }
                                                                IconButton(
                                                                        onClick = {
                                                                                isThemesExpanded =
                                                                                        !isThemesExpanded
                                                                        }
                                                                ) {
                                                                        Icon(
                                                                                imageVector =
                                                                                        if (isThemesExpanded
                                                                                        )
                                                                                                Icons.Default
                                                                                                        .ExpandLess
                                                                                        else
                                                                                                Icons.Default
                                                                                                        .ExpandMore,
                                                                                contentDescription =
                                                                                        "Expandir/Recolher",
                                                                                tint = Color.White
                                                                        )
                                                                }
                                                        }
                                                }

                                                val basicoTheme = remember {
                                                        ThemeMetadata(
                                                                name = "Default",
                                                                description =
                                                                        "带全新 Sport 风格的主主题。",
                                                                version = "1.0.0",
                                                                thumbnailUrl = "thumb_default",
                                                                isLocal = true,
                                                                isDownloaded = true
                                                        )
                                                }

                                                val allDisplayThemes =
                                                        remember(githubThemes, localThemes) {
                                                                val list =
                                                                        mutableListOf<
                                                                                ThemeMetadata>()
                                                                list.add(basicoTheme)

                                                                // 1. Add all local themes (except
                                                                // "Default" and "Básico")
                                                                localThemes.forEach { local ->
                                                                        if (local.name != "Default"
                                                                        ) {
                                                                                // Look for a newer
                                                                                // version in
                                                                                // githubThemes
                                                                                val remote =
                                                                                        githubThemes
                                                                                                .find {
                                                                                                        it.name ==
                                                                                                                local.name
                                                                                                }
                                                                                if (remote != null
                                                                                ) {
                                                                                        list.add(
                                                                                                remote.copy(
                                                                                                        isDownloaded =
                                                                                                                true
                                                                                                )
                                                                                        )
                                                                                } else {
                                                                                        list.add(
                                                                                                local
                                                                                        )
                                                                                }
                                                                        }
                                                                }

                                                                // 2. Add GitHub themes that are NOT
                                                                // local
                                                                githubThemes.forEach { github ->
                                                                        if (github.name != "Default" &&
                                                                                        list.none {
                                                                                                it.name ==
                                                                                                        github.name
                                                                                        }
                                                                        ) {
                                                                                list.add(
                                                                                        github.copy(
                                                                                                isDownloaded =
                                                                                                        false
                                                                                        )
                                                                                )
                                                                        }
                                                                }

                                                                // Sort: Default/Básico first, then
                                                                // installed ones, then the rest
                                                                list.sortedWith(
                                                                        compareByDescending<
                                                                                        ThemeMetadata> {
                                                                                it.name == "Default"
                                                                        }
                                                                                .thenByDescending {
                                                                                        it.isDownloaded
                                                                                }
                                                                                .thenBy { it.name }
                                                                )
                                                        }

                                                if (isFetchingThemes && githubThemes.isEmpty()) {
                                                        Box(
                                                                modifier =
                                                                        Modifier.fillMaxWidth()
                                                                                .padding(32.dp),
                                                                contentAlignment = Alignment.Center
                                                        ) {
                                                                CircularProgressIndicator(
                                                                        color = AppColors.Primary
                                                                )
                                                        }
                                                } else if (githubThemes.isEmpty() &&
                                                                !isFetchingThemes &&
                                                                localThemes.isEmpty()
                                                ) {
                                                        // Only show error if we have NO themes at
                                                        // all (unlikely since Default is
                                                        // hardcoded)
                                                        Text(
                                                                "未找到主题或加载失败。",
                                                                color = Color(0xFF636D77),
                                                                fontSize = 14.sp,
                                                                modifier =
                                                                        Modifier.padding(
                                                                                vertical = 16.dp
                                                                        )
                                                        )
                                                }

                                                AnimatedVisibility(
                                                        visible = isThemesExpanded,
                                                        enter = expandVertically(),
                                                        exit = shrinkVertically()
                                                ) {
                                                        Column(
                                                                modifier =
                                                                        Modifier.padding(
                                                                                top = 16.dp
                                                                        ),
                                                                verticalArrangement =
                                                                        Arrangement.spacedBy(12.dp)
                                                        ) {
                                                                allDisplayThemes.forEach { theme ->
                                                                        val isDownloaded =
                                                                                theme.isDownloaded ||
                                                                                        theme.name ==
                                                                                                "Default"
                                                                        val isSelected =
                                                                                selectedTheme ==
                                                                                        theme.name

                                                                        val local =
                                                                                localThemes.find {
                                                                                        it.name ==
                                                                                                theme.name
                                                                                }
                                                                        val hasUpdate =
                                                                                if (local != null) {
                                                                                        ThemeManager
                                                                                                .getInstance(
                                                                                                        context
                                                                                                )
                                                                                                .isNewerVersion(
                                                                                                        local.version,
                                                                                                        theme.version
                                                                                                )
                                                                                } else false

                                                                        ThemeCard(
                                                                                theme = theme,
                                                                                isDownloaded =
                                                                                        isDownloaded,
                                                                                isSelected =
                                                                                        isSelected,
                                                                                hasUpdate =
                                                                                        hasUpdate,
                                                                                isDownloading =
                                                                                        downloadingThemeName ==
                                                                                                theme.name,
                                                                                onAction = {
                                                                                        if (isDownloaded
                                                                                        ) {
                                                                                                // Apply theme
                                                                                                selectedTheme =
                                                                                                        theme.name
                                                                                                prefs
                                                                                                        .edit {
                                                                                                                putString(
                                                                                                                        SharedPreferencesKeys
                                                                                                                                .VIRTUAL_CLUSTER_THEME
                                                                                                                                .key,
                                                                                                                        theme.name
                                                                                                                )
                                                                                                                if (theme.name ==
                                                                                                                                "Default"
                                                                                                                ) {
                                                                                                                        putString(
                                                                                                                                SharedPreferencesKeys
                                                                                                                                        .ACTIVE_CUSTOM_THEME
                                                                                                                                        .key,
                                                                                                                                ""
                                                                                                                        )
                                                                                                                } else {
                                                                                                                        putString(
                                                                                                                                SharedPreferencesKeys
                                                                                                                                        .ACTIVE_CUSTOM_THEME
                                                                                                                                        .key,
                                                                                                                                theme.folderName
                                                                                                                        )
                                                                                                                }
                                                                                                        }
                                                                                                Toast.makeText(
                                                                                                                context,
                                                                                                                "主题 ${theme.name} 已应用！",
                                                                                                                Toast.LENGTH_SHORT
                                                                                                        )
                                                                                                        .show()
                                                                                        } else {
                                                                                                // Download theme
                                                                                                downloadingThemeName =
                                                                                                        theme.name
                                                                                                scope
                                                                                                        .launch {
                                                                                                                try {
                                                                                                                        val success =
                                                                                                                                ThemeManager
                                                                                                                                        .getInstance(
                                                                                                                                                context
                                                                                                                                        )
                                                                                                                                        .downloadTheme(
                                                                                                                                                theme
                                                                                                                                        )
                                                                                                                        if (success
                                                                                                                        ) {
                                                                                                                                localThemes =
                                                                                                                                        ThemeManager
                                                                                                                                                .getInstance(
                                                                                                                                                        context
                                                                                                                                                )
                                                                                                                                                .getLocalThemes()
                                                                                                                                Toast.makeText(
                                                                                                                                                context,
                                                                                                                                                "主题 ${theme.name} 已安装！点击应用。",
                                                                                                                                                Toast.LENGTH_SHORT
                                                                                                                                        )
                                                                                                                                        .show()
                                                                                                                        } else {
                                                                                                                                Toast.makeText(
                                                                                                                                                context,
                                                                                                                                                "下载主题 ${theme.name} 失败",
                                                                                                                                                Toast.LENGTH_SHORT
                                                                                                                                        )
                                                                                                                                        .show()
                                                                                                                        }
                                                                                                                } finally {
                                                                                                                        downloadingThemeName =
                                                                                                                                null
                                                                                                                }
                                                                                                        }
                                                                                        }
                                                                                },
                                                                                onUpdate = {
                                                                                        downloadingThemeName =
                                                                                                theme.name
                                                                                        scope
                                                                                                .launch {
                                                                                                        try {
                                                                                                                val success =
                                                                                                                        ThemeManager
                                                                                                                                .getInstance(
                                                                                                                                        context
                                                                                                                                )
                                                                                                                                .downloadTheme(
                                                                                                                                        theme
                                                                                                                                )
                                                                                                                if (success
                                                                                                                ) {
                                                                                                                        localThemes =
                                                                                                                                ThemeManager
                                                                                                                                        .getInstance(
                                                                                                                                                context
                                                                                                                                        )
                                                                                                                                        .getLocalThemes()
                                                                                                                        Toast.makeText(
                                                                                                                                        context,
                                                                                                                                        "主题 ${theme.name} 已更新！",
                                                                                                                                        Toast.LENGTH_SHORT
                                                                                                                                )
                                                                                                                                .show()
                                                                                                                } else {
                                                                                                                        Toast.makeText(
                                                                                                                                        context,
                                                                                                                                        "更新主题 ${theme.name} 失败",
                                                                                                                                        Toast.LENGTH_SHORT
                                                                                                                                )
                                                                                                                                .show()
                                                                                                                }
                                                                                                        } finally {
                                                                                                                downloadingThemeName =
                                                                                                                        null
                                                                                                        }
                                                                                                }
                                                                                },
                                                                                onDelete =
                                                                                        if (isDownloaded &&
                                                                                                        theme.name !=
                                                                                                                "Default"
                                                                                        ) {
                                                                                                {
                                                                                                        scope
                                                                                                                .launch {
                                                                                                                        val themeDir =
                                                                                                                                java.io
                                                                                                                                        .File(
                                                                                                                                                java.io
                                                                                                                                                        .File(
                                                                                                                                                                context.filesDir,
                                                                                                                                                                "themes"
                                                                                                                                                        ),
                                                                                                                                                theme.folderName
                                                                                                                                        )
                                                                                                                        if (themeDir.exists()
                                                                                                                        ) {
                                                                                                                                themeDir.deleteRecursively()
                                                                                                                                if (selectedTheme ==
                                                                                                                                                theme.name
                                                                                                                                ) {
                                                                                                                                        selectedTheme =
                                                                                                                                                "Default"
                                                                                                                                        prefs
                                                                                                                                                .edit {
                                                                                                                                                        putString(
                                                                                                                                                                SharedPreferencesKeys
                                                                                                                                                                        .VIRTUAL_CLUSTER_THEME
                                                                                                                                                                        .key,
                                                                                                                                                                "Default"
                                                                                                                                                        )
                                                                                                                                                        putString(
                                                                                                                                                                SharedPreferencesKeys
                                                                                                                                                                        .ACTIVE_CUSTOM_THEME
                                                                                                                                                                        .key,
                                                                                                                                                                ""
                                                                                                                                                        )
                                                                                                                                                }
                                                                                                                                }
                                                                                                                                // Refresh local themes
                                                                                                                                localThemes =
                                                                                                                                        ThemeManager
                                                                                                                                                .getInstance(
                                                                                                                                                        context
                                                                                                                                                )
                                                                                                                                                .getLocalThemes()
                                                                                                                                Toast.makeText(
                                                                                                                                                context,
                                                                                                                                                "主题 ${theme.name} 已删除！",
                                                                                                                                                Toast.LENGTH_SHORT
                                                                                                                                        )
                                                                                                                                        .show()
                                                                                                                        }
                                                                                                                }
                                                                                                }
                                                                                        } else null
                                                                        )
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                }

                // ODÔMETRO E REVISÃO CARD
                val odometerAlpha = if (allClusterFunctionsEnabled) 1f else 0.4f
                StyledCard(modifier = Modifier.padding(horizontal = 8.dp).alpha(odometerAlpha)) {
                        Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                        "显示里程与保养提醒",
                                                        color = Color.White,
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                        "面板显示车辆总里程并跟踪下次保养（保养提醒仅显示数秒）",
                                                        color = Color(0xFFB0B8C4),
                                                        fontSize = 14.sp
                                                )
                                        }
                                        Switch(
                                                checked = enableOdometerAndRevision,
                                                enabled = allClusterFunctionsEnabled,
                                                onCheckedChange = {
                                                        enableOdometerAndRevision = it
                                                        prefs.edit {
                                                                putBoolean(
                                                                        SharedPreferencesKeys
                                                                                .ENABLE_INSTRUMENT_ODOMETER_AND_REVISION
                                                                                .key,
                                                                        it
                                                                )
                                                        }
                                                },
                                                modifier = Modifier.scale(0.9f),
                                                colors =
                                                        SwitchDefaults.colors(
                                                                checkedThumbColor =
                                                                        br.com.redesurftank
                                                                                .havalshisuku.ui
                                                                                .components
                                                                                .AppColors
                                                                                .TextPrimary,
                                                                checkedTrackColor =
                                                                        br.com.redesurftank
                                                                                .havalshisuku.ui
                                                                                .components
                                                                                .AppColors.Primary,
                                                                uncheckedThumbColor =
                                                                        br.com.redesurftank
                                                                                .havalshisuku.ui
                                                                                .components
                                                                                .AppColors
                                                                                .TextSecondary,
                                                                uncheckedTrackColor =
                                                                        br.com.redesurftank
                                                                                .havalshisuku.ui
                                                                                .components
                                                                                .AppColors
                                                                                .ButtonSecondary,
                                                                uncheckedBorderColor =
                                                                        Color.Transparent,
                                                                checkedBorderColor =
                                                                        Color.Transparent
                                                        )
                                        )
                                }

                                if (enableOdometerAndRevision && allClusterFunctionsEnabled) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        HorizontalDivider(
                                                color = Color(0xFF3A3F47),
                                                thickness = 1.dp
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))

                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                                "下次保养",
                                                                color = Color(0xFFB0B8C4),
                                                                fontSize = 14.sp
                                                        )
                                                        if (nextKm == 0) {
                                                                Text(
                                                                        "面板上显示下次保养需先登记保养或购车日期",
                                                                        color = Color(0xFFFFB74D),
                                                                        fontSize = 16.sp,
                                                                        lineHeight = 20.sp,
                                                                        fontWeight =
                                                                                FontWeight.Medium
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        4.dp
                                                                                )
                                                                )
                                                                Text(
                                                                        "里程： ${String.format("%,d", ServiceManager.getInstance().totalOdometer)} km",
                                                                        color = Color.White,
                                                                        fontSize = 18.sp,
                                                                        fontWeight = FontWeight.Bold
                                                                )
                                                        } else {
                                                                val nextKmLabel =
                                                                        String.format(
                                                                                "%,d",
                                                                                nextKm
                                                                        ) + " km"
                                                                val nextDateLabel =
                                                                        dateFormatter.format(
                                                                                nextDate
                                                                        )
                                                                Text(
                                                                        "$nextKmLabel ou $nextDateLabel",
                                                                        color = Color.White,
                                                                        fontSize = 22.sp,
                                                                        fontWeight = FontWeight.Bold
                                                                )
                                                        }
                                                }

                                                Button(
                                                        onClick = {
                                                                tempKm =
                                                                        ServiceManager.getInstance()
                                                                                .totalOdometer
                                                                                .toString()
                                                                tempDate =
                                                                        System.currentTimeMillis()
                                                                showRegisterDialog = true
                                                        },
                                                        enabled = allClusterFunctionsEnabled,
                                                        colors =
                                                                ButtonDefaults.buttonColors(
                                                                        containerColor =
                                                                                Color(0xFF4A9EFF)
                                                                ),
                                                        shape = RoundedCornerShape(8.dp)
                                                ) {
                                                        if (nextKm == 0) {
                                                                Text(
                                                                        "登记保养/购车日期",
                                                                        fontWeight = FontWeight.Bold
                                                                )
                                                        } else {
                                                                Text(
                                                                        "登记保养",
                                                                        fontWeight = FontWeight.Bold
                                                                )
                                                        }
                                                }
                                        }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Collapsible History
                                Row(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .clickable {
                                                                expandedHistory = !expandedHistory
                                                        }
                                                        .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Text(
                                                "历史记录（${revisionHistory.size}）",
                                                color = Color(0xFF4A9EFF),
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                                imageVector =
                                                        if (expandedHistory)
                                                                Icons.Default.ExpandLess
                                                        else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = Color(0xFF4A9EFF)
                                        )
                                }

                                AnimatedVisibility(visible = expandedHistory) {
                                        Column(
                                                modifier = Modifier.padding(top = 8.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                                if (revisionHistory.isEmpty()) {
                                                        Text(
                                                                "暂无保养记录",
                                                                color = Color(0xFF636D77),
                                                                fontSize = 14.sp,
                                                                modifier =
                                                                        Modifier.padding(
                                                                                vertical = 8.dp
                                                                        )
                                                        )
                                                } else {
                                                        revisionHistory
                                                                .sortedByDescending { it.km }
                                                                .forEach { entry ->
                                                                        Row(
                                                                                modifier =
                                                                                        Modifier.fillMaxWidth()
                                                                                                .background(
                                                                                                        Color(
                                                                                                                0xFF1E2228
                                                                                                        ),
                                                                                                        RoundedCornerShape(
                                                                                                                8.dp
                                                                                                        )
                                                                                                )
                                                                                                .padding(
                                                                                                        horizontal =
                                                                                                                16.dp,
                                                                                                        vertical =
                                                                                                                10.dp
                                                                                                ),
                                                                                horizontalArrangement =
                                                                                        Arrangement
                                                                                                .SpaceBetween,
                                                                                verticalAlignment =
                                                                                        Alignment
                                                                                                .CenterVertically
                                                                        ) {
                                                                                Column {
                                                                                        Text(
                                                                                                if (entry.km != 0) {
                                                                                                        "${String.format("%,d", entry.km) } km"
                                                                                                } else {
                                                                                                        "购车日期"
                                                                                                },
                                                                                                color =
                                                                                                        Color.White,
                                                                                                fontWeight =
                                                                                                        FontWeight
                                                                                                                .Bold
                                                                                        )
                                                                                        Text(
                                                                                                dateFormatter
                                                                                                        .format(
                                                                                                                entry.date
                                                                                                        ),
                                                                                                color =
                                                                                                        Color(
                                                                                                                0xFFB0B8C4
                                                                                                        ),
                                                                                                fontSize =
                                                                                                        12.sp
                                                                                        )
                                                                                }
                                                                                IconButton(
                                                                                        onClick = {
                                                                                                val newHistory =
                                                                                                        revisionHistory
                                                                                                                .filter {
                                                                                                                        it !=
                                                                                                                                entry
                                                                                                                }
                                                                                                revisionHistory =
                                                                                                        newHistory
                                                                                                saveRevisionHistory(
                                                                                                        prefs,
                                                                                                        newHistory
                                                                                                )
                                                                                        }
                                                                                ) {
                                                                                        Icon(
                                                                                                Icons.Default
                                                                                                        .Delete,
                                                                                                contentDescription =
                                                                                                        "删除",
                                                                                                tint =
                                                                                                        Color(
                                                                                                                0xFFFF4B4B
                                                                                                        ),
                                                                                                modifier =
                                                                                                        Modifier.size(
                                                                                                                20.dp
                                                                                                        )
                                                                                        )
                                                                                }
                                                                        }
                                                                }
                                                }
                                        }
                                }
                        }
                }

                Spacer(modifier = Modifier.height(16.dp))
                DisplayAppConfigSection()
        }

        // Register Revision Dialog
        if (showRegisterDialog) {
                AlertDialog(
                        onDismissRequest = { showRegisterDialog = false },
                        containerColor = Color(0xFF1E2228),
                        titleContentColor = Color.White,
                        textContentColor = Color.White,
                        title = {
                                Text(
                                        if (revisionHistory.isEmpty()) {
                                                "登记购车或保养"
                                        } else {
                                                "登记保养"
                                        },
                                        fontWeight = FontWeight.Bold
                                )
                        },
                        text = {
                                Column(
                                        modifier = Modifier.padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                        Text(
                                                if (revisionHistory.isEmpty()) {
                                                        "填写本次保养信息以自动推算下次保养。若要登记购车日期，请把里程填为 0。"
                                                } else {
                                                        "填写本次保养信息，自动计算下次保养日期。"
                                                },
                                                color = Color(0xFFB0B8C4),
                                                fontSize = 14.sp
                                        )

                                        StyledTextField(
                                                value = tempKm,
                                                onValueChange = {
                                                        if (it.isEmpty() || it.toIntOrNull() != null
                                                        )
                                                                tempKm = it
                                                },
                                                label = { Text("当前里程") },
                                                modifier = Modifier.fillMaxWidth(),
                                                keyboardOptions =
                                                        KeyboardOptions(
                                                                keyboardType = KeyboardType.Number
                                                        )
                                        )

                                        Column {
                                                Text(
                                                        if (tempKm == "0") {
                                                                "保养日期"
                                                        } else {
                                                                "购车日期"
                                                        },
                                                        color = Color(0xFFB0B8C4),
                                                        fontSize = 12.sp
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                OutlinedButton(
                                                        onClick = {
                                                                showDatePickerForRegister = true
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors =
                                                                ButtonDefaults.outlinedButtonColors(
                                                                        contentColor = Color.White
                                                                ),
                                                        border =
                                                                BorderStroke(
                                                                        1.dp,
                                                                        Color(0xFF3A3F47)
                                                                ),
                                                        shape = RoundedCornerShape(8.dp)
                                                ) { Text(dateFormatter.format(tempDate)) }
                                        }
                                }
                        },
                        confirmButton = {
                                Button(
                                        onClick = {
                                                val km = tempKm.toIntOrNull() ?: 0
                                                if (km >= 0) {
                                                        val newEntry = RevisionEntry(km, tempDate)
                                                        val newHistory = revisionHistory + newEntry
                                                        revisionHistory = newHistory
                                                        saveRevisionHistory(prefs, newHistory)
                                                        showRegisterDialog = false
                                                }
                                        },
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFF4A9EFF)
                                                )
                                ) { Text("确认", fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = {
                                TextButton(onClick = { showRegisterDialog = false }) {
                                        Text("取消", color = Color(0xFFB0B8C4))
                                }
                        }
                )
        }

        if (showDatePickerForRegister) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = tempDate

                LaunchedEffect(Unit) {
                        DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                                val cal = Calendar.getInstance()
                                                cal.set(year, month, day)
                                                tempDate = cal.timeInMillis
                                                showDatePickerForRegister = false
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                )
                                .apply {
                                        setOnDismissListener { showDatePickerForRegister = false }
                                        show()
                                }
                }
        }
}

@Composable
fun DisplayAppConfigSection() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var configs by remember { mutableStateOf(DisplayAppLauncher.getAllConfigs()) }
        var showConfigDialog by remember { mutableStateOf(false) }
        var editingPackage by remember { mutableStateOf<String?>(null) }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Text(
                                "副屏应用",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                        )
                        Button(
                                onClick = {
                                        editingPackage = null
                                        showConfigDialog = true
                                },
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF4A9EFF)
                                        ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                                Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("添加", color = Color.White, fontSize = 13.sp)
                        }
                }

                if (configs.isEmpty()) {
                        StyledCard {
                                Text(
                                        "尚未配置应用。\n点击“添加”配置要在另一屏幕显示的应用。",
                                        color = Color(0xFFB0B8C4),
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(20.dp)
                                )
                        }
                } else {
                        configs.forEach { config ->
                                val pm = context.packageManager
                                val appName =
                                        if (!config.customName.isNullOrBlank()) {
                                                config.customName
                                        } else {
                                                try {
                                                        pm.getApplicationInfo(config.packageName, 0)
                                                                .let {
                                                                        pm.getApplicationLabel(it)
                                                                                .toString()
                                                                }
                                                } catch (_: Exception) {
                                                        config.packageName
                                                }
                                        }
                                val appIcon =
                                        try {
                                                pm.getApplicationIcon(config.packageName)
                                        } catch (_: Exception) {
                                                null
                                        }
                                val displayLabel =
                                        TargetDisplay.fromId(config.displayId)?.label
                                                ?: "屏幕 ${config.displayId}"

                                StyledCard(
                                        modifier =
                                                Modifier.clickable {
                                                        editingPackage = config.packageName
                                                        showConfigDialog = true
                                                }
                                ) {
                                        Column(
                                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Box(
                                                                modifier =
                                                                        Modifier.size(48.dp)
                                                                                .clip(
                                                                                        RoundedCornerShape(
                                                                                                10.dp
                                                                                        )
                                                                                )
                                                                                .background(
                                                                                        Color(
                                                                                                0xFF2A2F37
                                                                                        )
                                                                                ),
                                                                contentAlignment = Alignment.Center
                                                        ) {
                                                                if (config.substituteIcon != null) {
                                                                        Icon(
                                                                                imageVector =
                                                                                        when (config.substituteIcon
                                                                                        ) {
                                                                                                "nav" ->
                                                                                                        Icons.Default
                                                                                                                .Place
                                                                                                "music" ->
                                                                                                        Icons.Default
                                                                                                                .PlayArrow
                                                                                                "video" ->
                                                                                                        Icons.Default
                                                                                                                .Movie
                                                                                                "settings" ->
                                                                                                        Icons.Default
                                                                                                                .Settings
                                                                                                "haval" ->
                                                                                                        Icons.Default
                                                                                                                .DirectionsCar
                                                                                                else ->
                                                                                                        Icons.Default
                                                                                                                .Android
                                                                                        },
                                                                                contentDescription =
                                                                                        null,
                                                                                tint = Color.White,
                                                                                modifier =
                                                                                        Modifier.size(
                                                                                                32.dp
                                                                                        )
                                                                        )
                                                                } else if (appIcon != null) {
                                                                        AsyncImage(
                                                                                model = appIcon,
                                                                                contentDescription =
                                                                                        null,
                                                                                modifier =
                                                                                        Modifier.fillMaxSize(),
                                                                                contentScale =
                                                                                        ContentScale
                                                                                                .Fit
                                                                        )
                                                                } else {
                                                                        Icon(
                                                                                Icons.Default
                                                                                        .Android,
                                                                                contentDescription =
                                                                                        null,
                                                                                tint = Color.White,
                                                                                modifier =
                                                                                        Modifier.size(
                                                                                                32.dp
                                                                                        )
                                                                        )
                                                                }
                                                        }
                                                        Spacer(Modifier.width(16.dp))
                                                        Column(modifier = Modifier.weight(4f)) {
                                                                Text(
                                                                        appName,
                                                                        color = Color.White,
                                                                        fontSize = 16.sp,
                                                                        fontWeight =
                                                                                FontWeight.Medium
                                                                )
                                                                Text(
                                                                        displayLabel,
                                                                        color = Color(0xFFB0B8C4),
                                                                        fontSize = 12.sp
                                                                )
                                                                Text(
                                                                        "位置：${config.x},${config.y}｜尺寸：${config.width}x${config.height}",
                                                                        color = Color(0xFF808080),
                                                                        fontSize = 11.sp
                                                                )
                                                        }
                                                        Column(modifier = Modifier.weight(6f)) {
                                                                // Reordering arrows
                                                                Row(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth(),
                                                                        horizontalArrangement =
                                                                                Arrangement.End
                                                                ) {
                                                                        IconButton(
                                                                                onClick = {
                                                                                        DisplayAppLauncher
                                                                                                .moveConfigUp(
                                                                                                        config.packageName
                                                                                                )
                                                                                        configs =
                                                                                                DisplayAppLauncher
                                                                                                        .getAllConfigs()
                                                                                },
                                                                                modifier =
                                                                                        Modifier.size(
                                                                                                32.dp
                                                                                        )
                                                                        ) {
                                                                                Icon(
                                                                                        Icons.Default
                                                                                                .ExpandLess,
                                                                                        contentDescription =
                                                                                                "Subir",
                                                                                        tint =
                                                                                                Color(
                                                                                                        0xFF4A9EFF
                                                                                                )
                                                                                )
                                                                        }
                                                                        IconButton(
                                                                                onClick = {
                                                                                        DisplayAppLauncher
                                                                                                .moveConfigDown(
                                                                                                        config.packageName
                                                                                                )
                                                                                        configs =
                                                                                                DisplayAppLauncher
                                                                                                        .getAllConfigs()
                                                                                },
                                                                                modifier =
                                                                                        Modifier.size(
                                                                                                32.dp
                                                                                        )
                                                                        ) {
                                                                                Icon(
                                                                                        Icons.Default
                                                                                                .ExpandMore,
                                                                                        contentDescription =
                                                                                                "Descer",
                                                                                        tint =
                                                                                                Color(
                                                                                                        0xFF4A9EFF
                                                                                                )
                                                                                )
                                                                        }
                                                                }

                                                                // Action buttons row
                                                                Column(
                                                                        verticalArrangement =
                                                                                Arrangement
                                                                                        .spacedBy(
                                                                                                8.dp
                                                                                        )
                                                                ) {
                                                                        Row(
                                                                                modifier =
                                                                                        Modifier.fillMaxWidth(),
                                                                                horizontalArrangement =
                                                                                        Arrangement
                                                                                                .spacedBy(
                                                                                                        8.dp
                                                                                                )
                                                                        ) {
                                                                                // Abrir aqui (main
                                                                                // display for
                                                                                // interaction)
                                                                                Button(
                                                                                        onClick = {
                                                                                                scope
                                                                                                        .launch {
                                                                                                                DisplayAppLauncher
                                                                                                                        .launchOnMainDisplay(
                                                                                                                                config
                                                                                                                        )
                                                                                                        }
                                                                                        },
                                                                                        colors =
                                                                                                ButtonDefaults
                                                                                                        .buttonColors(
                                                                                                                containerColor =
                                                                                                                        Color(
                                                                                                                                0xFF3A3F47
                                                                                                                        )
                                                                                                        ),
                                                                                        contentPadding =
                                                                                                PaddingValues(
                                                                                                        horizontal =
                                                                                                                8.dp,
                                                                                                        vertical =
                                                                                                                4.dp
                                                                                                ),
                                                                                        modifier =
                                                                                                Modifier.weight(
                                                                                                        1f
                                                                                                ),
                                                                                        shape =
                                                                                                RoundedCornerShape(
                                                                                                        8.dp
                                                                                                )
                                                                                ) {
                                                                                        Icon(
                                                                                                Icons.Default
                                                                                                        .PlayArrow,
                                                                                                contentDescription =
                                                                                                        null,
                                                                                                tint =
                                                                                                        Color.White,
                                                                                                modifier =
                                                                                                        Modifier.size(
                                                                                                                16.dp
                                                                                                        )
                                                                                        )
                                                                                        Spacer(
                                                                                                Modifier.width(
                                                                                                        4.dp
                                                                                                )
                                                                                        )
                                                                                        Text(
                                                                                                "Trazer",
                                                                                                color =
                                                                                                        Color.White,
                                                                                                fontSize =
                                                                                                        12.sp
                                                                                        )
                                                                                }
                                                                                // Enviar para tela
                                                                                // secundária
                                                                                Button(
                                                                                        onClick = {
                                                                                                scope
                                                                                                        .launch {
                                                                                                                DisplayAppLauncher
                                                                                                                        .sendToDisplay(
                                                                                                                                config
                                                                                                                        )
                                                                                                        }
                                                                                        },
                                                                                        colors =
                                                                                                ButtonDefaults
                                                                                                        .buttonColors(
                                                                                                                containerColor =
                                                                                                                        Color(
                                                                                                                                0xFF4A9EFF
                                                                                                                        )
                                                                                                        ),
                                                                                        contentPadding =
                                                                                                PaddingValues(
                                                                                                        horizontal =
                                                                                                                8.dp,
                                                                                                        vertical =
                                                                                                                4.dp
                                                                                                ),
                                                                                        modifier =
                                                                                                Modifier.weight(
                                                                                                        1f
                                                                                                ),
                                                                                        shape =
                                                                                                RoundedCornerShape(
                                                                                                        8.dp
                                                                                                )
                                                                                ) {
                                                                                        Icon(
                                                                                                Icons.AutoMirrored
                                                                                                        .Filled
                                                                                                        .Send,
                                                                                                contentDescription =
                                                                                                        null,
                                                                                                tint =
                                                                                                        Color.White,
                                                                                                modifier =
                                                                                                        Modifier.size(
                                                                                                                16.dp
                                                                                                        )
                                                                                        )
                                                                                        Spacer(
                                                                                                Modifier.width(
                                                                                                        4.dp
                                                                                                )
                                                                                        )
                                                                                        Text(
                                                                                                "发送",
                                                                                                color =
                                                                                                        Color.White,
                                                                                                fontSize =
                                                                                                        12.sp
                                                                                        )
                                                                                }
                                                                        }
                                                                        Row(
                                                                                modifier =
                                                                                        Modifier.fillMaxWidth(),
                                                                                horizontalArrangement =
                                                                                        Arrangement
                                                                                                .spacedBy(
                                                                                                        8.dp
                                                                                                )
                                                                        ) {
                                                                                // Editar
                                                                                Button(
                                                                                        onClick = {
                                                                                                editingPackage =
                                                                                                        config.packageName
                                                                                                showConfigDialog =
                                                                                                        true
                                                                                        },
                                                                                        colors =
                                                                                                ButtonDefaults
                                                                                                        .buttonColors(
                                                                                                                containerColor =
                                                                                                                        Color(
                                                                                                                                0xFF2A2F37
                                                                                                                        )
                                                                                                        ),
                                                                                        contentPadding =
                                                                                                PaddingValues(
                                                                                                        horizontal =
                                                                                                                8.dp,
                                                                                                        vertical =
                                                                                                                4.dp
                                                                                                ),
                                                                                        modifier =
                                                                                                Modifier.weight(
                                                                                                        2f
                                                                                                ),
                                                                                        shape =
                                                                                                RoundedCornerShape(
                                                                                                        8.dp
                                                                                                )
                                                                                ) {
                                                                                        Icon(
                                                                                                Icons.Default
                                                                                                        .Edit,
                                                                                                contentDescription =
                                                                                                        null,
                                                                                                tint =
                                                                                                        Color.White,
                                                                                                modifier =
                                                                                                        Modifier.size(
                                                                                                                16.dp
                                                                                                        )
                                                                                        )
                                                                                        Spacer(
                                                                                                Modifier.width(
                                                                                                        4.dp
                                                                                                )
                                                                                        )
                                                                                        Text(
                                                                                                "编辑",
                                                                                                color =
                                                                                                        Color.White,
                                                                                                fontSize =
                                                                                                        12.sp
                                                                                        )
                                                                                }
                                                                                // Matar app
                                                                                Button(
                                                                                        onClick = {
                                                                                                scope
                                                                                                        .launch {
                                                                                                                DisplayAppLauncher
                                                                                                                        .killApp(
                                                                                                                                config.packageName
                                                                                                                        )
                                                                                                        }
                                                                                        },
                                                                                        colors =
                                                                                                ButtonDefaults
                                                                                                        .buttonColors(
                                                                                                                containerColor =
                                                                                                                        Color(
                                                                                                                                0x33FF4A4A
                                                                                                                        )
                                                                                                        ),
                                                                                        contentPadding =
                                                                                                PaddingValues(
                                                                                                        horizontal =
                                                                                                                8.dp,
                                                                                                        vertical =
                                                                                                                4.dp
                                                                                                ),
                                                                                        modifier =
                                                                                                Modifier.weight(
                                                                                                        1f
                                                                                                ),
                                                                                        shape =
                                                                                                RoundedCornerShape(
                                                                                                        8.dp
                                                                                                )
                                                                                ) {
                                                                                        Icon(
                                                                                                Icons.Default
                                                                                                        .Close,
                                                                                                contentDescription =
                                                                                                        null,
                                                                                                tint =
                                                                                                        Color(
                                                                                                                0xFFFF4A4A
                                                                                                        ),
                                                                                                modifier =
                                                                                                        Modifier.size(
                                                                                                                14.dp
                                                                                                        )
                                                                                        )
                                                                                        Spacer(
                                                                                                Modifier.width(
                                                                                                        4.dp
                                                                                                )
                                                                                        )
                                                                                        Text(
                                                                                                "结束",
                                                                                                color =
                                                                                                        Color(
                                                                                                                0xFFFF4A4A
                                                                                                        ),
                                                                                                fontSize =
                                                                                                        11.sp
                                                                                        )
                                                                                }
                                                                                // Remover config
                                                                                // (kill + delete)
                                                                                Button(
                                                                                        onClick = {
                                                                                                scope
                                                                                                        .launch {
                                                                                                                DisplayAppLauncher
                                                                                                                        .killApp(
                                                                                                                                config.packageName
                                                                                                                        )
                                                                                                        }
                                                                                                DisplayAppLauncher
                                                                                                        .deleteConfig(
                                                                                                                config.packageName
                                                                                                        )
                                                                                                configs =
                                                                                                        DisplayAppLauncher
                                                                                                                .getAllConfigs()
                                                                                        },
                                                                                        colors =
                                                                                                ButtonDefaults
                                                                                                        .buttonColors(
                                                                                                                containerColor =
                                                                                                                        Color(
                                                                                                                                0x33FF4A4A
                                                                                                                        )
                                                                                                        ),
                                                                                        contentPadding =
                                                                                                PaddingValues(
                                                                                                        horizontal =
                                                                                                                8.dp,
                                                                                                        vertical =
                                                                                                                4.dp
                                                                                                ),
                                                                                        modifier =
                                                                                                Modifier.weight(
                                                                                                        1f
                                                                                                ),
                                                                                        shape =
                                                                                                RoundedCornerShape(
                                                                                                        8.dp
                                                                                                )
                                                                                ) {
                                                                                        Icon(
                                                                                                Icons.Default
                                                                                                        .Delete,
                                                                                                contentDescription =
                                                                                                        null,
                                                                                                tint =
                                                                                                        Color(
                                                                                                                0xFFFF4A4A
                                                                                                        ),
                                                                                                modifier =
                                                                                                        Modifier.size(
                                                                                                                14.dp
                                                                                                        )
                                                                                        )
                                                                                        Spacer(
                                                                                                Modifier.width(
                                                                                                        4.dp
                                                                                                )
                                                                                        )
                                                                                        Text(
                                                                                                "移除",
                                                                                                color =
                                                                                                        Color(
                                                                                                                0xFFFF4A4A
                                                                                                        ),
                                                                                                fontSize =
                                                                                                        11.sp
                                                                                        )
                                                                                }
                                                                        }
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }

        if (showConfigDialog) {
                DisplayAppConfigDialog(
                        existingConfig =
                                editingPackage?.let { pkg ->
                                        configs.find { it.packageName == pkg }
                                },
                        onDismiss = { showConfigDialog = false },
                        onSave = { config ->
                                DisplayAppLauncher.saveConfig(config)
                                configs = DisplayAppLauncher.getAllConfigs()
                                showConfigDialog = false
                        }
                )
        }
}

data class InstalledAppInfo(
        val packageName: String,
        val activityName: String,
        val label: String,
        val icon: android.graphics.drawable.Drawable?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayAppConfigDialog(
        existingConfig: DisplayAppConfig?,
        onDismiss: () -> Unit,
        onSave: (DisplayAppConfig) -> Unit
) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        // App selection state
        var selectedApp by remember { mutableStateOf<InstalledAppInfo?>(null) }
        var showAppPicker by remember { mutableStateOf(false) }

        // Display selection
        var selectedDisplay by remember {
                mutableStateOf(
                        existingConfig?.let { TargetDisplay.fromId(it.displayId) }
                                ?: TargetDisplay.CLUSTER
                )
        }
        var displayDropdownExpanded by remember { mutableStateOf(false) }
        var selectedIconColor by remember { mutableStateOf(existingConfig?.iconColor ?: "#FFFFFF") }

        // Get display resolution for sliders
        val resolution =
                remember(selectedDisplay) {
                        DisplayAppLauncher.getDisplayResolution(selectedDisplay.id)
                }

        // Position & size
        var posX by remember { mutableIntStateOf(existingConfig?.x ?: 0) }
        var posY by remember { mutableIntStateOf(existingConfig?.y ?: 0) }
        var sizeW by remember { mutableIntStateOf(existingConfig?.width ?: resolution.first) }
        var sizeH by remember { mutableIntStateOf(existingConfig?.height ?: resolution.second) }
        var selectedSubIcon by remember { mutableStateOf(existingConfig?.substituteIcon) }

        val substituteIcons =
                listOf(
                        "nav" to "地图",
                        "music" to "音乐",
                        "video" to "视频",
                        "settings" to "系统设置",
                        "haval" to "车辆",
                        "game" to "游戏",
                        "tv" to "电视",
                        "phone" to "电话",
                        "chat" to "聊天",
                        "map_alt" to "探索"
                )

        // Preview tracking
        var previewActive by remember { mutableStateOf(false) }
        var previewJob by remember { mutableStateOf<Job?>(null) }

        var customName by remember { mutableStateOf(existingConfig?.customName ?: "") }
        var showRenameDialog by remember { mutableStateOf(false) }

        // Helper to build config from current state
        fun currentConfig(): DisplayAppConfig? {
                val app = selectedApp ?: return null
                return DisplayAppConfig(
                        packageName = app.packageName,
                        activityName = app.activityName,
                        displayId = selectedDisplay.id,
                        x = posX,
                        y = posY,
                        width = sizeW,
                        height = sizeH,
                        substituteIcon = selectedSubIcon,
                        iconColor = selectedIconColor,
                        customName = if (customName.isBlank()) null else customName
                )
        }

        // Load existing app info and auto-launch preview
        LaunchedEffect(existingConfig) {
                if (existingConfig != null) {
                        val pm = context.packageManager
                        val label =
                                try {
                                        pm.getApplicationInfo(existingConfig.packageName, 0).let {
                                                pm.getApplicationLabel(it).toString()
                                        }
                                } catch (_: Exception) {
                                        existingConfig.packageName
                                }
                        val icon =
                                try {
                                        pm.getApplicationIcon(existingConfig.packageName)
                                } catch (_: Exception) {
                                        null
                                }
                        selectedApp =
                                InstalledAppInfo(
                                        existingConfig.packageName,
                                        existingConfig.activityName,
                                        label,
                                        icon
                                )
                        // Auto-launch with existing config for visual reference
                        previewActive = true
                        DisplayAppLauncher.launchApp(existingConfig)
                }
        }

        // When display changes with an app already selected, reset bounds to full screen and
        // re-launch
        LaunchedEffect(selectedDisplay) {
                val res = DisplayAppLauncher.getDisplayResolution(selectedDisplay.id)
                if (existingConfig == null || existingConfig.displayId != selectedDisplay.id) {
                        posX = 0
                        posY = 0
                        sizeW = res.first
                        sizeH = res.second
                }
                // Re-launch on new display if preview is active
                if (previewActive && selectedApp != null) {
                        delay(300)
                        currentConfig()?.let { DisplayAppLauncher.launchApp(it) }
                }
        }

        // Debounced live preview — updates in real-time as sliders move
        LaunchedEffect(posX, posY, sizeW, sizeH) {
                if (previewActive && selectedApp != null) {
                        previewJob?.cancel()
                        previewJob =
                                scope.launch {
                                        delay(500)
                                        currentConfig()?.let { DisplayAppLauncher.resizeApp(it) }
                                }
                }
        }

        Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
                Card(
                        modifier =
                                Modifier.fillMaxWidth(0.35f)
                                        .wrapContentHeight()
                                        .border(1.dp, Color(0xFF1D2430), RoundedCornerShape(12.dp)),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = Color(0xFF13151A).copy(alpha = 1.0f)
                                ),
                        shape = RoundedCornerShape(12.dp)
                ) {
                        Column(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .wrapContentHeight()
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                                .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                                Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Text(
                                                text =
                                                        if (existingConfig != null) "编辑应用"
                                                        else "添加应用",
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                        )
                                        IconButton(
                                                onClick = onDismiss,
                                                modifier = Modifier.size(32.dp)
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "关闭",
                                                        tint = Color.White
                                                )
                                        }
                                }

                                Spacer(Modifier.height(8.dp))

                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                        "应用",
                                                        color = Color(0xFFB0B8C4),
                                                        fontSize = 12.sp
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically,
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(8.dp)
                                                ) {
                                                        Button(
                                                                onClick = { showAppPicker = true },
                                                                modifier = Modifier.weight(1f),
                                                                colors =
                                                                        ButtonDefaults.buttonColors(
                                                                                containerColor =
                                                                                        Color(
                                                                                                0xFF2A2F37
                                                                                        )
                                                                        ),
                                                                shape = RoundedCornerShape(8.dp),
                                                                contentPadding =
                                                                        PaddingValues(
                                                                                horizontal = 12.dp,
                                                                                vertical = 8.dp
                                                                        )
                                                        ) {
                                                                Row(
                                                                        verticalAlignment =
                                                                                Alignment
                                                                                        .CenterVertically
                                                                ) {
                                                                        if (selectedApp != null) {
                                                                                AsyncImage(
                                                                                        model =
                                                                                                selectedApp
                                                                                                        ?.icon,
                                                                                        contentDescription =
                                                                                                null,
                                                                                        modifier =
                                                                                                Modifier.size(
                                                                                                                24.dp
                                                                                                        )
                                                                                                        .clip(
                                                                                                                RoundedCornerShape(
                                                                                                                        4.dp
                                                                                                                )
                                                                                                        ),
                                                                                        contentScale =
                                                                                                ContentScale
                                                                                                        .Fit
                                                                                )
                                                                                Spacer(
                                                                                        Modifier.width(
                                                                                                8.dp
                                                                                        )
                                                                                )
                                                                        }
                                                                        Text(
                                                                                if (customName
                                                                                                .isNotBlank()
                                                                                )
                                                                                        customName
                                                                                else
                                                                                        selectedApp
                                                                                                ?.label
                                                                                                ?: "Selecionar app...",
                                                                                color =
                                                                                        if (selectedApp !=
                                                                                                        null
                                                                                        )
                                                                                                (if (customName
                                                                                                                .isNotBlank()
                                                                                                )
                                                                                                        Color(
                                                                                                                0xFF4A9EFF
                                                                                                        )
                                                                                                else
                                                                                                        Color.White)
                                                                                        else
                                                                                                Color(
                                                                                                        0xFF808080
                                                                                                ),
                                                                                fontSize = 14.sp,
                                                                                maxLines = 1,
                                                                                overflow =
                                                                                        TextOverflow
                                                                                                .Ellipsis
                                                                        )
                                                                }
                                                        }
                                                        IconButton(
                                                                onClick = {
                                                                        showRenameDialog = true
                                                                },
                                                                modifier =
                                                                        Modifier.size(36.dp)
                                                                                .background(
                                                                                        Color(
                                                                                                0xFF2A2F37
                                                                                        ),
                                                                                        RoundedCornerShape(
                                                                                                8.dp
                                                                                        )
                                                                                )
                                                        ) {
                                                                Icon(
                                                                        Icons.Default.Edit,
                                                                        contentDescription =
                                                                                "重命名",
                                                                        tint =
                                                                                if (customName
                                                                                                .isNotBlank()
                                                                                )
                                                                                        Color(
                                                                                                0xFF4A9EFF
                                                                                        )
                                                                                else Color.White,
                                                                        modifier =
                                                                                Modifier.size(18.dp)
                                                                )
                                                        }
                                                }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                        "目标屏幕",
                                                        color = Color(0xFFB0B8C4),
                                                        fontSize = 12.sp
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                ExposedDropdownMenuBox(
                                                        expanded = displayDropdownExpanded,
                                                        onExpandedChange = {
                                                                displayDropdownExpanded = it
                                                        }
                                                ) {
                                                        TextField(
                                                                value = selectedDisplay.label,
                                                                onValueChange = {},
                                                                readOnly = true,
                                                                trailingIcon = {
                                                                        ExposedDropdownMenuDefaults
                                                                                .TrailingIcon(
                                                                                        expanded =
                                                                                                displayDropdownExpanded
                                                                                )
                                                                },
                                                                modifier =
                                                                        Modifier.fillMaxWidth()
                                                                                .menuAnchor(
                                                                                        MenuAnchorType
                                                                                                .PrimaryNotEditable
                                                                                ),
                                                                textStyle =
                                                                        androidx.compose.ui.text
                                                                                .TextStyle(
                                                                                        fontSize =
                                                                                                14.sp
                                                                                ),
                                                                colors =
                                                                        TextFieldDefaults.colors(
                                                                                focusedContainerColor =
                                                                                        Color(
                                                                                                0xFF2A2F37
                                                                                        ),
                                                                                unfocusedContainerColor =
                                                                                        Color(
                                                                                                0xFF2A2F37
                                                                                        ),
                                                                                focusedTextColor =
                                                                                        Color.White,
                                                                                unfocusedTextColor =
                                                                                        Color.White,
                                                                                focusedIndicatorColor =
                                                                                        Color(
                                                                                                0xFF4A9EFF
                                                                                        ),
                                                                                unfocusedIndicatorColor =
                                                                                        Color(
                                                                                                0xFF3A3F47
                                                                                        )
                                                                        )
                                                        )
                                                        ExposedDropdownMenu(
                                                                expanded = displayDropdownExpanded,
                                                                onDismissRequest = {
                                                                        displayDropdownExpanded =
                                                                                false
                                                                }
                                                        ) {
                                                                TargetDisplay.entries.forEach {
                                                                        display ->
                                                                        DropdownMenuItem(
                                                                                text = {
                                                                                        Text(
                                                                                                display.label,
                                                                                                color =
                                                                                                        Color.White
                                                                                        )
                                                                                },
                                                                                onClick = {
                                                                                        selectedDisplay =
                                                                                                display
                                                                                        displayDropdownExpanded =
                                                                                                false
                                                                                }
                                                                        )
                                                                }
                                                        }
                                                }
                                        }
                                }

                                // Resolution info
                                Text(
                                        "分辨率：${resolution.first} x ${resolution.second}｜位置：$posX,$posY",
                                        color = Color(0xFF808080),
                                        fontSize = 11.sp
                                )

                                // Position sliders
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                                SliderWithLabel(
                                                        label = "X 坐标",
                                                        value = posX,
                                                        range = 0..resolution.first,
                                                        onValueChange = { posX = it }
                                                )
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                                SliderWithLabel(
                                                        label = "Y 坐标",
                                                        value = posY,
                                                        range = 0..resolution.second,
                                                        onValueChange = { posY = it },
                                                        specialSnap = 135
                                                )
                                        }
                                }

                                // Size sliders
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                                SliderWithLabel(
                                                        label = "宽度",
                                                        value = sizeW,
                                                        range = 100..resolution.first,
                                                        onValueChange = { sizeW = it }
                                                )
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                                SliderWithLabel(
                                                        label = "高度",
                                                        value = sizeH,
                                                        range = 100..resolution.second,
                                                        onValueChange = { sizeH = it }
                                                )
                                        }
                                }

                                // Substitute Icon Selection
                                Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                                "替代图标",
                                                color = Color(0xFFB0B8C4),
                                                fontSize = 12.sp
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                        ) {
                                                item {
                                                        Box(
                                                                modifier =
                                                                        Modifier.size(44.dp)
                                                                                .clip(
                                                                                        RoundedCornerShape(
                                                                                                8.dp
                                                                                        )
                                                                                )
                                                                                .background(
                                                                                        if (selectedSubIcon ==
                                                                                                        null
                                                                                        )
                                                                                                Color(
                                                                                                        0xFF4A9EFF
                                                                                                )
                                                                                        else
                                                                                                Color(
                                                                                                        0xFF2A2F37
                                                                                                )
                                                                                )
                                                                                .clickable {
                                                                                        selectedSubIcon =
                                                                                                null
                                                                                }
                                                                                .padding(4.dp),
                                                                contentAlignment = Alignment.Center
                                                        ) {
                                                                Text(
                                                                        "默认",
                                                                        color = Color.White,
                                                                        fontSize = 9.sp,
                                                                        textAlign = TextAlign.Center
                                                                )
                                                        }
                                                }
                                                items(substituteIcons) { (id, label) ->
                                                        val isSelected = selectedSubIcon == id
                                                        Box(
                                                                modifier =
                                                                        Modifier.size(44.dp)
                                                                                .clip(
                                                                                        RoundedCornerShape(
                                                                                                8.dp
                                                                                        )
                                                                                )
                                                                                .background(
                                                                                        if (isSelected
                                                                                        )
                                                                                                Color(
                                                                                                        0xFF4A9EFF
                                                                                                )
                                                                                        else
                                                                                                Color(
                                                                                                        0xFF2A2F37
                                                                                                )
                                                                                )
                                                                                .clickable {
                                                                                        selectedSubIcon =
                                                                                                id
                                                                                }
                                                                                .padding(4.dp),
                                                                contentAlignment = Alignment.Center
                                                        ) {
                                                                Icon(
                                                                        imageVector =
                                                                                when (id) {
                                                                                        "nav" ->
                                                                                                Icons.Default
                                                                                                        .Place
                                                                                        "music" ->
                                                                                                Icons.Default
                                                                                                        .PlayArrow
                                                                                        "video" ->
                                                                                                Icons.Default
                                                                                                        .Movie
                                                                                        "settings" ->
                                                                                                Icons.Default
                                                                                                        .Settings
                                                                                        "haval" ->
                                                                                                Icons.Default
                                                                                                        .DirectionsCar
                                                                                        "game" ->
                                                                                                Icons.Default
                                                                                                        .SportsEsports
                                                                                        "tv" ->
                                                                                                Icons.Default
                                                                                                        .Tv
                                                                                        "phone" ->
                                                                                                Icons.Default
                                                                                                        .Phone
                                                                                        "chat" ->
                                                                                                Icons.Default
                                                                                                        .Chat
                                                                                        "map_alt" ->
                                                                                                Icons.Default
                                                                                                        .Map
                                                                                        else ->
                                                                                                Icons.Default
                                                                                                        .Android
                                                                                },
                                                                        contentDescription = null,
                                                                        tint = Color.White,
                                                                        modifier =
                                                                                Modifier.size(24.dp)
                                                                )
                                                        }
                                                }
                                        }
                                }

                                // Consolidated Color Selector
                                Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                                "强调色",
                                                color = Color(0xFFB0B8C4),
                                                fontSize = 12.sp
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        val colorOptions =
                                                listOf(
                                                        "#FFFFFF",
                                                        "#ECEFF1",
                                                        "#FF0000",
                                                        "#FF4B4B",
                                                        "#00FF00",
                                                        "#0000FF",
                                                        "#4A9EFF",
                                                        "#90CAF9",
                                                        "#FFFF00",
                                                        "#FF00FF",
                                                        "#00FFFF",
                                                        "#FFA500",
                                                        "#800080",
                                                        "#808080"
                                                )
                                        LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(vertical = 2.dp)
                                        ) {
                                                items(colorOptions) { colorHex ->
                                                        val color =
                                                                try {
                                                                        Color(
                                                                                android.graphics
                                                                                        .Color
                                                                                        .parseColor(
                                                                                                colorHex
                                                                                        )
                                                                        )
                                                                } catch (_: Exception) {
                                                                        Color.White
                                                                }
                                                        Box(
                                                                modifier =
                                                                        Modifier.size(28.dp)
                                                                                .clip(CircleShape)
                                                                                .background(color)
                                                                                .border(
                                                                                        width =
                                                                                                if (selectedIconColor
                                                                                                                .uppercase() ==
                                                                                                                colorHex.uppercase()
                                                                                                )
                                                                                                        2.dp
                                                                                                else
                                                                                                        1.dp,
                                                                                        color =
                                                                                                if (selectedIconColor
                                                                                                                .uppercase() ==
                                                                                                                colorHex.uppercase()
                                                                                                )
                                                                                                        Color.White
                                                                                                else
                                                                                                        Color.White
                                                                                                                .copy(
                                                                                                                        alpha =
                                                                                                                                0.2f
                                                                                                                ),
                                                                                        shape =
                                                                                                CircleShape
                                                                                )
                                                                                .clickable {
                                                                                        selectedIconColor =
                                                                                                colorHex
                                                                                }
                                                        )
                                                }
                                        }
                                }

                                // Live preview status
                                if (previewActive && selectedApp != null) {
                                        Text(
                                                "Preview ativo — ajuste os sliders e veja em tempo real",
                                                color = Color(0xFF4A9EFF),
                                                fontSize = 12.sp
                                        )
                                }

                                // Action buttons
                                Spacer(Modifier.height(8.dp))
                                Button(
                                        onClick = { currentConfig()?.let { onSave(it) } },
                                        enabled = selectedApp != null,
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFF4A9EFF)
                                                ),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                ) {
                                        Text(
                                                "保存",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                        )
                                }
                        }
                }
        }

        if (showAppPicker) {
                AppPickerDialog(
                        onDismiss = { showAppPicker = false },
                        onAppSelected = { app ->
                                selectedApp = app
                                showAppPicker = false
                                // Auto-launch full screen on target display for visual reference
                                previewActive = true
                                scope.launch {
                                        DisplayAppLauncher.launchApp(
                                                DisplayAppConfig(
                                                        packageName = app.packageName,
                                                        activityName = app.activityName,
                                                        displayId = selectedDisplay.id,
                                                        x = posX,
                                                        y = posY,
                                                        width = sizeW,
                                                        height = sizeH
                                                )
                                        )
                                }
                        }
                )
        }

        if (showRenameDialog) {
                var tempName by remember { mutableStateOf(customName) }
                AlertDialog(
                        onDismissRequest = { showRenameDialog = false },
                        title = {
                                Text(
                                        "自定义名称",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                )
                        },
                        containerColor = Color(0xFF1E2228),
                        titleContentColor = Color.White,
                        textContentColor = Color.White,
                        text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                                "为此快捷方式设置自定义名称：",
                                                color = Color(0xFFB0B8C4),
                                                fontSize = 14.sp
                                        )
                                        TextField(
                                                value = tempName,
                                                onValueChange = { tempName = it },
                                                placeholder = {
                                                        Text(
                                                                selectedApp?.label
                                                                        ?: "原始名称",
                                                                color = Color(0xFF808080)
                                                        )
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                colors =
                                                        TextFieldDefaults.colors(
                                                                focusedContainerColor =
                                                                        Color(0xFF2A2F37),
                                                                unfocusedContainerColor =
                                                                        Color(0xFF2A2F37),
                                                                focusedTextColor = Color.White,
                                                                unfocusedTextColor = Color.White,
                                                                focusedIndicatorColor =
                                                                        Color(0xFF4A9EFF),
                                                                unfocusedIndicatorColor =
                                                                        Color(0xFF3A3F47)
                                                        )
                                        )
                                        if (tempName.isNotBlank()) {
                                                TextButton(
                                                        onClick = { tempName = "" },
                                                        modifier = Modifier.align(Alignment.End)
                                                ) {
                                                        Text(
                                                                "恢复默认",
                                                                color = Color(0xFFFF4B4B),
                                                                fontSize = 12.sp
                                                        )
                                                }
                                        }
                                }
                        },
                        confirmButton = {
                                Button(
                                        onClick = {
                                                customName = tempName
                                                showRenameDialog = false
                                        },
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFF4A9EFF)
                                                )
                                ) { Text("确定", fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = {
                                TextButton(onClick = { showRenameDialog = false }) {
                                        Text("取消", color = Color(0xFFB0B8C4))
                                }
                        }
                )
        }
}

@Composable
fun SliderWithLabel(
        label: String,
        value: Int,
        range: IntRange,
        onValueChange: (Int) -> Unit,
        step: Int = 1,
        specialSnap: Int? = null
) {
        Column {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                        Text(label, color = Color(0xFFB0B8C4), fontSize = 12.sp)
                        Text("$value", color = Color.White, fontSize = 12.sp)
                }
                Slider(
                        value = value.toFloat(),
                        onValueChange = {
                                var snapped = (kotlin.math.round(it / step) * step).toInt()
                                val snapTolerance = if (step == 1) 10 else step
                                if (specialSnap != null &&
                                                kotlin.math.abs(snapped - specialSnap) <=
                                                        snapTolerance
                                ) {
                                        snapped = specialSnap
                                }
                                onValueChange(snapped.coerceIn(range))
                        },
                        valueRange = range.first.toFloat()..range.last.toFloat(),
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                                SliderDefaults.colors(
                                        thumbColor = Color(0xFF4A9EFF),
                                        activeTrackColor = Color(0xFF4A9EFF),
                                        inactiveTrackColor = Color(0xFF2C3139)
                                )
                )
        }
}

@Composable
fun AppPickerDialog(onDismiss: () -> Unit, onAppSelected: (InstalledAppInfo) -> Unit) {
        val context = LocalContext.current
        var searchQuery by remember { mutableStateOf("") }
        val installedApps = remember {
                val pm = context.packageManager
                val intent =
                        Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
                val apps =
                        pm.queryIntentActivities(intent, 0)
                                .map { resolveInfo ->
                                        InstalledAppInfo(
                                                packageName = resolveInfo.activityInfo.packageName,
                                                activityName = resolveInfo.activityInfo.name,
                                                label = resolveInfo.loadLabel(pm).toString(),
                                                icon =
                                                        try {
                                                                resolveInfo.loadIcon(pm)
                                                        } catch (_: Exception) {
                                                                null
                                                        }
                                        )
                                }
                                .toMutableList()

                apps.sortedBy { it.label.lowercase() }
        }

        var showManualInput by remember { mutableStateOf(false) }
        var manualPkg by remember { mutableStateOf("") }
        var manualActivity by remember { mutableStateOf("") }
        var manualLabel by remember { mutableStateOf("") }

        Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
                Card(
                        modifier =
                                Modifier.fillMaxWidth(0.30f)
                                        .wrapContentHeight()
                                        .border(1.dp, Color(0xFF1D2430), RoundedCornerShape(12.dp)),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = Color(0xFF13151A).copy(alpha = 1.0f)
                                ),
                        shape = RoundedCornerShape(12.dp)
                ) {
                        Column(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .wrapContentHeight()
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Text(
                                                text = "选择应用",
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                        )
                                        IconButton(
                                                onClick = onDismiss,
                                                modifier = Modifier.size(32.dp)
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "关闭",
                                                        tint = Color.White
                                                )
                                        }
                                }

                                if (showManualInput) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                TextField(
                                                        value = manualLabel,
                                                        onValueChange = { manualLabel = it },
                                                        placeholder = {
                                                                Text(
                                                                        "应用名称（如：YouTube）",
                                                                        color = Color(0xFF808080)
                                                                )
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        singleLine = true,
                                                        colors =
                                                                TextFieldDefaults.colors(
                                                                        focusedContainerColor =
                                                                                Color(0xFF2A2F37),
                                                                        unfocusedContainerColor =
                                                                                Color(0xFF2A2F37),
                                                                        focusedTextColor =
                                                                                Color.White,
                                                                        unfocusedTextColor =
                                                                                Color.White
                                                                )
                                                )
                                                TextField(
                                                        value = manualPkg,
                                                        onValueChange = { manualPkg = it },
                                                        placeholder = {
                                                                Text(
                                                                        "包名（如：com.google.android.youtube）",
                                                                        color = Color(0xFF808080)
                                                                )
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        singleLine = true,
                                                        colors =
                                                                TextFieldDefaults.colors(
                                                                        focusedContainerColor =
                                                                                Color(0xFF2A2F37),
                                                                        unfocusedContainerColor =
                                                                                Color(0xFF2A2F37),
                                                                        focusedTextColor =
                                                                                Color.White,
                                                                        unfocusedTextColor =
                                                                                Color.White
                                                                )
                                                )
                                                TextField(
                                                        value = manualActivity,
                                                        onValueChange = { manualActivity = it },
                                                        placeholder = {
                                                                Text(
                                                                        "Activity（可选）",
                                                                        color = Color(0xFF808080)
                                                                )
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        singleLine = true,
                                                        colors =
                                                                TextFieldDefaults.colors(
                                                                        focusedContainerColor =
                                                                                Color(0xFF2A2F37),
                                                                        unfocusedContainerColor =
                                                                                Color(0xFF2A2F37),
                                                                        focusedTextColor =
                                                                                Color.White,
                                                                        unfocusedTextColor =
                                                                                Color.White
                                                                )
                                                )
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(8.dp)
                                                ) {
                                                        Button(
                                                                onClick = {
                                                                        showManualInput = false
                                                                },
                                                                modifier = Modifier.weight(1f),
                                                                colors =
                                                                        ButtonDefaults.buttonColors(
                                                                                containerColor =
                                                                                        Color(
                                                                                                0xFF2A2F37
                                                                                        )
                                                                        )
                                                        ) { Text("取消", color = Color.White) }
                                                        Button(
                                                                onClick = {
                                                                        if (manualPkg
                                                                                        .isNotBlank() &&
                                                                                        manualLabel
                                                                                                .isNotBlank()
                                                                        ) {
                                                                                onAppSelected(
                                                                                        InstalledAppInfo(
                                                                                                manualPkg,
                                                                                                manualActivity,
                                                                                                manualLabel,
                                                                                                null
                                                                                        )
                                                                                )
                                                                        }
                                                                },
                                                                modifier = Modifier.weight(1f),
                                                                enabled =
                                                                        manualPkg.isNotBlank() &&
                                                                                manualLabel
                                                                                        .isNotBlank(),
                                                                colors =
                                                                        ButtonDefaults.buttonColors(
                                                                                containerColor =
                                                                                        Color(
                                                                                                0xFF4A9EFF
                                                                                        )
                                                                        )
                                                        ) { Text("添加", color = Color.White) }
                                                }
                                        }
                                } else {
                                        Row(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(bottom = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                                TextField(
                                                        value = searchQuery,
                                                        onValueChange = { searchQuery = it },
                                                        placeholder = {
                                                                Text(
                                                                        "搜索…",
                                                                        color = Color(0xFF808080)
                                                                )
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        singleLine = true,
                                                        colors =
                                                                TextFieldDefaults.colors(
                                                                        focusedContainerColor =
                                                                                Color(0xFF2A2F37),
                                                                        unfocusedContainerColor =
                                                                                Color(0xFF2A2F37),
                                                                        focusedTextColor =
                                                                                Color.White,
                                                                        unfocusedTextColor =
                                                                                Color.White,
                                                                        focusedIndicatorColor =
                                                                                Color(0xFF4A9EFF),
                                                                        unfocusedIndicatorColor =
                                                                                Color(0xFF3A3F47)
                                                                )
                                                )
                                                Button(
                                                        onClick = { showManualInput = true },
                                                        colors =
                                                                ButtonDefaults.buttonColors(
                                                                        containerColor =
                                                                                Color(0xFF2A2F37)
                                                                ),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding =
                                                                PaddingValues(
                                                                        horizontal = 8.dp,
                                                                        vertical = 4.dp
                                                                )
                                                ) {
                                                        Text(
                                                                "手动",
                                                                color = Color.White,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                        )
                                                }
                                        }
                                }

                                val filteredApps =
                                        if (searchQuery.isBlank()) installedApps
                                        else
                                                installedApps.filter {
                                                        it.label.contains(
                                                                searchQuery,
                                                                ignoreCase = true
                                                        ) ||
                                                                it.packageName.contains(
                                                                        searchQuery,
                                                                        ignoreCase = true
                                                                )
                                                }

                                LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 80.dp),
                                        modifier = Modifier.heightIn(max = 350.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        items(filteredApps) { app ->
                                                Column(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .clip(
                                                                                RoundedCornerShape(
                                                                                        8.dp
                                                                                )
                                                                        )
                                                                        .background(
                                                                                Color(0xFF2A2F37)
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.5f
                                                                                        )
                                                                        )
                                                                        .clickable {
                                                                                onAppSelected(app)
                                                                        }
                                                                        .padding(8.dp),
                                                        horizontalAlignment =
                                                                Alignment.CenterHorizontally,
                                                        verticalArrangement =
                                                                Arrangement.spacedBy(4.dp)
                                                ) {
                                                        AsyncImage(
                                                                model = app.icon,
                                                                contentDescription = app.label,
                                                                modifier = Modifier.size(44.dp),
                                                                contentScale = ContentScale.Fit
                                                        )
                                                        Text(
                                                                text = app.label,
                                                                color = Color.White,
                                                                fontSize = 10.sp,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                textAlign = TextAlign.Center
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}

@Composable
fun CurrentValuesTab() {
        val prefs =
                App.getDeviceProtectedContext()
                        .getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
        val advancedUse = prefs.getBoolean(SharedPreferencesKeys.ADVANCE_USE.key, false)
        val dataMap = remember {
                mutableStateMapOf<String, String>().apply {
                        putAll(ServiceManager.getInstance().allCurrentCachedData)
                }
        }
        var showConfigDialog by remember { mutableStateOf(false) }
        val allConstants = remember { CarConstants.entries.map { it.value } }
        val defaultKeys = remember {
                ServiceManager.DEFAULT_KEYS.map { it.value }
        } // Assuming DEFAULT_KEYS is Array<CarConstants>
        val filteredConstants = remember { allConstants.filter { it !in defaultKeys } }
        val monitoredSet = remember {
                mutableStateOf(
                        prefs.getStringSet(
                                SharedPreferencesKeys.CAR_MONITOR_PROPERTIES.key,
                                emptySet()
                        )
                                ?: emptySet()
                )
        }
        val tempChecked = remember {
                mutableStateMapOf<String, Boolean>().apply {
                        allConstants.forEach { this[it] = monitoredSet.value.contains(it) }
                }
        }
        var showUpdateDialog by remember { mutableStateOf(false) }
        var selectedKey by remember { mutableStateOf("") }
        var newValue by remember { mutableStateOf("") }
        var searchQueryValues by remember { mutableStateOf("") }
        var searchQueryConfig by remember { mutableStateOf("") }
        DisposableEffect(Unit) {
                val listener = IDataChanged { key, value -> dataMap[key] = value ?: "" }
                ServiceManager.getInstance().addDataChangedListener(listener)
                onDispose { ServiceManager.getInstance().removeDataChangedListener(listener) }
        }
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                if (advancedUse) {
                        Button(
                                onClick = { showConfigDialog = true },
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF4A9EFF)
                                        )
                        ) { Text("配置", color = Color.White) }
                        Spacer(Modifier.height(8.dp))
                }
                TextField(
                        value = searchQueryValues,
                        onValueChange = { searchQueryValues = it },
                        label = { Text("搜索数值") },
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                                TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF2A2F37),
                                        unfocusedContainerColor = Color(0xFF2A2F37),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color(0xFFB0B8C4),
                                        focusedIndicatorColor = Color(0xFF4A9EFF),
                                        unfocusedIndicatorColor = Color(0xFF3A3F47),
                                        focusedLabelColor = Color(0xFF4A9EFF),
                                        unfocusedLabelColor = Color(0xFFB0B8C4)
                                )
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val filteredData =
                                dataMap.toList()
                                        .filter {
                                                it.first
                                                        .lowercase()
                                                        .contains(searchQueryValues.lowercase())
                                        }
                                        .sortedBy { it.first }
                        items(filteredData) { (key, value) ->
                                Card(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                        .then(
                                                                if (advancedUse)
                                                                        Modifier.clickable {
                                                                                selectedKey = key
                                                                                newValue = value
                                                                                showUpdateDialog =
                                                                                        true
                                                                        }
                                                                else Modifier
                                                        ),
                                        colors =
                                                CardDefaults.cardColors(
                                                        containerColor = Color(0xFF13151A)
                                                ),
                                        elevation =
                                                CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                        Text(
                                                "$key: $value",
                                                modifier = Modifier.padding(8.dp),
                                                color = Color.White,
                                                fontSize = 18.sp
                                        )
                                }
                        }
                }
        }
        if (showConfigDialog && advancedUse) {
                AlertDialog(
                        onDismissRequest = { showConfigDialog = false },
                        title = { Text("配置监控") },
                        text = {
                                Column {
                                        TextField(
                                                value = searchQueryConfig,
                                                onValueChange = { searchQueryConfig = it },
                                                label = { Text("搜索常量") },
                                                modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        val checked =
                                                filteredConstants
                                                        .filter { tempChecked[it] ?: false }
                                                        .sorted()
                                        val unchecked =
                                                filteredConstants
                                                        .filter { !(tempChecked[it] ?: false) }
                                                        .sorted()
                                        val sortedConstants =
                                                (checked + unchecked).filter {
                                                        it.lowercase()
                                                                .contains(
                                                                        searchQueryConfig
                                                                                .lowercase()
                                                                )
                                                }
                                        LazyColumn {
                                                items(sortedConstants) { constant ->
                                                        Row(
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically
                                                        ) {
                                                                Checkbox(
                                                                        checked =
                                                                                tempChecked[
                                                                                        constant]
                                                                                        ?: false,
                                                                        onCheckedChange = {
                                                                                tempChecked[
                                                                                        constant] =
                                                                                        it
                                                                        }
                                                                )
                                                                Text(constant)
                                                        }
                                                }
                                        }
                                }
                        },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                val newSet =
                                                        tempChecked.filterValues { it }.keys.toSet()
                                                prefs.edit {
                                                        putStringSet(
                                                                SharedPreferencesKeys
                                                                        .CAR_MONITOR_PROPERTIES
                                                                        .key,
                                                                newSet
                                                        )
                                                }
                                                monitoredSet.value = newSet
                                                showConfigDialog = false
                                                ServiceManager.getInstance()
                                                        .updateMonitoringProperties()
                                                dataMap.clear()
                                                dataMap.putAll(
                                                        ServiceManager.getInstance()
                                                                .allCurrentCachedData
                                                )
                                        }
                                ) { Text("保存") }
                        },
                        dismissButton = {
                                TextButton(
                                        onClick = {
                                                allConstants.forEach {
                                                        tempChecked[it] =
                                                                monitoredSet.value.contains(it)
                                                }
                                                showConfigDialog = false
                                        }
                                ) { Text("取消") }
                        }
                )
        }
        if (showUpdateDialog && advancedUse) {
                AlertDialog(
                        onDismissRequest = { showUpdateDialog = false },
                        title = { Text("更新 $selectedKey") },
                        text = {
                                TextField(
                                        value = newValue,
                                        onValueChange = { newValue = it },
                                        label = { Text("新值") }
                                )
                        },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                ServiceManager.getInstance()
                                                        .updateData(selectedKey, newValue)
                                                showUpdateDialog = false
                                        }
                                ) { Text("更新") }
                        },
                        dismissButton = {
                                TextButton(onClick = { showUpdateDialog = false }) {
                                        Text("取消")
                                }
                        }
                )
        }
}

@Composable
fun ContentArea(content: @Composable () -> Unit) {
        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .background(AppColors.Background)
                                .padding(AppDimensions.ContentPadding)
        ) { content() }
}

@Composable
fun AppActionButton(
        text: String,
        onClick: () -> Unit,
        isPrimary: Boolean,
        modifier: Modifier = Modifier
) {
        Button(
                onClick = onClick,
                modifier = modifier.fillMaxWidth().height(48.dp),
                colors =
                        ButtonDefaults.buttonColors(
                                containerColor =
                                        if (isPrimary) AppColors.Primary
                                        else AppColors.ButtonSecondary
                        ),
                shape = RoundedCornerShape(AppDimensions.ButtonCornerRadius),
                contentPadding = PaddingValues(0.dp)
        ) {
                Text(
                        text = text,
                        color = AppColors.TextPrimary,
                        fontSize = if (isPrimary) 14.sp else 13.sp,
                        fontWeight = if (isPrimary) FontWeight.Medium else FontWeight.Normal
                )
        }
}

@Composable
fun InstallAppsTab() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var isLoading by remember { mutableStateOf(true) }
        var apps by remember { mutableStateOf(listOf<AppInfo>()) }
        var downloadingApp by remember { mutableStateOf<String?>(null) }
        var downloadProgress by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
        val pm = context.packageManager
        val requestPermissionLauncher =
                rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                ) { /* Permission requested */}
        var showPermissionDialog by remember { mutableStateOf(false) }
        var installResult by remember { mutableStateOf("") }
        var urlInput by remember { mutableStateOf("") }
        var downloadingUrl by remember { mutableStateOf(false) }
        var urlProgress by remember { mutableFloatStateOf(0f) }

        LaunchedEffect(Unit) {
                scope.launch(Dispatchers.IO) {
                        try {
                                val url =
                                        URL(
                                                "https://raw.githubusercontent.com/bobaoapae/haval-impulse-static-files/refs/heads/main/apps.json?rnd=${System.currentTimeMillis()}"
                                        )
                                val conn = url.openConnection() as HttpURLConnection
                                if (conn.responseCode == 200) {
                                        val reader =
                                                BufferedReader(InputStreamReader(conn.inputStream))
                                        val jsonString = reader.use { it.readText() }
                                        val jsonArray = JSONArray(jsonString)
                                        val appList = mutableListOf<AppInfo>()
                                        for (i in 0 until jsonArray.length()) {
                                                val obj = jsonArray.getJSONObject(i)
                                                val iconUrl = obj.optString("appIcon", "")
                                                appList.add(
                                                        AppInfo(
                                                                obj.getString("appName"),
                                                                obj.getString("appVersion"),
                                                                obj.getString("appPackageName"),
                                                                obj.getString("appLink"),
                                                                if (iconUrl.isNotEmpty() &&
                                                                                iconUrl != "null"
                                                                )
                                                                        iconUrl
                                                                else null
                                                        )
                                                )
                                                // Debug log
                                                Log.d(
                                                        TAG,
                                                        "App: ${obj.getString("appName")}, Icon URL: $iconUrl"
                                                )
                                        }
                                        apps = appList
                                }
                        } catch (e: Exception) {
                                Log.e(TAG, "Error loading apps", e)
                        } finally {
                                isLoading = false
                        }
                }
        }

        fun getInstalledVersion(packageName: String): String? {
                return try {
                        val info = pm.getPackageInfo(packageName, 0)
                        info.versionName
                } catch (e: PackageManager.NameNotFoundException) {
                        null
                }
        }

        fun compareVersions(v1: String?, v2: String): Int {
                if (v1 == null) return -1
                val clean1 = v1.removeSuffix("-preview")
                val clean2 = v2.removeSuffix("-preview")
                val parts1 = clean1.split(".").map { it.toIntOrNull() ?: 0 }
                val parts2 = clean2.split(".").map { it.toIntOrNull() ?: 0 }
                for (i in 0 until min(parts1.size, parts2.size)) {
                        if (parts1[i] > parts2[i]) return 1
                        if (parts1[i] < parts2[i]) return -1
                }
                return parts1.size.compareTo(parts2.size)
        }

        fun startDownload(app: AppInfo) {
                downloadingApp = app.packageName
                downloadProgress =
                        downloadProgress.toMutableMap().apply { put(app.packageName, 0f) }
                scope.launch(Dispatchers.IO) {
                        try {
                                val file =
                                        File(
                                                context.getExternalFilesDir(null),
                                                "${app.packageName}.apk"
                                        )
                                val url = URL(app.link)
                                val conn = url.openConnection() as HttpURLConnection
                                val length = conn.contentLength
                                val input = BufferedInputStream(conn.inputStream)
                                val output = FileOutputStream(file)
                                val buffer = ByteArray(4096)
                                var bytesRead: Int
                                var total = 0
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                        output.write(buffer, 0, bytesRead)
                                        total += bytesRead
                                        if (length > 0) {
                                                downloadProgress =
                                                        downloadProgress.toMutableMap().apply {
                                                                put(
                                                                        app.packageName,
                                                                        total.toFloat() / length
                                                                )
                                                        }
                                        }
                                }
                                output.close()
                                input.close()
                                withContext(Dispatchers.Main) {
                                        if (!pm.canRequestPackageInstalls()) {
                                                showPermissionDialog = true
                                                return@withContext
                                        }
                                        val uri =
                                                FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.provider",
                                                        file
                                                )
                                        val intent =
                                                Intent(Intent.ACTION_VIEW).apply {
                                                        setDataAndType(
                                                                uri,
                                                                "application/vnd.android.package-archive"
                                                        )
                                                        addFlags(
                                                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                        )
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                        context.startActivity(intent)
                                }
                        } catch (e: Exception) {
                                Log.e(TAG, "下载失败", e)
                        } finally {
                                downloadingApp = null
                        }
                }
        }

        fun startDownloadFromUrl(urlString: String) {
                downloadingUrl = true
                urlProgress = 0f
                scope.launch(Dispatchers.IO) {
                        try {
                                val file = File(context.getExternalFilesDir(null), "custom.apk")
                                val url = URL(urlString)
                                val conn = url.openConnection() as HttpURLConnection
                                val length = conn.contentLength
                                val input = BufferedInputStream(conn.inputStream)
                                val output = FileOutputStream(file)
                                val buffer = ByteArray(4096)
                                var bytesRead: Int
                                var total = 0
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                        output.write(buffer, 0, bytesRead)
                                        total += bytesRead
                                        if (length > 0) {
                                                urlProgress = total.toFloat() / length
                                        }
                                }
                                output.close()
                                input.close()
                                withContext(Dispatchers.Main) {
                                        if (!pm.canRequestPackageInstalls()) {
                                                showPermissionDialog = true
                                                return@withContext
                                        }
                                        val uri =
                                                FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.provider",
                                                        file
                                                )
                                        val intent =
                                                Intent(Intent.ACTION_VIEW).apply {
                                                        setDataAndType(
                                                                uri,
                                                                "application/vnd.android.package-archive"
                                                        )
                                                        addFlags(
                                                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                        )
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                        context.startActivity(intent)
                                }
                        } catch (e: Exception) {
                                Log.e(TAG, "下载失败", e)
                        } finally {
                                downloadingUrl = false
                        }
                }
        }

        fun uninstall(packageName: String) {
                val intent =
                        Intent(Intent.ACTION_DELETE).apply {
                                data = Uri.parse("package:$packageName")
                        }
                context.startActivity(intent)
        }

        LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                // URL Input Section
                item(span = { GridItemSpan(4) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        TextField(
                                                value = urlInput,
                                                onValueChange = { urlInput = it },
                                                label = { Text("APK 下载地址") },
                                                modifier = Modifier.weight(1f),
                                                colors =
                                                        TextFieldDefaults.colors(
                                                                focusedContainerColor =
                                                                        Color(0xFF2A2F37),
                                                                unfocusedContainerColor =
                                                                        Color(0xFF2A2F37),
                                                                focusedTextColor = Color.White,
                                                                unfocusedTextColor =
                                                                        Color(0xFFB0B8C4),
                                                                focusedIndicatorColor =
                                                                        Color(0xFF4A9EFF),
                                                                unfocusedIndicatorColor =
                                                                        Color(0xFF3A3F47),
                                                                focusedLabelColor =
                                                                        Color(0xFF4A9EFF),
                                                                unfocusedLabelColor =
                                                                        Color(0xFFB0B8C4)
                                                        )
                                        )
                                        if (!downloadingUrl) {
                                                Button(
                                                        onClick = {
                                                                if (urlInput.isNotEmpty())
                                                                        startDownloadFromUrl(
                                                                                urlInput
                                                                        )
                                                        },
                                                        colors =
                                                                ButtonDefaults.buttonColors(
                                                                        containerColor =
                                                                                Color(0xFF4A9EFF)
                                                                ),
                                                        modifier = Modifier.height(56.dp),
                                                        shape = RoundedCornerShape(0.dp)
                                                ) { Text("从网址安装", color = Color.White) }
                                        }
                                }

                                if (downloadingUrl) {
                                        LinearProgressIndicator(
                                                progress = { urlProgress },
                                                modifier = Modifier.fillMaxWidth(),
                                                color = Color(0xFF4A9EFF)
                                        )
                                }

                                if (installResult.isNotEmpty()) {
                                        Text(installResult, color = Color.White, fontSize = 14.sp)
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                        "可用应用：",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium
                                )
                        }
                }

                // Loading indicator
                if (isLoading) {
                        item(span = { GridItemSpan(4) }) {
                                Box(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        contentAlignment = Alignment.Center
                                ) { CircularProgressIndicator(color = Color(0xFF4A9EFF)) }
                        }
                } else {
                        // Apps Grid - Ordenados por prioridade: Atualizar > Instalar > Instalados
                        // Dentro de cada grupo, ordena alfabeticamente
                        val sortedApps =
                                apps.sortedWith(
                                        compareBy(
                                                { app ->
                                                        val installedVersion =
                                                                getInstalledVersion(app.packageName)
                                                        val isInstalled = installedVersion != null
                                                        val needsUpdate =
                                                                isInstalled &&
                                                                        compareVersions(
                                                                                installedVersion,
                                                                                app.version
                                                                        ) < 0

                                                        when {
                                                                needsUpdate ->
                                                                        0 // Prioridade máxima:
                                                                // precisa atualizar
                                                                !isInstalled ->
                                                                        1 // Segunda prioridade:
                                                                // disponível para
                                                                // instalar
                                                                else -> 2 // Última prioridade: já
                                                        // instalado e
                                                        // atualizado
                                                        }
                                                },
                                                { app ->
                                                        app.name.lowercase()
                                                } // Ordenação alfabética dentro de cada grupo
                                        )
                                )

                        items(sortedApps) { app ->
                                val installedVersion = getInstalledVersion(app.packageName)
                                val isInstalled = installedVersion != null
                                val needsUpdate =
                                        isInstalled &&
                                                compareVersions(installedVersion, app.version) < 0
                                val progress = downloadProgress[app.packageName] ?: 0f

                                Card(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .aspectRatio(1.2f)
                                                        .padding(
                                                                vertical = 16.dp,
                                                                horizontal = 16.dp
                                                        )
                                                        .border(
                                                                width = 1.dp,
                                                                color = Color(0xFF1D2430),
                                                                shape = RoundedCornerShape(0.dp),
                                                        ),
                                        colors =
                                                CardDefaults.cardColors(
                                                        containerColor = Color(0xFF13151A)
                                                ),
                                        shape = RoundedCornerShape(12.dp),
                                        elevation =
                                                CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                        Column(
                                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                Column(
                                                        horizontalAlignment =
                                                                Alignment.CenterHorizontally
                                                ) {
                                                        // App Icon Container with padding
                                                        Box(
                                                                modifier = Modifier.size(80.dp),
                                                                contentAlignment = Alignment.Center
                                                        ) {
                                                                Surface(
                                                                        modifier =
                                                                                Modifier.fillMaxSize()
                                                                                        .padding(
                                                                                                8.dp
                                                                                        ),
                                                                        shape =
                                                                                RoundedCornerShape(
                                                                                        12.dp
                                                                                ),
                                                                        color = Color(0xFF2A2F37)
                                                                ) {
                                                                        if (!app.iconUrl
                                                                                        .isNullOrEmpty()
                                                                        ) {
                                                                                AsyncImage(
                                                                                        model =
                                                                                                ImageRequest
                                                                                                        .Builder(
                                                                                                                context
                                                                                                        )
                                                                                                        .data(
                                                                                                                app.iconUrl
                                                                                                        )
                                                                                                        .crossfade(
                                                                                                                true
                                                                                                        )
                                                                                                        .diskCachePolicy(
                                                                                                                CachePolicy
                                                                                                                        .ENABLED
                                                                                                        )
                                                                                                        .memoryCachePolicy(
                                                                                                                CachePolicy
                                                                                                                        .ENABLED
                                                                                                        )
                                                                                                        .allowHardware(
                                                                                                                false
                                                                                                        )
                                                                                                        .build(),
                                                                                        contentDescription =
                                                                                                app.name,
                                                                                        modifier =
                                                                                                Modifier.fillMaxSize(),
                                                                                        contentScale =
                                                                                                ContentScale
                                                                                                        .Crop,
                                                                                        onError = {
                                                                                                Log.e(
                                                                                                        TAG,
                                                                                                        "Error loading icon for ${app.name}: ${it.result.throwable}"
                                                                                                )
                                                                                        }
                                                                                )
                                                                        } else {
                                                                                Box(
                                                                                        contentAlignment =
                                                                                                Alignment
                                                                                                        .Center,
                                                                                        modifier =
                                                                                                Modifier.fillMaxSize()
                                                                                ) {
                                                                                        Icon(
                                                                                                Icons.Default
                                                                                                        .Build,
                                                                                                contentDescription =
                                                                                                        app.name,
                                                                                                tint =
                                                                                                        Color(
                                                                                                                0xFF4A9EFF
                                                                                                        ),
                                                                                                modifier =
                                                                                                        Modifier.size(
                                                                                                                32.dp
                                                                                                        )
                                                                                        )
                                                                                }
                                                                        }
                                                                }
                                                        }

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        // App Name
                                                        Text(
                                                                app.name,
                                                                fontSize = 16.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = Color.White,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis,
                                                                textAlign = TextAlign.Center,
                                                                lineHeight = 18.sp
                                                        )

                                                        // Version info
                                                        Text(
                                                                "v${app.version}",
                                                                fontSize = 12.sp,
                                                                color = Color(0xFFB0B8C4),
                                                                lineHeight = 14.sp
                                                        )

                                                        if (isInstalled) {
                                                                Text(
                                                                        "已装版本：v${installedVersion}",
                                                                        fontSize = 11.sp,
                                                                        color = Color(0xFF808080),
                                                                        maxLines = 1,
                                                                        overflow =
                                                                                TextOverflow
                                                                                        .Ellipsis,
                                                                        lineHeight = 12.sp
                                                                )
                                                        }
                                                }

                                                // Action Button Section
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                        if (downloadingApp == app.packageName) {
                                                                Column(
                                                                        horizontalAlignment =
                                                                                Alignment
                                                                                        .CenterHorizontally
                                                                ) {
                                                                        LinearProgressIndicator(
                                                                                progress = {
                                                                                        progress
                                                                                },
                                                                                modifier =
                                                                                        Modifier.fillMaxWidth()
                                                                                                .height(
                                                                                                        2.dp
                                                                                                ),
                                                                                color =
                                                                                        Color(
                                                                                                0xFF4A9EFF
                                                                                        ),
                                                                                trackColor =
                                                                                        Color(
                                                                                                0xFF3A3F47
                                                                                        )
                                                                        )
                                                                        Text(
                                                                                "${(progress * 100).toInt()}%",
                                                                                color = Color.White,
                                                                                fontSize = 12.sp
                                                                        )
                                                                }
                                                        } else {
                                                                Column(
                                                                        verticalArrangement =
                                                                                Arrangement
                                                                                        .spacedBy(
                                                                                                4.dp
                                                                                        )
                                                                ) {
                                                                        // Botão principal (Instalar
                                                                        // ou Atualizar)
                                                                        if (!isInstalled ||
                                                                                        needsUpdate
                                                                        ) {
                                                                                AppActionButton(
                                                                                        text =
                                                                                                if (!isInstalled
                                                                                                )
                                                                                                        "安装"
                                                                                                else
                                                                                                        "更新",
                                                                                        onClick = {
                                                                                                startDownload(
                                                                                                        app
                                                                                                )
                                                                                        },
                                                                                        isPrimary =
                                                                                                true
                                                                                )
                                                                        }

                                                                        // Botão de desinstalar
                                                                        if (isInstalled) {
                                                                                AppActionButton(
                                                                                        text =
                                                                                                "卸载",
                                                                                        onClick = {
                                                                                                uninstall(
                                                                                                        app.packageName
                                                                                                )
                                                                                        },
                                                                                        isPrimary =
                                                                                                false
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }

        if (showPermissionDialog) {
                AlertDialog(
                        onDismissRequest = { showPermissionDialog = false },
                        title = { Text("需要权限") },
                        text = { Text("请允许安装未知来源应用。") },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                showPermissionDialog = false
                                                val intent =
                                                        Intent(
                                                                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
                                                                )
                                                                .apply {
                                                                        data =
                                                                                Uri.parse(
                                                                                        "package:${context.packageName}"
                                                                                )
                                                                }
                                                requestPermissionLauncher.launch(intent)
                                        }
                                ) { Text("设置") }
                        },
                        dismissButton = {
                                TextButton(onClick = { showPermissionDialog = false }) {
                                        Text("取消")
                                }
                        }
                )
        }
}

@Composable
fun InformacoesTab() {
        val stableVersionName = "2.0.0"
        val stableApkUrl =
                "https://github.com/leandrosavn/haval-impulse/releases/download/v2.0.0/app-release.apk"
        val context = LocalContext.current
        val prefs =
                App.getDeviceProtectedContext()
                        .getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
        var isActive by remember {
                mutableStateOf(ServiceManager.getInstance().isServicesInitialized)
        }
        var bypassSelfInstallationCheck by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.BYPASS_SELF_INSTALLATION_INTEGRITY_CHECK.key,
                                false
                        )
                )
        }
        var selfInstallationCheck by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.SELF_INSTALLATION_INTEGRITY_CHECK.key,
                                false
                        )
                )
        }
        var formattedTime by remember { mutableStateOf("Não inicializado") }
        var formattedTime2 by remember { mutableStateOf("Não inicializado") }
        var formattedTime3 by remember { mutableStateOf("Não inicializado") }
        var version by remember { mutableStateOf("未知") }
        var clickCount by remember { mutableIntStateOf(0) }
        var showAdvancedDialog by remember { mutableStateOf(false) }
        var showUpdateDialog by remember { mutableStateOf(false) }
        var updateMessage by remember { mutableStateOf("") }
        var isDownloading by remember { mutableStateOf(false) }
        var downloadProgress by remember { mutableFloatStateOf(0f) }
        var downloadError by remember { mutableStateOf<String?>(null) }
        var downloadJob by remember { mutableStateOf<Job?>(null) }
        var showUpdateCheckDialog by remember { mutableStateOf(false) }
        var updateCheckResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
        var isCheckingUpdates by remember { mutableStateOf(false) }
        var showBetaUpdates by remember {
                mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.SHOW_BETA_UPDATES.key, false))
        }
        val scope = rememberCoroutineScope()
        val requestPermissionLauncher =
                rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                ) { /* Permission requested */}
        var showPermissionDialog by remember { mutableStateOf(false) }
        var showRevertDialog by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
                try {
                        val packageInfo =
                                context.packageManager.getPackageInfo(context.packageName, 0)
                        version = packageInfo.versionName ?: "未知"
                } catch (e: PackageManager.NameNotFoundException) {
                        version = "错误"
                }
        }

        LaunchedEffect(Unit) {
                while (true) {
                        isActive = ServiceManager.getInstance().isServicesInitialized
                        val timeBoot = ServiceManager.getInstance().timeBootReceived
                        formattedTime =
                                if (isActive && timeBoot > 0) {
                                        val minutes = timeBoot / 60000
                                        val seconds = (timeBoot / 1000) % 60
                                        val millis = timeBoot % 1000
                                        String.format("%02d:%02d.%03d", minutes, seconds, millis)
                                } else {
                                        "Não inicializado"
                                }
                        val timeStart = ServiceManager.getInstance().timeStartInitialization
                        formattedTime2 =
                                if (isActive && timeStart > 0) {
                                        val minutes = timeStart / 60000
                                        val seconds = (timeStart / 1000) % 60
                                        val millis = timeStart % 1000
                                        String.format("%02d:%02d.%03d", minutes, seconds, millis)
                                } else {
                                        "Não inicializado"
                                }
                        val timeInit = ServiceManager.getInstance().timeInitialized
                        formattedTime3 =
                                if (isActive && timeInit > 0) {
                                        val minutes = timeInit / 60000
                                        val seconds = (timeInit / 1000) % 60
                                        val millis = timeInit % 1000
                                        String.format("%02d:%02d.%03d", minutes, seconds, millis)
                                } else {
                                        "Não inicializado"
                                }
                        delay(100)
                }
        }

        suspend fun getAllReleaseInfo(): UpdateCheckResult {
                return withContext(Dispatchers.IO) {
                        try {
                                val url =
                                        URL(
                                                "https://api.github.com/repos/leandrosavn/haval-impulse/releases"
                                        )
                                val conn = url.openConnection() as HttpURLConnection
                                conn.requestMethod = "GET"
                                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                                if (conn.responseCode == 200) {
                                        val reader =
                                                BufferedReader(InputStreamReader(conn.inputStream))
                                        val response = reader.use { it.readText() }
                                        val releases = JSONArray(response)

                                        var latestRelease: ReleaseInfo? = null
                                        var latestPreview: ReleaseInfo? = null

                                        for (i in 0 until releases.length()) {
                                                val release = releases.getJSONObject(i)
                                                val isPrerelease = release.getBoolean("prerelease")
                                                val tag = release.getString("tag_name")
                                                val assets = release.getJSONArray("assets")
                                                var dlUrl: String? = null
                                                for (j in 0 until assets.length()) {
                                                        val asset = assets.getJSONObject(j)
                                                        if (asset.getString("name").endsWith(".apk")
                                                        ) {
                                                                dlUrl =
                                                                        asset.getString(
                                                                                "browser_download_url"
                                                                        )
                                                                break
                                                        }
                                                }
                                                if (dlUrl != null) {
                                                        val info =
                                                                ReleaseInfo(
                                                                        tag,
                                                                        dlUrl,
                                                                        isPrerelease
                                                                )
                                                        if (isPrerelease && latestPreview == null) {
                                                                latestPreview = info
                                                        } else if (!isPrerelease &&
                                                                        latestRelease == null
                                                        ) {
                                                                latestRelease = info
                                                        }
                                                }
                                                if (latestRelease != null && latestPreview != null)
                                                        break
                                        }

                                        UpdateCheckResult(latestRelease, latestPreview)
                                } else UpdateCheckResult(null, null)
                        } catch (e: Exception) {
                                Log.w(TAG, "Error fetching release info", e)
                                UpdateCheckResult(null, null)
                        }
                }
        }

        fun compareVersions(v1: String, v2: String): Int {
                val clean1 = v1.removeSuffix("-preview")
                val clean2 = v2.removeSuffix("-preview")
                val parts1 = clean1.split(".").map { it.toIntOrNull() ?: 0 }
                val parts2 = clean2.split(".").map { it.toIntOrNull() ?: 0 }
                for (i in 0 until min(parts1.size, parts2.size)) {
                        if (parts1[i] > parts2[i]) return 1
                        if (parts1[i] < parts2[i]) return -1
                }
                return parts1.size.compareTo(parts2.size)
        }

        fun startDownload(
                url: String,
                resetTargetVersion: String? = null,
                forceDowngrade: Boolean = false
        ) {
                isDownloading = true
                downloadProgress = 0f
                downloadJob =
                        scope.launch(Dispatchers.IO) {
                                try {
                                        val file =
                                                File(
                                                        context.getExternalFilesDir(null),
                                                        "update.apk"
                                                )
                                        withContext(Dispatchers.IO) {
                                                val dlUrl = URL(url)
                                                val conn =
                                                        dlUrl.openConnection() as HttpURLConnection
                                                val length = conn.contentLength
                                                val input = BufferedInputStream(conn.inputStream)
                                                val output = FileOutputStream(file)
                                                val buffer = ByteArray(4096)
                                                var bytesRead: Int
                                                var total = 0
                                                while (input.read(buffer).also { bytesRead = it } !=
                                                        -1) {
                                                        output.write(buffer, 0, bytesRead)
                                                        total += bytesRead
                                                        if (length > 0)
                                                                downloadProgress =
                                                                        total.toFloat() / length
                                                }
                                                output.close()
                                                input.close()
                                        }
                                        isDownloading = false

                                        if (resetTargetVersion != null) {
                                                prefs.edit()
                                                        .putString(
                                                                SharedPreferencesKeys
                                                                        .PENDING_RESET_TARGET_VERSION
                                                                        .key,
                                                                resetTargetVersion
                                                        )
                                                        .apply()
                                        }

                                        // Voltar para versão estável = normalmente um downgrade,
                                        // que o instalador do sistema (Intent) NÃO permite. Usa o
                                        // Shizuku com `pm install -d` (permite downgrade) e
                                        // `-i com.google.android.packageinstaller` (fura o bloqueio
                                        // de instalação da Beantechs).
                                        if (forceDowngrade) {
                                                val tmp =
                                                        "/data/local/tmp/havalimpulse-stable.apk"
                                                val cmd =
                                                        "cp '${file.absolutePath}' $tmp && " +
                                                                "chmod 644 $tmp && " +
                                                                "pm install -i com.google.android.packageinstaller -r -d -g $tmp && " +
                                                                "am start -n br.com.redesurftank.havalshisuku/.SplashActivity; " +
                                                                "rm -f $tmp"
                                                val out =
                                                        ShizukuUtils.runCommandAndGetOutput(
                                                                arrayOf("sh", "-c", cmd)
                                                        )
                                                if (!out.contains("Success", ignoreCase = true)) {
                                                        withContext(Dispatchers.Main) {
                                                                downloadError =
                                                                        "重新安装稳定版失败。 " +
                                                                                "请确认 Shizuku 已激活（状态：已激活）。 " +
                                                                                "输出： " +
                                                                                out.ifBlank {
                                                                                        "(vazia)"
                                                                                }
                                                        }
                                                }
                                                return@launch
                                        }

                                        withContext(Dispatchers.Main) {
                                                if (!context.packageManager
                                                                .canRequestPackageInstalls()
                                                ) {
                                                        showPermissionDialog = true
                                                        return@withContext
                                                }
                                                val uri =
                                                        FileProvider.getUriForFile(
                                                                context,
                                                                "${context.packageName}.provider",
                                                                file
                                                        )
                                                val intent =
                                                        Intent(Intent.ACTION_VIEW).apply {
                                                                setDataAndType(
                                                                        uri,
                                                                        "application/vnd.android.package-archive"
                                                                )
                                                                addFlags(
                                                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                                )
                                                                addFlags(
                                                                        Intent.FLAG_ACTIVITY_NEW_TASK
                                                                )
                                                        }
                                                context.startActivity(intent)
                                        }
                                } catch (e: Exception) {
                                        Log.e(TAG, "下载失败", e)
                                        isDownloading = false
                                        downloadError = e.message ?: "未知错误"
                                }
                        }
        }

        val scrollState = rememberScrollState()

        Column(
                modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                // Seção de Status
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF13151A)),
                        shape = RoundedCornerShape(12.dp)
                ) {
                        Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                                Text(
                                        "系统状态",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                )

                                HorizontalDivider(color = Color(0xFF1D2430))

                                if (!bypassSelfInstallationCheck) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                Text(
                                                        "安装成功：",
                                                        color = Color(0xFFB0B8C4)
                                                )
                                                Text(
                                                        if (selfInstallationCheck) "是" else "否",
                                                        color =
                                                                if (selfInstallationCheck)
                                                                        Color(0xFF4ADE80)
                                                                else Color(0xFFEF4444)
                                                )
                                        }
                                }

                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                        Text("状态：", color = Color(0xFFB0B8C4))
                                        Text(
                                                if (isActive) "已启用" else "未启用",
                                                color =
                                                        if (isActive) Color(0xFF4ADE80)
                                                        else Color(0xFFEF4444),
                                                fontWeight = FontWeight.Medium
                                        )
                                }

                                if (isActive) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                Text(
                                                        "Boot Completed:",
                                                        color = Color(0xFFB0B8C4),
                                                        fontSize = 14.sp
                                                )
                                                Text(
                                                        formattedTime,
                                                        color = Color.White,
                                                        fontSize = 14.sp
                                                )
                                        }
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                Text(
                                                        "开始：",
                                                        color = Color(0xFFB0B8C4),
                                                        fontSize = 14.sp
                                                )
                                                Text(
                                                        formattedTime2,
                                                        color = Color.White,
                                                        fontSize = 14.sp
                                                )
                                        }
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                Text(
                                                        "初始化：",
                                                        color = Color(0xFFB0B8C4),
                                                        fontSize = 14.sp
                                                )
                                                Text(
                                                        formattedTime3,
                                                        color = Color.White,
                                                        fontSize = 14.sp
                                                )
                                        }
                                }

                                HorizontalDivider(color = Color(0xFF1D2430))

                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Column {
                                                Text(
                                                        "版本",
                                                        color = Color(0xFFB0B8C4),
                                                        fontSize = 14.sp
                                                )
                                                Text(
                                                        version,
                                                        color = Color.White,
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        modifier =
                                                                Modifier.clickable {
                                                                        clickCount++
                                                                        if (clickCount >= 5) {
                                                                                showAdvancedDialog =
                                                                                        true
                                                                                clickCount = 0
                                                                        }
                                                                }
                                                )
                                        }

                                        Button(
                                                onClick = {
                                                        isCheckingUpdates = true
                                                        scope.launch {
                                                                val result = getAllReleaseInfo()
                                                                updateCheckResult = result
                                                                isCheckingUpdates = false
                                                                showUpdateCheckDialog = true
                                                        }
                                                },
                                                modifier = Modifier.height(48.dp),
                                                colors =
                                                        ButtonDefaults.buttonColors(
                                                                containerColor = AppColors.Primary
                                                        ),
                                                shape =
                                                        RoundedCornerShape(
                                                                AppDimensions.ButtonCornerRadius
                                                        )
                                        ) {
                                                Icon(
                                                        Icons.Default.Refresh,
                                                        contentDescription = "检查更新",
                                                        modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("检查更新", fontSize = 14.sp)
                                        }
                                }

                                HorizontalDivider(color = Color(0xFF1D2430))

                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                ) {
                                        Button(
                                                onClick = {
                                                        val intent =
                                                                Intent(Intent.ACTION_MAIN).apply {
                                                                        component =
                                                                                ComponentName(
                                                                                        "com.android.settings",
                                                                                        "com.android.settings.Settings"
                                                                                )
                                                                }
                                                        context.startActivity(intent)
                                                },
                                                modifier = Modifier.height(48.dp),
                                                colors =
                                                        ButtonDefaults.buttonColors(
                                                                containerColor = AppColors.Primary
                                                        ),
                                                shape =
                                                        RoundedCornerShape(
                                                                AppDimensions.ButtonCornerRadius
                                                        )
                                        ) {
                                                Icon(
                                                        Icons.Default.Settings,
                                                        contentDescription = "设置",
                                                        modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                        "打开 Android 设置",
                                                        color = Color.White
                                                )
                                        }
                                }
                        }
                }

                // Seção de Versão Estável
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF13151A)),
                        shape = RoundedCornerShape(12.dp)
                ) {
                        Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                                Text(
                                        "稳定版本",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                )

                                HorizontalDivider(color = Color(0xFF1D2430))

                                Text(
                                        "重新安装参考稳定版（v$stableVersionName）。" +
                                                "若新版本出现问题，可用它回到可靠的基础版本。",
                                        fontSize = 14.sp,
                                        color = Color(0xFFB0B8C4),
                                        lineHeight = 20.sp
                                )

                                if (version == stableVersionName) {
                                        Text(
                                                "当前已是稳定版。",
                                                fontSize = 13.sp,
                                                color = Color(0xFF4ADE80)
                                        )
                                }

                                Button(
                                        onClick = { showRevertDialog = true },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFFFF9800)
                                                ),
                                        shape =
                                                RoundedCornerShape(
                                                        AppDimensions.ButtonCornerRadius
                                                )
                                ) {
                                        Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                                if (version == stableVersionName)
                                                        "重装稳定版"
                                                else
                                                        "返回稳定版（v$stableVersionName）",
                                                fontSize = 14.sp,
                                                color = Color.White
                                        )
                                }
                        }
                }

                // Seção de Contribuição
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF13151A)),
                        shape = RoundedCornerShape(12.dp)
                ) {
                        Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                                Text(
                                        "支持项目开发",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                )

                                HorizontalDivider(color = Color(0xFF1D2430))

                                Text(
                                        "支持本项目的持续开发！你的贡献对应用后续维护非常重要。",
                                        fontSize = 14.sp,
                                        color = Color(0xFFB0B8C4),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp
                                )

                                // QR Code
                                Image(
                                        painter = painterResource(id = R.drawable.qrcode),
                                        contentDescription = "捐赠二维码",
                                        modifier = Modifier.size(200.dp).padding(8.dp),
                                        contentScale = ContentScale.Fit
                                )

                                Text(
                                        "扫描二维码或使用 PIX 捐赠：joaovitorbor@gmail.com",
                                        fontSize = 16.sp,
                                        color = Color(0xFFB0B8C4),
                                        textAlign = TextAlign.Center
                                )

                                Text(
                                        "感谢你的支持！🙏",
                                        fontSize = 14.sp,
                                        color = Color(0xFF4ADE80),
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                )
                        }
                }
        }

        if (showAdvancedDialog) {
                AlertDialog(
                        onDismissRequest = { showAdvancedDialog = false },
                        title = { Text("确认") },
                        text = {
                                Text(
                                        "要启用高级模式吗？可能导致系统不稳定，请自行承担风险。"
                                )
                        },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                showAdvancedDialog = false
                                                prefs.edit {
                                                        putBoolean(
                                                                SharedPreferencesKeys.ADVANCE_USE
                                                                        .key,
                                                                true
                                                        )
                                                }
                                        }
                                ) { Text("启用") }
                        },
                        dismissButton = {
                                TextButton(onClick = { showAdvancedDialog = false }) {
                                        Text("取消")
                                }
                        }
                )
        }

        if (showUpdateDialog) {
                AlertDialog(
                        onDismissRequest = { showUpdateDialog = false },
                        title = { Text("更新检查") },
                        text = { Text(updateMessage) },
                        confirmButton = {
                                TextButton(onClick = { showUpdateDialog = false }) { Text("确定") }
                        }
                )
        }

        if (isCheckingUpdates) {
                AlertDialog(
                        onDismissRequest = {},
                        title = { Text("正在检查更新…") },
                        text = {
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        Text("正在获取可用版本…")
                                }
                        },
                        confirmButton = {}
                )
        }

        if (showUpdateCheckDialog && updateCheckResult != null) {
                val result = updateCheckResult!!
                val isPreviewChannel = version.contains("-preview")
                val currentChannel = if (isPreviewChannel) "测试版" else "稳定版"
                val currentClean = version.removePrefix("v")

                AlertDialog(
                        onDismissRequest = { showUpdateCheckDialog = false },
                        title = { Text("更新") },
                        text = {
                                Column(
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                ) {
                                        // Canal atual
                                        Text(
                                                "当前通道：$currentChannel ($version)",
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp
                                        )

                                        if (isPreviewChannel) {
                                                // --- Usuário está em Preview ---
                                                val hasPreviewUpdate =
                                                        result.latestPreview != null &&
                                                                compareVersions(
                                                                        result.latestPreview.tag
                                                                                .removePrefix("v"),
                                                                        currentClean
                                                                ) > 0
                                                val hasReleaseUpgrade =
                                                        result.latestRelease != null &&
                                                                compareVersions(
                                                                        result.latestRelease.tag
                                                                                .removePrefix("v"),
                                                                        currentClean
                                                                ) > 0

                                                // Preview mais nova?
                                                if (hasPreviewUpdate) {
                                                        Card(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                colors =
                                                                        CardDefaults.cardColors(
                                                                                containerColor =
                                                                                        Color(
                                                                                                0xFF1A1D24
                                                                                        )
                                                                        )
                                                        ) {
                                                                Column(
                                                                        modifier =
                                                                                Modifier.padding(
                                                                                        12.dp
                                                                                )
                                                                ) {
                                                                        Text(
                                                                                "新测试版：${result.latestPreview!!.tag}",
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold,
                                                                                fontSize = 14.sp
                                                                        )
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.height(
                                                                                                8.dp
                                                                                        )
                                                                        )
                                                                        Button(
                                                                                onClick = {
                                                                                        showUpdateCheckDialog =
                                                                                                false
                                                                                        startDownload(
                                                                                                result.latestPreview
                                                                                                        .downloadUrl
                                                                                        )
                                                                                },
                                                                                modifier =
                                                                                        Modifier.align(
                                                                                                Alignment
                                                                                                        .End
                                                                                        ),
                                                                                colors =
                                                                                        ButtonDefaults
                                                                                                .buttonColors(
                                                                                                        containerColor =
                                                                                                                AppColors
                                                                                                                        .Primary
                                                                                                ),
                                                                                shape =
                                                                                        RoundedCornerShape(
                                                                                                AppDimensions
                                                                                                        .ButtonCornerRadius
                                                                                        )
                                                                        ) { Text("更新") }
                                                                }
                                                        }
                                                }

                                                // Release disponível para voltar ao estável (só se
                                                // build number maior —
                                                // Intent não permite downgrade)
                                                if (hasReleaseUpgrade) {
                                                        Card(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                colors =
                                                                        CardDefaults.cardColors(
                                                                                containerColor =
                                                                                        Color(
                                                                                                0xFF1A1D24
                                                                                        )
                                                                        )
                                                        ) {
                                                                Column(
                                                                        modifier =
                                                                                Modifier.padding(
                                                                                        12.dp
                                                                                )
                                                                ) {
                                                                        Text(
                                                                                "稳定版：${result.latestRelease!!.tag}",
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold,
                                                                                fontSize = 14.sp
                                                                        )
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.height(
                                                                                                4.dp
                                                                                        )
                                                                        )
                                                                        Text(
                                                                                "返回稳定版时应用数据会被重置。",
                                                                                fontSize = 12.sp,
                                                                                color =
                                                                                        Color(
                                                                                                0xFFFF9800
                                                                                        )
                                                                        )
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.height(
                                                                                                8.dp
                                                                                        )
                                                                        )
                                                                        Button(
                                                                                onClick = {
                                                                                        showUpdateCheckDialog =
                                                                                                false
                                                                                        startDownload(
                                                                                                url =
                                                                                                        result.latestRelease
                                                                                                                .downloadUrl,
                                                                                                resetTargetVersion =
                                                                                                        result.latestRelease
                                                                                                                .tag
                                                                                                                .removePrefix(
                                                                                                                        "v"
                                                                                                                )
                                                                                        )
                                                                                },
                                                                                modifier =
                                                                                        Modifier.align(
                                                                                                Alignment
                                                                                                        .End
                                                                                        ),
                                                                                colors =
                                                                                        ButtonDefaults
                                                                                                .buttonColors(
                                                                                                        containerColor =
                                                                                                                Color(
                                                                                                                        0xFFFF9800
                                                                                                                )
                                                                                                ),
                                                                                shape =
                                                                                        RoundedCornerShape(
                                                                                                AppDimensions
                                                                                                        .ButtonCornerRadius
                                                                                        )
                                                                        ) {
                                                                                Text(
                                                                                        "返回稳定版"
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                }

                                                if (!hasPreviewUpdate && !hasReleaseUpgrade) {
                                                        Text(
                                                                "当前已是最新版本",
                                                                fontSize = 14.sp,
                                                                color = Color(0xFF4ADE80)
                                                        )
                                                }
                                        } else {
                                                // --- Usuário está em Release (Estável) ---
                                                val hasReleaseUpdate =
                                                        result.latestRelease != null &&
                                                                compareVersions(
                                                                        result.latestRelease.tag
                                                                                .removePrefix("v"),
                                                                        currentClean
                                                                ) > 0
                                                val hasPreviewAvailable =
                                                        showBetaUpdates &&
                                                                result.latestPreview != null &&
                                                                compareVersions(
                                                                        result.latestPreview.tag
                                                                                .removePrefix("v"),
                                                                        currentClean
                                                                ) > 0

                                                // Update estável disponível?
                                                if (hasReleaseUpdate) {
                                                        Card(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                colors =
                                                                        CardDefaults.cardColors(
                                                                                containerColor =
                                                                                        Color(
                                                                                                0xFF1A1D24
                                                                                        )
                                                                        )
                                                        ) {
                                                                Column(
                                                                        modifier =
                                                                                Modifier.padding(
                                                                                        12.dp
                                                                                )
                                                                ) {
                                                                        Text(
                                                                                "新版本：${result.latestRelease.tag}",
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold,
                                                                                fontSize = 14.sp
                                                                        )
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.height(
                                                                                                8.dp
                                                                                        )
                                                                        )
                                                                        Button(
                                                                                onClick = {
                                                                                        showUpdateCheckDialog =
                                                                                                false
                                                                                        startDownload(
                                                                                                result.latestRelease
                                                                                                        .downloadUrl
                                                                                        )
                                                                                },
                                                                                modifier =
                                                                                        Modifier.align(
                                                                                                Alignment
                                                                                                        .End
                                                                                        ),
                                                                                colors =
                                                                                        ButtonDefaults
                                                                                                .buttonColors(
                                                                                                        containerColor =
                                                                                                                AppColors
                                                                                                                        .Primary
                                                                                                ),
                                                                                shape =
                                                                                        RoundedCornerShape(
                                                                                                AppDimensions
                                                                                                        .ButtonCornerRadius
                                                                                        )
                                                                        ) { Text("更新") }
                                                                }
                                                        }
                                                }

                                                // Toggle beta
                                                HorizontalDivider(color = Color(0xFF1D2430))
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween,
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Text(
                                                                "显示测试版",
                                                                fontSize = 14.sp
                                                        )
                                                        Switch(
                                                                checked = showBetaUpdates,
                                                                onCheckedChange = {
                                                                        showBetaUpdates = it
                                                                        prefs.edit()
                                                                                .putBoolean(
                                                                                        SharedPreferencesKeys
                                                                                                .SHOW_BETA_UPDATES
                                                                                                .key,
                                                                                        it
                                                                                )
                                                                                .apply()
                                                                },
                                                                modifier = Modifier.scale(0.9f),
                                                                colors =
                                                                        SwitchDefaults.colors(
                                                                                checkedThumbColor =
                                                                                        br.com
                                                                                                .redesurftank
                                                                                                .havalshisuku
                                                                                                .ui
                                                                                                .components
                                                                                                .AppColors
                                                                                                .TextPrimary,
                                                                                checkedTrackColor =
                                                                                        br.com
                                                                                                .redesurftank
                                                                                                .havalshisuku
                                                                                                .ui
                                                                                                .components
                                                                                                .AppColors
                                                                                                .Primary,
                                                                                uncheckedThumbColor =
                                                                                        br.com
                                                                                                .redesurftank
                                                                                                .havalshisuku
                                                                                                .ui
                                                                                                .components
                                                                                                .AppColors
                                                                                                .TextSecondary,
                                                                                uncheckedTrackColor =
                                                                                        br.com
                                                                                                .redesurftank
                                                                                                .havalshisuku
                                                                                                .ui
                                                                                                .components
                                                                                                .AppColors
                                                                                                .ButtonSecondary,
                                                                                uncheckedBorderColor =
                                                                                        Color.Transparent,
                                                                                checkedBorderColor =
                                                                                        Color.Transparent
                                                                        )
                                                        )
                                                }

                                                // Preview disponível (só aparece se toggle ativo)
                                                if (hasPreviewAvailable) {
                                                        Text(
                                                                "测试版面向发烧友和技术型用户，可能包含缺陷、不稳定或不完整功能，请自行承担风险。",
                                                                fontSize = 11.sp,
                                                                color = Color(0xFFFF9800),
                                                                lineHeight = 14.sp
                                                        )
                                                        Card(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                colors =
                                                                        CardDefaults.cardColors(
                                                                                containerColor =
                                                                                        Color(
                                                                                                0xFF1A1D24
                                                                                        )
                                                                        )
                                                        ) {
                                                                Column(
                                                                        modifier =
                                                                                Modifier.padding(
                                                                                        12.dp
                                                                                )
                                                                ) {
                                                                        Text(
                                                                                "测试版：${result.latestPreview!!.tag}",
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold,
                                                                                fontSize = 14.sp,
                                                                                color =
                                                                                        Color(
                                                                                                0xFFFF9800
                                                                                        )
                                                                        )
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.height(
                                                                                                4.dp
                                                                                        )
                                                                        )
                                                                        Text(
                                                                                "实验版本，可能包含缺陷或系统不稳定。",
                                                                                fontSize = 12.sp,
                                                                                color =
                                                                                        Color(
                                                                                                0xFFB0B8C4
                                                                                        )
                                                                        )
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.height(
                                                                                                8.dp
                                                                                        )
                                                                        )
                                                                        Button(
                                                                                onClick = {
                                                                                        showUpdateCheckDialog =
                                                                                                false
                                                                                        startDownload(
                                                                                                result.latestPreview
                                                                                                        .downloadUrl
                                                                                        )
                                                                                },
                                                                                modifier =
                                                                                        Modifier.align(
                                                                                                Alignment
                                                                                                        .End
                                                                                        ),
                                                                                colors =
                                                                                        ButtonDefaults
                                                                                                .buttonColors(
                                                                                                        containerColor =
                                                                                                                Color(
                                                                                                                        0xFFFF9800
                                                                                                                )
                                                                                                ),
                                                                                shape =
                                                                                        RoundedCornerShape(
                                                                                                AppDimensions
                                                                                                        .ButtonCornerRadius
                                                                                        )
                                                                        ) { Text("安装测试版") }
                                                                }
                                                        }
                                                }

                                                if (!hasReleaseUpdate && !hasPreviewAvailable) {
                                                        Text(
                                                                "当前已是最新版本",
                                                                fontSize = 14.sp,
                                                                color = Color(0xFF4ADE80)
                                                        )
                                                }
                                        }
                                }
                        },
                        confirmButton = {
                                TextButton(onClick = { showUpdateCheckDialog = false }) {
                                        Text("关闭")
                                }
                        }
                )
        }

        if (showRevertDialog) {
                AlertDialog(
                        onDismissRequest = { showRevertDialog = false },
                        title = { Text("返回稳定版") },
                        text = {
                                Text(
                                        "将下载并安装稳定版 v$stableVersionName。\n\n" +
                                                "应用的数据和设置将被重置（此次安装可能是降级）。 " +
                                                "需要 Shizuku 已授权，是否继续？"
                                )
                        },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                showRevertDialog = false
                                                startDownload(
                                                        url = stableApkUrl,
                                                        resetTargetVersion = stableVersionName,
                                                        forceDowngrade = true
                                                )
                                        }
                                ) { Text("继续") }
                        },
                        dismissButton = {
                                TextButton(onClick = { showRevertDialog = false }) {
                                        Text("取消")
                                }
                        }
                )
        }

        if (isDownloading) {
                AlertDialog(
                        onDismissRequest = {},
                        title = { Text("正在下载更新") },
                        text = {
                                Column {
                                        LinearProgressIndicator(progress = { downloadProgress })
                                        Text("${(downloadProgress * 100).toInt()}%")
                                }
                        },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                downloadJob?.cancel()
                                                isDownloading = false
                                        }
                                ) { Text("取消") }
                        }
                )
        }

        if (downloadError != null) {
                AlertDialog(
                        onDismissRequest = { downloadError = null },
                        title = { Text("下载失败") },
                        text = { Text(downloadError!!) },
                        confirmButton = {
                                TextButton(onClick = { downloadError = null }) { Text("确定") }
                        },
                        dismissButton = {
                                TextButton(onClick = { downloadError = null }) { Text("取消") }
                        }
                )
        }

        if (showPermissionDialog) {
                AlertDialog(
                        onDismissRequest = { showPermissionDialog = false },
                        title = { Text("需要权限") },
                        text = { Text("请允许安装未知来源应用。") },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                showPermissionDialog = false
                                                val intent =
                                                        Intent(
                                                                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
                                                                )
                                                                .apply {
                                                                        data =
                                                                                Uri.parse(
                                                                                        "package:${context.packageName}"
                                                                                )
                                                                }
                                                requestPermissionLauncher.launch(intent)
                                        }
                                ) { Text("设置") }
                        },
                        dismissButton = {
                                TextButton(onClick = { showPermissionDialog = false }) {
                                        Text("取消")
                                }
                        }
                )
        }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
        HavalShisukuTheme { MainScreen() }
}
