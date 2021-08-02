# RallyApp with custom navigation transitions

This is a an example of how to use a modified `NavHost` that allows a degree of customisation for the transition.
The original source code is from the
[Navigation in Jetpack Compose Codelab](https://developer.android.com/codelabs/jetpack-compose-navigation).

[ModdedNavHost.kt](app/src/main/java/com/example/compose/rally/ModdedNavHost.kt) is a modified version of `NavHost` from the navigation compose library, though specifically the [hardcoded `Crossfade`](https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/navigation/navigation-compose/src/main/java/androidx/navigation/compose/NavHost.kt#131) has been replaced with a more flexible `AnimatedContent`. The overloaded `NavHost` composables take a `NavTransition` that offer some flexibility.

To briefly go over the API modification, lets say you define your routes with an enum such as:

```
enum class Route {
    
    SCREEN_A, SCREEN_B;
    
    companion object {
        fun from(navBackStackEntry: NavBackStackEntry?): Route? =
            navBackStackEntry.routeName?.let { routeName: String ->
                try {
                    valueOf(routeName)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }

        private val NavBackStackEntry?.routeName: String?
            get() = this?.destination?.route?.substringBefore('/')
    }
```
Then all you have to add to your NavHost is a `NavTransition`, optionally using a built in `NavTransition` factory such as `horizontalSlideFade`:
```
NavHost(
        navController = navController,
        startDestination = Route.A.name,
        // this transition property is the addition 
        transition = NavTransition.horizontalSlideFade { initial: NavBackStackEntry, target: NavBackStackEntry ->
            Route.from(initial)!!.compareTo(Route.from(target)!!)
        },
        modifier = modifier,
    ) {
      // graph definition
    }
```

Horizontal transition:

https://user-images.githubusercontent.com/5406413/127901922-9f1aa6fb-59ad-4d9d-9c3a-7df49e7ff60c.mp4

Vertical transition:

https://user-images.githubusercontent.com/5406413/127901948-8f2aaff4-7073-4d0b-84af-3b140556e313.mp4

Fade out > fade in transition (not as nice as crossfade admitedly):

https://user-images.githubusercontent.com/5406413/127901995-e0fcbfc9-79ec-459d-a44e-dc238d5fe68e.mp4

