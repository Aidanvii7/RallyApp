@file:OptIn(
    ExperimentalAnimationApi::class,
)

@file:Suppress("PackageDirectoryMismatch", "PackageName")

package androidx.navigation.compose

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.*
import de.jodamob.reflect.SuperReflect
import kotlinx.coroutines.flow.StateFlow

private const val NAV_PACKAGE = "navigation_compose_release"

@Stable
@Immutable
class NavTransition(
    val default: () -> ContentTransform,
    val forward: () -> ContentTransform = default,
    val backward: () -> ContentTransform = default,
    val comparator: (initial: NavBackStackEntry, target: NavBackStackEntry) -> Int = { _, _ -> 0 },
) {
    constructor(
        forward: () -> ContentTransform,
        backward: () -> ContentTransform,
        comparator: (initial: NavBackStackEntry, target: NavBackStackEntry) -> Int,
    ) : this(forward, forward, backward, comparator)

    companion object {
        val fadeOutFadeIn = NavTransition(
            default = {
                fadeIn(animationSpec = tween(durationMillis = 220, delayMillis = 90)) with fadeOut(animationSpec = tween(durationMillis = 90))
            },
        )
        fun horizontalSlideFade(
            comparator: (initial: NavBackStackEntry, target: NavBackStackEntry) -> Int,
        ) = NavTransition(
            forward = {
                (slideInHorizontally({ it }) + fadeIn() with slideOutHorizontally({ -it }) + fadeOut())
            },
            backward = {
                (slideInHorizontally({ -it }) + fadeIn() with slideOutHorizontally({ it }) + fadeOut())
            },
            comparator = comparator,
        )

        fun verticalSlideFade(
            comparator: (initial: NavBackStackEntry, target: NavBackStackEntry) -> Int,
        ) = NavTransition(
            forward = {
                (slideInVertically({ it }) + fadeIn() with slideOutVertically({ -it }) + fadeOut())
            },
            backward = {
                (slideInVertically({ -it }) + fadeIn() with slideOutVertically({ it }) + fadeOut())
            },
            comparator = comparator,
        )
    }
}

@Composable
fun NavHost(
    navController: NavHostController,
    transition: NavTransition,
    startDestination: String,
    modifier: Modifier = Modifier,
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit
) {
    NavHost(
        navController = navController,
        transition = transition,
        graph = remember(route, startDestination, builder) {
            navController.createGraph(startDestination, route, builder)
        },
        modifier = modifier
    )
}

@Composable
fun NavHost(
    navController: NavHostController,
    transition: NavTransition,
    graph: NavGraph,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "NavHost requires a ViewModelStoreOwner to be provided via LocalViewModelStoreOwner"
    }
    val onBackPressedDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    val onBackPressedDispatcher = onBackPressedDispatcherOwner?.onBackPressedDispatcher

    // on successful recompose we setup the navController with proper inputs
    // after the first time, this will only happen again if one of the inputs changes
    DisposableEffect(navController, lifecycleOwner, viewModelStoreOwner, onBackPressedDispatcher) {
        navController.setLifecycleOwner(lifecycleOwner)
        navController.setViewModelStore(viewModelStoreOwner.viewModelStore)
        if (onBackPressedDispatcher != null) {
            navController.setOnBackPressedDispatcher(onBackPressedDispatcher)
        }

        onDispose { }
    }

    SideEffect { navController.graph = graph }

    val saveableStateHolder = rememberSaveableStateHolder()

    // Find the ComposeNavigator, returning early if it isn't found
    // (such as is the case when using TestNavHostController)
    val composeNavigator: ComposeNavigator =
        navController.navigatorProvider.get<Navigator<out NavDestination>>("composable") as? ComposeNavigator ?: return
    val backStack by composeNavigator
        .getProperty<StateFlow<List<NavBackStackEntry>>>("backStack", NAV_PACKAGE)
        .collectAsState()
    val transitionsInProgress by composeNavigator
        .getProperty<StateFlow<Map<NavBackStackEntry, NavigatorState.OnTransitionCompleteListener>>>("transitionsInProgress", NAV_PACKAGE)
        .collectAsState()
    val backStackEntry: NavBackStackEntry? = transitionsInProgress.keys.lastOrNull { entry ->
        entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    } ?: backStack.lastOrNull { entry ->
        entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }
    SideEffect {
        // When we place the first entry on the backstack we won't get a call on onDispose since
        // the Crossfade will remain in the compose hierarchy. We need to move that entry to
        // RESUMED separately.
        if (backStack.size == 1 && transitionsInProgress.size == 1) {
            transitionsInProgress.forEach { entry ->
                entry.value.onTransitionComplete()
            }
        }
    }
    if (backStackEntry != null) {
        // while in the scope of the composable, we provide the navBackStackEntry as the
        // ViewModelStoreOwner and LifecycleOwner
        AnimatedContent(
            targetState = backStackEntry,
            modifier = modifier,
            transitionSpec = {
                val initialRoute: NavBackStackEntry = initialState
                val targetRoute: NavBackStackEntry = targetState
                when {
                    transition.comparator(initialRoute, targetRoute) < 0 -> transition.forward()
                    transition.comparator(initialRoute, targetRoute) > 0 -> transition.backward()
                    transition.comparator(initialRoute, targetRoute) == 0 -> transition.default()
                    else -> illegalState
                }
            },
        ) { currentEntry: NavBackStackEntry ->
            currentEntry.LocalOwnersProvider(saveableStateHolder) {
                val destination: ComposeNavigator.Destination = currentEntry.destination as ComposeNavigator.Destination
                val content: @Composable (NavBackStackEntry) -> Unit = destination.getProperty("content", NAV_PACKAGE)
                content(currentEntry)
            }
            DisposableEffect(currentEntry) {
                onDispose {
                    transitionsInProgress.forEach { entry ->
                        entry.value.onTransitionComplete()
                    }
                }
            }
        }
        /*
        // This is the original crossfade code from Google...
        Crossfade(backStackEntry, modifier) { currentEntry ->
            currentEntry.LocalOwnersProvider(saveableStateHolder) {
                val destination: ComposeNavigator.Destination = currentEntry.destination as ComposeNavigator.Destination
                val content: @Composable (NavBackStackEntry) -> Unit = destination.getProperty("content", NAV_PACKAGE)
                content(currentEntry)
            }
            DisposableEffect(currentEntry) {
                onDispose {
                    transitionsInProgress.forEach { entry ->
                        entry.value.onTransitionComplete()
                    }
                }
            }
        }*/
    }

    val dialogNavigator = navController.navigatorProvider.get<Navigator<out NavDestination>>("dialog") as? DialogNavigator ?: return
// Show any dialog destinations
    DialogHost(dialogNavigator)
}

private fun <V> Any.getProperty(
    name: String,
    packageName: String? = null,
): V {
    val propertyGetter = "get${name.replaceFirstChar { it.uppercase() }}${packageName?.let { "\$$packageName" }?: ""}"
    return SuperReflect.on(this).call(propertyGetter).get()
}

private val illegalState: Nothing
    get() = throw IllegalStateException("this is an illegal state that should never happen")