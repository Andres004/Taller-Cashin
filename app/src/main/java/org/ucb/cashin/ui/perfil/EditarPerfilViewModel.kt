package org.ucb.cashin.ui.perfil

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ucb.cashin.R
import org.ucb.cashin.data.AuthRepository
import org.ucb.cashin.data.PerfilRepository
import org.ucb.cashin.di.fabricaDe
import org.ucb.cashin.model.ErrorApp
import org.ucb.cashin.model.Moneda
import org.ucb.cashin.model.Perfil
import org.ucb.cashin.model.comoErrorApp
import org.ucb.cashin.util.Validaciones

/** Todo lo que "Editar perfil" necesita para dibujarse. */
data class EditarPerfilUiState(
    val perfil: Perfil? = null,
    val avatar: AvatarCashi = AvatarCashi.FELIZ,
    val nombre: String = "",
    val moneda: Moneda = Moneda.BOB,
    /** Null = no dijo qué día le pagan. */
    val diaCobro: Int? = null,
    /** Solo las cuentas con correo pueden cambiar la contraseña (con Google no aparece). */
    val puedeCambiarContrasena: Boolean = true,
    val cargando: Boolean = true,
    val errorCarga: ErrorApp? = null,
    @StringRes val errorNombre: Int? = null,
    val guardando: Boolean = false,
    val errorGuardar: ErrorApp? = null,
    val guardado: Boolean = false,
)

/** HU-05: cambiar avatar, nombre, moneda y día de cobro. Se guarda todo junto con "Guardar cambios". */
class EditarPerfilViewModel(
    private val auth: AuthRepository,
    private val perfilRepository: PerfilRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditarPerfilUiState(puedeCambiarContrasena = auth.esCuentaConCorreo()))
    val uiState: StateFlow<EditarPerfilUiState> = _uiState.asStateFlow()

    init { cargar() }

    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, errorCarga = null) }
            perfilRepository.obtener()
                .onSuccess { perfil ->
                    _uiState.update {
                        it.copy(
                            perfil = perfil,
                            avatar = AvatarCashi.de(perfil.avatar),
                            nombre = perfil.nombre,
                            moneda = perfil.moneda,
                            diaCobro = perfil.diaCobro,
                            cargando = false,
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(cargando = false, errorCarga = e.comoErrorApp()) } }
        }
    }

    fun onAvatar(avatar: AvatarCashi) = _uiState.update { it.copy(avatar = avatar) }

    /** El nombre no puede pasar de 80 caracteres, igual que en la base de datos. */
    fun onNombre(valor: String) = _uiState.update { it.copy(nombre = valor.take(Validaciones.MAX_NOMBRE), errorNombre = null) }

    fun onMoneda(moneda: Moneda) = _uiState.update { it.copy(moneda = moneda) }

    /** Tocar el día elegido otra vez lo quita. */
    fun onDiaCobro(dia: Int?) = _uiState.update {
        it.copy(diaCobro = if (dia == it.diaCobro) null else dia?.takeIf { d -> d in 1..31 })
    }

    fun guardar() {
        val estado = _uiState.value
        val perfil = estado.perfil ?: return
        if (estado.guardando) return
        if (!Validaciones.nombreValido(estado.nombre)) {
            _uiState.update { it.copy(errorNombre = R.string.validacion_nombre) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, errorGuardar = null) }
            perfilRepository.actualizarTodo(
                id = perfil.id,
                nombre = estado.nombre.trim(),
                moneda = estado.moneda,
                avatar = estado.avatar.clave,
                diaCobro = estado.diaCobro,
                diaCobroCambio = estado.diaCobro != perfil.diaCobro,
            )
                .onSuccess { _uiState.update { it.copy(guardando = false, guardado = true) } }
                .onFailure { e -> _uiState.update { it.copy(guardando = false, errorGuardar = e.comoErrorApp()) } }
        }
    }

    companion object {
        val Factory = fabricaDe { EditarPerfilViewModel(it.authRepository, it.perfilRepository) }
    }
}
