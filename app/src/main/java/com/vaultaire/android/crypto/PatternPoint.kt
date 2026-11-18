package com.vaultaire.android.crypto

data class PatternPoint(val row: Int, val col: Int) {
    val index: Int get() = row * 5 + col
}
