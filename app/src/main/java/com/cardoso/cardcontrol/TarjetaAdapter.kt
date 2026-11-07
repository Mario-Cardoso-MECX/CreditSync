package com.cardoso.cardcontrol

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cardoso.cardcontrol.databinding.ItemTarjetaBinding
import java.text.NumberFormat
import java.util.Locale

class TarjetaAdapter(private var tarjetas: List<Tarjeta>) :
    RecyclerView.Adapter<TarjetaAdapter.TarjetaViewHolder>() {

    inner class TarjetaViewHolder(val binding: ItemTarjetaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(tarjeta: Tarjeta) {
            binding.tvBanco.text = tarjeta.banco
            binding.tvNumeros.text = "•••• •••• •••• ${tarjeta.ultimosDigitos}"

            // Formatear a moneda (ej. $8,600.00)
            val formatMoneda = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
            binding.tvLimiteValor.text = formatMoneda.format(tarjeta.limiteCredito)

            // Mostrar porcentaje de uso
            val porcentajeEntero = (tarjeta.usoReportadoPorcentaje * 100).toInt()
            binding.tvUso.text = "Uso: $porcentajeEntero%"

            // Aplicar el color del banco al fondo de la tarjeta
            binding.bgTarjeta.setBackgroundColor(Color.parseColor(tarjeta.colorHex))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TarjetaViewHolder {
        val binding = ItemTarjetaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TarjetaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TarjetaViewHolder, position: Int) {
        holder.bind(tarjetas[position])
    }

    override fun getItemCount(): Int = tarjetas.size

    // Método para cuando implementemos la base de datos y ordenemos por frecuencia de uso
    fun actualizarTarjetas(nuevasTarjetas: List<Tarjeta>) {
        tarjetas = nuevasTarjetas
        notifyDataSetChanged()
    }
}