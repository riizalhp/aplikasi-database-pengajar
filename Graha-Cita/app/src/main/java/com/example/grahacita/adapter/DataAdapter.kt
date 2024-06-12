package com.example.grahacita.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.grahacita.DetailActivity
import com.example.grahacita.R
import com.example.grahacita.model.DataGuru
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

class DataAdapter(private var context: Context, private var data: ArrayList<DataGuru>) : RecyclerView.Adapter<DataAdapter.DataViewHolder>(){

    private lateinit var mListener: OnItemClickListener

    interface OnItemClickListener{
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(clickListener: OnItemClickListener){
        mListener = clickListener
    }

    fun searchDataList(searchList: ArrayList<DataGuru>){
        data = searchList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return DataViewHolder(itemView, mListener)
    }

    override fun onBindViewHolder(holder: DataViewHolder, position: Int) {
        holder.bindData(data[position])
    }

    override fun getItemCount(): Int {
       return data.size
    }

    inner class DataViewHolder(itemView: View, clickListener: OnItemClickListener): RecyclerView.ViewHolder(itemView){
        private val imageCache = ConcurrentHashMap<String, Bitmap>()

        val profile: ImageView = itemView.findViewById(R.id.imageProfile)
        val namaGuru: TextView = itemView.findViewById(R.id.namaGuru)
        val namaMapel: TextView = itemView.findViewById(R.id.namaMapel)
        val namaKota: TextView = itemView.findViewById(R.id.namaKota)
        val statusBooking: TextView = itemView.findViewById(R.id.statusBooking)
        val cardStatus: CardView = itemView.findViewById(R.id.cardStatus)

        init {
            itemView.setOnClickListener{
                clickListener.onItemClick(adapterPosition)
            }
        }

        fun bindData(d: DataGuru){
            loadImageFromUrl(d.profile_url!!, profile)
            namaGuru.text = d.nama_guru
            namaMapel.text = d.mapel
            namaKota.text = d.kota
            if (d.status == "Sudah Dibooking") {
                cardStatus.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary))
            } else {
                cardStatus.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorSecondary))
                statusBooking.setTextColor(Color.WHITE)
            }
            statusBooking.text = d.status

        }

        fun loadImageFromUrl(urlString: String, imageView: ImageView) {
            if (imageCache.containsKey(urlString)) {
                imageView.setImageBitmap(imageCache[urlString])
                return
            }

            val handler = Handler(Looper.getMainLooper())
            val thread = Thread {
                var bitmap: Bitmap? = null
                var connection: HttpURLConnection? = null
                try {
                    val url = URL(urlString)
                    connection = url.openConnection() as HttpURLConnection
                    connection.doInput = true
                    connection.connect()
                    val input: InputStream = connection.inputStream
                    bitmap = BitmapFactory.decodeStream(input)
                    bitmap?.let {
                        imageCache[urlString] = it
                        handler.post {
                            imageView.setImageBitmap(it)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    connection?.disconnect()
                }
            }
            thread.start()
        }
    }

}