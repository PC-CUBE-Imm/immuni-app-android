package it.ministerodellasalute.immuni.ui.favourite

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import it.ministerodellasalute.immuni.R
import it.ministerodellasalute.immuni.logic.user.models.GreenCertificateUser
import it.ministerodellasalute.immuni.ui.greencertificate.GreenCertificateFragment
import it.ministerodellasalute.immuni.ui.greencertificate.GreenCertificateViewModel
import it.ministerodellasalute.immuni.ui.greencertificate.GreenPassAdapter

class FavouriteDGCAdapter (
    val context: Context,
    val fragment: GreenCertificateFragment,
    val viewModel: GreenCertificateViewModel
) :
    RecyclerView.Adapter<FavouriteDGCAdapter.GreenPassVH>() {

    var data: List<GreenCertificateUser> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class GreenPassVH(v: View) : RecyclerView.ViewHolder(v) {
        val noQrCodeLayout: ConstraintLayout = v.findViewById(R.id.noQrCodeLayout)
        val listDGC: ConstraintLayout = v.findViewById(R.id.listDGC)

        val dateEvent: TextView = v.findViewById(R.id.dateEvent)
        val nameForename: TextView = v.findViewById(R.id.nameForename)
        val eventType: TextView = v.findViewById(R.id.eventType)
    }

    override fun getItemCount(): Int = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavouriteDGCAdapter.GreenPassVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.favourite_dgc_item, parent, false)
        return GreenPassVH(v)
    }

    override fun onBindViewHolder(holder: GreenPassVH, position: Int) {
        val greenCertificate = data[position]
        when (true) {
            greenCertificate.data?.recoveryStatements != null -> {
                holder.dateEvent = greenCertificate.data.recoveryStatements.get(0).certificateValidFrom
            }
            greenCertificate.data?.tests != null -> {

            }
            greenCertificate.data?.vaccinations != null -> {

            }
            greenCertificate.data?.exemptions != null -> {

            }
        }


        holder.dateEvent = greenCertificate.data.recoveryStatements[0].
    }
}
