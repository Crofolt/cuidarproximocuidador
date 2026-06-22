package com.mesawa.cuidarproximocuidador.ui.perfil.configuracoes

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.mesawa.cuidarproximocuidador.R

class ConfiguracoesFragment : Fragment() {
    private lateinit var viewModel: ConfiguracoesViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.activity_perfil_configuracoes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[ConfiguracoesViewModel::class.java]
        view.findViewById<TextView>(R.id.buttonVoltarConfiguracoes).setOnClickListener { requireActivity().finish() }
        view.findViewById<View>(R.id.rowTrocarSenha).setOnClickListener { abrirTrocarSenha() }
        view.findViewById<View>(R.id.rowEnderecoAtendimento).setOnClickListener { abrirEndereco() }
        view.findViewById<View>(R.id.rowPausarPerfil).setOnClickListener {
            confirmar("Pausar perfil", "Seu perfil ficará invisível para novas propostas até você reativar.", "pausar_perfil")
        }
        view.findViewById<View>(R.id.rowExcluirConta).setOnClickListener {
            confirmar("Excluir conta", "Essa ação será enviada para análise de segurança antes de remover seus dados.", "excluir_conta")
        }
    }

    private fun aviso(texto: String) {
        Toast.makeText(requireContext(), texto, Toast.LENGTH_LONG).show()
    }

    private fun abrirTrocarSenha() {
        val layout = campos("Senha atual", "Nova senha", "Confirmar nova senha")
        dialog("Trocar senha", layout) {
            val valores = valores(layout)
            viewModel.alterarSenha(valores[1], valores[2], ::sucessoEVoltar, ::aviso)
        }
    }

    private fun abrirEndereco() {
        val layout = campos("Endereço base", "Cidade", "Raio de atendimento em km")
        dialog("Endereço e regiões", layout) {
            val valores = valores(layout)
            viewModel.salvarEndereco(valores[0], valores[1], valores[2], ::sucessoEVoltar, ::aviso)
        }
    }

    private fun sucessoEVoltar(texto: String) {
        aviso(texto)
        requireActivity().finish()
    }

    private fun confirmar(titulo: String, mensagem: String, tipo: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(titulo)
            .setMessage(mensagem)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar") { _, _ ->
                viewModel.solicitarStatus(tipo, mensagem, ::aviso, ::aviso)
            }
            .show()
    }

    private fun campos(vararg hints: String): LinearLayout {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(42, 12, 42, 0)
            hints.forEach { hint ->
                addView(EditText(requireContext()).apply {
                    this.hint = hint
                    setSingleLine(true)
                })
            }
        }
    }

    private fun dialog(titulo: String, view: View, onSalvar: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle(titulo)
            .setView(view)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar") { _, _ -> onSalvar() }
            .show()
    }

    private fun valores(layout: LinearLayout): List<String> {
        return (0 until layout.childCount).map { index ->
            (layout.getChildAt(index) as? EditText)?.text?.toString()?.trim().orEmpty()
        }
    }
}
