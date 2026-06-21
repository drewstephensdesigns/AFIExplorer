/*
 * // Copyright (c) 2021 Andrew Stephens. All rights reserved.
 * // Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package com.drewcodesit.afiexplorer.utils.filter

import android.content.Context
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SearchBarManager(
    private val fab: FloatingActionButton,
    private val searchRow: View,
    private val searchEditText: EditText,
    cancel: ImageButton,
    private val bottomNav: BottomNavigationView
) {

    // Track state to avoid redundant animation
    private var isExpanded = false

    init {
        fab.setOnClickListener { expand() }
        cancel.setOnClickListener { collapse() }

        // Collapse when user submits or backpress
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if(actionId == EditorInfo.IME_ACTION_SEARCH){
                collapse()
                true
            } else false
        }
    }

    fun expand(){
        if (isExpanded) return
        isExpanded = true

        searchRow.visibility = View.VISIBLE

        // Fade in the search row while scaling the FAB out
        searchRow.animate()
            .alpha(1f)
            .setDuration(200)
            .start()

        fab.animate()
            .scaleX(0f)
            .scaleY(0f)
            .setDuration(150)
            .withEndAction { fab.visibility = View.GONE }
            .start()

        bottomNav.animate()
            .translationY(bottomNav.height.toFloat())
            .setDuration(200)
            .withEndAction { bottomNav.visibility = View.GONE }
            .start()

        // Open soft keyboard and focus the field automatically
        searchEditText.requestFocus()
        searchEditText.postDelayed({
            val imm = searchEditText.context
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT)
        }, 200)
    }

    fun collapse(){
        if (!isExpanded) return
        isExpanded = false

        searchEditText.text?.clear()

        // Hide keyboard first, then reverse the animation
        val imm = searchEditText.context
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)

        searchRow.animate()
            .alpha(0f)
            .setDuration(150)
            .withEndAction { searchRow.visibility = View.GONE }
            .start()

        fab.visibility = View.VISIBLE
        fab.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .start()

        // Slide bottom nav back up into position
        bottomNav.visibility = View.VISIBLE
        bottomNav.animate()
            .translationY(0f)
            .setDuration(200)
            .start()
    }

    // let fragment forward back-press
    fun isExpanded() = isExpanded
}