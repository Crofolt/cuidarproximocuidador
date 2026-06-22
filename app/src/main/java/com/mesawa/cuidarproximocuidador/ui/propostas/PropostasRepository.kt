package com.mesawa.cuidarproximocuidador.ui.propostas

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mesawa.cuidarproximocuidador.data.firestore.CuidadorFirestoreTree
import com.mesawa.cuidarproximocuidador.data.local.LocalSqlStore

class PropostasRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val localSql: LocalSqlStore = LocalSqlStore.instance,
    private val tree: CuidadorFirestoreTree = CuidadorFirestoreTree(firestore)
) {
    fun carregarPropostaDemo(onSuccess: (PropostaCuidado) -> Unit) {
        val uid = auth.currentUser?.uid.orEmpty()
        tree.encontrarIdProfissional(
            uid = uid,
            idPreferido = "",
            onSuccess = { idProfissional ->
                firestore.collection("contratacoes")
                    .whereEqualTo("cuidadorUid", uid)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { query ->
                        val doc = query.documents.firstOrNull()
                        if (doc == null) {
                            onSuccess(propostaFallback())
                        } else {
                            onSuccess(
                                propostaFallback().copy(
                                    id = doc.id,
                                    idosoNome = texto(doc.get("idosoNome")).ifBlank { texto(doc.get("idoso.nome")) }.ifBlank { "Irene" },
                                    endereco = texto(doc.get("enderecoAtendimento")).ifBlank { "Rua das Flores, Maringa" },
                                    cidade = texto(doc.get("cidade")).ifBlank { "Maringa" },
                                    uf = texto(doc.get("uf")).ifBlank { "PR" },
                                    fotoIdosoUrl = texto(doc.get("fotoIdosoUrl")).ifBlank { texto(doc.get("idosoFotoUrl")) },
                                    status = PropostaStatus.from(texto(doc.get("status"))),
                                )
                            )
                        }
                    }
                    .addOnFailureListener { onSuccess(propostaFallback()) }
                tree.salvarIdProfissional(uid, idProfissional)
            },
            onFailure = { onSuccess(propostaFallback()) }
        )
    }

    fun enviarCandidatura(proposta: PropostaCuidado, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val uid = auth.currentUser?.uid.orEmpty()
        tree.encontrarIdProfissional(
            uid = uid,
            idPreferido = "",
            onSuccess = { idProfissional ->
                val payload = mapOf(
                    "status" to PropostaStatus.CANDIDATADA.firestore,
                    "uid_auth" to uid,
                    "cuidadorId" to idProfissional,
                    "id_profissional" to idProfissional,
                    "propostaId" to proposta.id,
                    "idosoNome" to proposta.idosoNome,
                    "cidade" to proposta.cidade,
                    "uf" to proposta.uf,
                    "valorOferecido" to proposta.valorOferecido,
                    "mensagem" to "Tenho interesse no cuidado e autorizo o envio do meu perfil profissional para a familia.",
                    "criado_em" to Timestamp.now(),
                    "atualizado_em" to Timestamp.now()
                )

                localSql.salvarRegistro(uid, "candidaturas", proposta.id, payload)
                val batch = firestore.batch()
                val candidaturaPublica = firestore.collection("contratacoes")
                    .document(proposta.id)
                    .collection("candidaturas")
                    .document(idProfissional)
                val candidaturaPrivada = tree.cuidadorDoc(idProfissional)
                    .collection("candidaturas")
                    .document(proposta.id)
                batch.set(candidaturaPublica, payload, SetOptions.merge())
                batch.set(candidaturaPrivada, payload, SetOptions.merge())
                batch.commit()
                    .addOnSuccessListener {
                        localSql.salvarRegistro(uid, "candidaturas", proposta.id, payload, sincronizado = true)
                        onSuccess()
                    }
                    .addOnFailureListener { onFailure("Nao consegui enviar seu perfil agora.") }
            },
            onFailure = onFailure
        )
    }

    fun atualizarStatus(proposta: PropostaCuidado, status: PropostaStatus, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val uid = auth.currentUser?.uid.orEmpty()
        tree.encontrarIdProfissional(
            uid = uid,
            idPreferido = "",
            onSuccess = { idProfissional ->
                val payload = mapOf(
                    "status" to status.firestore,
                    "uid_auth" to uid,
                    "cuidadorId" to idProfissional,
                    "id_profissional" to idProfissional,
                    "propostaId" to proposta.id,
                    "idosoNome" to proposta.idosoNome,
                    "cidade" to proposta.cidade,
                    "uf" to proposta.uf,
                    "atualizado_em" to Timestamp.now()
                )

                localSql.salvarRegistro(uid, "candidaturas", proposta.id, payload)
                val batch = firestore.batch()
                batch.set(
                    firestore.collection("contratacoes").document(proposta.id),
                    mapOf("status" to status.firestore, "atualizado_em" to Timestamp.now()),
                    SetOptions.merge()
                )
                batch.set(
                    tree.cuidadorDoc(idProfissional).collection("candidaturas").document(proposta.id),
                    payload,
                    SetOptions.merge()
                )
                batch.commit()
                    .addOnSuccessListener {
                        localSql.salvarRegistro(uid, "candidaturas", proposta.id, payload, sincronizado = true)
                        onSuccess()
                    }
                    .addOnFailureListener { onFailure("Nao consegui atualizar a proposta agora.") }
            },
            onFailure = onFailure
        )
    }

    private fun propostaFallback() = PropostaCuidado()

    private fun texto(value: Any?): String = value?.toString()?.trim().orEmpty()
}
