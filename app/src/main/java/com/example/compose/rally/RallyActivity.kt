/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.compose.rally

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.*
import androidx.navigation.compose.*
import com.example.compose.rally.Route.*
import com.example.compose.rally.data.Account
import com.example.compose.rally.data.UserData
import com.example.compose.rally.ui.accounts.AccountsBody
import com.example.compose.rally.ui.accounts.SingleAccountBody
import com.example.compose.rally.ui.bills.BillsBody
import com.example.compose.rally.ui.components.RallyTabRow
import com.example.compose.rally.ui.overview.OverviewBody
import com.example.compose.rally.ui.theme.RallyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * This Activity recreates part of the Rally Material Study from
 * https://material.io/design/material-studies/rally.html
 */
class RallyActivity : ComponentActivity() {

    lateinit var navHostController: NavHostController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("AIDAN", "onCreate")
        setContent {
            val navHostController: NavHostController = rememberNavController()
            LaunchedEffect(navHostController) {
                this@RallyActivity.navHostController = navHostController
            }
            RallyApp(navHostController)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent ?: return
        val condition = intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

        navDeepLink {
            uriPattern = "rally://${Accounts.name}/{name}"
        }.action
        Log.d("AIDAN", "onNewIntent: condition: $condition, handleDeepLink: ${navHostController.handleDeepLink(intent)}")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("AIDAN", "onDestroy")
    }
}

@Composable
fun RallyApp(
    navHostController: NavHostController,
) {
    RallyTheme {
        val routes = remember { Route.values().toList() }
        val backstackEntry: NavBackStackEntry? by navHostController.currentBackStackEntryAsState()
        val currentScreen: Route? = remember(backstackEntry?.id) {
            Route.from(backstackEntry)
        }
        Scaffold(
            topBar = {
                RallyTabRow(
                    routes = routes,
                    onTabSelected = { screen ->
                        navHostController.navigate(screen.name)
                    },
                    currentScreen = currentScreen,
                )
            }
        ) { innerPadding ->
            RallyNavHost(navHostController, modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
fun RallyNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    NavHost(
        navController = navController,
        startDestination = Overview.name,
        transition = NavTransition.verticalSlideFade { initial: NavBackStackEntry, target: NavBackStackEntry ->
            Route.from(initial)!!.compareTo(Route.from(target)!!)
        },
        modifier = modifier
    ) {
        composable(Overview.name) {
            OverviewBody(
                onClickSeeAllAccounts = { navController.navigate(Accounts.name) },
                onClickSeeAllBills = { navController.navigate(Bills.name) },
                onAccountClick = { //name ->
                    //navController.navigateToSingleAccount(name)
                    scope.launch {
                        navController.navigateToSingleAccount(accountName = "Home Savings")
                        delay(timeMillis = 3000)
                        navController.navigateToSingleAccount(accountName = "Car Savings")
                    }
                },
            )
        }
        composable(Accounts.name) {
            AccountsBody(accounts = UserData.accounts) { //_ ->
                //navController.navigateToSingleAccount(accountName = name)
                scope.launch {
                    navController.navigateToSingleAccount(accountName = "Home Savings")
                    delay(timeMillis = 3000)
                    navController.navigateToSingleAccount(accountName = "Car Savings")
                }
            }
        }
        composable(Bills.name) {
            BillsBody(bills = UserData.bills)
        }
        val accountsName = Accounts.name
        composable(
            route = "$accountsName/{name}",
            arguments = listOf(
                navArgument("name") {
                    type = NavType.StringType
                }
            ),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "rally://$accountsName/{name}"
                }
            ),
        ) { entry ->
            val accountName: String? = remember(entry) {
                entry.arguments?.getString("name")
            }
            val account: Account = remember(accountName) {
                UserData.getAccount(accountName)
            }
            SingleAccountBody(account = account)
            LaunchedEffect(accountName) {
                Log.d("AIDAN", "$accountsName/$accountName")
            }
        }
    }
}

private fun NavHostController.navigateToSingleAccount(accountName: String) {
    navigate(
        route = "${Accounts.name}/$accountName",
    ) {
        launchSingleTop = true
        popUpTo(Overview.name)
    }
}