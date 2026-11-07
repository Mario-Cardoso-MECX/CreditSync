package com.cardoso.cardcontrol

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cardoso.cardcontrol.databinding.ActivityAddTarjetaBinding

class AddTarjetaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTarjetaBinding
    private var isFlipped = false // Saber si estamos viendo la parte de atrás

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTarjetaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Botón regresar
        binding.toolbarAdd.setNavigationOnClickListener { finish() }

        // Alejar la "cámara" virtual de Android para que el giro 3D no se recorte
        val cameraDist = 8000 * resources.displayMetrics.density
        binding.cardFrente.cameraDistance = cameraDist
        binding.cardAtras.cameraDistance = cameraDist

        configurarEfectoEspejoTextos()
        configurarGiroAlTocarCVC()

        binding.btnGuardar.setOnClickListener {
            val numero = binding.etNumero.text.toString()
            if (validarAlgoritmoLuhn(numero)) {
                Toast.makeText(this, "✅ Tarjeta Válida", Toast.LENGTH_SHORT).show()
                // Aquí en el futuro la guardaremos en la base de datos (Room)
            } else {
                binding.etNumero.error = "Número de tarjeta inválido"
                Toast.makeText(this, "❌ La tarjeta no existe", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun configurarEfectoEspejoTextos() {
        // Todo lo que escribas abajo, se dibuja en la tarjeta arriba en tiempo real
        binding.etNumero.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                binding.tvPreviewNumero.text = if (s.isNullOrEmpty()) "•••• •••• •••• ••••" else s.toString()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.etNombre.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                binding.tvPreviewNombre.text = if (s.isNullOrEmpty()) "NOMBRE DEL TITULAR" else s.toString()
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
    }

    private fun configurarGiroAlTocarCVC() {
        binding.etCvc.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !isFlipped) {
                girarTarjeta(mostrarAtras = true)
                isFlipped = true
            } else if (!hasFocus && isFlipped) {
                girarTarjeta(mostrarAtras = false)
                isFlipped = false
            }
        }
    }

    private fun girarTarjeta(mostrarAtras: Boolean) {
        val vistaVisible = if (mostrarAtras) binding.cardFrente else binding.cardAtras
        val vistaOculta = if (mostrarAtras) binding.cardAtras else binding.cardFrente

        // Animación 1: Girar la vista actual 90 grados (hasta ponerla de filo)
        val anim1 = ObjectAnimator.ofFloat(vistaVisible, "rotationY", 0f, 90f)
        anim1.duration = 150
        anim1.interpolator = AccelerateDecelerateInterpolator()

        // Animación 2: Girar la otra vista desde -90 grados (de filo) hasta 0
        val anim2 = ObjectAnimator.ofFloat(vistaOculta, "rotationY", -90f, 0f)
        anim2.duration = 150
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

    // El famoso Algoritmo de Luhn que usan Visa/Mastercard/Amex para verificar si la tarjeta es falsa
    private fun validarAlgoritmoLuhn(cardNumber: String): Boolean {
        if (cardNumber.length < 13) return false
        var sum = 0
        var alternate = false
        for (i in cardNumber.length - 1 downTo 0) {
            var n = cardNumber.substring(i, i + 1).toIntOrNull() ?: return false
            if (alternate) {
                n *= 2
                if (n > 9) {
                    n = (n % 10) + 1
                }
            }
            sum += n
            alternate = !alternate
        }
        return (sum % 10 == 0)
    }
}