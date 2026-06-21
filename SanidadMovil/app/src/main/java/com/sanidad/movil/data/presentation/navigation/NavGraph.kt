package com.sanidad.movil.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sanidad.movil.MyApplication
import com.sanidad.movil.data.local.TokenDataStore
import com.sanidad.movil.data.presentation.AppViewModelFactory
import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.repository.AuthRepository
import com.sanidad.movil.presentation.screens.alerts.AlertsScreen
import com.sanidad.movil.presentation.screens.clientes.ClientesScreen
import com.sanidad.movil.presentation.screens.dashboard.DashboardScreen
import com.sanidad.movil.presentation.screens.devoluciones.DevolucionesScreen
import com.sanidad.movil.presentation.screens.devolucionesProveedor.DevolucionesProveedorScreen
import com.sanidad.movil.presentation.screens.login.LoginScreen
import com.sanidad.movil.presentation.screens.lotes.LotesScreen
import com.sanidad.movil.presentation.screens.medicamentos.MedicamentosScreen
import com.sanidad.movil.presentation.screens.perdidas.PerdidasScreen
import com.sanidad.movil.presentation.screens.proveedores.ProveedoresScreen
import com.sanidad.movil.presentation.screens.recetas.RecetasScreen
import com.sanidad.movil.presentation.screens.recommendations.RecommendationsScreen
import com.sanidad.movil.presentation.screens.ubicaciones.UbicacionesScreen
import com.sanidad.movil.presentation.screens.usuarios.UsuariosScreen
import com.sanidad.movil.presentation.screens.ventas.VentaCreateScreen
import com.sanidad.movil.presentation.screens.ventas.VentasScreen
import com.sanidad.movil.presentation.screens.ventas.VentasViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ── Paleta de diseño unificada ──
private val Slate50 = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate700 = Color(0xFF334155)
private val Slate900 = Color(0xFF0F172A)
private val Primary = Color(0xFF4F46E5)
private val PrimaryLight = Color(0xFFEEF2FF)
private val White = Color.White

