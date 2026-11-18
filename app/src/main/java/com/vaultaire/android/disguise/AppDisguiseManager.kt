package com.vaultaire.android.disguise

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

enum class DisguiseIcon(val aliasClassName: String, val displayName: String) {
    DEFAULT("com.vaultaire.android.MainActivity", "Vaultaire"),
    CALCULATOR("com.vaultaire.android.alias.CalculatorAlias", "Calculator"),
    PRINTER("com.vaultaire.android.alias.PrinterAlias", "Printer"),
    NOTES("com.vaultaire.android.alias.NotesAlias", "Notes"),
    SCANNER("com.vaultaire.android.alias.ScannerAlias", "Scanner"),
    WEATHER("com.vaultaire.android.alias.WeatherAlias", "Weather"),
    FILES("com.vaultaire.android.alias.FilesAlias", "Files");

    companion object {
        fun fromClassName(className: String): DisguiseIcon {
            return entries.find { it.aliasClassName == className } ?: DEFAULT
        }
    }
}

interface AppDisguiseManager {
    fun setDisguiseIcon(targetIcon: DisguiseIcon): Boolean
    fun getCurrentDisguiseIcon(): DisguiseIcon
    fun reconcileDisguiseState()
}

class AppDisguiseManagerImpl(
    private val context: Context,
    private val isProUserProvider: () -> Boolean
) : AppDisguiseManager {

    companion object {
        private const val TAG = "AppDisguiseManager"
        private const val PREFS_NAME = "vaultaire_disguise_prefs"
        private const val KEY_SELECTED_ALIAS = "selected_alias_class"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun setDisguiseIcon(targetIcon: DisguiseIcon): Boolean {
        if (targetIcon != DisguiseIcon.DEFAULT && !isProUserProvider()) {
            Log.w(TAG, "Attempted to set disguise icon ${targetIcon.name} without active Pro license.")
            return false
        }

        val packageManager = context.packageManager
        val packageName = context.packageName

        try {
            DisguiseIcon.entries.forEach { icon ->
                val component = ComponentName(packageName, icon.aliasClassName)
                val newState = if (icon == targetIcon) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }

                packageManager.setComponentEnabledSetting(
                    component,
                    newState,
                    PackageManager.DONT_KILL_APP
                )
            }

            prefs.edit().putString(KEY_SELECTED_ALIAS, targetIcon.aliasClassName).apply()
            Log.i(TAG, "Successfully updated disguise icon component to: ${targetIcon.aliasClassName}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update component enabled state", e)
            return false
        }
    }

    override fun getCurrentDisguiseIcon(): DisguiseIcon {
        val savedAlias = prefs.getString(KEY_SELECTED_ALIAS, DisguiseIcon.DEFAULT.aliasClassName)
            ?: DisguiseIcon.DEFAULT.aliasClassName
        return DisguiseIcon.fromClassName(savedAlias)
    }

    override fun reconcileDisguiseState() {
        val desiredIcon = if (isProUserProvider()) getCurrentDisguiseIcon() else DisguiseIcon.DEFAULT
        setDisguiseIcon(desiredIcon)
    }
}
