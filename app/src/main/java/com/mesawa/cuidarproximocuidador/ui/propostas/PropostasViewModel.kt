package com.mesawa.cuidarproximocuidador.ui.propostas

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PropostasViewModel(
    private val repository: PropostasRepository = PropostasRepository()
) : ViewModel() {
    private val _proposta = MutableLiveData(PropostaCuidado())
    val proposta: LiveData<PropostaCuidado> = _proposta

    fun carregar() {
        repository.carregarPropostaDemo { _proposta.value = it }
    }

    fun enviarCandidatura(onSuccess: (PropostaCuidado) -> Unit, onFailure: (String) -> Unit) {
        val atual = _proposta.value ?: PropostaCuidado()
        repository.enviarCandidatura(
            proposta = atual,
            onSuccess = {
                val atualizada = atual.copy(status = PropostaStatus.CANDIDATADA)
                _proposta.value = atualizada
                onSuccess(atualizada)
            },
            onFailure = onFailure
        )
    }

    fun recusarProposta(onSuccess: (PropostaCuidado) -> Unit, onFailure: (String) -> Unit) {
        val atual = _proposta.value ?: PropostaCuidado()
        repository.atualizarStatus(
            proposta = atual,
            status = PropostaStatus.RECUSADA,
            onSuccess = {
                val atualizada = atual.copy(status = PropostaStatus.RECUSADA)
                _proposta.value = atualizada
                onSuccess(atualizada)
            },
            onFailure = onFailure
        )
    }

    fun aceitarProposta(onSuccess: (PropostaCuidado) -> Unit, onFailure: (String) -> Unit) {
        val atual = _proposta.value ?: PropostaCuidado()
        repository.atualizarStatus(
            proposta = atual,
            status = PropostaStatus.ACEITA,
            onSuccess = {
                val atualizada = atual.copy(status = PropostaStatus.ACEITA, inicioAtendimentoMillis = System.currentTimeMillis())
                _proposta.value = atualizada
                onSuccess(atualizada)
            },
            onFailure = onFailure
        )
    }
}
