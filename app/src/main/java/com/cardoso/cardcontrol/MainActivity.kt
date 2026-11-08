package com.cardoso.cardcontrol

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
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

        // 5 Tarjetas Simuladas
        val misTarjetas = listOf(
            Tarjeta(1, "Nubank", "8760", 8600.0, 0.10, "#8A05BE"),
            Tarjeta(2, "Klar", "4006", 33000.0, 0.39, "#000000"),
            Tarjeta(3, "Mercado Pago", "0474", 6400.0, 0.0, "#009EE3"),
            Tarjeta(4, "RappiCard", "1234", 15000.0, 0.20, "#FF4000"),
            Tarjeta(5, "DidiCard", "9876", 5000.0, 0.05, "#FF7F00")
        )

        val adapter = TarjetaAdapter(misTarjetas)

        binding.vpTarjetas.adapter = adapter
        binding.vpTarjetas.offscreenPageLimit = 3
        binding.vpTarjetas.setPageTransformer(StackPageTransformer())

        binding.vpTarjetas.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val tarjetaActiva = misTarjetas[position]
            }
        })

        // Menú Hamburguesa SEGURO
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_add_card -> {
                    // AQUÍ ESTÁ LA MAGIA: this@MainActivity asegura que no haya crasheos de contexto
                    startActivity(android.content.Intent(this@MainActivity, AddTarjetaActivity::class.java))
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    inner class StackPageTransformer : ViewPager2.PageTransformer {
        override fun transformPage(page: View, position: Float) {
            if (page.width == 0) return

            page.translationX = -position * page.width
            val absPosition = Math.abs(position)

            if (absPosition <= 1) {
                page.translationX += position * 60f
                val scale = 1f - (0.1f * absPosition)
                page.scaleX = scale
                page.scaleY = scale
                page.alpha = 1f - (0.2f * absPosition)
                page.translationZ = -absPosition * 10f
            } else {
                val sign = if (position > 0) 1 else -1
                page.translationX += sign * 60f
                page.scaleX = 0.9f
                page.scaleY = 0.9f
                page.alpha = 0f
                page.translationZ = -absPosition * 10f
            }
        }
    }
}