package com.mesawa.cuidarproximocuidador.ui.perfil.avaliacoes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AvaliacoesViewModel : ViewModel() {
    private val repository = AvaliacoesRepository()
    private val _titulo = MutableLiveData("5.0")
    val titulo: LiveData<String> = _titulo
    private val _avaliacoes = MutableLiveData<List<AvaliacaoRecebida>>(emptyList())
    val avaliacoes: LiveData<List<AvaliacaoRecebida>> = _avaliacoes
    private val _mensagem = MutableLiveData("Carregando avaliacoes...")
    val mensagem: LiveData<String> = _mensagem

    fun carregar(uid: String, cuidadorId: String) {
        repository.carregar(
            uid = uid,
            cuidadorId = cuidadorId,
            onSuccess = { avaliacoes ->
                val media = avaliacoes.map { it.estrelas }.average().takeIf { !it.isNaN() } ?: 5.0
                _titulo.value = String.format("%.1f", media)
                _avaliacoes.value = avaliacoes
                _mensagem.value = ""
            },
            onError = {
                _mensagem.value = "Nao consegui carregar as avaliacoes agora."
            }
        )
    }
}
