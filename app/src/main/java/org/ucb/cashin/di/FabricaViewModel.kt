package org.ucb.cashin.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import org.ucb.cashin.CashinApp

/**
 * Crea la fábrica de un ViewModel tomando sus repositorios del [AppContainer].
 *
 *     class LoginViewModel(private val auth: AuthRepository) : ViewModel() {
 *         companion object { val Factory = fabricaDe { LoginViewModel(it.authRepository) } }
 *     }
 *
 * y en la pantalla:  viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory)
 */
inline fun <reified VM : ViewModel> fabricaDe(
    crossinline crear: (AppContainer) -> VM,
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) as CashinApp
        crear(app.requireContainer())
    }
}
