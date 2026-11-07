package com.cardoso.cardcontrol

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.cardoso.cardcontrol.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.topAppBar)

        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.topAppBar,
            R.string.app_name, R.string.app_name
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val misTarjetas = listOf(
            Tarjeta(1, "Nubank", "8760", 8600.0, 0.10, "#8A05BE"),
            Tarjeta(2, "Klar", "4006", 33000.0, 0.39, "#000000"),
            Tarjeta(3, "Mercado Pago", "0474", 6400.0, 0.0, "#009EE3")
        )

        val adapter = TarjetaAdapter(misTarjetas)

        // --- CONFIGURACIÓN DEL MAZO 3D (STACK) ---
        binding.vpTarjetas.adapter = adapter

        // Cargamos hasta 3 tarjetas extra ocultas detrás para que el efecto sea fluido
        binding.vpTarjetas.offscreenPageLimit = 3

        // Aquí aplicamos la matemática para apilarlas
        binding.vpTarjetas.setPageTransformer(StackPageTransformer())

        // Listener para saber qué tarjeta quedó al frente (Posición 0, 1, 2...)
        binding.vpTarjetas.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val tarjetaActiva = misTarjetas[position]
                // ¡Aquí actualizaremos los números de "Estrategia de Score"!
            }
        })
    }

    // --- EL MOTOR QUE LAS ENCIMA UNA DETRÁS DE OTRA ---
    inner class StackPageTransformer : ViewPager2.PageTransformer {
        override fun transformPage(page: View, position: Float) {
            // position <= 0: La tarjeta de enfrente o la que estás deslizando para quitarla
            // position > 0: Las tarjetas "esperando a salir" que están detrás

            if (position <= 0f) {
                page.translationX = 0f
                page.translationZ = 0f // Al frente en 3D
                page.alpha = 1f + position // Se desvanece al deslizarla
                page.scaleX = 1f
                page.scaleY = 1f
            } else {
                // 1. Cancelamos su posición natural (que sería a la derecha) y las traemos detrás de la central
                page.translationX = -page.width * position

                // 2. Las "asomamos" un poco hacia la derecha para que sepas que ahí están
                val asomarsePorLado = 60f * position
                page.translationX += asomarsePorLado

                // 3. Las hacemos ligeramente más pequeñas (Efecto profundidad)
                val scale = 1f - (0.1f * position)
                page.scaleX = scale
                page.scaleY = scale

                // 4. Las oscurecemos un poco
                page.alpha = 1f - (0.2f * position)

                // 5. Las mandamos hacia atrás en la capa Z real
                page.translationZ = -position * 10f
            }
        }
    }
}