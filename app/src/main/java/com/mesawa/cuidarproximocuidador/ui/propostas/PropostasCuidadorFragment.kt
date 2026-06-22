package com.mesawa.cuidarproximocuidador.ui.propostas

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.mesawa.cuidarproximocuidador.R
import com.mesawa.cuidarproximocuidador.ui.perfil.ImagemUrlLoader

class PropostasCuidadorFragment : Fragment() {
    private lateinit var viewModel: PropostasViewModel
    private var propostaAtual = PropostaCuidado()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_cuidador_propostas, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[PropostasViewModel::class.java]
        view.findViewById<Button>(R.id.buttonVerProposta).setOnClickListener {
            startActivity(Intent(requireContext(), PropostaDetalheActivity::class.java))
        }
        view.findViewById<Button>(R.id.buttonAceitarPropostaLista).setOnClickListener {
            viewModel.aceitarProposta(
                onSuccess = {
                    propostaAtual = it
                    preencher(view, it)
                    Toast.makeText(requireContext(), "Proposta aceita. Atendimento em andamento.", Toast.LENGTH_LONG).show()
                },
                onFailure = { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
            )
        }
        view.findViewById<Button>(R.id.buttonRecusarPropostaLista).setOnClickListener {
            viewModel.recusarProposta(
                onSuccess = {
                    propostaAtual = it
                    preencher(view, it)
                    Toast.makeText(requireContext(), "Proposta recusada e registrada no historico.", Toast.LENGTH_LONG).show()
                },
                onFailure = { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
            )
        }
        view.findViewById<TextView>(R.id.chipAtivas).setOnClickListener { mostrarAtivas(view) }
        view.findViewById<TextView>(R.id.chipHistorico).setOnClickListener { mostrarHistorico(view) }
        viewModel.proposta.observe(viewLifecycleOwner) {
            propostaAtual = it
            preencher(view, it)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.carregar()
    }

    private fun preencher(view: View, proposta: PropostaCuidado) {
        view.findViewById<TextView>(R.id.textResumoPropostas).text = when (proposta.status) {
            PropostaStatus.NOVA -> "Voce recebeu uma proposta de um cliente que analisou seu curriculo."
            PropostaStatus.CANDIDATADA -> "Seu curriculo foi enviado. Aguarde o cliente mandar uma proposta."
            PropostaStatus.ACEITA -> "Proposta aceita. O prazo do atendimento esta em andamento."
            PropostaStatus.RECUSADA -> "Proposta recusada e registrada no historico."
            PropostaStatus.FINALIZADA -> "Cuidado finalizado e enviado ao historico."
        }
        view.findViewById<TextView>(R.id.textIdosoProposta).text = "${proposta.idosoNome}, ${proposta.idosoIdade} anos"
        view.findViewById<TextView>(R.id.textCondicaoProposta).text = proposta.condicao
        view.findViewById<TextView>(R.id.textValorProposta).text = "Precisa\n${proposta.tarefas.firstOrNull().orEmpty()}"
        view.findViewById<TextView>(R.id.textTempoProposta).text = proposta.tempoTexto
        view.findViewById<TextView>(R.id.textDistanciaProposta).text = "${proposta.cidade}\n${proposta.uf}"
        view.findViewById<TextView>(R.id.textTarefasProposta).text = proposta.tarefas.joinToString(", ")
        configurarStatus(view.findViewById(R.id.textStatusProposta), proposta.status)
        view.findViewById<Button>(R.id.buttonVerProposta).text = when (proposta.status) {
            PropostaStatus.NOVA -> "Ver detalhes da proposta"
            PropostaStatus.CANDIDATADA -> "Ver candidatura enviada"
            PropostaStatus.ACEITA -> "Conversar com cliente"
            PropostaStatus.RECUSADA, PropostaStatus.FINALIZADA -> "Ver historico"
        }
        configurarAcoesEProgresso(view, proposta)
        val foto = view.findViewById<ImageView>(R.id.imageIdosoProposta)
        if (proposta.fotoIdosoUrl.isNotBlank()) ImagemUrlLoader.carregar(foto, proposta.fotoIdosoUrl)
        if (proposta.status == PropostaStatus.FINALIZADA || proposta.status == PropostaStatus.RECUSADA) {
            mostrarHistorico(view)
        } else {
            mostrarAtivas(view)
        }
    }

    private fun mostrarAtivas(view: View) {
        view.findViewById<View>(R.id.cardPropostaAtual).visibility =
            if (propostaAtual.status == PropostaStatus.FINALIZADA || propostaAtual.status == PropostaStatus.RECUSADA) View.GONE else View.VISIBLE
        view.findViewById<View>(R.id.cardHistoricoVazio).visibility = View.GONE
        view.findViewById<TextView>(R.id.chipAtivas).setBackgroundResource(R.drawable.bg_chip_selected_green)
        view.findViewById<TextView>(R.id.chipHistorico).setBackgroundResource(R.drawable.bg_chip_soft)
    }

    private fun mostrarHistorico(view: View) {
        view.findViewById<View>(R.id.cardPropostaAtual).visibility = View.GONE
        view.findViewById<View>(R.id.cardHistoricoVazio).visibility = View.VISIBLE
        view.findViewById<TextView>(R.id.chipAtivas).setBackgroundResource(R.drawable.bg_chip_soft)
        view.findViewById<TextView>(R.id.chipHistorico).setBackgroundResource(R.drawable.bg_chip_selected_green)
        view.findViewById<TextView>(R.id.textHistorico).text =
            when (propostaAtual.status) {
                PropostaStatus.RECUSADA -> "${propostaAtual.idosoNome} • ${propostaAtual.tempoTexto} • ${propostaAtual.cidade}, ${propostaAtual.uf} • recusada"
                PropostaStatus.FINALIZADA -> "${propostaAtual.idosoNome} • ${propostaAtual.tempoTexto} • ${propostaAtual.cidade}, ${propostaAtual.uf} • finalizado"
                else -> "Quando uma proposta for recusada ou finalizada, ela aparece aqui com status, valor e observacoes."
            }
    }

    private fun configurarStatus(view: TextView, status: PropostaStatus) {
        when (status) {
            PropostaStatus.NOVA -> {
                view.text = "Proposta recebida"
                view.setTextColor(if (propostaAtual.compativel) 0xFF17643A.toInt() else 0xFF075FA8.toInt())
                view.setBackgroundResource(if (propostaAtual.compativel) R.drawable.bg_chip_selected_green else R.drawable.bg_chip_soft)
            }
            PropostaStatus.CANDIDATADA -> {
                view.text = "Proposta enviada"
                view.setTextColor(0xFF17643A.toInt())
                view.setBackgroundResource(R.drawable.bg_chip_selected_green)
            }
            PropostaStatus.ACEITA -> {
                view.text = "Aceita pelo cliente"
                view.setTextColor(0xFF17643A.toInt())
                view.setBackgroundResource(R.drawable.bg_chip_selected_green)
            }
            PropostaStatus.RECUSADA -> {
                view.text = "Recusada"
                view.setTextColor(0xFFBD1F3A.toInt())
                view.setBackgroundResource(R.drawable.bg_chip_selected_red)
            }
            PropostaStatus.FINALIZADA -> {
                view.text = "No historico"
                view.setTextColor(0xFF45606A.toInt())
                view.setBackgroundResource(R.drawable.bg_chip_soft)
            }
        }
    }

    private fun configurarAcoesEProgresso(view: View, proposta: PropostaCuidado) {
        val card = view.findViewById<View>(R.id.cardPropostaAtual)
        val acoes = view.findViewById<View>(R.id.containerAcoesProposta)
        val progresso = view.findViewById<View>(R.id.containerProgressoProposta)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressProposta)
        val textoProgresso = view.findViewById<TextView>(R.id.textProgressoProposta)

        card.setBackgroundResource(
            if (proposta.status == PropostaStatus.ACEITA) R.drawable.bg_chip_selected_green else R.drawable.bg_proposal_card
        )
        acoes.visibility = if (proposta.status == PropostaStatus.NOVA) View.VISIBLE else View.GONE
        progresso.visibility = if (proposta.status == PropostaStatus.ACEITA) View.VISIBLE else View.GONE

        if (proposta.status == PropostaStatus.ACEITA) {
            progressBar.progress = proposta.progressoAtendimento()
            textoProgresso.text = "Em andamento • ${proposta.tempoRestanteTexto()} • ${proposta.progressoAtendimento()}%"
        }
    }

}
