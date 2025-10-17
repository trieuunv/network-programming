package org.np

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import org.np.ui.mailregister.MailRegisterScreen
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.np.ui.chatmulticast.ChatMulticastScreen
import org.np.ui.register.RegisterScreen
import org.np.ui.setup.SetUpScreen

val customFont = FontFamily(
    Font("fonts/roboto.ttf")
)

val customTypography = Typography(
    bodyLarge = TextStyle(fontFamily = customFont, fontSize = 16.sp),
    titleLarge = TextStyle(fontFamily = customFont, fontSize = 24.sp)
)

@Composable
@Preview
fun App() {
    MaterialTheme(typography = customTypography) {
        Navigator(ChatMulticastScreen()) {
            navigator -> SlideTransition(navigator)
        }
    }
}