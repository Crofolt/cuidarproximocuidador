package com.mesawa.cuidarproximocuidador.ui.propostas

data class PropostaCuidado(
    val id: String = "proposta_irene_demo",
    val idosoNome: String = "Irene",
    val idosoIdade: Int = 80,
    val condicao: String = "Saudavel, precisa de apoio preventivo",
    val endereco: String = "Rua das Flores, Maringa",
    val cidade: String = "Maringa",
    val uf: String = "PR",
    val horas: Int = 4,
    val valorOferecido: Double = 320.0,
    val distanciaKm: Double = 2.4,
    val qualificacoesDesejadas: List<String> = listOf("mobilidade", "medicacao", "companhia"),
    val compativel: Boolean = true,
    val tarefas: List<String> = listOf(
        "Companhia e conversa",
        "Banho assistido",
        "Lembrete de medicacao",
        "Prevencao de quedas"
    ),
    val observacoes: String = "Prefere cuidadora paciente e calma. Possui leve dificuldade para caminhar.",
    val responsavelNome: String = "Familia da Irene",
    val responsavelTelefone: String = "44999999999",
    val fotoIdosoUrl: String = "",
    val status: PropostaStatus = PropostaStatus.NOVA,
    val inicioAtendimentoMillis: Long = System.currentTimeMillis(),
    val duracaoDias: Int = 3
) {
    val tempoTexto: String get() = "${horas}h"
    val fimAtendimentoMillis: Long get() = inicioAtendimentoMillis + duracaoDias * DIA_MILLIS

    fun progressoAtendimento(agoraMillis: Long = System.currentTimeMillis()): Int {
        val duracao = (fimAtendimentoMillis - inicioAtendimentoMillis).coerceAtLeast(1L)
        val passado = (agoraMillis - inicioAtendimentoMillis).coerceIn(0L, duracao)
        return ((passado * 100) / duracao).toInt()
    }

    fun tempoRestanteTexto(agoraMillis: Long = System.currentTimeMillis()): String {
        val restante = (fimAtendimentoMillis - agoraMillis).coerceAtLeast(0L)
        val dias = ((restante + DIA_MILLIS - 1) / DIA_MILLIS).toInt()
        return when {
            restante == 0L -> "Prazo concluido, pronto para finalizar"
            dias <= 1 -> "Finaliza hoje"
            else -> "Faltam $dias dias"
        }
    }

    companion object {
        private const val DIA_MILLIS = 24L * 60L * 60L * 1000L
    }
}

enum class PropostaStatus(val firestore: String) {
    NOVA("nova"),
    RECUSADA("recusada"),
    CANDIDATADA("candidatura_enviada"),
    ACEITA("aceita"),
    FINALIZADA("finalizada");

    companion object {
        fun from(value: String?) = entries.firstOrNull {
            it.firestore == value || it.name.equals(value, ignoreCase = true)
        } ?: NOVA
    }
}
