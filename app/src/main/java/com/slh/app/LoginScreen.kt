package com.slh.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun LoginScreen(
    onLoginSuccess: (DemoUser) -> Unit
) {

    // =================================================================
    // STATE
    // =================================================================

    var selectedRole by remember {
        mutableStateOf(UserRole.STUDENT)
    }

    var username by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    val branding =
        MainLoginBrandingStore.branding


    // =================================================================
    // RESPONSIVE DIMENSIONS
    // =================================================================

    val density = LocalDensity.current

    /*
     * Screen height ko dp mein read kar rahe hain.
     *
     * Isse chhote mobile par:
     * - logo chhota
     * - fonts chhote
     * - spacing chhoti
     *
     * aur bade mobile par:
     * - UI thoda spacious
     * rahega.
     */
    val screenHeightDp =
        LocalConfiguration.current.screenHeightDp.dp

    val isSmallScreen =
        screenHeightDp < 700.dp

    val isVerySmallScreen =
        screenHeightDp < 620.dp


    // =================================================================
    // KEYBOARD
    // =================================================================

    val loginScrollState =
        rememberScrollState()

    val isKeyboardVisible =
        WindowInsets.ime.getBottom(density) > 0

    val usernameBringIntoViewRequester =
        remember {
            BringIntoViewRequester()
        }

    val passwordBringIntoViewRequester =
        remember {
            BringIntoViewRequester()
        }

    val loginButtonBringIntoViewRequester =
        remember {
            BringIntoViewRequester()
        }

    // Keyboard open → scroll Login button near keyboard
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible) {
            delay(280)
            loginButtonBringIntoViewRequester.bringIntoView()
        }
    }



    // =================================================================
    // RESPONSIVE VALUES
    // =================================================================

    val topSpace =
        when {
            isKeyboardVisible -> 8.dp
            isVerySmallScreen -> 28.dp
            isSmallScreen -> 36.dp
            else -> 48.dp
        }

    val logoSize =
        when {
            isKeyboardVisible -> {
                when {
                    isVerySmallScreen -> 56.dp
                    isSmallScreen -> 64.dp
                    else -> 72.dp
                }
            }
            isVerySmallScreen -> 88.dp
            isSmallScreen -> 102.dp
            else -> 116.dp
        }

    val logoCircleSize =
        when {
            isKeyboardVisible -> logoSize + 12.dp
            isVerySmallScreen -> 104.dp
            isSmallScreen -> 118.dp
            else -> 132.dp
        }

    val cardCorner =
        when {
            isVerySmallScreen -> 22.dp
            isSmallScreen -> 24.dp
            else -> 28.dp
        }

    val cardHorizontalPadding =
        when {
            isVerySmallScreen -> 15.dp
            isSmallScreen -> 18.dp
            else -> 21.dp
        }

    val cardVerticalPadding =
        when {
            isVerySmallScreen -> 15.dp
            isSmallScreen -> 17.dp
            else -> 21.dp
        }

    val titleSize =
        when {
            isVerySmallScreen -> 19.sp
            isSmallScreen -> 20.sp
            else -> 22.sp
        }

    val fieldHeight =
        when {
            isVerySmallScreen -> 52.dp
            isSmallScreen -> 54.dp
            else -> 56.dp
        }

    val roleButtonHeight =
        when {
            isVerySmallScreen -> 44.dp
            isSmallScreen -> 47.dp
            else -> 50.dp
        }

    val loginButtonHeight =
        when {
            isVerySmallScreen -> 50.dp
            isSmallScreen -> 52.dp
            else -> 54.dp
        }


    // =================================================================
    // BACK
    // =================================================================

    BackHandler {
        // Login screen par back press intentionally no action.
    }


    // =================================================================
    // MAIN SCREEN
    // =================================================================

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.08f
                        ),
                        MaterialTheme.colorScheme.tertiary.copy(
                            alpha = 0.05f
                        ),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {

        // =============================================================
        // DECORATIVE TOP CIRCLE
        // =============================================================

        Box(
            modifier = Modifier
                .size(
                    if (isSmallScreen) 140.dp else 185.dp
                )
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primary.copy(
                        alpha = 0.10f
                    )
                )
        )


        // =============================================================
        // DECORATIVE BOTTOM CIRCLE
        // =============================================================

        Box(
            modifier = Modifier
                .size(
                    if (isSmallScreen) 110.dp else 145.dp
                )
                .align(Alignment.BottomStart)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.tertiary.copy(
                        alpha = 0.08f
                    )
                )
        )


        // =============================================================
        // MAIN COLUMN
        // =============================================================

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            // =========================================================
            // LOGIN CONTENT
            // =========================================================

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()

                    /*
                     * Keyboard closed:
                     * normal layout.
                     *
                     * Keyboard open:
                     * scroll enabled.
                     */
                    .verticalScroll(loginScrollState)

                    /*
                     * IME padding content ko keyboard ke
                     * upar rakhta hai.
                     */
                    .imePadding()

                    .navigationBarsPadding()

                    .padding(
                        horizontal =
                            if (isSmallScreen) {
                                12.dp
                            } else {
                                18.dp
                            }
                    ),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                // =====================================================
                // TOP SPACE
                // =====================================================

                Spacer(
                    modifier = Modifier.height(topSpace)
                )


                // =====================================================
                // MAINTENANCE MODE
                // =====================================================

                if (PlatformSettingsStore.maintenanceMode) {

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 430.dp),

                        shape =
                            RoundedCornerShape(
                                if (isSmallScreen) {
                                    12.dp
                                } else {
                                    14.dp
                                }
                            ),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .errorContainer
                            )
                    ) {

                        Row(
                            modifier =
                                Modifier.padding(
                                    if (isSmallScreen) {
                                        11.dp
                                    } else {
                                        14.dp
                                    }
                                ),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Settings,

                                contentDescription =
                                    null,

                                tint =
                                    MaterialTheme
                                        .colorScheme
                                        .onErrorContainer,

                                modifier =
                                    Modifier.size(
                                        if (isSmallScreen) {
                                            20.dp
                                        } else {
                                            24.dp
                                        }
                                    )
                            )

                            Spacer(
                                modifier =
                                    Modifier.size(
                                        if (isSmallScreen) {
                                            8.dp
                                        } else {
                                            10.dp
                                        }
                                    )
                            )

                            Column {

                                Text(
                                    text =
                                        "Platform under maintenance",

                                    fontWeight =
                                        FontWeight.Bold,

                                    fontSize =
                                        if (isSmallScreen) {
                                            13.sp
                                        } else {
                                            14.sp
                                        },

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onErrorContainer
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(2.dp)
                                )

                                Text(
                                    text =
                                        "Only Principal Admin login works right now. Other logins are temporarily disabled.",

                                    fontSize =
                                        if (isSmallScreen) {
                                            10.sp
                                        } else {
                                            12.sp
                                        },

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onErrorContainer
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(
                                if (isSmallScreen) {
                                    10.dp
                                } else {
                                    16.dp
                                }
                            )
                    )
                }


                // =====================================================
                // LOGO
                // =====================================================

                /*
                 * Logo ko pehle se thoda neeche/comfortable position
                 * par rakha gaya hai.
                 *
                 * Logo ke neeche:
                 * - SLH name
                 * - tagline
                 *
                 * intentionally remove kar diye gaye hain.
                 */

                Box(
                    contentAlignment =
                        Alignment.Center
                ) {

                    Box(
                        modifier = Modifier
                            .size(logoCircleSize)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme
                                    .colorScheme
                                    .primary
                                    .copy(
                                        alpha = 0.10f
                                    )
                            )
                    )

                    if (!branding.logoUri.isNullOrBlank()) {

                        AsyncImage(
                            model =
                                branding.logoUri,

                            contentDescription =
                                "Coaching Logo",

                            modifier =
                                Modifier
                                    .size(logoSize)
                                    .clip(CircleShape),

                            contentScale =
                                ContentScale.Crop
                        )

                    } else {

                        Image(
                            painter =
                                painterResource(
                                    id =
                                        R.drawable
                                            .slh_app_icon
                                ),

                            contentDescription =
                                "SLH Logo",

                            modifier =
                                Modifier
                                    .size(logoSize)
                                    .clip(CircleShape),

                            contentScale =
                                ContentScale.Crop
                        )
                    }
                }


                /*
                 * Logo aur Login Card ke beech controlled spacing.
                 */
                Spacer(
                    modifier =
                        Modifier.height(
                            if (isKeyboardVisible) {
                                8.dp
                            } else if (isVerySmallScreen) {
                                14.dp
                            } else if (isSmallScreen) {
                                17.dp
                            } else {
                                22.dp
                            }
                        )
                )


                // =====================================================
                // LOGIN CARD
                // =====================================================

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp),

                    shape =
                        RoundedCornerShape(cardCorner),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme
                                    .colorScheme
                                    .surface
                        ),

                    elevation =
                        CardDefaults.cardElevation(
                            defaultElevation = 7.dp
                        )
                ) {

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal =
                                        cardHorizontalPadding,

                                    vertical =
                                        cardVerticalPadding
                                )
                    ) {

                        // =================================================
                        // HEADER
                        // =================================================

                        Text(
                            text =
                                "Welcome Back",

                            fontSize =
                                titleSize,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurface
                        )

                        Spacer(
                            modifier =
                                Modifier.height(3.dp)
                        )

                        Text(
                            text =
                                "Choose your account and login to continue",

                            fontSize =
                                if (isSmallScreen) {
                                    10.sp
                                } else {
                                    12.sp
                                },

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,

                            maxLines = 2
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    if (isSmallScreen) {
                                        12.dp
                                    } else {
                                        17.dp
                                    }
                                )
                        )


                        // =================================================
                        // LOGIN AS
                        // =================================================

                        Text(
                            text =
                                "Login as",

                            fontSize =
                                if (isSmallScreen) {
                                    10.sp
                                } else {
                                    12.sp
                                },

                            fontWeight =
                                FontWeight.SemiBold,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    if (isSmallScreen) {
                                        7.dp
                                    } else {
                                        9.dp
                                    }
                                )
                        )


                        // =================================================
                        // ROLE ROW 1
                        // =================================================

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    if (isSmallScreen) {
                                        7.dp
                                    } else {
                                        9.dp
                                    }
                                )
                        ) {

                            PremiumLoginRoleButton(
                                title = "Student",

                                icon = {
                                    Icon(
                                        imageVector =
                                            Icons.Default.School,

                                        contentDescription =
                                            null,

                                        modifier =
                                            Modifier.size(
                                                if (isSmallScreen) {
                                                    16.dp
                                                } else {
                                                    19.dp
                                                }
                                            )
                                    )
                                },

                                selected =
                                    selectedRole ==
                                            UserRole.STUDENT,

                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(
                                            roleButtonHeight
                                        )
                            ) {

                                selectedRole =
                                    UserRole.STUDENT

                                errorMessage = ""
                            }


                            PremiumLoginRoleButton(
                                title = "Teacher",

                                icon = {
                                    Icon(
                                        imageVector =
                                            Icons.Default.Person,

                                        contentDescription =
                                            null,

                                        modifier =
                                            Modifier.size(
                                                if (isSmallScreen) {
                                                    16.dp
                                                } else {
                                                    19.dp
                                                }
                                            )
                                    )
                                },

                                selected =
                                    selectedRole ==
                                            UserRole.TEACHER,

                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(
                                            roleButtonHeight
                                        )
                            ) {

                                selectedRole =
                                    UserRole.TEACHER

                                errorMessage = ""
                            }
                        }


                        Spacer(
                            modifier =
                                Modifier.height(
                                    if (isSmallScreen) {
                                        7.dp
                                    } else {
                                        9.dp
                                    }
                                )
                        )


                        // =================================================
                        // ROLE ROW 2
                        // =================================================

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    if (isSmallScreen) {
                                        7.dp
                                    } else {
                                        9.dp
                                    }
                                )
                        ) {

                            PremiumLoginRoleButton(
                                title = "Admin",

                                icon = {
                                    Icon(
                                        imageVector =
                                            Icons.Default
                                                .AdminPanelSettings,

                                        contentDescription =
                                            null,

                                        modifier =
                                            Modifier.size(
                                                if (isSmallScreen) {
                                                    16.dp
                                                } else {
                                                    19.dp
                                                }
                                            )
                                    )
                                },

                                selected =
                                    selectedRole ==
                                            UserRole
                                                .COACHING_ADMIN,

                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(
                                            roleButtonHeight
                                        )
                            ) {

                                selectedRole =
                                    UserRole
                                        .COACHING_ADMIN

                                errorMessage = ""
                            }


                            PremiumLoginRoleButton(
                                title = "Developer",

                                icon = {
                                    Icon(
                                        imageVector =
                                            Icons.Default.Settings,

                                        contentDescription =
                                            null,

                                        modifier =
                                            Modifier.size(
                                                if (isSmallScreen) {
                                                    16.dp
                                                } else {
                                                    19.dp
                                                }
                                            )
                                    )
                                },

                                selected =
                                    selectedRole ==
                                            UserRole
                                                .PRINCIPAL_ADMIN,

                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(
                                            roleButtonHeight
                                        )
                            ) {

                                selectedRole =
                                    UserRole
                                        .PRINCIPAL_ADMIN

                                errorMessage = ""
                            }
                        }


                        // =================================================
                        // SPACE BEFORE USERNAME
                        // =================================================

                        Spacer(
                            modifier =
                                Modifier.height(
                                    if (isSmallScreen) {
                                        14.dp
                                    } else {
                                        19.dp
                                    }
                                )
                        )


                        // =================================================
                        // USERNAME
                        // =================================================

                        OutlinedTextField(
                            value =
                                username,

                            onValueChange = {
                                username = it
                                errorMessage = ""
                            },

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .bringIntoViewRequester(
                                        usernameBringIntoViewRequester
                                    )

                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            // WindowInsets.ime is @Composable — do not
                                            // call it inside a coroutine. Always bring
                                            // the field into view after a short delay.
                                            scope.launch {
                                                delay(250)
                                                usernameBringIntoViewRequester
                                                    .bringIntoView()
                                            }
                                        }
                                    },

                            singleLine = true,

                            textStyle = MaterialTheme.typography.bodyLarge,

                            label = {
                                Text(
                                    text = "Username",
                                    fontSize =
                                        if (isSmallScreen) {
                                            11.sp
                                        } else {
                                            13.sp
                                        }
                                )
                            },

                            placeholder = {
                                Text(
                                    text = "Enter username",
                                    fontSize =
                                        if (isSmallScreen) {
                                            11.sp
                                        } else {
                                            13.sp
                                        }
                                )
                            },

                            leadingIcon = {
                                Icon(
                                    imageVector =
                                        Icons.Default.Person,

                                    contentDescription =
                                        "Username",

                                    modifier =
                                        Modifier.size(
                                            if (isSmallScreen) {
                                                18.dp
                                            } else {
                                                21.dp
                                            }
                                        )
                                )
                            },

                            keyboardOptions =
                                KeyboardOptions(
                                    keyboardType =
                                        KeyboardType.Text
                                ),

                            shape =
                                RoundedCornerShape(14.dp),

                            colors =
                                OutlinedTextFieldDefaults
                                    .colors(
                                        focusedBorderColor =
                                            MaterialTheme
                                                .colorScheme
                                                .primary,

                                        unfocusedBorderColor =
                                            MaterialTheme
                                                .colorScheme
                                                .outline,

                                        focusedLabelColor =
                                            MaterialTheme
                                                .colorScheme
                                                .primary,

                                        cursorColor =
                                            MaterialTheme
                                                .colorScheme
                                                .primary
                                    )
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    if (isSmallScreen) {
                                        9.dp
                                    } else {
                                        13.dp
                                    }
                                )
                        )


                        // =================================================
                        // PASSWORD
                        // =================================================

                        OutlinedTextField(
                            value =
                                password,

                            onValueChange = {
                                password = it
                                errorMessage = ""
                            },

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .bringIntoViewRequester(
                                        passwordBringIntoViewRequester
                                    )

                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            // WindowInsets.ime is @Composable — do not
                                            // call it inside a coroutine.
                                            scope.launch {
                                                delay(250)
                                                passwordBringIntoViewRequester
                                                    .bringIntoView()
                                                delay(80)
                                                loginButtonBringIntoViewRequester
                                                    .bringIntoView()
                                            }
                                        }
                                    },

                            singleLine = true,

                            textStyle = MaterialTheme.typography.bodyLarge,

                            label = {
                                Text(
                                    text = "Password",
                                    fontSize =
                                        if (isSmallScreen) {
                                            11.sp
                                        } else {
                                            13.sp
                                        }
                                )
                            },

                            placeholder = {
                                Text(
                                    text = "Enter password",
                                    fontSize =
                                        if (isSmallScreen) {
                                            11.sp
                                        } else {
                                            13.sp
                                        }
                                )
                            },

                            leadingIcon = {
                                Icon(
                                    imageVector =
                                        Icons.Default.Lock,

                                    contentDescription =
                                        "Password",

                                    modifier =
                                        Modifier.size(
                                            if (isSmallScreen) {
                                                18.dp
                                            } else {
                                                21.dp
                                            }
                                        )
                                )
                            },

                            trailingIcon = {

                                IconButton(
                                    onClick = {
                                        passwordVisible =
                                            !passwordVisible
                                    }
                                ) {

                                    Icon(
                                        imageVector =
                                            if (
                                                passwordVisible
                                            ) {
                                                Icons.Default
                                                    .VisibilityOff
                                            } else {
                                                Icons.Default
                                                    .Visibility
                                            },

                                        contentDescription =
                                            if (
                                                passwordVisible
                                            ) {
                                                "Hide password"
                                            } else {
                                                "Show password"
                                            },

                                        modifier =
                                            Modifier.size(
                                                if (isSmallScreen) {
                                                    18.dp
                                                } else {
                                                    21.dp
                                                }
                                            )
                                    )
                                }
                            },

                            visualTransformation =
                                if (passwordVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },

                            keyboardOptions =
                                KeyboardOptions(
                                    keyboardType =
                                        KeyboardType.Password
                                ),

                            shape =
                                RoundedCornerShape(14.dp),

                            colors =
                                OutlinedTextFieldDefaults
                                    .colors(
                                        focusedBorderColor =
                                            MaterialTheme
                                                .colorScheme
                                                .primary,

                                        unfocusedBorderColor =
                                            MaterialTheme
                                                .colorScheme
                                                .outline,

                                        focusedLabelColor =
                                            MaterialTheme
                                                .colorScheme
                                                .primary,

                                        cursorColor =
                                            MaterialTheme
                                                .colorScheme
                                                .primary
                                    )
                        )


                        // =================================================
                        // ERROR
                        // =================================================

                        if (
                            errorMessage.isNotBlank()
                        ) {

                            Spacer(
                                modifier =
                                    Modifier.height(7.dp)
                            )

                            Text(
                                text =
                                    errorMessage,

                                modifier =
                                    Modifier.fillMaxWidth(),

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .error,

                                fontSize =
                                    if (isSmallScreen) {
                                        10.sp
                                    } else {
                                        12.sp
                                    },

                                fontWeight =
                                    FontWeight.Medium
                            )
                        }


                        // =================================================
                        // BIOMETRIC LOGIN
                        // =================================================

                        if (
                            BiometricHelper
                                .isHardwareAvailable(
                                    context
                                ) &&
                            BiometricHelper
                                .isEnabled(
                                    context
                                )
                        ) {

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        if (isSmallScreen) {
                                            10.dp
                                        } else {
                                            14.dp
                                        }
                                    )
                            )

                            OutlinedButton(
                                onClick = {

                                    val activity =
                                        context as?
                                                androidx
                                                .fragment
                                                .app
                                                .FragmentActivity

                                    if (
                                        activity == null
                                    ) {

                                        errorMessage =
                                            "Biometric not available on this screen."

                                        return@OutlinedButton
                                    }

                                    BiometricHelper
                                        .authenticate(

                                            activity =
                                                activity,

                                            onSuccess = {

                                                val savedUser =
                                                    BiometricHelper
                                                        .savedUsername(
                                                            context
                                                        )
                                                        ?.trim()
                                                        .orEmpty()

                                                val savedRoleName =
                                                    BiometricHelper
                                                        .savedRole(
                                                            context
                                                        )

                                                if (
                                                    savedUser
                                                        .isBlank() ||
                                                    savedRoleName
                                                        .isNullOrBlank()
                                                ) {

                                                    errorMessage =
                                                        "No saved account for biometric. Login with password once, then enable biometric in Profile → Settings."

                                                    return@authenticate
                                                }

                                                val role =
                                                    try {

                                                        UserRole
                                                            .valueOf(
                                                                savedRoleName
                                                            )

                                                    } catch (
                                                        _: Exception
                                                    ) {

                                                        errorMessage =
                                                            "Invalid saved role."

                                                        return@authenticate
                                                    }

                                                selectedRole =
                                                    role

                                                username =
                                                    savedUser


                                                // =====================================
                                                // STUDENT
                                                // =====================================

                                                if (
                                                    role ==
                                                    UserRole.STUDENT
                                                ) {

                                                    val local =
                                                        StudentStore
                                                            .students
                                                            .firstOrNull {

                                                                it.username
                                                                    .equals(
                                                                        savedUser,
                                                                        ignoreCase =
                                                                            true
                                                                    ) &&
                                                                        it.status ==
                                                                        AccountStatus
                                                                            .APPROVED
                                                            }

                                                    if (
                                                        local != null
                                                    ) {

                                                        onLoginSuccess(
                                                            DemoUser(
                                                                id =
                                                                    local.id,

                                                                username =
                                                                    local.username,

                                                                password =
                                                                    "",

                                                                role =
                                                                    UserRole
                                                                        .STUDENT,

                                                                status =
                                                                    local.status,

                                                                coachingId =
                                                                    local.coachingId,

                                                                displayName =
                                                                    local.name
                                                            )
                                                        )

                                                    } else {

                                                        errorMessage =
                                                            "Student not found on this phone. Login with password once."
                                                    }

                                                } else {

                                                    // =================================
                                                    // STAFF / ADMIN / DEVELOPER
                                                    // =================================

                                                    val local =
                                                        UserAccountStore
                                                            .findByUsername(
                                                                savedUser
                                                            )
                                                            ?.takeIf {

                                                                it.role ==
                                                                        role &&
                                                                        it.status ==
                                                                        AccountStatus
                                                                            .APPROVED
                                                            }

                                                    if (
                                                        local != null
                                                    ) {

                                                        onLoginSuccess(
                                                            local.copy(
                                                                password =
                                                                    ""
                                                            )
                                                        )

                                                    } else {

                                                        errorMessage =
                                                            "Account not found on this phone. Login with password once, then enable biometric in Settings."
                                                    }
                                                }
                                            },

                                            onError = {
                                                    msg ->
                                                errorMessage =
                                                    msg
                                            }
                                        )
                                },

                                modifier =
                                    Modifier.fillMaxWidth(),

                                shape =
                                    RoundedCornerShape(
                                        13.dp
                                    ),

                                contentPadding =
                                    PaddingValues(
                                        vertical =
                                            if (isSmallScreen) {
                                                7.dp
                                            } else {
                                                9.dp
                                            }
                                    )
                            ) {

                                Text(
                                    text =
                                        AppStrings
                                            .useBiometric,

                                    fontSize =
                                        if (isSmallScreen) {
                                            11.sp
                                        } else {
                                            13.sp
                                        },

                                    fontWeight =
                                        FontWeight.SemiBold
                                )
                            }
                        }


                        // =================================================
                        // LOGIN BUTTON
                        // =================================================

                        Spacer(
                            modifier =
                                Modifier.height(
                                    if (isSmallScreen) {
                                        10.dp
                                    } else {
                                        14.dp
                                    }
                                )
                        )

                        Button(
                            onClick = {

                                // =================================================
                                // EMPTY FIELDS
                                // =================================================

                                if (
                                    username
                                        .trim()
                                        .isBlank() ||
                                    password.isBlank()
                                ) {

                                    errorMessage =
                                        "Please enter username and password."

                                    return@Button
                                }


                                // =================================================
                                // MAINTENANCE MODE
                                // =================================================

                                if (
                                    PlatformSettingsStore
                                        .maintenanceMode &&
                                    selectedRole !=
                                    UserRole
                                        .PRINCIPAL_ADMIN
                                ) {

                                    errorMessage =
                                        AppStrings
                                            .maintenanceBody

                                    return@Button
                                }


                                if (isLoading) {
                                    return@Button
                                }


                                isLoading = true
                                errorMessage = ""


                                val enteredUsername =
                                    username.trim()


                                // =================================================
                                // STUDENT LOGIN
                                // =================================================

                                if (
                                    selectedRole ==
                                    UserRole.STUDENT
                                ) {

                                    val matchedStudent =
                                        StudentStore
                                            .students
                                            .firstOrNull {

                                                it.username
                                                    .trim()
                                                    .equals(
                                                        enteredUsername,
                                                        ignoreCase =
                                                            true
                                                    )
                                            }


                                    // =============================================
                                    // LOCAL PASSWORD VERIFICATION
                                    // =============================================

                                    val student =
                                        if (
                                            matchedStudent != null &&
                                            PasswordHasher.verify(
                                                password,
                                                matchedStudent.password
                                            )
                                        ) {

                                            if (
                                                !PasswordHasher
                                                    .isHashed(
                                                        matchedStudent
                                                            .password
                                                    )
                                            ) {

                                                StudentStore
                                                    .updateStudent(
                                                        matchedStudent
                                                            .copy(
                                                                password =
                                                                    PasswordHasher
                                                                        .hash(
                                                                            password
                                                                        )
                                                            )
                                                    )
                                            }

                                            matchedStudent

                                        } else {
                                            null
                                        }


                                    // =============================================
                                    // CLOUD STUDENT
                                    // =============================================

                                    if (
                                        student == null &&
                                        matchedStudent == null
                                    ) {

                                        scope.launch {

                                            val outcome =
                                                try {

                                                    withContext(
                                                        Dispatchers.IO
                                                    ) {

                                                        FirebaseStudentAuthRepository
                                                            .cloudLogin(
                                                                context =
                                                                    context,

                                                                username =
                                                                    enteredUsername,

                                                                password =
                                                                    password
                                                            )
                                                    }

                                                } catch (
                                                    _: Exception
                                                ) {

                                                    CloudLoginOutcome
                                                        .ServerBlocked
                                                }


                                            isLoading =
                                                false


                                            when (
                                                outcome
                                            ) {

                                                is CloudLoginOutcome
                                                .Success -> {

                                                    val cloudStudent =
                                                        outcome.value

                                                    when (
                                                        cloudStudent
                                                            .status
                                                    ) {

                                                        AccountStatus
                                                            .APPROVED -> {

                                                            val cachedStudent =
                                                                cloudStudent
                                                                    .copy(
                                                                        password =
                                                                            PasswordHasher
                                                                                .hash(
                                                                                    password
                                                                                )
                                                                    )

                                                            StudentStore
                                                                .cacheFromCloud(
                                                                    cachedStudent
                                                                )

                                                            try {

                                                                FirebaseStudentAuthRepository
                                                                    .syncStudentProfile(
                                                                        context =
                                                                            context,

                                                                        student =
                                                                            cachedStudent
                                                                    )

                                                            } catch (
                                                                _: Exception
                                                            ) {
                                                                // Login continues.
                                                            }

                                                            onLoginSuccess(
                                                                DemoUser(
                                                                    id =
                                                                        cachedStudent
                                                                            .id,

                                                                    username =
                                                                        cachedStudent
                                                                            .username,

                                                                    password =
                                                                        cachedStudent
                                                                            .password,

                                                                    role =
                                                                        UserRole
                                                                            .STUDENT,

                                                                    status =
                                                                        cachedStudent
                                                                            .status,

                                                                    coachingId =
                                                                        cachedStudent
                                                                            .coachingId,

                                                                    displayName =
                                                                        cachedStudent
                                                                            .name
                                                                )
                                                            )
                                                        }

                                                        AccountStatus
                                                            .PENDING -> {

                                                            SLHFirebase
                                                                .signOutAll()

                                                            errorMessage =
                                                                "Your account is still pending approval."
                                                        }

                                                        AccountStatus
                                                            .REJECTED -> {

                                                            SLHFirebase
                                                                .signOutAll()

                                                            errorMessage =
                                                                "Your account has been rejected."
                                                        }

                                                        AccountStatus
                                                            .SUSPENDED -> {

                                                            SLHFirebase
                                                                .signOutAll()

                                                            errorMessage =
                                                                "Your account is suspended."
                                                        }
                                                    }
                                                }

                                                CloudLoginOutcome
                                                    .WrongPassword -> {

                                                    errorMessage =
                                                        "Invalid student username or password."
                                                }

                                                CloudLoginOutcome
                                                    .NotFound -> {

                                                    errorMessage =
                                                        "No student account found for this username. Ask your coaching to check the username."
                                                }

                                                CloudLoginOutcome
                                                    .NoInternet -> {

                                                    errorMessage =
                                                        "No internet connection. Connect to WiFi/mobile data and try again."
                                                }

                                                CloudLoginOutcome
                                                    .ServerBlocked -> {

                                                    errorMessage =
                                                        "Could not reach the server right now. Please try again in a moment."
                                                }
                                            }
                                        }

                                    } else if (
                                        student == null
                                    ) {

                                        isLoading =
                                            false

                                        errorMessage =
                                            "Invalid student username or password."

                                    } else {

                                        // =============================================
                                        // LOCAL STUDENT STATUS
                                        // =============================================

                                        when (
                                            student.status
                                        ) {

                                            AccountStatus
                                                .APPROVED -> {

                                                scope.launch {

                                                    val firebaseAuthenticated =
                                                        try {

                                                            FirebaseStudentAuthRepository
                                                                .authenticateStudent(
                                                                    context =
                                                                        context,

                                                                    username =
                                                                        enteredUsername,

                                                                    password =
                                                                        password,

                                                                    keepSignedIn =
                                                                        true
                                                                )

                                                        } catch (
                                                            _: Exception
                                                        ) {

                                                            false
                                                        }


                                                    if (
                                                        firebaseAuthenticated
                                                    ) {

                                                        try {

                                                            FirebaseStudentAuthRepository
                                                                .syncStudentProfile(
                                                                    context =
                                                                        context,

                                                                    student =
                                                                        student
                                                                )

                                                        } catch (
                                                            _: Exception
                                                        ) {
                                                            // Continue login.
                                                        }
                                                    }


                                                    isLoading =
                                                        false


                                                    val demoUser =
                                                        DemoUser(
                                                            id =
                                                                student.id,

                                                            username =
                                                                student.username,

                                                            password =
                                                                student.password,

                                                            role =
                                                                UserRole
                                                                    .STUDENT,

                                                            status =
                                                                student.status,

                                                            coachingId =
                                                                student.coachingId,

                                                            displayName =
                                                                student.name
                                                        )


                                                    onLoginSuccess(
                                                        demoUser
                                                    )
                                                }
                                            }


                                            AccountStatus
                                                .PENDING -> {

                                                isLoading =
                                                    false

                                                errorMessage =
                                                    "Your account is still pending approval."
                                            }


                                            AccountStatus
                                                .REJECTED -> {

                                                isLoading =
                                                    false

                                                errorMessage =
                                                    "Your account has been rejected."
                                            }


                                            AccountStatus
                                                .SUSPENDED -> {

                                                isLoading =
                                                    false

                                                errorMessage =
                                                    "Your account is suspended."
                                            }
                                        }
                                    }


                                } else {

                                    // =================================================
                                    // TEACHER / ADMIN / DEVELOPER
                                    // =================================================

                                    val matchedUser =
                                        UserAccountStore
                                            .getAll()
                                            .firstOrNull {

                                                it.username
                                                    .trim()
                                                    .equals(
                                                        enteredUsername,
                                                        ignoreCase =
                                                            true
                                                    ) &&
                                                        it.role ==
                                                        selectedRole
                                            }


                                    val user =
                                        if (
                                            matchedUser != null &&
                                            PasswordHasher.verify(
                                                password,
                                                matchedUser.password
                                            )
                                        ) {

                                            if (
                                                !PasswordHasher
                                                    .isHashed(
                                                        matchedUser
                                                            .password
                                                    )
                                            ) {

                                                UserAccountStore
                                                    .update(
                                                        matchedUser
                                                            .copy(
                                                                password =
                                                                    PasswordHasher
                                                                        .hash(
                                                                            password
                                                                        )
                                                            )
                                                    )
                                            }

                                            matchedUser

                                        } else {
                                            null
                                        }


                                    // =============================================
                                    // CLOUD ROLE LOGIN
                                    // =============================================

                                    if (
                                        user == null &&
                                        matchedUser == null
                                    ) {

                                        scope.launch {

                                            val outcome =
                                                try {

                                                    withContext(
                                                        Dispatchers.IO
                                                    ) {

                                                        FirebaseRoleAuthRepository
                                                            .cloudLogin(
                                                                context =
                                                                    context,

                                                                username =
                                                                    enteredUsername,

                                                                password =
                                                                    password,

                                                                role =
                                                                    selectedRole
                                                            )
                                                    }

                                                } catch (
                                                    _: Exception
                                                ) {

                                                    CloudLoginOutcome
                                                        .ServerBlocked
                                                }


                                            isLoading =
                                                false


                                            when (
                                                outcome
                                            ) {

                                                is CloudLoginOutcome
                                                .Success -> {

                                                    val cloudUser =
                                                        outcome.value

                                                    when (
                                                        cloudUser
                                                            .status
                                                    ) {

                                                        AccountStatus
                                                            .APPROVED -> {

                                                            UserAccountStore
                                                                .add(
                                                                    cloudUser
                                                                        .copy(
                                                                            password =
                                                                                PasswordHasher
                                                                                    .hash(
                                                                                        password
                                                                                    )
                                                                        )
                                                                )

                                                            onLoginSuccess(
                                                                cloudUser
                                                            )
                                                        }

                                                        AccountStatus
                                                            .PENDING -> {

                                                            FirebaseRoleAuthRepository
                                                                .signOut()

                                                            errorMessage =
                                                                "Your account is still pending approval."
                                                        }

                                                        AccountStatus
                                                            .REJECTED -> {

                                                            FirebaseRoleAuthRepository
                                                                .signOut()

                                                            errorMessage =
                                                                "Your account has been rejected."
                                                        }

                                                        AccountStatus
                                                            .SUSPENDED -> {

                                                            FirebaseRoleAuthRepository
                                                                .signOut()

                                                            errorMessage =
                                                                "Your account is suspended."
                                                        }
                                                    }
                                                }

                                                CloudLoginOutcome
                                                    .WrongPassword -> {

                                                    errorMessage =
                                                        "Invalid username, password or login type."
                                                }

                                                CloudLoginOutcome
                                                    .NotFound -> {

                                                    errorMessage =
                                                        "No account found for this username/login type. Ask your admin to check it."
                                                }

                                                CloudLoginOutcome
                                                    .NoInternet -> {

                                                    errorMessage =
                                                        "No internet connection. Connect to WiFi/mobile data and try again."
                                                }

                                                CloudLoginOutcome
                                                    .ServerBlocked -> {

                                                    errorMessage =
                                                        "Could not reach the server right now. Please try again in a moment."
                                                }
                                            }
                                        }

                                    } else if (
                                        user == null
                                    ) {

                                        isLoading =
                                            false

                                        errorMessage =
                                            "Invalid username, password or login type."

                                    } else {

                                        // =============================================
                                        // LOCAL USER STATUS
                                        // =============================================

                                        when (
                                            user.status
                                        ) {

                                            AccountStatus
                                                .APPROVED -> {

                                                scope.launch {

                                                    try {

                                                        withContext(
                                                            Dispatchers.IO
                                                        ) {

                                                            FirebaseRoleAuthRepository
                                                                .migrateCurrentUser(
                                                                    context =
                                                                        context,

                                                                    user =
                                                                        user,

                                                                    password =
                                                                        password
                                                                )
                                                        }

                                                    } catch (
                                                        _: Exception
                                                    ) {
                                                        // Local login continues.
                                                    }


                                                    val cloudStatus =
                                                        try {

                                                            FirebaseRoleAuthRepository
                                                                .currentCloudStatus()

                                                        } catch (
                                                            _: Exception
                                                        ) {

                                                            null
                                                        }


                                                    isLoading =
                                                        false


                                                    if (
                                                        cloudStatus !=
                                                        null &&
                                                        cloudStatus !=
                                                        AccountStatus
                                                            .APPROVED
                                                    ) {

                                                        FirebaseRoleAuthRepository
                                                            .signOut()

                                                        errorMessage =
                                                            when (
                                                                cloudStatus
                                                            ) {

                                                                AccountStatus
                                                                    .SUSPENDED ->
                                                                    "Your account is suspended."

                                                                AccountStatus
                                                                    .REJECTED ->
                                                                    "Your account has been rejected."

                                                                else ->
                                                                    "Your account is still pending approval."
                                                            }

                                                    } else {

                                                        onLoginSuccess(
                                                            user
                                                        )
                                                    }
                                                }
                                            }


                                            AccountStatus
                                                .PENDING -> {

                                                isLoading =
                                                    false

                                                errorMessage =
                                                    "Your account is still pending approval."
                                            }


                                            AccountStatus
                                                .REJECTED -> {

                                                isLoading =
                                                    false

                                                errorMessage =
                                                    "Your account has been rejected."
                                            }


                                            AccountStatus
                                                .SUSPENDED -> {

                                                isLoading =
                                                    false

                                                errorMessage =
                                                    "Your account is suspended."
                                            }
                                        }
                                    }
                                }
                            },

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(
                                        loginButtonHeight
                                    )
                                    .bringIntoViewRequester(
                                        loginButtonBringIntoViewRequester
                                    ),

                            enabled =
                                !isLoading,

                            shape =
                                RoundedCornerShape(
                                    14.dp
                                ),

                            colors =
                                ButtonDefaults
                                    .buttonColors(
                                        containerColor =
                                            MaterialTheme
                                                .colorScheme
                                                .primary,

                                        contentColor =
                                            MaterialTheme
                                                .colorScheme
                                                .onPrimary
                                    )
                        ) {

                            if (isLoading) {

                                CircularProgressIndicator(
                                    modifier =
                                        Modifier.size(
                                            if (isSmallScreen) {
                                                19.dp
                                            } else {
                                                21.dp
                                            }
                                        ),

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onPrimary,

                                    strokeWidth =
                                        2.5.dp
                                )

                            } else {

                                Text(
                                    text =
                                        "Login",

                                    fontSize =
                                        if (isSmallScreen) {
                                            14.sp
                                        } else {
                                            16.sp
                                        },

                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }
                        }


                        /*
                         * IMPORTANT:
                         *
                         * Pehle yahan 12.dp + bahar 90.dp tha.
                         *
                         * Ab unnecessary large blank space remove
                         * kar diya gaya hai.
                         */
                        Spacer(
                            modifier =
                                Modifier.height(
                                    if (isKeyboardVisible) {
                                        4.dp
                                    } else if (isSmallScreen) {
                                        7.dp
                                    } else {
                                        10.dp
                                    }
                                )
                        )
                    }
                }


                // =====================================================
                // BOTTOM SCROLL SPACE
                // =====================================================

                /*
                 * Keyboard open hone par sirf minimum space.
                 *
                 * Isse Login button ke neeche huge blank area nahi
                 * banega.
                 */
                Spacer(
                    modifier =
                        Modifier.height(
                            if (isKeyboardVisible) {
                                4.dp
                            } else {
                                if (isSmallScreen) {
                                    18.dp
                                } else {
                                    24.dp
                                }
                            }
                        )
                )
            }


            // =========================================================
            // FIXED POWERED BY
            // =========================================================

            /*
             * Keyboard open hone par PoweredBySLH ko fixed bottom
             * area mein rakhne ke bajay compact rakha gaya hai.
             *
             * Isse keyboard ke saath layout par unnecessary pressure
             * nahi aata.
             */

            if (!isKeyboardVisible) {

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(
                                start = 18.dp,
                                top = 2.dp,
                                end = 18.dp,
                                bottom = 5.dp
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    PoweredBySLH(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .widthIn(
                                    max = 280.dp
                                )
                    )
                }
            }
        }
    }
}


// =====================================================================
// PREMIUM LOGIN ROLE BUTTON
// =====================================================================

@Composable
private fun PremiumLoginRoleButton(
    title: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,

        modifier =
            modifier,

        shape =
            RoundedCornerShape(13.dp),

        contentPadding =
            PaddingValues(
                horizontal = 5.dp,
                vertical = 3.dp
            ),

        colors =
            ButtonDefaults.buttonColors(
                containerColor =
                    if (selected) {

                        MaterialTheme
                            .colorScheme
                            .primary

                    } else {

                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                    },

                contentColor =
                    if (selected) {

                        MaterialTheme
                            .colorScheme
                            .onPrimary

                    } else {

                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                    }
            ),

        elevation =
            ButtonDefaults.buttonElevation(
                defaultElevation =
                    if (selected) {
                        2.dp
                    } else {
                        0.dp
                    }
            )
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.Center,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            icon()

            Spacer(
                modifier =
                    Modifier.size(5.dp)
            )

            Text(
                text =
                    title,

                fontSize =
                    11.sp,

                fontWeight =
                    if (selected) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Medium
                    },

                maxLines = 1,

                textAlign =
                    TextAlign.Center
            )
        }
    }
}