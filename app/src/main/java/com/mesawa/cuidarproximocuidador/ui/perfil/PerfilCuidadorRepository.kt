package com.mesawa.cuidarproximocuidador.ui.perfil

import android.net.Uri
import com.google.firebase.firestore.Source
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.mesawa.cuidarproximocuidador.data.firestore.CuidadorFirestoreTree
import com.mesawa.cuidarproximocuidador.data.local.LocalSqlStore
import java.util.Locale

class PerfilCuidadorRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val localSql: LocalSqlStore = LocalSqlStore.instance,
    private val tree: CuidadorFirestoreTree = CuidadorFirestoreTree(firestore)
) {

    val uidAtual: String
        get() = auth.currentUser?.uid.orEmpty()

    fun sair() {
        auth.signOut()
    }

    fun carregarPerfil(
        cuidadorId: String,
        fallbackNome: String,
        fallbackEspecialidade: String,
        fallbackFotoUrl: String,
        onSuccess: (PerfilCuidadorDados) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid.orEmpty()
        tree.encontrarIdProfissional(
            uid = uid,
            idPreferido = cuidadorId,
            onSuccess = { idPrivado ->
                tree.cadastroDoc(idPrivado)
                    .get(Source.SERVER)
                    .addOnSuccessListener { cadastro ->
                        tree.profissionalPublicoDoc(idPrivado)
                    .get(Source.SERVER)
                    .addOnSuccessListener { doc ->
                        val publico = doc.data
                        val dadosPessoais = cadastro.get("dados_pessoais") as? Map<*, *>
                        val atendimento = cadastro.get("atendimento") as? Map<*, *>
                        val perfilPublico = cadastro.get("perfil_publico") as? Map<*, *>
                        val fotoPrivada = texto(dadosPessoais?.get("foto_url"))
                            .ifBlank { texto(perfilPublico?.get("fotoUrl")) }
                            .ifBlank { texto(cadastro.get("fotoUrl")) }

                        onSuccess(
                            PerfilCuidadorDados(
                                id = texto(publico?.get("id_profissional")).ifBlank { idPrivado },
                                uid = uid,
                                nome = texto(dadosPessoais?.get("nome_publico"))
                                    .ifBlank { texto(dadosPessoais?.get("nome")) }
                                    .ifBlank { texto(publico?.get("nome")) }
                                    .ifBlank { fallbackNome.ifBlank { "Cuidadora" } },
                                especialidade = texto(publico?.get("especialidade")).ifBlank { fallbackEspecialidade.ifBlank { "Cuidadora profissional" } },
                                cidade = texto(atendimento?.get("cidade"))
                                    .ifBlank { texto(dadosPessoais?.get("cidade")) }
                                    .ifBlank { texto(publico?.get("cidade")) }
                                    .ifBlank { texto(publico?.get("localizacao")) },
                                avaliacao = numero(publico?.get("avaliacao")),
                                atendimentos = inteiro(publico?.get("atendimentos")),
                                faturamentoMes = numero(publico?.get("faturamentoMes")).ifZero { calcularFaturamentoEstimado(publico) },
                                fotoUrl = fotoPrivada.ifBlank { texto(publico?.get("fotoUrl")).ifBlank { fallbackFotoUrl } },
                                ativo = ativo(publico?.get("ativo")),
                                reconhecimentoFacial = ativo(publico?.get("reconhecimentoFacial"))
                            )
                        )
                    }
                    .addOnFailureListener { onFailure("Não consegui carregar os dados do perfil agora.") }
                    }
                    .addOnFailureListener { onFailure("Não consegui carregar os dados do perfil agora.") }
            },
            onFailure = onFailure
        )
    }

    fun salvarFotoPerfil(
        cuidadorId: String,
        fotoUri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            onFailure("Entre novamente para alterar a foto.")
            return
        }

        val ref = storage.reference.child("cuidadores/$uid/perfil/foto_perfil.jpg")
        ref.putFile(fotoUri)
            .continueWithTask { task ->
                if (!task.isSuccessful) task.exception?.let { throw it }
                ref.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                val fotoUrl = downloadUri.toString()
                salvarFotoUrl(uid, cuidadorId, fotoUrl, onSuccess, onFailure)
            }
            .addOnFailureListener { error -> onFailure(mensagemStorage(error)) }
    }

    private fun salvarFotoUrl(
        uid: String,
        cuidadorId: String,
        fotoUrl: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        tree.encontrarIdProfissional(
            uid = uid,
            idPreferido = cuidadorId,
            onSuccess = { idProfissional ->
                val payload = mapOf(
                    "uid" to uid,
                    "id_profissional" to idProfissional,
                    "fotoUrl" to fotoUrl
                )
                localSql.salvarRegistro(uid, "perfil", "foto", payload)

                tree.cadastroDoc(idProfissional)
                    .set(
                        mapOf(
                            "uid" to uid,
                            "uid_auth" to uid,
                            "id_profissional" to idProfissional,
                            "dados_pessoais" to mapOf("foto_url" to fotoUrl),
                            "perfil_publico" to mapOf(
                                "id_profissional" to idProfissional,
                                "fotoUrl" to fotoUrl
                            ),
                            "fotoUrl" to FieldValue.delete(),
                            "atualizado_em" to com.google.firebase.Timestamp.now()
                        ),
                        SetOptions.merge()
                    )

                tree.salvarPerfilPublico(
                    uid = uid,
                    idProfissional = idProfissional,
                    dados = mapOf("fotoUrl" to fotoUrl)
                )

                tree.profissionaisLegadoDoc()
                    .update(mapOf("medicos.$idProfissional.fotoUrl" to fotoUrl))
                    .addOnSuccessListener {
                        tree.removerFotoPublicaTopLevel()
                        localSql.salvarRegistro(uid, "perfil", "foto", payload, sincronizado = true)
                        onSuccess(fotoUrl)
                    }
                    .addOnFailureListener { onFailure("Foto enviada, mas não consegui atualizar o perfil profissional.") }
            },
            onFailure = onFailure
        )
    }

    private fun mensagemStorage(error: Exception): String {
        val storageError = error as? StorageException
        val motivo = when (storageError?.errorCode) {
            StorageException.ERROR_NOT_AUTHENTICATED -> "entre novamente na conta"
            StorageException.ERROR_NOT_AUTHORIZED -> "as regras do Storage bloquearam o envio"
            StorageException.ERROR_BUCKET_NOT_FOUND -> "o Firebase Storage ainda não está habilitado"
            StorageException.ERROR_QUOTA_EXCEEDED -> "a cota do Storage foi excedida"
            else -> error.localizedMessage ?: "erro desconhecido"
        }
        return "Não consegui enviar a foto: $motivo."
    }

    private fun calcularFaturamentoEstimado(dados: Map<*, *>?): Double {
        val atendimentos = inteiro(dados?.get("atendimentos"))
        val valorHora = numero(dados?.get("valorHora")).ifZero { numero(dados?.get("valor_hora")) }
        return atendimentos * valorHora
    }

    private fun texto(value: Any?): String = value?.toString()?.trim().orEmpty()

    private fun numero(value: Any?): Double {
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.replace("R$", "", true)
                .replace(".", "")
                .replace(",", ".")
                .trim()
                .toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    private fun inteiro(value: Any?): Int {
        return when (value) {
            is Number -> value.toInt()
            is String -> value.filter { it.isDigit() }.toIntOrNull() ?: 0
            else -> 0
        }
    }

    private fun ativo(value: Any?): Boolean {
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() == 1
            is String -> value.lowercase(Locale.ROOT) in listOf("true", "sim", "ativo", "aprovado", "aprovada")
            else -> false
        }
    }

    private fun Double.ifZero(block: () -> Double): Double = if (this == 0.0) block() else this
}
