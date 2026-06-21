/*
 * // Copyright (c) 2021 Andrew Stephens. All rights reserved.
 * // Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package com.drewcodesit.afiexplorer.ui.options

data class OptionsItems(
    val id: Int,
    val title: String,
    val iconRes: Int,
    val onClick: () -> Unit
)