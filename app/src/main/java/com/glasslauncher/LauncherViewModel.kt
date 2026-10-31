package com.glasslauncher

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppRepository(app)

    var apps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    private var dockPackages by mutableStateOf<List<String>>(emptyList())

    /** Incrementa toda vez que o usuário aperta Home com o launcher já aberto. */
    var homeSignal by mutableIntStateOf(0)
        private set

    val dockApps: List<AppInfo> by derivedStateOf {
        val picked = dockPackages
            .mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }
            .distinct()
        val filler = apps.filter { it !in picked }.take((4 - picked.size).coerceAtLeast(0))
        (picked + filler).take(4)
    }

    val gridApps: List<AppInfo> by derivedStateOf {
        val dockKeys = dockApps.map { it.key }.toSet()
        apps.filterNot { it.key in dockKeys }
    }

    fun refresh() {
        viewModelScope.launch {
            apps = repo.load()
            dockPackages = repo.defaultDockPackages()
        }
    }

    fun onHomePressed() {
        homeSignal++
    }
}
