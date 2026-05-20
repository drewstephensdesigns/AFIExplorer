package com.drewcodesit.afiexplorer.models

data class OptionItems(
    val id: Int,
    val title: String,
    val iconRes: Int,
    val onClick: () -> Unit
)
