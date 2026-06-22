package com.mesawa.cuidarproximocuidador.Login

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.mesawa.cuidarproximocuidador.data.firestore.CuidadorFirestoreTree
import java.util.Locale

class CuidadorRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val tree: CuidadorFirestoreTree = CuidadorFirestoreTree(firestore)
) {

    fun validarCuidador(
        usuario: FirebaseUser,
        onSuccess: (CuidadorPerfil) -> Unit,
        onBlocked: (String) -> Unit
    ) {
        val uid = usuario.uid
        val email = usuario.email.orEmpty().lowercase(Locale.ROOT)

        firestore.collection("profissionais_publicos")
            .document(uid)
            .get()
            .addOnSuccessListener { docPublico ->
                if (docPublico.exists()) {
                    val ativo = estaAtivo(docPublico.get("ativo"))
                    val perfil = CuidadorPerfil(
                        id = texto(docPublico.get("id_profissional")).ifBlank { uid },
                        nome = texto(docPublico.get("nome")).ifBlank { usuario.displayName ?: "Cuidador" },
                        especialidade = texto(docPublico.get("especialidade")).ifBlank { "Cuidador profissional" },
                        ativo = ativo,
                        fotoUrl = texto(docPublico.get("fotoUrl")).ifBlank { texto(docPublico.get("foto_url")) }
                    )
                    if (!ativo) {
                        onBlocked("Seu cadastro foi encontrado, mas ainda esta inativo.")
                    } else {
                        tree.salvarIdProfissional(uid, perfil.id)
                        onSuccess(perfil)
                    }
                    return@addOnSuccessListener
                }
                validarLegado(usuario, uid, email, onSuccess, onBlocked)
            }
            .addOnFailureListener {
                validarLegado(usuario, uid, email, onSuccess, onBlocked)
            }
    }

    private fun validarLegado(
        usuario: FirebaseUser,
        uid: String,
        email: String,
        onSuccess: (CuidadorPerfil) -> Unit,
        onBlocked: (String) -> Unit
    ) {
        firestore.collection("cuidadores")
            .document("profissionais")
            .get()
            .addOnSuccessListener { doc ->
                val medicosMap = doc.get("medicos") as? Map<*, *>
                val perfil = medicosMap
                    ?.mapNotNull { (id, value) ->
                        val dados = value as? Map<*, *> ?: return@mapNotNull null
                        val uidCadastro = texto(dados["uid"])
                        val emailCadastro = (texto(dados["email"]).ifBlank { texto(dados["emailCuidador"]) })
                            .lowercase(Locale.ROOT)

                        if (uidCadastro == uid || (email.isNotBlank() && emailCadastro == email)) {
                            CuidadorPerfil(
                                id = id.toString(),
                                nome = texto(dados["nome"]).ifBlank { usuario.displayName ?: "Cuidador" },
                                especialidade = texto(dados["especialidade"]).ifBlank { "Cuidador profissional" },
                                ativo = estaAtivo(dados["ativo"]),
                                fotoUrl = texto(dados["fotoUrl"]).ifBlank { texto(dados["foto_url"]) }
                            )
                        } else {
                            null
                        }
                    }
                    ?.firstOrNull()

                when {
                    perfil == null -> onBlocked("Este login ainda nao esta vinculado a um cuidador cadastrado.")
                    !perfil.ativo -> onBlocked("Seu cadastro foi encontrado, mas ainda esta inativo.")
                    else -> {
                        tree.salvarIdProfissional(uid, perfil.id)
                        onSuccess(perfil)
                    }
                }
            }
            .addOnFailureListener {
                onBlocked("Nao consegui consultar a base de cuidadores agora.")
            }
    }

    private fun texto(value: Any?): String = value?.toString()?.trim().orEmpty()

    private fun estaAtivo(value: Any?): Boolean {
        return when (value) {
            is Boolean -> value
            is String -> value.equals("true", true) || value.equals("ativo", true) || value.equals("sim", true)
            is Number -> value.toInt() == 1
            else -> false
        }
    }
}
