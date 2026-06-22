package com.mesawa.cuidarproximocuidador.ui.propostas

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.mesawa.cuidarproximocuidador.R
import com.mesawa.cuidarproximocuidador.ui.perfil.ImagemUrlLoader
import java.text.NumberFormat
import java.util.Locale

class PropostaDetalheActivity : FragmentActivity() {
    private lateinit var viewModel: PropostasViewModel
    private var proposta = PropostaCuidado()
    private var modoCandidatura = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
        setContentView(R.layout.activity_proposta_detalhe)
        modoCandidatura = intent.getBooleanExtra(EXTRA_MODO_CANDIDATURA, false)

        viewModel = ViewModelProvider(this)[PropostasViewModel::class.java]
        findViewById<TextView>(R.id.buttonVoltarProposta).setOnClickListener { finish() }
        findViewById<Button>(R.id.buttonEnviarPerfilProposta).setOnClickListener {
            if (modoCandidatura) confirmarEnvioPerfil() else confirmarAceite()
        }
        findViewById<Button>(R.id.buttonVoltarSemEnviar).setOnClickListener {
            if (modoCandidatura) finish() else confirmarRecusa()
        }
        findViewById<Button>(R.id.buttonVerPropostas).setOnClickListener { finish() }
        findViewById<Button>(R.id.buttonChatProposta).setOnClickListener {
            if (proposta.status == PropostaStatus.ACEITA) {
                Toast.makeText(this, "Conversa pelo app aberta para esta proposta.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Mensagem da candidatura salva para a familia.", Toast.LENGTH_LONG).show()
            }
        }

