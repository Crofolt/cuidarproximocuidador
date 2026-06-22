package com.mesawa.cuidarproximocuidador.ui.perfil.dados

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mesawa.cuidarproximocuidador.data.firestore.CuidadorFirestoreTree
import com.mesawa.cuidarproximocuidador.data.local.LocalSqlStore

class DadosProfissionaisRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val localSql: LocalSqlStore = LocalSqlStore.instance,
    private val tree: CuidadorFirestoreTree = CuidadorFirestoreTree(firestore)
) {
    fun salvar(
        cuidadorId: String,
        form: DadosProfissionaisForm,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            onFailure("Entre novamente para salvar seu currículo.")
            return
        }

        val valorHora = form.valorHora.replace(",", ".").toDoubleOrNull() ?: 0.0
        val profissional = mapOf(
            "especialidade" to form.funcao,
            "funcao" to form.funcao,
            "ano_inicio" to form.anoInicio,
            "ano_fim" to form.anoFim,
            "experiencia" to experienciaTexto(form),
            "valorHora" to valorHora,
            "valor_hora" to valorHora,
            "facilidades" to form.especialidades,
            "bio" to form.bio,
            "sobreVoce" to form.sobreVoce,
            "sobre_voce" to form.sobreVoce,
            "disponibilidade" to disponibilidadeTexto(form),
            "dias_disponiveis" to form.diasDisponiveis,
            "horario_inicio" to form.horaInicio,
            "horario_fim" to form.horaFim,
            "certificados" to form.certificados,
            "habilidades" to mapOf(
                "alzheimer_demencias" to form.atendeAlzheimer,
                "mobilidade_banho_quedas" to form.atendeMobilidade,
                "medicamentos" to form.atendeMedicamentos,
                "companhia_alimentacao_rotina" to form.atendeCompanhia
            ),
            "atualizado_em" to FieldValue.serverTimestamp()
        )
        val cadastro = mapOf(
            "qualificacao" to mapOf(
                "especialidade" to form.funcao,
                "funcao" to form.funcao,
                "ano_inicio" to form.anoInicio,
                "ano_fim" to form.anoFim,
                "experiencia" to experienciaTexto(form),
                "facilidades" to form.especialidades,
                "bio" to form.bio,
                "sobre_voce" to form.sobreVoce,
                "certificados" to form.certificados,
                "alzheimer_demencias" to form.atendeAlzheimer,
                "mobilidade_quedas" to form.atendeMobilidade,
                "medicacao" to form.atendeMedicamentos,
                "companhia_rotina" to form.atendeCompanhia
            ),
            "atendimento" to mapOf(
                "valor_hora" to valorHora,
                "disponibilidade" to disponibilidadeTexto(form),
                "dias_disponiveis" to form.diasDisponiveis,
                "horario_inicio" to form.horaInicio,
                "horario_fim" to form.horaFim
            ),
            "atualizado_em" to FieldValue.serverTimestamp()
        )

        localSql.salvarRegistro(
            uid = uid,
            tipo = "dados_profissionais",
            chave = "curriculo",
            payload = mapOf(
                "cuidadorId" to cuidadorId,
                "profissional" to profissional,
                "cadastro" to cadastro
            )
        )

        tree.encontrarIdProfissional(
            uid = uid,
            idPreferido = cuidadorId,
            onSuccess = { idProfissional ->
                salvarCadastro(uid, idProfissional, cadastro, profissional)
                salvarProfissional(uid, idProfissional, profissional, onSuccess, onFailure)
            },
            onFailure = onFailure
        )
    }

    private fun salvarCadastro(
        uid: String,
        idProfissional: String,
        cadastro: Map<String, Any>,
        profissional: Map<String, Any>
    ) {
        tree.cadastroDoc(idProfissional).set(
            cadastro + mapOf(
                "uid" to uid,
                "uid_auth" to uid,
                "id_profissional" to idProfissional,
                "perfil_publico" to mapOf(
                    "id_profissional" to idProfissional,
                    "especialidade" to profissional["especialidade"],
                    "funcao" to profissional["funcao"],
                    "valorHora" to profissional["valorHora"],
                    "valor_hora" to profissional["valor_hora"],
                    "bio" to profissional["bio"],
                    "sobre_voce" to profissional["sobre_voce"],
                    "disponibilidade" to profissional["disponibilidade"]
                )
            ),
            SetOptions.merge()
        )
        tree.salvarPerfilPublico(uid, idProfissional, profissional)
    }

    private fun experienciaTexto(form: DadosProfissionaisForm): String {
        return when {
            form.anoInicio.isNotBlank() && form.anoFim.isNotBlank() -> "${form.anoInicio} até ${form.anoFim}"
            form.anoInicio.isNotBlank() -> "Desde ${form.anoInicio}"
            else -> ""
        }
    }

    private fun disponibilidadeTexto(form: DadosProfissionaisForm): String {
        val dias = form.diasDisponiveis.joinToString(", ")
        val horario = listOf(form.horaInicio, form.horaFim).filter { it.isNotBlank() }.joinToString(" às ")
        return listOf(dias, horario).filter { it.isNotBlank() }.joinToString(" • ")
    }

    private fun salvarProfissional(
        uid: String,
        cuidadorId: String,
        dados: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val docRef = tree.profissionaisLegadoDoc()
        val id = cuidadorId.trim()
        if (id.isNotBlank()) {
            docRef.get()
                .addOnSuccessListener { doc ->
                    val atual = ((doc.get("medicos") as? Map<*, *>)?.get(id) as? Map<*, *>)
                        ?.mapKeys { it.key.toString() }
                        .orEmpty()
                    docRef.update(mapOf("medicos.$id" to atual + dados))
                        .addOnSuccessListener {
                            tree.salvarIdProfissional(uid, id)
                            localSql.salvarRegistro(uid, "dados_profissionais", "curriculo", mapOf("cuidadorId" to id, "profissional" to (atual + dados)), sincronizado = true)
                            onSuccess()
                        }
                        .addOnFailureListener { onFailure("Não consegui atualizar seu perfil profissional.") }
                }
                .addOnFailureListener { onFailure("Não consegui consultar seu perfil profissional.") }
            return
        }

        docRef.get()
            .addOnSuccessListener { doc ->
                val medicos = doc.get("medicos") as? Map<*, *>
                val encontrado = medicos?.entries?.firstOrNull { (_, value) ->
                    val map = value as? Map<*, *> ?: return@firstOrNull false
                    map["uid"]?.toString() == uid
                }?.key?.toString().orEmpty()

                if (encontrado.isBlank()) {
                    onFailure("Não encontrei seu perfil profissional aprovado para atualizar.")
                } else {
                    val atual = ((medicos?.get(encontrado) as? Map<*, *>)
                        ?.mapKeys { it.key.toString() })
                        .orEmpty()
                    docRef.update(mapOf("medicos.$encontrado" to atual + dados))
                        .addOnSuccessListener {
                            localSql.salvarRegistro(uid, "dados_profissionais", "curriculo", mapOf("cuidadorId" to encontrado, "profissional" to (atual + dados)), sincronizado = true)
                            onSuccess()
                        }
                        .addOnFailureListener { onFailure("Não consegui atualizar seu perfil profissional.") }
                }
            }
            .addOnFailureListener { onFailure("Não consegui consultar seu perfil profissional.") }
    }
}
