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

        // Configurar Hamburguesa... (código anterior)

        // *** APLICAR EFECTO DE CARRUSEL APILADO ***
        // Usamos HeroCarouselStrategy para que una tarjeta resalte al centro y las demás se asomen a los lados
        val carouselLayoutManager = CarouselLayoutManager(HeroCarouselStrategy())

        // Lo asignamos al RecyclerView
        binding.rvTarjetas.layoutManager = carouselLayoutManager

        // (Para que no se amontonen mal)
        binding.rvTarjetas.setHasFixedSize(true)
    }
}