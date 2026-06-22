package com.mesawa.cuidarproximocuidador.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.mesawa.cuidarproximocuidador.R
import com.mesawa.cuidarproximocuidador.atualizacao.AppUpdateCoordinator
import com.mesawa.cuidarproximocuidador.ui.analise.AnaliseCuidadorFragment
import com.mesawa.cuidarproximocuidador.ui.home.InicioCuidadorFragment
import com.mesawa.cuidarproximocuidador.ui.perfil.PerfilCuidadorFragment
import com.mesawa.cuidarproximocuidador.ui.propostas.PropostasCuidadorFragment

class HomeCuidadorActivity : FragmentActivity() {

    private lateinit var navItems: List<NavItem>
    private var updateCoordinator: AppUpdateCoordinator? = null
    private val updateLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        updateCoordinator?.retomarAtualizacaoInterrompida()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_cuidador)

        navItems = listOf(
            NavItem(findViewById(R.id.navInicio), findViewById(R.id.iconNavInicio), findViewById(R.id.textNavInicio)),
            NavItem(findViewById(R.id.navPropostas), findViewById(R.id.iconNavPropostas), findViewById(R.id.textNavPropostas), findViewById(R.id.badgeNavPropostas)),
            NavItem(findViewById(R.id.navAnalise), findViewById(R.id.iconNavAnalise), findViewById(R.id.textNavAnalise)),
            NavItem(findViewById(R.id.navPerfil), findViewById(R.id.iconNavPerfil), findViewById(R.id.textNavPerfil))
        )

        findViewById<View>(R.id.navInicio).setOnClickListener { open(InicioCuidadorFragment(), 0) }
        findViewById<View>(R.id.navPropostas).setOnClickListener { open(PropostasCuidadorFragment(), 1) }
        findViewById<View>(R.id.navAnalise).setOnClickListener { open(AnaliseCuidadorFragment(), 2) }
        findViewById<View>(R.id.navPerfil).setOnClickListener { open(PerfilCuidadorFragment(), 3) }

        if (savedInstanceState == null) {
            open(InicioCuidadorFragment(), 0)
        }

        updateCoordinator = AppUpdateCoordinator(this, updateLauncher)
        updateCoordinator?.verificarAtualizacao()
    }

    override fun onResume() {
        super.onResume()
        updateCoordinator?.retomarAtualizacaoInterrompida()
    }

    override fun onDestroy() {
        updateCoordinator?.liberar()
        super.onDestroy()
    }

    private fun open(fragment: Fragment, selectedIndex: Int) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.homeContainer, fragment)
            .commit()
        navItems.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            val color = getColor(if (selected) android.R.color.holo_blue_dark else android.R.color.darker_gray)
            item.container.setBackgroundResource(if (selected) R.drawable.bg_nav_item_selected else android.R.color.transparent)
            item.icon.setColorFilter(color)
            item.label.setTextColor(color)
            item.badge?.visibility = if (selected) View.GONE else View.VISIBLE
        }
    }

    private data class NavItem(
        val container: View,
        val icon: ImageView,
        val label: TextView,
        val badge: TextView? = null
    )

    companion object {
        const val EXTRA_ID = "cuidador_id"
        const val EXTRA_NOME = "cuidador_nome"
        const val EXTRA_ESPECIALIDADE = "cuidador_especialidade"
        const val EXTRA_FOTO_URL = "cuidador_foto_url"
    }
}
