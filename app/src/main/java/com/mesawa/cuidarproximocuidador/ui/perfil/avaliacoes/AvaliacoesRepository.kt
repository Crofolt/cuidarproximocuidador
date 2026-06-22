package com.mesawa.cuidarproximocuidador.ui.perfil.avaliacoes

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.mesawa.cuidarproximocuidador.data.firestore.CuidadorFirestoreTree
import com.mesawa.cuidarproximocuidador.data.local.LocalSqlStore

class AvaliacoesRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val localSql: LocalSqlStore = LocalSqlStore.instance,
    private val tree: CuidadorFirestoreTree = CuidadorFirestoreTree(firestore)
) {
    fun carregar(
        uid: String,
        cuidadorId: String,
        onSuccess: (List<AvaliacaoRecebida>) -> Unit,
        onError: () -> Unit
    ) {
        if (uid.isBlank()) {
            onSuccess(avaliacoesFake())
            return
        }

        tree.comCuidadorDocDaConta(
            uid = uid,
            idPreferido = cuidadorId,
            onSuccess = { idProfissional, cuidadorRef ->
                val ref = cuidadorRef.collection("avaliacoes")
                ref.orderBy("criadoEm", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    salvarAvaliacoesIniciais(uid, idProfissional, ref) {
                        onSuccess(avaliacoesFake())
                    }
                    return@addOnSuccessListener
                }

                val avaliacoes = snapshot.documents.mapNotNull { doc ->
                    val cliente = doc.getString("cliente").orEmpty()
                    val comentario = doc.getString("comentario").orEmpty()
                    val estrelas = doc.getDouble("estrelas") ?: 5.0
                    val data = doc.getString("data").orEmpty()
                    if (cliente.isBlank() || comentario.isBlank()) null else {
                        AvaliacaoRecebida(cliente, comentario, estrelas, data)
                    }
                }
                avaliacoes.forEachIndexed { index, avaliacao ->
                    salvarLocal(uid, cuidadorId, "server_$index", avaliacao, sincronizado = true)
                }
                onSuccess(avaliacoes.ifEmpty { avaliacoesFake() })
            }
                    .addOnFailureListener { onError() }
            },
            onFailure = { onError() }
        )
    }

    private fun salvarAvaliacoesIniciais(
        uid: String,
        cuidadorId: String,
        ref: com.google.firebase.firestore.CollectionReference,
        onComplete: () -> Unit
    ) {
        val avaliacoes = avaliacoesFake()
        var pendentes = avaliacoes.size + 1
        fun concluirUma() {
            pendentes -= 1
            if (pendentes == 0) onComplete()
        }

        avaliacoes.forEachIndexed { index, avaliacao ->
            salvarLocal(uid, cuidadorId, "fake_${index + 1}", avaliacao)
            ref.document("fake_${index + 1}")
                .set(
                    mapOf(
                        "cuidadorUid" to uid,
                        "cuidadorId" to cuidadorId,
                        "cliente" to avaliacao.cliente,
                        "comentario" to avaliacao.comentario,
                        "estrelas" to avaliacao.estrelas,
                        "data" to avaliacao.data,
                        "criadoEm" to Timestamp.now()
                    ),
                    SetOptions.merge()
                )
                .addOnCompleteListener {
                    salvarLocal(uid, cuidadorId, "fake_${index + 1}", avaliacao, sincronizado = it.isSuccessful)
                    concluirUma()
                }
        }

        tree.cuidadorDoc(cuidadorId)
            .set(
                mapOf(
                    "avaliacoesResumo" to mapOf(
                        "media" to 5.0,
                        "total" to avaliacoes.size,
                        "recomendacao" to 100
                    )
                ),
                SetOptions.merge()
            )
            .addOnCompleteListener { concluirUma() }
    }

    private fun avaliacoesFake() = listOf(
        AvaliacaoRecebida("Ana Paula", "Muito carinhosa e pontual.", 5.0, "Atendimento concluido"),
        AvaliacaoRecebida("Roberto Lima", "Profissional tranquila e organizada.", 5.0, "Atendimento concluido"),
        AvaliacaoRecebida("Mariana Costa", "Passou seguranca durante todo o cuidado.", 5.0, "Atendimento concluido")
    )

    private fun salvarLocal(
        uid: String,
        cuidadorId: String,
        chave: String,
        avaliacao: AvaliacaoRecebida,
        sincronizado: Boolean = false
    ) {
        localSql.salvarRegistro(
            uid = uid,
            tipo = "avaliacao",
            chave = chave,
            payload = mapOf(
                "cuidadorId" to cuidadorId,
                "cliente" to avaliacao.cliente,
                "comentario" to avaliacao.comentario,
                "estrelas" to avaliacao.estrelas,
                "data" to avaliacao.data
            ),
            sincronizado = sincronizado
        )
    }
}
