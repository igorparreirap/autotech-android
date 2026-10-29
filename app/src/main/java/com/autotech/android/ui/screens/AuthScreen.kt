package com.autotech.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.autotech.android.AutoTechApplication
import com.autotech.android.BuildConfig
import com.autotech.android.ui.components.AtButtonStyle
import com.autotech.android.ui.components.AtCard
import com.autotech.android.ui.components.AtField
import com.autotech.android.ui.components.AtLogo
import com.autotech.android.ui.components.AtMessage
import com.autotech.android.ui.components.AtPillButton
import com.autotech.android.ui.components.AtScreen
import com.autotech.android.ui.components.AtSegmented
import com.autotech.android.ui.components.Reveal
import com.autotech.android.ui.theme.AtColors
import com.autotech.android.work.AlertScheduler
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(onSignedIn: () -> Unit, onForgotPassword: (String) -> Unit = {}) {
    val context = LocalContext.current
    val app = context.applicationContext as AutoTechApplication
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var registerMode by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }

    val credentialManager = remember { CredentialManager.create(context) }

    fun submit() {
        scope.launch {
            loading = true
            error = null
            val result = if (registerMode) {
                app.graph.authRepository.register(email, password, name)
            } else {
                app.graph.authRepository.login(email, password)
            }
            result
                .onSuccess { id ->
                    app.setSession(id)
                    AlertScheduler.enqueuePeriodic(app)
                    onSignedIn()
                }
                .onFailure { error = it.message ?: "Erro desconhecido." }
            loading = false
        }
    }

    fun signInWithGoogle() {
        scope.launch {
            val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
            if (clientId.isBlank() || clientId == "YOUR_WEB_CLIENT_ID") {
                error = "Login com Google não configurado: defina GOOGLE_WEB_CLIENT_ID no local.properties."
                return@launch
            }
            loading = true
            error = null
            try {
                // Fluxo do botão "Continuar com o Google": abre o seletor de contas
                // do Google (e permite adicionar uma conta, se não houver nenhuma).
                val signInOption = GetSignInWithGoogleOption.Builder(clientId).build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(signInOption)
                    .build()
                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    // RNF-02: o token do Google é usado só aqui, para identificar o usuário, e não é
                    // gravado em lugar nenhum; da sessão guarda-se apenas o id interno (SharedPreferences privado).
                    val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                    app.graph.authRepository.signInWithGoogle(
                        email = googleCred.id,
                        displayName = googleCred.displayName ?: googleCred.id
                    ).onSuccess { id ->
                        app.setSession(id)
                        AlertScheduler.enqueuePeriodic(app)
                        onSignedIn()
                    }.onFailure { err ->
                        error = err.message ?: "Erro ao autenticar."
                    }
                } else {
                    error = "Tipo de credencial não suportado."
                }
            } catch (_: GetCredentialCancellationException) {
                // cancelado pelo usuário — não exibe erro
            } catch (e: NoCredentialException) {
                error = "Nenhuma conta Google disponível neste dispositivo. " +
                    "Detalhe: ${e.localizedMessage}"
            } catch (e: GetCredentialException) {
                // Erro mais comum em desenvolvimento: SHA-1 do app ou Client ID Web
                // diferentes dos cadastrados no Google Cloud (código 10 / 28444).
                error = "Falha no login com Google (${e.type}). Confira o Client ID Web, " +
                    "o SHA-1 do app no Google Cloud e se seu e-mail está como usuário de teste. " +
                    "Detalhe: ${e.localizedMessage}"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = "Falha no login com Google: ${e.localizedMessage}"
            }
            loading = false
        }
    }

    AtScreen(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Reveal(0) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AtLogo(42.dp)
                Spacer(Modifier.width(10.dp))
                Text("AutoTech", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .background(AtColors.Card, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .padding(end = 6.dp)
                                .background(AtColors.Accent, CircleShape)
                                .padding(4.dp)
                        )
                        Text("Offline-first", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        Reveal(1) {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 18.dp)) {
                Text(
                    buildAnnotatedString {
                        append("Sua manutenção,\n")
                        withStyle(SpanStyle(color = AtColors.Muted)) { append("sob controle.") }
                    },
                    style = MaterialTheme.typography.displaySmall
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Revisões por tempo e quilometragem, abastecimentos e serviços próximos em um só lugar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AtColors.Muted
                )
            }
        }

        Reveal(2) {
            AtCard(Modifier.fillMaxWidth()) {
                AtSegmented(
                    options = listOf("Entrar", "Criar conta"),
                    selectedIndex = if (registerMode) 1 else 0,
                    onSelect = { registerMode = it == 1; error = null },
                    container = AtColors.Field
                )
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AnimatedVisibility(
                        visible = registerMode,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        AtField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Nome",
                            leadingIcon = Icons.Rounded.Person
                        )
                    }
                    AtField(
                        value = email,
                        onValueChange = { email = it },
                        label = "E-mail",
                        leadingIcon = Icons.Rounded.Mail,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    AtField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Senha",
                        leadingIcon = Icons.Rounded.Lock,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailing = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    if (showPassword) "Ocultar senha" else "Mostrar senha"
                                )
                            }
                        }
                    )
                    // RF-01: recuperação de senha por código enviado ao e-mail
                    if (!registerMode) {
                        androidx.compose.material3.TextButton(
                            onClick = { onForgotPassword(email.trim()) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Esqueci minha senha", color = AtColors.Primary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                error?.let {
                    Spacer(Modifier.height(12.dp))
                    AtMessage(it, isError = true)
                }

                Spacer(Modifier.height(16.dp))
                AtPillButton(
                    text = if (registerMode) "Cadastrar" else "Entrar",
                    onClick = { submit() },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.AutoMirrored.Rounded.ArrowForward,
                    loading = loading
                )

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(Modifier.weight(1f), color = AtColors.Line)
                    Text("ou", style = MaterialTheme.typography.bodySmall, color = AtColors.Muted, modifier = Modifier.padding(horizontal = 12.dp))
                    HorizontalDivider(Modifier.weight(1f), color = AtColors.Line)
                }

                GoogleButton(enabled = !loading, onClick = { signInWithGoogle() })
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GoogleButton(enabled: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = CircleShape,
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF747775))
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "G" nas cores oficiais do Google (azul dominante)
            Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.width(12.dp))
            Text("Continuar com o Google", color = Color(0xFF1F1F1F), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}
