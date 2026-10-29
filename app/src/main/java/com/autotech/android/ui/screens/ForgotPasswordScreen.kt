package com.autotech.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.autotech.android.AutoTechApplication
import com.autotech.android.domain.PasswordResetRules
import com.autotech.android.ui.components.AtButtonStyle
import com.autotech.android.ui.components.AtCard
import com.autotech.android.ui.components.AtCardTitle
import com.autotech.android.ui.components.AtField
import com.autotech.android.ui.components.AtHeader
import com.autotech.android.ui.components.AtMessage
import com.autotech.android.ui.components.AtPillButton
import com.autotech.android.ui.components.AtScreen
import com.autotech.android.ui.theme.AtColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * RF-01: recuperação de senha. Etapa 1: e-mail cadastrado → código de 6 números enviado por e-mail.
 * Etapa 2: código + nova senha. Depois volta ao login.
 */
@Composable
fun ForgotPasswordScreen(initialEmail: String, onBack: () -> Unit, onDone: () -> Unit) {
    val app = LocalContext.current.applicationContext as AutoTechApplication
    val repo = app.graph.passwordResetRepository
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf(initialEmail) }
    var codeSent by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    var resendIn by remember { mutableLongStateOf(0L) }

    // contagem para o "Reenviar código"
    LaunchedEffect(codeSent, resendIn) {
        if (codeSent && resendIn > 0) {
            delay(1000)
            resendIn = repo.secondsUntilResend(email)
        }
    }

    fun send() {
        scope.launch {
            loading = true
            error = null
            repo.requestCode(email)
                .onSuccess {
                    codeSent = true
                    code = ""
                    resendIn = repo.secondsUntilResend(email)
                }
                .onFailure { error = it.message }
            loading = false
        }
    }

    fun reset() {
        scope.launch {
            loading = true
            error = null
            repo.resetPassword(email, code, password, confirmation)
                .onSuccess { done = true }
                .onFailure { error = it.message }
            loading = false
        }
    }

    AtScreen {
        AtHeader("Esqueci minha senha", "Recuperação pelo e-mail cadastrado", onBack = onBack)

        when {
            done -> AtCard(Modifier.fillMaxWidth()) {
                AtCardTitle("Senha alterada", "Já pode entrar com a nova senha")
                Spacer(Modifier.height(14.dp))
                AtPillButton("Voltar para o login", onDone, Modifier.fillMaxWidth())
            }

            !codeSent -> AtCard(Modifier.fillMaxWidth()) {
                AtCardTitle("Seu e-mail", "Enviaremos um código de ${PasswordResetRules.CODE_LENGTH} números")
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AtField(
                        email, { email = it }, "E-mail cadastrado",
                        leadingIcon = Icons.Rounded.Mail,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    error?.let { AtMessage(it, isError = true) }
                    AtPillButton("Enviar código", { send() }, Modifier.fillMaxWidth(), leadingIcon = Icons.Rounded.Send, loading = loading)
                    Text(
                        "É preciso estar conectado à internet. Contas criadas com o Google não têm senha: entre pelo botão “Continuar com o Google”.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AtColors.Muted
                    )
                }
            }

            else -> AtCard(Modifier.fillMaxWidth()) {
                AtCardTitle("Código e nova senha", "O código vale 15 minutos")
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AtMessage("Enviamos o código para $email. Se não aparecer, veja também a caixa de spam.", isError = false)
                    AtField(
                        code, { v -> code = v.filter { it.isDigit() }.take(PasswordResetRules.CODE_LENGTH) }, "Código",
                        placeholder = "000000",
                        leadingIcon = Icons.Rounded.Pin,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    val toggle: @Composable () -> Unit = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                if (showPassword) "Ocultar senha" else "Mostrar senha"
                            )
                        }
                    }
                    AtField(
                        password, { password = it }, "Nova senha",
                        leadingIcon = Icons.Rounded.Lock,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailing = toggle
                    )
                    AtField(
                        confirmation, { confirmation = it }, "Repita a nova senha",
                        leadingIcon = Icons.Rounded.Lock,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
                    )
                    error?.let { AtMessage(it, isError = true) }
                    AtPillButton("Redefinir senha", { reset() }, Modifier.fillMaxWidth(), loading = loading)
                    AtPillButton(
                        if (resendIn > 0) "Reenviar código em $resendIn s" else "Reenviar código",
                        { send() },
                        Modifier.fillMaxWidth(),
                        style = AtButtonStyle.Light,
                        enabled = resendIn == 0L && !loading
                    )
                    AtPillButton(
                        "Usar outro e-mail",
                        { codeSent = false; error = null },
                        Modifier.fillMaxWidth(),
                        style = AtButtonStyle.Soft
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
