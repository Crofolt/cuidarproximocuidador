package com.mesawa.cuidarproximocuidador.ui.perfil.avaliacoes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.mesawa.cuidarproximocuidador.R
import com.mesawa.cuidarproximocuidador.ui.HomeCuidadorActivity
import com.mesawa.cuidarproximocuidador.ui.perfil.ImagemUrlLoader
import com.mesawa.cuidarproximocuidador.ui.perfil.PerfilLocalCache

class AvaliacoesFragment : Fragment() {

    private lateinit var viewModel: AvaliacoesViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.activity_perfil_avaliacoes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        viewModel = ViewModelProvider(this)[AvaliacoesViewModel::class.java]

        val titulo = view.findViewById<TextView>(R.id.textAvaliacoesTitulo)
        val msg = view.findViewById<TextView>(R.id.textAvaliacoesMensagem)
        val container = view.findViewById<LinearLayout>(R.id.containerAvaliacoes)

        val imagem = view.findViewById<ImageView>(R.id.imageAvaliacaoPerfilBg)

        // BACK BUTTON
        view.findViewById<TextView>(R.id.buttonVoltarAvaliacoes)
            .setOnClickListener { requireActivity().onBackPressed() }

        // OBSERVERS
        viewModel.titulo.observe(viewLifecycleOwner) {
            titulo.text = it
        }

        viewModel.mensagem.observe(viewLifecycleOwner) {
            msg.text = it
        }

        viewModel.avaliacoes.observe(viewLifecycleOwner) {
            render(container, it)
        }

        carregarImagem(imagem)

        val cuidadorId =
            requireActivity().intent.getStringExtra(HomeCuidadorActivity.EXTRA_ID).orEmpty()

        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        viewModel.carregar(uid, cuidadorId)
    }

    private fun carregarImagem(imageView: ImageView) {
        val cache = PerfilLocalCache(requireContext())

        val id = requireActivity().intent.getStringExtra(HomeCuidadorActivity.EXTRA_ID).orEmpty()
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        val fotoUrl =
            cache.carregar(uid)?.fotoUrl
                ?: cache.carregar(id)?.fotoUrl
                ?: requireActivity().intent.getStringExtra(HomeCuidadorActivity.EXTRA_FOTO_URL)

        ImagemUrlLoader.carregar(imageView, fotoUrl ?: "")
    }

    private fun render(container: LinearLayout, lista: List<AvaliacaoRecebida>) {
        container.removeAllViews()

        lista.forEach {
            container.addView(card(it))
        }
    }

    private fun card(av: AvaliacaoRecebida): View {
        val density = resources.displayMetrics.density

        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_profile_card)
            elevation = 6f * density
            setPadding(
                (16 * density).toInt(),
                (14 * density).toInt(),
                (16 * density).toInt(),
                (14 * density).toInt()
            )
        }

        val estrelas = TextView(requireContext()).apply {
            text = gerarEstrelas(av.estrelas)
            textSize = 18f
            setTextColor(android.graphics.Color.parseColor("#F59E0B"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val nome = TextView(requireContext()).apply {
            text = av.cliente
            textSize = 16f
            setTextColor(android.graphics.Color.parseColor("#0F172A"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val comentario = TextView(requireContext()).apply {
            text = av.comentario
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#475569"))
            setPadding(0, (6 * density).toInt(), 0, 0)
        }

        val data = TextView(requireContext()).apply {
            text = av.data
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#94A3B8"))
            setPadding(0, (8 * density).toInt(), 0, 0)
        }

        card.addView(estrelas)
        card.addView(nome)
        card.addView(comentario)

        if (av.data.isNotBlank()) {
            card.addView(data)
        }

        return card
    }

    private fun gerarEstrelas(nota: Double): String {
        val cheia = nota.toInt().coerceIn(0, 5)
        return "★ ".repeat(cheia).trim()
    }
}