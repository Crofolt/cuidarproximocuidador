package com.mesawa.cuidarproximocuidador.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mesawa.cuidarproximocuidador.ui.propostas.PropostaCuidado
import com.mesawa.cuidarproximocuidador.ui.propostas.PropostasRepository

class InicioCuidadorViewModel(
    private val propostasRepository: PropostasRepository = PropostasRepository()
) : ViewModel() {
    private val _proposta = MutableLiveData(PropostaCuidado())
    val proposta: LiveData<PropostaCuidado> = _proposta

    private val _online = MutableLiveData(false)
    val online: LiveData<Boolean> = _online

    fun carregar() {
        propostasRepository.carregarPropostaDemo { _proposta.value = it }
    }

    fun alternarOnline() {
        _online.value = !(_online.value ?: false)
    }
}