// ── Modelo de cada entrada del menú ──
private data class DrawerItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    // ── Estado del Drawer ──
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // ── Auth ──
    val tokenDataStore = remember { TokenDataStore(MyApplication.instance) }
    val authRepository = remember { AuthRepository(tokenDataStore = tokenDataStore) }
    var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }

    // Callback para la venta creada (se guarda temporalmente)
    val onVentaCreadaCallback = remember { mutableStateOf<((com.sanidad.movil.data.remote.dto.VentaResponse) -> Unit)?>(null) }

    LaunchedEffect(Unit) {
        val token = tokenDataStore.tokenFlow.first()
        NetworkModule.setToken(token)
        isLoggedIn = token != null
    }

    if (isLoggedIn == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    val startDestination = if (isLoggedIn == true) "dashboard" else "login"

    // ── Lista de entradas del menú ──
    val drawerItems = listOf(
        DrawerItem("Dashboard", "dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
        DrawerItem("Medicamentos", "medicamentos", Icons.Filled.Medication, Icons.Outlined.Medication),
        DrawerItem("Clientes", "clientes", Icons.Filled.People, Icons.Outlined.People),
        DrawerItem("Ventas", "ventas", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
        DrawerItem("Usuarios", "usuarios", Icons.Filled.Person, Icons.Outlined.Person),
        DrawerItem("Proveedores", "proveedores", Icons.Filled.Business, Icons.Outlined.Business),
        DrawerItem("Lotes", "lotes", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
        DrawerItem("Devoluciones", "devoluciones", Icons.Filled.CompareArrows, Icons.Outlined.CompareArrows),
        DrawerItem("Dev. Proveedor", "devoluciones_proveedor", Icons.Filled.LocalShipping, Icons.Outlined.LocalShipping),
        DrawerItem("Recetas", "recetas", Icons.Filled.Receipt, Icons.Outlined.Receipt),
        DrawerItem("Ubicaciones", "ubicaciones", Icons.Filled.Warehouse, Icons.Outlined.Warehouse),
        DrawerItem("Alertas", "alertas", Icons.Filled.NotificationsActive, Icons.Outlined.NotificationsActive),
        DrawerItem("Pérdidas", "perdidas", Icons.Filled.TrendingDown, Icons.Outlined.TrendingDown),
        DrawerItem("Recomendaciones", "recomendaciones", Icons.Filled.Lightbulb, Icons.Outlined.Lightbulb)
    )

    // ── Ruta activa ──
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // ── Contenido del Drawer (compartido entre los dos modos) ──
    val drawerContent: @Composable ColumnScope.() -> Unit = {
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.LocalPharmacy, null, tint = Primary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Farmacia Sanidad", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Slate900)
                Text("Gestión de Inventario", fontSize = 12.sp, color = Slate500)
            }
        }
        Spacer(Modifier.height(16.dp))
        Divider(color = Slate200)
        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(drawerItems) { _, item ->
                val isSelected = currentRoute == item.route
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            tint = if (isSelected) Primary else Slate500
                        )
                    },
                    label = {
                        Text(
                            item.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Primary else Slate700
                        )
                    },
                    selected = isSelected,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate(item.route) {
                            popUpTo("dashboard") { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = PrimaryLight,
                        unselectedContainerColor = White,
                        selectedIconColor = Primary,
                        unselectedIconColor = Slate500
                    ),
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }

        Divider(color = Slate200)
        // Cerrar sesión
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Logout, null, tint = Slate500) },
            label = { Text("Cerrar sesión", color = Slate700) },
            selected = false,
            onClick = {
                coroutineScope.launch {
                    drawerState.close()
                    authRepository.logout()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            },
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.height(16.dp))
    }

    if (isLoggedIn == true) {
        if (isLandscape || configuration.screenWidthDp >= 600) {
            // ── Modo Landscape / Tablet ──
            val railState = rememberDrawerState(DrawerValue.Closed)

            ModalNavigationDrawer(
                drawerState = railState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = White,
                        modifier = Modifier.width(300.dp)
                    ) {
                        drawerContent()
                    }
                },
                gesturesEnabled = true
            ) {
                Row(Modifier.fillMaxSize()) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(White),
                        containerColor = White,
                        header = {
                            FloatingActionButton(
                                onClick = {
                                    coroutineScope.launch {
                                        if (railState.isClosed) railState.open() else railState.close()
                                    }
                                },
                                modifier = Modifier.padding(8.dp),
                                containerColor = PrimaryLight,
                                contentColor = Primary
                            ) {
                                Icon(Icons.Filled.Menu, "Menú")
                            }
                        }
                    ) {
                        drawerItems.forEach { item ->
                            val isSelected = currentRoute == item.route
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = {
                                    coroutineScope.launch { railState.close() }
                                    navController.navigate(item.route) {
                                        popUpTo("dashboard") { saveState = true }
                                        launchSingleTop = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label,
                                        tint = if (isSelected) Primary else Slate500
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Primary,
                                    unselectedIconColor = Slate500,
                                    indicatorColor = PrimaryLight
                                )
                            )
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        buildNavGraph(
                            navController = navController,
                            authRepository = authRepository,
                            coroutineScope = coroutineScope,
                            onVentaCreadaCallback = onVentaCreadaCallback
                        )
                    }
                }
            }
        } else {
            // ── Modo Vertical / Teléfono ──
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = White,
                        modifier = Modifier.width(300.dp)
                    ) {
                        drawerContent()
                    }
                },
                gesturesEnabled = true
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Sanidad", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                    Icon(Icons.Filled.Menu, "Menú", tint = Slate700)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = White,
                                titleContentColor = Slate900
                            )
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        buildNavGraph(
                            navController = navController,
                            authRepository = authRepository,
                            coroutineScope = coroutineScope,
                            onVentaCreadaCallback = onVentaCreadaCallback
                        )
                    }
                }
            }
        }
    } else {
        // ── No logueado ──
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("login") {
                LoginScreen(
                    viewModel = viewModel(factory = AppViewModelFactory.login(authRepository)),
                    onLoginSuccess = {
                        isLoggedIn = true
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

// ── Construcción del grafo de navegación (pantallas autenticadas) ──
fun NavGraphBuilder.buildNavGraph(
    navController: NavHostController,
    authRepository: AuthRepository,
    coroutineScope: CoroutineScope,
    onVentaCreadaCallback: MutableState<((com.sanidad.movil.data.remote.dto.VentaResponse) -> Unit)?>
) {
    // Ruta login para manejar el logout desde el menú
    composable("login") {
        LoginScreen(
            viewModel = viewModel(factory = AppViewModelFactory.login(authRepository)),
            onLoginSuccess = {
                navController.navigate("dashboard") {
                    popUpTo("login") { inclusive = true }
                }
            }
        )
    }

    composable("dashboard") {
        DashboardScreen(
            viewModel = viewModel(factory = AppViewModelFactory.dashboard()),
            onNavigateAlerts = { navController.navigate("alertas") },
            onNavigateRecommendations = { navController.navigate("recomendaciones") },
            onLogout = {
                coroutineScope.launch {
                    authRepository.logout()
                    navController.navigate("login") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            }
        )
    }

    composable("medicamentos") {
        MedicamentosScreen(viewModel = viewModel(factory = AppViewModelFactory.medicamentos()))
    }

    composable("clientes") {
        ClientesScreen(viewModel = viewModel(factory = AppViewModelFactory.clientes()))
    }

    composable("ventas") {
        VentasScreen(
            ventasViewModel = viewModel(factory = AppViewModelFactory.ventas()),
            onNuevaVenta = { callback ->
                // Guardamos el callback que refrescará la lista al crear una venta
                onVentaCreadaCallback.value = callback
                navController.navigate("venta_create")
            }
        )
    }

    composable("venta_create") {
        VentaCreateScreen(
            viewModel = viewModel(factory = AppViewModelFactory.ventaCreate()),
            usuarioId = com.sanidad.movil.data.UserSession.userId,
            onVentaExitosa = { venta ->
                onVentaCreadaCallback.value?.invoke(venta)
                onVentaCreadaCallback.value = null
                navController.popBackStack()
            },
            onCancelar = {
                onVentaCreadaCallback.value = null
                navController.popBackStack()
            }
        )
    }

    composable("usuarios") {
        UsuariosScreen(viewModel = viewModel(factory = AppViewModelFactory.usuarios()))
    }

    composable("proveedores") {
        ProveedoresScreen(viewModel = viewModel(factory = AppViewModelFactory.proveedores()))
    }

    composable("lotes") {
        LotesScreen(viewModel = viewModel(factory = AppViewModelFactory.lotes()))
    }

    composable("devoluciones") {
        DevolucionesScreen(viewModel = viewModel(factory = AppViewModelFactory.devoluciones()))
    }

    composable("devoluciones_proveedor") {
        DevolucionesProveedorScreen(viewModel = viewModel(factory = AppViewModelFactory.devolucionesProveedor()))
    }

    composable("recetas") {
        RecetasScreen(viewModel = viewModel(factory = AppViewModelFactory.recetas()))
    }

    composable("ubicaciones") {
        UbicacionesScreen(viewModel = viewModel(factory = AppViewModelFactory.ubicaciones()))
    }

    composable("alertas") {
        AlertsScreen(viewModel = viewModel(factory = AppViewModelFactory.alerts()))
    }

    composable("perdidas") {
        PerdidasScreen(viewModel = viewModel(factory = AppViewModelFactory.perdidas()))
    }

    composable("recomendaciones") {
        RecommendationsScreen(viewModel = viewModel(factory = AppViewModelFactory.recommendations()))
    }
}