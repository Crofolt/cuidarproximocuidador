package com.mesawa.cuidarproximocuidador.ui.perfil.suporte

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.mesawa.cuidarproximocuidador.data.firestore.CuidadorFirestoreTree
import com.mesawa.cuidarproximocuidador.data.local.LocalSqlStore

class SuporteSacRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val localSql: LocalSqlStore = LocalSqlStore.instance,
    private val tree: CuidadorFirestoreTree = CuidadorFirestoreTree(firestore)
) {
    fun salvarMensagem(
        uid: String,
        cuidadorId: String,
        pergunta: String,
        resposta: String
    ) {
        if (uid.isBlank() || pergunta.isBlank()) return
        val payload = mapOf(
            "cuidadorUid" to uid,
            "cuidadorId" to cuidadorId,
            "origem" to "chat_ia",
            "pergunta" to pergunta,
            "resposta" to resposta,
            "status" to "respondido_ia"
        )
        localSql.salvarEvento(uid, "sac_mensagem", payload)

        tree.comCuidadorDocDaConta(
            uid = uid,
            idPreferido = cuidadorId,
            onSuccess = { _, cuidadorRef ->
                cuidadorRef.collection("sac_mensagens")
                    .add(payload + ("criadoEm" to Timestamp.now()))
                    .addOnSuccessListener {
                        localSql.salvarRegistro(uid, "sac_mensagem", it.id, payload, sincronizado = true)
                    }
            },
            onFailure = {}
        )
    }

    fun salvarAcaoSac(uid: String, cuidadorId: String, tipo: String, destino: String) {
        if (uid.isBlank()) return
        val payload = mapOf(
            "cuidadorUid" to uid,
            "cuidadorId" to cuidadorId,
            "origem" to tipo,
            "destino" to destino,
            "status" to "acionado"
        )
        localSql.salvarEvento(uid, "sac_mensagem", payload)

        tree.comCuidadorDocDaConta(
            uid = uid,
            idPreferido = cuidadorId,
            onSuccess = { _, cuidadorRef ->
                cuidadorRef.collection("sac_mensagens")
                    .add(payload + ("criadoEm" to Timestamp.now()))
                    .addOnSuccessListener {
                        localSql.salvarRegistro(uid, "sac_mensagem", it.id, payload, sincronizado = true)
                    }
            },
            onFailure = {}
        )
    }
}
