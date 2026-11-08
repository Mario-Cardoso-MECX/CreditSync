package com.cardoso.cardcontrol

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.cardoso.cardcontrol.databinding.ActivityAddTarjetaBinding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import java.text.NumberFormat
import java.util.Locale

class AddTarjetaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTarjetaBinding
    private var isFlipped = false
    private var colorSeleccionado = "#6750A4"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ESTO EVITA QUE LA TARJETA SE SUBA AL ABRIR EL TECLADO (El secreto de la UX fija)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        binding = ActivityAddTarjetaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarAdd)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarAdd.setNavigationOnClickListener { finish() }

        val scale = resources.displayMetrics.density
        binding.cardFrente.cameraDistance = 8000 * scale
        binding.cardAtras.cameraDistance = 8000 * scale

        configurarUXyValidaciones()
        configurarGiroAlTocarCVC()
        configurarSelectorColores()

        binding.btnGuardar.setOnClickListener {
            val numero = binding.etNumero.text.toString()
            val fecha = binding.etFecha.text.toString()
            val cvv = binding.etCvc.text.toString()

            if (numero.isEmpty() || binding.etBanco.text.isNullOrEmpty() || binding.etLimite.text.isNullOrEmpty()) {
                mostrarAlerta("⚠️ Llena todos los campos clave", false)
                return@setOnClickListener
            }

            if (!validarAlgoritmoLuhn(numero)) {
                binding.etNumero.error = "Número inválido"
                mostrarAlerta("❌ La tarjeta no existe (Error de algoritmo)", false)
                return@setOnClickListener
            }

            if (!validarFecha(fecha)) {
                binding.etFecha.error = "Fecha incorrecta o vencida"
                mostrarAlerta("❌ Revisa la fecha de vencimiento", false)
                return@setOnClickListener
            }

            if (cvv.length < 3) {
                binding.etCvc.error = "Faltan dígitos"
                mostrarAlerta("❌ El CVV está incompleto", false)
                return@setOnClickListener
            }

            mostrarAlerta("✅ Tarjeta procesada y válida. Guardando...", true)
            // PRÓXIMAMENTE: ¡Guardar en la Base de Datos Local Room!
            Handler(Looper.getMainLooper()).postDelayed({ finish() }, 1200)
        }
    }

    private fun configurarUXyValidaciones() {
        binding.etNumero.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val num = s.toString()
                val formateado = num.chunked(4).joinToString(" ")
                binding.tvPreviewNumero.text = if (num.isEmpty()) "•••• •••• •••• ••••" else formateado
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.etNombre.filters = arrayOf(InputFilter.AllCaps())
        binding.etNombre.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                binding.tvPreviewNombre.text = if (s.isNullOrEmpty()) "NOMBRE DEL TITULAR" else s.toString()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.etFecha.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating || s == null) return
                isUpdating = true
                var texto = s.toString().replace("/", "")
                if (texto.length > 2) {
                    texto = "${texto.substring(0, 2)}/${texto.substring(2)}"
                }
                binding.etFecha.setText(texto)
                binding.etFecha.setSelection(texto.length)
                binding.tvPreviewFecha.text = if (texto.isEmpty()) "MM/AA" else texto
                isUpdating = false
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.etCvc.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                binding.tvPreviewCvc.text = if (s.isNullOrEmpty()) "•••" else s.toString()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.etBanco.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                binding.tvPreviewBanco.text = if (s.isNullOrEmpty()) "MI BANCO" else s.toString().uppercase()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // EL FORMATEADOR ESTILO CAJERO AUTOMÁTICO (Escribes y pone comas/moneda solo)
        binding.etLimite.addTextChangedListener(object : TextWatcher {
            private var current = ""
            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    binding.etLimite.removeTextChangedListener(this)
                    val cleanString = s.toString().replace(Regex("[^\\d]"), "")

                    if (cleanString.isNotEmpty()) {
                        val parsed = cleanString.toDouble()
                        val formatted = NumberFormat.getCurrencyInstance(Locale.US).format(parsed / 100)
                        current = formatted
                        binding.etLimite.setText(formatted)
                        binding.etLimite.setSelection(formatted.length)
                        binding.tvPreviewLimiteValor.text = formatted
                    } else {
                        current = ""
                        binding.etLimite.setText("")
                        binding.tvPreviewLimiteValor.text = "$0.00"
                    }
                    binding.etLimite.addTextChangedListener(this)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun configurarSelectorColores() {
        val clickListener = View.OnClickListener { view ->
            val colorHex = when (view.id) {
                R.id.colorNu -> "#8A05BE"
                R.id.colorKlar -> "#000000"
                R.id.colorMp -> "#009EE3"
                R.id.colorRappi -> "#FF4000"
                else -> "#6750A4"
            }
            aplicarColorFondo(colorHex)
        }

        binding.colorNu.setOnClickListener(clickListener)
        binding.colorKlar.setOnClickListener(clickListener)
        binding.colorMp.setOnClickListener(clickListener)
        binding.colorRappi.setOnClickListener(clickListener)

        binding.btnColorCustom.setOnClickListener {
            mostrarPaletaColores()
        }
    }

    private fun aplicarColorFondo(colorHex: String) {
        colorSeleccionado = colorHex
        binding.cardFrente.setCardBackgroundColor(Color.parseColor(colorHex))
        binding.cardAtras.setCardBackgroundColor(Color.parseColor(colorHex))
    }

    private fun mostrarPaletaColores() {
        val coloresPaleta = listOf(
            listOf("#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3"),
            listOf("#03A9F4", "#00BCD4", "#009688", "#4CAF50", "#8BC34A"),
            listOf("#CDDC39", "#FFEB3B", "#FFC107", "#FF9800", "#FF5722"),
            listOf("#795548", "#9E9E9E", "#607D8B", "#212121", "#000000"),
            listOf("#D32F2F", "#C2185B", "#7B1FA2", "#303F9F", "#1976D2"),
            listOf("#388E3C", "#FBC02D", "#E64A19", "#5D4037", "#455A64")
        )

        val contenedorPrincipal = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 32, 16, 32)
        }

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Selecciona el color")
            .setView(contenedorPrincipal)
            .create()

        val scale = resources.displayMetrics.density
        val size = (45 * scale).toInt()
        val margin = (6 * scale).toInt()

        for (fila in coloresPaleta) {
            val filaLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            for (colorHex in fila) {
                val burbuja = MaterialCardView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        setMargins(margin, margin, margin, margin)
                    }
                    setCardBackgroundColor(Color.parseColor(colorHex))
                    radius = (22 * scale)
                    strokeWidth = 0
                    setOnClickListener {
                        aplicarColorFondo(colorHex)
                        dialog.dismiss()
                    }
                }
                filaLayout.addView(burbuja)
            }
            contenedorPrincipal.addView(filaLayout)
        }
        dialog.show()
    }

    private fun configurarGiroAlTocarCVC() {
        binding.etCvc.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !isFlipped) girarTarjeta(mostrarAtras = true)
            else if (!hasFocus && isFlipped) girarTarjeta(mostrarAtras = false)
        }
    }

    private fun girarTarjeta(mostrarAtras: Boolean) {
        isFlipped = mostrarAtras
        val vistaVisible = if (mostrarAtras) binding.cardFrente else binding.cardAtras
        val vistaOculta = if (mostrarAtras) binding.cardAtras else binding.cardFrente

        val anim1 = ObjectAnimator.ofFloat(vistaVisible, "rotationY", 0f, 90f)
        anim1.duration = 200
        anim1.interpolator = AccelerateDecelerateInterpolator()

        val anim2 = ObjectAnimator.ofFloat(vistaOculta, "rotationY", -90f, 0f)
        anim2.duration = 200
        anim2.interpolator = AccelerateDecelerateInterpolator()

        anim1.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                vistaVisible.visibility = View.GONE
                vistaOculta.visibility = View.VISIBLE
                anim2.start()
            }
        })
        anim1.start()
    }

    private fun mostrarAlerta(mensaje: String, esExito: Boolean) {
        val snackbar = Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_SHORT)
        snackbar.duration = 1500
        val colorFondo = if (esExito) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")

        snackbar.view.backgroundTintList = ColorStateList.valueOf(colorFondo)
        snackbar.setTextColor(Color.WHITE)
        snackbar.show()
    }

    private fun validarAlgoritmoLuhn(cardNumber: String): Boolean {
        val numeroLimpio = cardNumber.replace(" ", "")
        if (numeroLimpio.length < 13) return false
        var sum = 0
        var alternate = false
        for (i in numeroLimpio.length - 1 downTo 0) {
            var n = numeroLimpio.substring(i, i + 1).toIntOrNull() ?: return false
            if (alternate) {
                n *= 2
                if (n > 9) n = (n % 10) + 1
            }
            sum += n
            alternate = !alternate
        }
        return (sum % 10 == 0)
    }

    private fun validarFecha(fecha: String): Boolean {
        if (fecha.length != 5) return false
        val parts = fecha.split("/")
        if (parts.size != 2) return false

        val mes = parts[0].toIntOrNull() ?: return false
        val anio = parts[1].toIntOrNull() ?: return false

        if (mes !in 1..12) return false
        if (anio < 24) return false

        return true
    }
}