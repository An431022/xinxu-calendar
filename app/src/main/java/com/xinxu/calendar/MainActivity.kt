package com.xinxu.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.xinxu.calendar.ui.calendar.MonthScreen
import com.xinxu.calendar.ui.theme.PaletteOverrideStore
import com.xinxu.calendar.ui.theme.XinxuCalendarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // 用户改过的色板要在第一帧之前就位，否则界面会先用出厂色画一遍再跳。
        PaletteOverrideStore.ensureLoaded(this)
        setContent {
            XinxuCalendarTheme {
                // 故意不要 TopAppBar：月视图的标题（"2026年10月"）本身就是页头，
                // 再压一条 AppBar 上去，屏幕上就只剩表格了。
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MonthScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
