/*
 * // Copyright (c) 2021 Andrew Stephens. All rights reserved.
 * // Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package com.drewcodesit.afiexplorer.ui.easterEgg

import android.content.Context
import androidx.core.content.edit

class BranchManager(context: Context) {

    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun getBranch(): Branch {
        val value = prefs.getString("branch", Branch.DEFAULT.name)
        return Branch.valueOf(value!!)
    }

    fun toggleBranch(): Branch {
        val newBranch = when (getBranch()) {
            Branch.DEFAULT -> Branch.AIR_FORCE
            Branch.AIR_FORCE -> Branch.SPACE_FORCE
            Branch.SPACE_FORCE -> Branch.DEFAULT
        }

        prefs.edit { putString("branch", newBranch.name) }
        return newBranch
    }
}