package com.mesawa.cuidarproximocuidador.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class CuidadorFirestoreTree(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun usuarioDoc(idUsuario: String) = firestore.collection("usuarios").document(idUsuario)

    fun usuarioIdCuidador(idProfissional: String) = "cuidador_$idProfissional"

    fun cuidadorDoc(idProfissional: String) = firestore.collection("cuidadores").document(idProfissional)

    fun cadastroDoc(idProfissional: String) = cuidadorDoc(idProfissional)

    fun cadastroLegadoDoc(uid: String) = firestore.collection("cuidadores_cadastros").document(uid)

    fun profissionalPublicoDoc(idProfissional: String) = firestore.collection("profissionais_publicos").document(idProfissional)

    fun comCuidadorDocDaConta(
        uid: String,
        idPreferido: String = "",
        onSuccess: (String, DocumentReference) -> Unit,
        onFailure: (String) -> Unit
    ) {
        encontrarIdProfissional(
            uid = uid,
            idPreferido = idPreferido,
            onSuccess = { idProfissional -> onSuccess(idProfissional, cuidadorDoc(idProfissional)) },
            onFailure = onFailure
        )
    }

    fun profissionaisLegadoDoc() = firestore.collection("cuidadores").document("profissionais")

    fun encontrarIdProfissional(
        uid: String,
        idPreferido: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val idLimpo = idPreferido.trim()
        if (idLimpo.isNotBlank()) {
            onSuccess(idLimpo)
            return
        }

        consultarUsuarioPorCampo(
            campo = "uid_auth",
            uid = uid,
            onSuccess = { idProfissional ->
                if (idProfissional.isNotBlank()) {
                    onSuccess(idProfissional)
                } else {
                    consultarUsuarioPorCampo(
                        campo = "uid",
                        uid = uid,
                        onSuccess = { idLegado ->
                            if (idLegado.isNotBlank()) {
                                onSuccess(idLegado)
                            } else {
                                consultarIndiceLegado(uid, onSuccess, onFailure)
                            }
                        },
                        onFailure = { consultarIndiceLegado(uid, onSuccess, onFailure) }
                    )
                }
            },
            onFailure = { consultarIndiceLegado(uid, onSuccess, onFailure) }
        )
    }

    fun salvarIdProfissional(uid: String, idProfissional: String) {
        if (uid.isBlank() || idProfissional.isBlank()) return
        val payload = mapOf(
            "uid" to uid,
            "uid_auth" to uid,
            "id_profissional" to idProfissional,
            "perfil_publico" to mapOf("id_profissional" to idProfissional),
            "atualizado_em" to Timestamp.now()
        )
        usuarioDoc(usuarioIdCuidador(idProfissional)).set(
            mapOf(
                "uid" to uid,
                "uid_auth" to uid,
                "tipo" to "cuidador",
                "cuidadorId" to idProfissional,
                "id_profissional" to idProfissional,
                "atualizadoEm" to Timestamp.now()
            ),
            SetOptions.merge()
        )
        cuidadorDoc(idProfissional).set(payload, SetOptions.merge())
        cadastroLegadoDoc(uid).set(payload, SetOptions.merge())
    }

    fun salvarPerfilPublico(uid: String, idProfissional: String, dados: Map<String, Any?>) {
        if (uid.isBlank()) return
        val publico = dados + mapOf(
            "uid" to uid,
            "uid_auth" to uid,
            "id_profissional" to idProfissional,
            "atualizado_em" to Timestamp.now()
        )
        profissionalPublicoDoc(idProfissional).set(publico, SetOptions.merge())
        cuidadorDoc(idProfissional).set(
            mapOf(
                "uid" to uid,
                "uid_auth" to uid,
                "id_profissional" to idProfissional,
                "perfil_publico" to publico,
                "atualizado_em" to Timestamp.now()
            ),
            SetOptions.merge()
        )
        cadastroLegadoDoc(uid).set(
            mapOf(
                "uid" to uid,
                "id_profissional" to idProfissional,
                "perfil_publico" to publico,
                "atualizado_em" to Timestamp.now()
            ),
            SetOptions.merge()
        )
    }

    fun removerFotoPublicaTopLevel() {
        profissionaisLegadoDoc().update(mapOf("fotoUrl" to FieldValue.delete()))
    }

    private fun encontrarIdNoIndice(uid: String, doc: DocumentSnapshot): String {
        val medicos = doc.get("medicos") as? Map<*, *> ?: return ""
        return medicos.entries.firstOrNull { (_, value) ->
            val map = value as? Map<*, *> ?: return@firstOrNull false
            texto(map["uid"]) == uid
        }?.key?.toString().orEmpty()
    }

    private fun consultarUsuarioPorCampo(
        campo: String,
        uid: String,
        onSuccess: (String) -> Unit,
        onFailure: () -> Unit
    ) {
        firestore.collection("usuarios")
            .whereEqualTo(campo, uid)
            .limit(1)
            .get()
            .addOnSuccessListener { query ->
                val usuario = query.documents.firstOrNull()
                val idProfissional = texto(usuario?.get("cuidadorId"))
                    .ifBlank { texto(usuario?.get("id_profissional")) }
                onSuccess(idProfissional)
            }
            .addOnFailureListener { onFailure() }
    }

    private fun consultarIndiceLegado(
        uid: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        profissionaisLegadoDoc().get()
            .addOnSuccessListener { doc ->
                val encontrado = encontrarIdNoIndice(uid, doc)
                if (encontrado.isBlank()) {
                    onFailure("Nao encontrei o perfil profissional ligado a esta conta.")
                } else {
                    onSuccess(encontrado)
                }
            }
            .addOnFailureListener { onFailure("Nao consegui consultar o indice de profissionais.") }
    }

    private fun texto(value: Any?): String = value?.toString()?.trim().orEmpty()
}
