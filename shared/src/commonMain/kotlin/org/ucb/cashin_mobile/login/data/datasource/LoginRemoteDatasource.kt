package org.ucb.cashin_mobile.login.data.datasource

import org.ucb.cashin_mobile.login.data.dto.LoginRequestDto
import org.ucb.cashin_mobile.login.data.dto.LoginResponseDto

interface LoginRemoteDatasource {

    suspend fun login(request: LoginRequestDto): LoginResponseDto
}