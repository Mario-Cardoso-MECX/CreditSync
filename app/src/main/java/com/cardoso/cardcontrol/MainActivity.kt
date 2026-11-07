package com.cardoso.cardcontrol

import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.HeroCarouselStrategy
import com.cardoso.cardcontrol.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.topAppBar)

        // 1. Configurar Menú Hamburguesa
        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.topAppBar,
            R.string.app_name, R.string.app_name
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // 2. Configurar el Carrusel Apilado (Cover Flow)
        val carouselLayoutManager = CarouselLayoutManager(HeroCarouselStrategy())
        binding.rvTarjetas.layoutManager = carouselLayoutManager
        binding.rvTarjetas.setHasFixedSize(true)

        // 3. Crear datos simulados basados en tu Excel
        // Aquí simulamos que ya consultamos Room y las ordenamos por "Frecuencia de Uso"
        val misTarjetas = listOf(
            // Posición 0: La más usada (Estará al frente/centro al abrir la app)
            Tarjeta(1, "Nubank", "8760", 8600.0, 0.10, "#8A05BE"), // Morado Nu
            // Posición 1: Segunda más usada (Se asomará por la derecha)
            Tarjeta(2, "Klar", "4006", 33000.0, 0.39, "#000000"), // Negro Klar
            // Posición 2: Tercera (Quedará oculta hasta hacer scroll)
            Tarjeta(3, "Mercado Pago", "0474", 6400.0, 0.0, "#009EE3") // Azul MP
        )

        // 4. Conectar el Adapter al RecyclerView
        val adapter = TarjetaAdapter(misTarjetas)
        binding.rvTarjetas.adapter = adapter
    }
}