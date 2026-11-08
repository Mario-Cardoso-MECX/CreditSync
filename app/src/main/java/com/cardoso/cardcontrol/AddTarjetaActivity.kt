package com.cardoso.cardcontrol

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
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
import com.google.android.material.textfield.TextInputLayout
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class AddTarjetaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTarjetaBinding
    private var isFlipped = false
    private var colorSeleccionado = "#6750A4"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

            // Validaciones silenciosas del backend
            val banco = binding.etBanco.text.toString()
            val limite = binding.etLimite.text.toString()
            val nombre = binding.etNombre.text.toString()
            val corte = binding.etDiaCorte.text.toString().toIntOrNull() ?: 0
            val pago = binding.etDiaPago.text.toString().toIntOrNull() ?: 0

            val bancoEsValido = validarBancoFuturo(banco)
            val limiteEsValido = validarScoreCrediticioFuturo(limite)
            val nombreEsValido = validarNombreLetras(nombre)

            val esValido = validarAlgoritmoLuhn(numero) &&
                    validarFecha(fecha) &&
                    cvv.length == 3 &&
                    bancoEsValido &&
                    limiteEsValido &&
                    nombreEsValido &&
                    corte in 1..31 &&
                    pago in 1..31

            if (esValido) {
                mostrarAlertaElegante("✅ Tarjeta lista. Guardando...")
                // PRÓXIMAMENTE: Base de Datos Room
                Handler(Looper.getMainLooper()).postDelayed({ finish() }, 1200)
            }
        }
    }

    // --- CREADOR DE PUNTITOS NEÓN ---
    private fun crearPuntitoNeon(esValido: Boolean): LayerDrawable {
        val scale = resources.displayMetrics.density
        val size = (16 * scale).toInt()
        val glowPadding = (4 * scale).toInt()

        val colorFuerte = if (esValido) Color.parseColor("#00E676") else Color.parseColor("#FF1744")
        val colorBrillo = if (esValido) Color.parseColor("#3300E676") else Color.parseColor("#33FF1744")

        // El halo luminoso (borde neón)
        val glowShape = ShapeDrawable(OvalShape())
        glowShape.intrinsicWidth = size + glowPadding
        glowShape.intrinsicHeight = size + glowPadding
        glowShape.paint.color = colorBrillo
        glowShape.paint.style = Paint.Style.FILL

        // El punto sólido central
        val coreShape = ShapeDrawable(OvalShape())
        coreShape.intrinsicWidth = size - glowPadding
        coreShape.intrinsicHeight = size - glowPadding
        coreShape.paint.color = colorFuerte
        coreShape.paint.style = Paint.Style.FILL

        val layer = LayerDrawable(arrayOf(glowShape, coreShape))
        layer.setLayerInset(1, glowPadding, glowPadding, glowPadding, glowPadding)
        return layer
    }

    private fun actualizarEstadoValidacion(layout: TextInputLayout, texto: String, isValid: Boolean) {
        if (texto.isEmpty()) {
            layout.endIconMode = TextInputLayout.END_ICON_NONE
        } else {
            layout.endIconMode = TextInputLayout.END_ICON_CUSTOM
            layout.endIconDrawable = crearPuntitoNeon(isValid)
            layout.setEndIconTintList(null) // Quitamos el tinte para que el neón brille con sus colores reales
        }
    }

    private fun configurarUXyValidaciones() {

        // CAMPOS CON PUNTITO NEÓN VISIBLE
        binding.etNumero.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val num = s.toString()
                val formateado = num.chunked(4).joinToString(" ")
                binding.tvPreviewNumero.text = if (num.isEmpty()) "•••• •••• •••• ••••" else formateado
                actualizarEstadoValidacion(binding.tilNumero, num, validarAlgoritmoLuhn(num))
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

                actualizarEstadoValidacion(binding.tilFecha, texto, validarFecha(texto))
                isUpdating = false
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.etCvc.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val texto = s.toString()
                binding.tvPreviewCvc.text = if (texto.isEmpty()) "•••" else texto
                actualizarEstadoValidacion(binding.tilCvc, texto, texto.length == 3)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.etDiaCorte.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val texto = s.toString()
                binding.tvPreviewCorte.text = if (texto.isEmpty()) "--" else texto
                val num = texto.toIntOrNull() ?: 0
                // Siempre validamos del 1 al 31, como dictan los contratos bancarios
                actualizarEstadoValidacion(binding.tilDiaCorte, texto, num in 1..31)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.etDiaPago.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val texto = s.toString()
                binding.tvPreviewPago.text = if (texto.isEmpty()) "--" else texto
                val num = texto.toIntOrNull() ?: 0
                // Siempre validamos del 1 al 31
                actualizarEstadoValidacion(binding.tilDiaPago, texto, num in 1..31)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // CAMPOS CON VALIDACIÓN INVISIBLE (Sin puntito neón)
        binding.etNombre.filters = arrayOf(InputFilter.AllCaps())
        binding.etNombre.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val texto = s.toString()
                binding.tvPreviewNombre.text = if (texto.isEmpty()) "NOMBRE DEL TITULAR" else texto
                // Valida silenciosamente
                val esValido = validarNombreLetras(texto)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.etBanco.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val texto = s.toString().uppercase()
                binding.tvPreviewBanco.text = if (texto.isEmpty()) "BANCO" else texto
                // Valida silenciosamente
                val esValido = validarBancoFuturo(texto)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

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

                        // Valida silenciosamente el score financiero
                        val esValido = validarScoreCrediticioFuturo(cleanString)
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

    // --- MÉTODOS DE VALIDACIÓN FUTURA INVISIBLE ---

    private fun validarNombreLetras(nombre: String): Boolean {
        // Solo permite letras y espacios
        val regex = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$".toRegex()
        return nombre.trim().isNotEmpty() && nombre.matches(regex)
    }

    private fun validarBancoFuturo(banco: String): Boolean {
        // TODO: A futuro aquí leeremos de un archivo o base de datos de bancos
        return banco.trim().isNotEmpty()
    }

    private fun validarScoreCrediticioFuturo(limiteStr: String): Boolean {
        // TODO: A futuro analizaremos el historial crediticio del usuario
        return limiteStr.isNotEmpty()
    }

    // --- FUNCIONES VISUALES Y MATEMÁTICAS ---
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
            listOf("#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5"),
            listOf("#03A9F4", "#00BCD4", "#009688", "#4CAF50", "#8BC34A"),
            listOf("#CDDC39", "#FFEB3B", "#FFC107", "#FF9800", "#FF5722"),
            listOf("#795548", "#9E9E9E", "#607D8B", "#455A64", "#37474F"),
            listOf("#D32F2F", "#C2185B", "#7B1FA2", "#303F9F", "#1976D2"),
            listOf("#388E3C", "#FBC02D", "#E64A19", "#5D4037", "#424242")
        )

        val contenedorPrincipal = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 32, 16, 32)
        }

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Selecciona un color")
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
                    strokeWidth = (1 * scale).toInt()
                    strokeColor = Color.parseColor("#33FFFFFF")
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

    private fun mostrarAlertaElegante(mensaje: String) {
        val snackbar = Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_SHORT)
        snackbar.duration = 1500
        snackbar.view.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
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