        viewModel.proposta.observe(this) {
            proposta = it
            preencher(it)
        }
        viewModel.carregar()
    }

    private fun preencher(proposta: PropostaCuidado) {
        findViewById<TextView>(R.id.textIdosoDetalhe).text = "${proposta.idosoNome}, ${proposta.idosoIdade} anos"
        findViewById<TextView>(R.id.textCondicaoDetalhe).text = proposta.condicao
        findViewById<TextView>(R.id.textTempoDetalhe).text = "${proposta.tempoTexto}\nTempo"
        findViewById<TextView>(R.id.textValorDetalhe).text = "${moeda(proposta.valorOferecido)}\nOferecido"
        findViewById<TextView>(R.id.textDistanciaDetalhe).text = "${proposta.cidade}\n${proposta.uf}"
        findViewById<TextView>(R.id.textEnderecoDetalhe).text = "Local: ${proposta.cidade}, ${proposta.uf}\nEndereco completo liberado apos escolha da familia."
        findViewById<TextView>(R.id.textTarefasDetalhe).text = "O que precisa ser feito\n${proposta.tarefas.joinToString("\n") { "• $it" }}"
        findViewById<TextView>(R.id.textObservacaoDetalhe).text =
            "Observacoes da familia\n${proposta.observacoes}\n\nQualificacoes desejadas\n${proposta.qualificacoesDesejadas.joinToString(", ")}"
        if (proposta.fotoIdosoUrl.isNotBlank()) {
            ImagemUrlLoader.carregar(findViewById<ImageView>(R.id.imageIdosoDetalhe), proposta.fotoIdosoUrl)
        }
        configurarStatus(proposta.status)
    }

    private fun confirmarEnvioPerfil() {
        AlertDialog.Builder(this)
            .setTitle("Enviar currículo?")
            .setMessage("Vamos enviar seu currículo do aplicativo para o cliente analisar esta oportunidade.")
            .setNegativeButton("Revisar", null)
            .setPositiveButton("Enviar currículo") { _, _ ->
                viewModel.enviarCandidatura(
                    onSuccess = {
                        proposta = it
                        configurarStatus(PropostaStatus.CANDIDATADA)
                        Toast.makeText(this, "Perfil enviado para a família.", Toast.LENGTH_LONG).show()
                    },
                    onFailure = { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
                )
            }
            .show()
    }

    private fun confirmarAceite() {
        AlertDialog.Builder(this)
            .setTitle("Aceitar proposta?")
            .setMessage("O cliente ja analisou seu curriculo. Ao aceitar, esta proposta entra em andamento e o prazo do atendimento comeca a contar.")
            .setNegativeButton("Revisar", null)
            .setPositiveButton("Aceitar") { _, _ ->
                viewModel.aceitarProposta(
                    onSuccess = {
                        proposta = it
                        configurarStatus(PropostaStatus.ACEITA)
                        Toast.makeText(this, "Proposta aceita. Atendimento em andamento.", Toast.LENGTH_LONG).show()
                    },
                    onFailure = { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
                )
            }
            .show()
    }

    private fun confirmarRecusa() {
        AlertDialog.Builder(this)
            .setTitle("Recusar proposta?")
            .setMessage("Essa oportunidade sai das propostas ativas e fica registrada no historico.")
            .setNegativeButton("Voltar", null)
            .setPositiveButton("Recusar") { _, _ ->
                viewModel.recusarProposta(
                    onSuccess = {
                        proposta = it
                        configurarStatus(PropostaStatus.RECUSADA)
                        Toast.makeText(this, "Proposta recusada e registrada no historico.", Toast.LENGTH_LONG).show()
                    },
                    onFailure = { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
                )
            }
            .show()
    }

    private fun abrirWhatsApp() {
        val telefone = proposta.responsavelTelefone.filter { it.isDigit() }
        if (telefone.isBlank()) {
            Toast.makeText(this, "Telefone do cliente ainda nao foi liberado.", Toast.LENGTH_LONG).show()
            return
        }
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$telefone")))
    }

    private fun configurarStatus(status: PropostaStatus) {
        val statusView = findViewById<TextView>(R.id.textStatusDetalhe)
        val enviada = findViewById<android.view.View>(R.id.containerCandidaturaEnviada)
        val enviar = findViewById<Button>(R.id.buttonEnviarPerfilProposta)
        val voltar = findViewById<Button>(R.id.buttonVoltarSemEnviar)
        val tituloEnviada = findViewById<TextView>(R.id.textTituloCandidaturaEnviada)
        val mensagemEnviada = findViewById<TextView>(R.id.textMensagemCandidaturaEnviada)
        val botaoPrincipal = findViewById<Button>(R.id.buttonVerPropostas)
        val botaoChat = findViewById<Button>(R.id.buttonChatProposta)
        botaoPrincipal.setOnClickListener { finish() }
        botaoChat.setOnClickListener {
            Toast.makeText(this, "Mensagem da candidatura salva para a familia.", Toast.LENGTH_LONG).show()
        }
        when (status) {
            PropostaStatus.NOVA -> {
                statusView.text = if (modoCandidatura) "Oportunidade" else "Proposta recebida"
                statusView.setTextColor(if (proposta.compativel) 0xFF17643A.toInt() else 0xFF075FA8.toInt())
                statusView.setBackgroundResource(if (proposta.compativel) R.drawable.bg_chip_selected_green else R.drawable.bg_chip_soft)
                enviada.visibility = android.view.View.GONE
                enviar.text = if (modoCandidatura) "Enviar meu currículo do aplicativo" else "Aceitar proposta"
                voltar.text = if (modoCandidatura) "Sair" else "Recusar proposta"
                enviar.visibility = android.view.View.VISIBLE
                voltar.visibility = android.view.View.VISIBLE
            }
            PropostaStatus.RECUSADA -> {
                statusView.text = "Recusada"
                statusView.setTextColor(0xFFBD1F3A.toInt())
                statusView.setBackgroundResource(R.drawable.bg_chip_selected_red)
                enviada.visibility = android.view.View.VISIBLE
                tituloEnviada.text = "Proposta recusada"
                mensagemEnviada.text = "Nada mais precisa ser feito. Esta proposta ficou registrada no historico."
                botaoPrincipal.text = "Ver historico"
                botaoChat.visibility = android.view.View.GONE
                enviar.visibility = android.view.View.GONE
                voltar.visibility = android.view.View.GONE
            }
            PropostaStatus.CANDIDATADA -> {
                statusView.text = "Proposta enviada"
                statusView.setTextColor(0xFF17643A.toInt())
                statusView.setBackgroundResource(R.drawable.bg_chip_selected_green)
                enviada.visibility = android.view.View.VISIBLE
                tituloEnviada.text = "Proposta enviada"
                mensagemEnviada.text = "Seu curriculo do app foi enviado para o cliente analisar. Aqui aparece aceito ou recusado quando ele responder."
                botaoPrincipal.text = "Ver propostas"
                botaoChat.text = "Mensagem"
                botaoChat.visibility = android.view.View.VISIBLE
                enviar.visibility = android.view.View.GONE
                voltar.visibility = android.view.View.GONE
            }
            PropostaStatus.ACEITA -> {
                statusView.text = "Aceita pelo cliente"
                statusView.setTextColor(0xFF17643A.toInt())
                statusView.setBackgroundResource(R.drawable.bg_chip_selected_green)
                enviada.visibility = android.view.View.VISIBLE
                tituloEnviada.text = "Cliente aceitou"
                mensagemEnviada.text = "Atendimento em andamento. O prazo de ${proposta.duracaoDias} dias esta contando ate finalizar."
                botaoPrincipal.text = "WhatsApp"
                botaoPrincipal.setOnClickListener { abrirWhatsApp() }
                botaoChat.text = "Chat do app"
                botaoChat.visibility = android.view.View.VISIBLE
                botaoChat.setOnClickListener {
                    Toast.makeText(this, "Conversa pelo app aberta para esta proposta.", Toast.LENGTH_LONG).show()
                }
                enviar.visibility = android.view.View.GONE
                voltar.visibility = android.view.View.GONE
            }
            PropostaStatus.FINALIZADA -> {
                statusView.text = "Finalizada"
                statusView.setTextColor(0xFF45606A.toInt())
                statusView.setBackgroundResource(R.drawable.bg_chip_soft)
                enviada.visibility = android.view.View.VISIBLE
                tituloEnviada.text = "No historico"
                mensagemEnviada.text = "Atendimento encerrado e guardado no historico da proposta."
                botaoPrincipal.text = "Ver historico"
                botaoChat.visibility = android.view.View.GONE
                enviar.visibility = android.view.View.GONE
                voltar.visibility = android.view.View.GONE
            }
        }
    }

    private fun moeda(valor: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)
    }

    companion object {
        const val EXTRA_MODO_CANDIDATURA = "modo_candidatura"
    }
}
