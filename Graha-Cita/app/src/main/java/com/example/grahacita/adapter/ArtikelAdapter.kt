package com.example.grahacita.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.grahacita.R
import com.example.grahacita.model.DataArtikel
import com.example.grahacita.model.DataGuru
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class ArtikelAdapter(private val context: Context, private var listArtikel: ArrayList<DataArtikel>) : RecyclerView.Adapter<ArtikelAdapter.ArtikelViewHolder>() {
    private lateinit var mListener: ArtikelAdapter.OnArticleClickListener

    interface OnArticleClickListener{
        fun onArticleClick(position: Int)
    }

    fun setOnItemClickListener(clickListener: OnArticleClickListener){
        mListener = clickListener
    }

    fun searchDataList(searchList: ArrayList<DataArtikel>){
        listArtikel = searchList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ArtikelAdapter.ArtikelViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_article, parent, false)
        return ArtikelViewHolder(itemView, mListener)
    }

    override fun onBindViewHolder(holder: ArtikelAdapter.ArtikelViewHolder, position: Int) {
        holder.bindData(listArtikel[position])
    }

    override fun getItemCount(): Int {
        return listArtikel.size
    }

    inner class ArtikelViewHolder(itemView: View, clickListener: ArtikelAdapter.OnArticleClickListener): RecyclerView.ViewHolder(itemView){
        private val imageCache = ConcurrentHashMap<String, Bitmap>()

        val thumbnail: ImageView = itemView.findViewById(R.id.imageThumbnail)
        val title: TextView = itemView.findViewById(R.id.textTitle)
        val description: TextView = itemView.findViewById(R.id.textDescription)
        val author: TextView = itemView.findViewById(R.id.textAuthor)
        val published: TextView = itemView.findViewById(R.id.textPublication)

        init {
            itemView.setOnClickListener{
                clickListener.onArticleClick(adapterPosition)
            }
        }

        fun bindData(d: DataArtikel){
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()) // Menambahkan EEEE untuk nama hari
            val date = inputFormat.parse(d.publishedAt)
            val outputDateString = outputFormat.format(date!!)


            loadImageFromUrl(d.urlToImage!!, thumbnail)
            title.text = d.title
            description.text = d.description
            author.text = d.author
            published.text = outputDateString
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