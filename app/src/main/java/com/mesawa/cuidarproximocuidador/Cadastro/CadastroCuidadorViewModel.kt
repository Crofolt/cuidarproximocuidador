package com.mesawa.cuidarproximocuidador.Cadastro

import com.google.firebase.firestore.FieldValue

class CadastroCuidadorViewModel {

    fun validar(dados: CadastroCuidadorDados): String? {
        return when {
            dados.nomeCompleto.isBlank() -> "Informe o nome completo."
            dados.cpf.length < 11 -> "Informe um CPF válido."
            dados.nascimento.isBlank() -> "Informe a data de nascimento."
            dados.telefone.length < 10 -> "Informe um telefone válido."
            !dados.email.contains("@") -> "Informe um email válido."
            dados.senha.length < 6 -> "A senha precisa ter pelo menos 6 caracteres."
            dados.cidade.isBlank() || dados.uf.length != 2 -> "Informe cidade e UF."
            dados.valorHora.isBlank() -> "Informe o valor por hora."
            dados.especialidade.isBlank() -> "Informe sua especialidade."
            dados.curso.isBlank() -> "Informe sua qualificação principal."
            dados.experiencia.isBlank() -> "Informe seu tempo de experiência."
            dados.antecedentes.isBlank() -> "Informe o protocolo ou data da certidão de antecedentes."
            dados.referenciaNome.isBlank() || dados.referenciaTelefone.length < 10 -> "Informe uma referência profissional."
            !dados.autorizouVerificacao -> "Autorize a verificação para enviar o cadastro."
            else -> null
        }
    }

    fun toFirestore(uid: String, dados: CadastroCuidadorDados, fotoUrl: String?): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "status" to "em_analise",
            "ativo" to false,
            "dados_pessoais" to mapOf(
                "nome" to dados.nomeCompleto,
                "cpf" to dados.cpf,
                "data_nascimento" to dados.nascimento,
                "telefone" to dados.telefone,
                "email" to dados.email,
                "foto_url" to fotoUrl
            ),
            "perfil_publico" to mapOf(
                "nome" to dados.nomeCompleto,
                "especialidade" to dados.especialidade,
                "cidade" to dados.cidade,
                "uf" to dados.uf.uppercase(),
                "fotoUrl" to fotoUrl,
                "ativo" to false
            ),
            "atendimento" to mapOf(
                "cidade" to dados.cidade,
                "cidade_normalizada" to normalizarCidade(dados.cidade),
                "uf" to dados.uf.uppercase(),
                "estado" to "Parana",
                "latitude" to dados.latitude,
                "longitude" to dados.longitude,
                "raio_km" to dados.raioKm,
                "valor_hora" to dados.valorHora,
                "disponibilidade" to dados.disponibilidade
            ),
            "qualificacao" to mapOf(
                "especialidade" to dados.especialidade,
                "curso" to dados.curso,
                "instituicao" to dados.instituicao,
                "experiencia" to dados.experiencia,
                "bio" to dados.bio,
                "medicacao" to dados.experienciaMedicacao,
                "mobilidade_quedas" to dados.experienciaMobilidade,
                "alzheimer_demencias" to dados.experienciaAlzheimer
            ),
            "seguranca" to mapOf(
                "antecedentes_protocolo_ou_data" to dados.antecedentes,
                "referencia_nome" to dados.referenciaNome,
                "referencia_telefone" to dados.referenciaTelefone,
                "autorizou_verificacao" to dados.autorizouVerificacao
            ),
            "criado_em" to FieldValue.serverTimestamp(),
            "atualizado_em" to FieldValue.serverTimestamp()
        )
    }

    private fun normalizarCidade(cidade: String): String {
        return cidade.lowercase()
            .replace("á", "a")
            .replace("ã", "a")
            .replace("â", "a")
            .replace("é", "e")
            .replace("ê", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ô", "o")
            .replace("õ", "o")
            .replace("ú", "u")
            .replace("ç", "c")
            .trim()
    }
}
