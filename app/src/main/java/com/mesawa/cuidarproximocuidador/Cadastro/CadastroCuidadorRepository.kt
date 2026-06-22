package com.mesawa.cuidarproximocuidador.Cadastro

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mesawa.cuidarproximocuidador.data.firestore.CuidadorFirestoreTree
import com.google.firebase.storage.FirebaseStorage
import com.mesawa.cuidarproximocuidador.data.local.LocalSqlStore

class CadastroCuidadorRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val viewModel: CadastroCuidadorViewModel = CadastroCuidadorViewModel(),
    private val localSql: LocalSqlStore = LocalSqlStore.instance,
    private val tree: CuidadorFirestoreTree = CuidadorFirestoreTree(firestore)
) {

    fun enviarFotoPerfil(
        uid: String,
        fotoUri: String?,
        onSuccess: (String?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (fotoUri.isNullOrBlank()) {
            onSuccess(null)
            return
        }

        val uri = Uri.parse(fotoUri)
        val ref = storage.reference.child("cuidadores/$uid/perfil/foto_perfil.jpg")
        ref.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                ref.downloadUrl
            }
            .addOnSuccessListener { downloadUri -> onSuccess(downloadUri.toString()) }
            .addOnFailureListener { onFailure("Não consegui enviar a foto. Tente escolher outra imagem.") }
    }

    fun salvar(
        uid: String,
        dados: CadastroCuidadorDados,
        fotoUrl: String?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val idProfissional = gerarIdProfissional(dados.nomeCompleto, dados.cpf)
        val payload = viewModel.toFirestore(uid, dados, fotoUrl) + mapOf(
            "id_profissional" to idProfissional,
            "uid_auth" to uid
        )
        localSql.salvarRegistro(uid, "cadastro_cuidador", "dados_principais", payload)

        tree.usuarioDoc(tree.usuarioIdCuidador(idProfissional)).set(
            mapOf(
                "uid" to uid,
                "uid_auth" to uid,
                "tipo" to "cuidador",
                "email" to dados.email,
                "cuidadorId" to idProfissional,
                "id_profissional" to idProfissional
            ),
            SetOptions.merge()
        )

        tree.cuidadorDoc(idProfissional)
            .set(payload, SetOptions.merge())
            .addOnSuccessListener {
                tree.cadastroLegadoDoc(uid).set(payload, SetOptions.merge())
                localSql.salvarRegistro(uid, "cadastro_cuidador", "dados_principais", payload, sincronizado = true)
                onSuccess()
            }
            .addOnFailureListener { onFailure("Não consegui salvar o cadastro agora.") }
    }

    private fun gerarIdProfissional(nome: String, cpf: String): String {
        val base = nome.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .trim()
            .replace(Regex("\\s+"), "_")
            .ifBlank { "cuidador" }
        val sufixo = cpf.filter { it.isDigit() }.takeLast(4)
        return if (sufixo.isBlank()) base else "${base}_$sufixo"
    }
}
