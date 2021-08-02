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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavBackStackEntry

/**
 * Screen metadata for Rally.
 */
enum class Route(val icon: ImageVector) {

    Overview(icon = Icons.Filled.PieChart),
    Accounts(icon = Icons.Filled.AttachMoney),
    Bills(icon = Icons.Filled.MoneyOff);

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
            get() {
                if (this == null) return null
                val routeName: String? = destination.route?.substringBefore('/')
                return routeName
            }
    }
}