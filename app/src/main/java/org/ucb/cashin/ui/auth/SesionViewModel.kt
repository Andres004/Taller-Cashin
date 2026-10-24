package org.ucb.cashin.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.ucb.cashin.data.AuthRepository
import org.ucb.cashin.data.EstadoSesion
import org.ucb.cashin.di.fabricaDe

/** Expone si hay sesión. CashinRaiz lo usa para mostrar el flujo de login o la app. */
class SesionViewModel(auth: AuthRepository) : ViewModel() {

    val estado: StateFlow<EstadoSesion> = auth.estadoSesion
        .stateIn(viewModelScope, SharingStarted.Eagerly, EstadoSesion.CARGANDO)

    companion object {
        val Factory = fabricaDe { SesionViewModel(it.authRepository) }
    }
}
