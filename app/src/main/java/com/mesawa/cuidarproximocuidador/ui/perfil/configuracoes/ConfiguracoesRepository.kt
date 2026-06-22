package com.mesawa.cuidarproximocuidador.ui.perfil.configuracoes

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mesawa.cuidarproximocuidador.data.firestore.CuidadorFirestoreTree
import com.mesawa.cuidarproximocuidador.data.local.LocalSqlStore

class ConfiguracoesRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val localSql: LocalSqlStore = LocalSqlStore.instance,
    private val tree: CuidadorFirestoreTree = CuidadorFirestoreTree(firestore)
) {
    fun salvarDadosConta(
        nome: String,
        telefone: String,
        cidade: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = uidAtual(onFailure) ?: return
        val payload = mapOf(
            "dados_pessoais.nome_publico" to nome,
            "dados_pessoais.telefone" to telefone,
            "dados_pessoais.cidade" to cidade,
            "atualizado_em" to Timestamp.now()
        )
        localSql.salvarRegistro(uid, "configuracoes", "dados_conta", payload)
        tree.encontrarIdProfissional(
            uid = uid,
            idPreferido = "",
            onSuccess = { idProfissional ->
                tree.cadastroDoc(idProfissional)
                    .set(
                        mapOf(
                            "uid" to uid,
                            "uid_auth" to uid,
                            "id_profissional" to idProfissional,
                            "dados_pessoais" to mapOf(
                                "nome_publico" to nome,
                                "telefone" to telefone,
                                "cidade" to cidade
                            ),
                            "perfil_publico" to mapOf(
                                "id_profissional" to idProfissional,
                                "nome" to nome,
                                "cidade" to cidade
                            ),
                            "atualizado_em" to Timestamp.now()
                        ),
                        SetOptions.merge()
                    )
                    .addOnSuccessListener {
                        tree.salvarPerfilPublico(
                            uid = uid,
                            idProfissional = idProfissional,
                            dados = mapOf("nome" to nome, "cidade" to cidade)
                        )
                        tree.profissionaisLegadoDoc().update(
                            mapOf(
                                "medicos.$idProfissional.nome" to nome,
                                "medicos.$idProfissional.cidade" to cidade
                            )
                        )
                        localSql.salvarRegistro(uid, "configuracoes", "dados_conta", payload, sincronizado = true)
                        onSuccess()
                    }
                    .addOnFailureListener { onFailure("Nao consegui salvar os dados da conta agora.") }
            },
            onFailure = onFailure
        )
    }

    fun salvarEndereco(
        endereco: String,
        cidade: String,
        raioKm: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = uidAtual(onFailure) ?: return
        val payload = mapOf(
            "endereco_base" to endereco,
            "cidade" to cidade,
            "raio_km" to raioKm,
            "atualizado_em" to Timestamp.now()
        )
        localSql.salvarRegistro(uid, "configuracoes", "endereco_atendimento", payload)
        tree.comCuidadorDocDaConta(
            uid = uid,
            onSuccess = { _, docRef ->
                docRef.set(
                    mapOf(
                        "uid" to uid,
                        "uid_auth" to uid,
                        "atendimento" to mapOf(
                            "endereco_base" to endereco,
                            "cidade" to cidade,
                            "raio_km" to raioKm
                        ),
                        "atualizado_em" to Timestamp.now()
                    ),
                    SetOptions.merge()
                )
                    .addOnSuccessListener {
                        localSql.salvarRegistro(uid, "configuracoes", "endereco_atendimento", payload, sincronizado = true)
                        onSuccess()
                    }
                    .addOnFailureListener { onFailure("Nao consegui salvar endereco e regioes agora.") }
            },
            onFailure = onFailure
        )
    }

    fun alterarSenha(novaSenha: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val user = auth.currentUser
        val uid = user?.uid
        if (user == null || uid.isNullOrBlank()) {
            onFailure("Entre novamente para trocar a senha.")
            return
        }
        val payload = mapOf("acao" to "troca_senha", "atualizado_em" to Timestamp.now())
        localSql.salvarEvento(uid, "seguranca_conta", payload)
        user.updatePassword(novaSenha)
            .addOnSuccessListener {
                registrarSolicitacao(uid, "senha_alterada", "Senha alterada pelo app")
                onSuccess()
            }
            .addOnFailureListener {
                onFailure("Nao consegui trocar a senha. Entre novamente e tente outra vez.")
            }
    }

    fun solicitarStatus(tipo: String, mensagem: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val uid = uidAtual(onFailure) ?: return
        val payload = mapOf(
            "tipo" to tipo,
            "mensagem" to mensagem,
            "status" to "solicitado",
            "criado_em" to Timestamp.now()
        )
        localSql.salvarEvento(uid, "solicitacao_conta", payload)
        tree.comCuidadorDocDaConta(
            uid = uid,
            onSuccess = { _, docRef ->
                docRef.collection("solicitacoes_conta")
                    .add(payload)
                    .addOnSuccessListener {
                        localSql.salvarRegistro(uid, "solicitacao_conta", it.id, payload, sincronizado = true)
                        onSuccess()
                    }
                    .addOnFailureListener { onFailure("Nao consegui registrar a solicitacao agora.") }
            },
            onFailure = onFailure
        )
    }

    private fun registrarSolicitacao(uid: String, tipo: String, mensagem: String) {
        val payload = mapOf(
            "tipo" to tipo,
            "mensagem" to mensagem,
            "status" to "concluido",
            "criado_em" to Timestamp.now()
        )
        localSql.salvarEvento(uid, "seguranca_conta", payload, sincronizado = true)
        tree.comCuidadorDocDaConta(
            uid = uid,
            onSuccess = { _, docRef -> docRef.collection("seguranca_conta").add(payload) },
            onFailure = {}
        )
    }

    private fun uidAtual(onFailure: (String) -> Unit): String? {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            onFailure("Entre novamente para salvar essa alteracao.")
            return null
        }
        return uid
    }
}
