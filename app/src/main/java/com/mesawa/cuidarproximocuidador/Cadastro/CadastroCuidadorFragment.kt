package com.mesawa.cuidarproximocuidador.Cadastro

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.mesawa.cuidarproximocuidador.R

class CadastroCuidadorFragment : Fragment() {

    interface Callbacks {
        fun onEnviarCadastro(dados: CadastroCuidadorDados)
    }

    private lateinit var messageView: TextView
    private lateinit var progress: ProgressBar
    private lateinit var enviarButton: Button
    private lateinit var fotoPreview: ImageView
    private var fotoPerfilUri: Uri? = null
    private var latitude: Double? = -23.4205
    private var longitude: Double? = -51.9333

    private val pedirLocalizacao = registerForActivityResult(ActivityResultContracts.RequestPermission()) { permitido ->
        if (permitido) preencherLocalizacaoAproximada(requireView()) else showMessage("Sem permissão de localização. Mantive Maringá, Paraná como cidade de atuação.")
    }

    private val escolherFoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            fotoPerfilUri = uri
            fotoPreview.setImageURI(uri)
            showMessage("Foto selecionada. Ela será enviada junto com seu cadastro.")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_cadastro_cuidador, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        messageView = view.findViewById(R.id.textCadastroMessage)
        progress = view.findViewById(R.id.progressCadastro)
        enviarButton = view.findViewById(R.id.buttonEnviarCadastro)
        fotoPreview = view.findViewById(R.id.imageFotoPerfilCadastro)
        configurarCidades(view)
        view.findViewById<Button>(R.id.buttonEscolherFotoPerfil).setOnClickListener {
            escolherFoto.launch("image/*")
        }
        view.findViewById<Button>(R.id.buttonUsarLocalizacaoCadastro).setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                preencherLocalizacaoAproximada(view)
            } else {
                pedirLocalizacao.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
        enviarButton.setOnClickListener { callbacks()?.onEnviarCadastro(coletarDados(view)) }
    }

    fun showMessage(message: String) {
        messageView.text = message
        messageView.visibility = View.VISIBLE
    }

    fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        enviarButton.isEnabled = !loading
        enviarButton.text = if (loading) "Enviando..." else "Enviar cadastro para análise"
    }

    private fun coletarDados(view: View): CadastroCuidadorDados {
        return CadastroCuidadorDados(
            nomeCompleto = text(view, R.id.editNomeCompleto),
            cpf = text(view, R.id.editCpf),
            nascimento = text(view, R.id.editNascimento),
            telefone = text(view, R.id.editTelefone),
            email = text(view, R.id.editEmailCadastro),
            senha = text(view, R.id.editSenhaCadastro),
            fotoPerfilUri = fotoPerfilUri?.toString(),
            cidade = text(view, R.id.editCidade),
            uf = text(view, R.id.editUf),
            latitude = latitude,
            longitude = longitude,
            raioKm = text(view, R.id.editRaio),
            valorHora = text(view, R.id.editValorHora),
            disponibilidade = text(view, R.id.editDisponibilidade),
            especialidade = text(view, R.id.editEspecialidade),
            curso = text(view, R.id.editCurso),
            instituicao = text(view, R.id.editInstituicao),
            experiencia = text(view, R.id.editExperiencia),
            bio = text(view, R.id.editBio),
            experienciaMedicacao = checked(view, R.id.checkMedicacao),
            experienciaMobilidade = checked(view, R.id.checkMobilidade),
            experienciaAlzheimer = checked(view, R.id.checkAlzheimer),
            antecedentes = text(view, R.id.editAntecedentes),
            referenciaNome = text(view, R.id.editReferenciaNome),
            referenciaTelefone = text(view, R.id.editReferenciaTelefone),
            autorizouVerificacao = checked(view, R.id.checkVerificacao)
        )
    }

    private fun text(view: View, id: Int): String = view.findViewById<EditText>(id).text.toString().trim()

    private fun checked(view: View, id: Int): Boolean = view.findViewById<CheckBox>(id).isChecked

    private fun callbacks(): Callbacks? = activity as? Callbacks

    private fun configurarCidades(view: View) {
        val cidades = listOf(
            "Maringá", "Sarandi", "Paiçandu", "Marialva", "Mandaguari", "Londrina", "Apucarana",
            "Cambé", "Rolândia", "Cianorte", "Campo Mourão", "Cascavel", "Foz do Iguaçu",
            "Curitiba", "Ponta Grossa", "Guarapuava", "Paranavaí", "Umuarama"
        )
        val cidade = view.findViewById<AutoCompleteTextView>(R.id.editCidade)
        cidade.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, cidades))
        cidade.setText("Maringá", false)
        view.findViewById<EditText>(R.id.editUf).setText("PR")
    }

    private fun preencherLocalizacaoAproximada(view: View) {
        val manager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = runCatching {
            manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }.getOrNull()
        latitude = location?.latitude ?: -23.4205
        longitude = location?.longitude ?: -51.9333
        view.findViewById<AutoCompleteTextView>(R.id.editCidade).setText("Maringá", false)
        view.findViewById<EditText>(R.id.editUf).setText("PR")
        showMessage("Local de atuação definido como Maringá, Paraná. Vamos filtrar propostas por essa cidade.")
    }

    companion object {
        fun newInstance() = CadastroCuidadorFragment()
    }
}
