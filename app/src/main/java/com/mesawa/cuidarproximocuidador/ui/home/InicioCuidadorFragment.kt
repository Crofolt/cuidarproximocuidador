package com.mesawa.cuidarproximocuidador.ui.home

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.mesawa.cuidarproximocuidador.R
import com.mesawa.cuidarproximocuidador.ui.perfil.ImagemUrlLoader
import com.mesawa.cuidarproximocuidador.ui.propostas.PropostaCuidado
import com.mesawa.cuidarproximocuidador.ui.propostas.PropostaDetalheActivity
import com.mesawa.cuidarproximocuidador.ui.propostas.PropostaStatus

class InicioCuidadorFragment : Fragment() {
    private lateinit var viewModel: InicioCuidadorViewModel
    private val bannerHandler = Handler(Looper.getMainLooper())
    private var bannerIndex = 0
    private var bannerCarousel: HorizontalScrollView? = null
    private var bannerTrack: LinearLayout? = null
    private var bannerDots: List<TextView> = emptyList()
    private val bannerRunnable = object : Runnable {
        override fun run() {
            mostrarBanner((bannerIndex + 1) % BANNER_TOTAL)
            bannerHandler.postDelayed(this, BANNER_DELAY_MS)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_cuidador_inicio, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[InicioCuidadorViewModel::class.java]
        val nome = FirebaseAuth.getInstance().currentUser?.displayName?.substringBefore(" ")?.ifBlank { "Kelly" } ?: "Kelly"
        view.findViewById<TextView>(R.id.textInicioNome).text = "Bem-vinda, $nome"
        view.findViewById<TextView>(R.id.textInicioSub).text = "Encontre propostas por cidade, horario e tipo de cuidado"
        view.findViewById<Button>(R.id.buttonDisponivel).setOnClickListener {
            Toast.makeText(requireContext(), "GPS ativado para encontrar propostas perto de voce.", Toast.LENGTH_LONG).show()
        }
        view.findViewById<Button>(R.id.buttonVerPropostaHome).setOnClickListener {
            startActivity(
                Intent(requireContext(), PropostaDetalheActivity::class.java)
                    .putExtra(PropostaDetalheActivity.EXTRA_MODO_CANDIDATURA, true)
            )
        }
        configurarAtiva(view)
        configurarBanners(view)
        viewModel.proposta.observe(viewLifecycleOwner) { preencherProposta(view, it) }
    }

    override fun onResume() {
        super.onResume()
        viewModel.carregar()
        bannerHandler.removeCallbacks(bannerRunnable)
        bannerHandler.postDelayed(bannerRunnable, BANNER_DELAY_MS)
    }

    override fun onPause() {
        bannerHandler.removeCallbacks(bannerRunnable)
        super.onPause()
    }

    override fun onDestroyView() {
        bannerHandler.removeCallbacks(bannerRunnable)
        bannerCarousel = null
        bannerTrack = null
        bannerDots = emptyList()
        super.onDestroyView()
    }

    private fun configurarAtiva(view: View) {
        val button = view.findViewById<Button>(R.id.buttonDisponivel)
        val status = view.findViewById<TextView>(R.id.textStatusDisponivel)
        button.text = "Me ache"
        status.text = "Maringá, PR"
        status.setTextColor(0xFF073B73.toInt())
        status.setBackgroundResource(R.drawable.bg_home_pin)
    }

    private fun configurarBanners(view: View) {
        bannerCarousel = view.findViewById(R.id.bannerCarousel)
        bannerTrack = view.findViewById(R.id.bannerTrack)
        bannerDots = listOf(
            view.findViewById(R.id.dotBanner1),
            view.findViewById(R.id.dotBanner2),
            view.findViewById(R.id.dotBanner3)
        )
        bannerDots.forEachIndexed { index, dot ->
            dot.setOnClickListener {
                bannerHandler.removeCallbacks(bannerRunnable)
                mostrarBanner(index)
                bannerHandler.postDelayed(bannerRunnable, BANNER_DELAY_MS)
            }
        }
        mostrarBanner(0)
    }

    private fun mostrarBanner(index: Int) {
        bannerIndex = index
        val target = bannerTrack?.getChildAt(index)?.left ?: 0
        bannerCarousel?.smoothScrollTo(target, 0)
        bannerDots.forEachIndexed { dotIndex, dot ->
            dot.setBackgroundResource(
                if (dotIndex == index) R.drawable.bg_banner_dot_active else R.drawable.bg_banner_dot_inactive
            )
            dot.layoutParams = dot.layoutParams.apply {
                width = if (dotIndex == index) 22.dp() else 8.dp()
            }
            dot.requestLayout()
        }
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun preencherProposta(view: View, proposta: PropostaCuidado) {
        view.findViewById<TextView>(R.id.textIdosoHome).text = "${proposta.idosoNome}, ${proposta.idosoIdade} anos"
        view.findViewById<TextView>(R.id.textResumoPropostaHome).text = "${proposta.condicao} • ${proposta.cidade}, ${proposta.uf}"
        view.findViewById<TextView>(R.id.textHomeValor).text = "Precisa\n${proposta.tarefas.take(1).joinToString()}"
        view.findViewById<TextView>(R.id.textHomeTempo).text = proposta.tempoTexto
        view.findViewById<TextView>(R.id.textHomeDistancia).text = "${proposta.cidade}\n${proposta.uf}"
        view.findViewById<TextView>(R.id.textMotivoPropostaHome).text =
            "Veja detalhes, compare distancia e envie seu curriculo do aplicativo."
        val tag = view.findViewById<TextView>(R.id.textTagPropostaHome)
        when (proposta.status) {
            PropostaStatus.NOVA -> {
                tag.text = if (proposta.compativel) "Nova proposta" else "Oportunidade"
                tag.setTextColor(if (proposta.compativel) 0xFF17643A.toInt() else 0xFF075FA8.toInt())
                tag.setBackgroundResource(if (proposta.compativel) R.drawable.bg_chip_selected_green else R.drawable.bg_chip_soft)
            }
            PropostaStatus.RECUSADA -> {
                tag.text = "Recusada"
                tag.setTextColor(0xFFBD1F3A.toInt())
                tag.setBackgroundResource(R.drawable.bg_chip_selected_red)
            }
            PropostaStatus.CANDIDATADA -> {
                tag.text = "Proposta enviada"
                tag.setTextColor(0xFF17643A.toInt())
                tag.setBackgroundResource(R.drawable.bg_chip_selected_green)
            }
            PropostaStatus.ACEITA -> {
                tag.text = "Aceita pelo cliente"
                tag.setTextColor(0xFF17643A.toInt())
                tag.setBackgroundResource(R.drawable.bg_chip_selected_green)
            }
            PropostaStatus.FINALIZADA -> {
                tag.text = "No historico"
                tag.setTextColor(0xFF45606A.toInt())
                tag.setBackgroundResource(R.drawable.bg_chip_soft)
            }
        }
        if (proposta.fotoIdosoUrl.isNotBlank()) {
            ImagemUrlLoader.carregar(view.findViewById<ImageView>(R.id.imageIdosoHome), proposta.fotoIdosoUrl)
        }
    }

    companion object {
        private const val BANNER_TOTAL = 3
        private const val BANNER_DELAY_MS = 12_000L
    }
}
