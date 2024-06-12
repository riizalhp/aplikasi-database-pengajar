package com.example.grahacita

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.grahacita.adapter.ArtikelAdapter
import com.example.grahacita.adapter.DataAdapter
import com.example.grahacita.data.Variabel
import com.example.grahacita.databinding.ActivityArtikelBinding
import com.example.grahacita.databinding.ActivityHomeBinding
import com.example.grahacita.model.DataArtikel
import com.example.grahacita.model.DataGuru
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class ArtikelActivity : AppCompatActivity() {
    private lateinit var binding: ActivityArtikelBinding

    private lateinit var datarecycleView: RecyclerView
    private lateinit var dataList: ArrayList<DataArtikel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityArtikelBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor(R.color.colorPrimary)
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        datarecycleView = findViewById(R.id.rvArticle)
        datarecycleView.layoutManager = LinearLayoutManager(this)
        datarecycleView.setHasFixedSize(true)

        dataList = arrayListOf<DataArtikel>()

        getArtikel()
    }

    fun getArtikel() {
        dataList.clear()
        showLoading()
        thread {
            var result = ""
            val urlStr = "${Variabel.ENDPOINAPI}/everything?q=pendidikan&language=id"

            try {
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.readTimeout = 15000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.setRequestProperty("Authorization", Variabel.APIKEY)
                conn.connect()

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    result = reader.use { it.readText() }
                } else {
                    val reader = BufferedReader(InputStreamReader(conn.errorStream))
                    result = reader.use { it.readText() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val json = JSONObject(result)
            val articlesArray = json.getJSONArray("articles")
            for (i in 0 until articlesArray.length()) {
                val articleObject = articlesArray.getJSONObject(i)
                val author = articleObject.getString("author")
                val title = articleObject.getString("title")
                val description = articleObject.getString("description")
                val url = articleObject.getString("url")
                val urlToImage = articleObject.getString("urlToImage")
                val publishedAt = articleObject.getString("publishedAt")

                dataList.add(DataArtikel(author, title, description, url, urlToImage, publishedAt))
            }

            runOnUiThread {
                hideLoading()
                if(result != ""){
                    val dataAdapter = ArtikelAdapter(applicationContext, dataList)
                    datarecycleView.adapter = dataAdapter

                    dataAdapter.setOnItemClickListener(object : ArtikelAdapter.OnArticleClickListener{
                        override fun onArticleClick(position: Int) {
                            val artikel = dataList[position]
                            val url = artikel.url

                            val alertDialogBuilder = AlertDialog.Builder(this@ArtikelActivity)
                            alertDialogBuilder.apply {
                                setTitle("Buka Tautan")
                                setMessage("Apakah Anda yakin ingin membuka tautan ${artikel.title}?")
                                setPositiveButton("Ya") { dialog, which ->
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    startActivity(intent)
                                }
                                setNegativeButton("Batal") { dialog, which ->
                                    dialog.dismiss()
                                }
                            }
                            val alertDialog = alertDialogBuilder.create()
                            alertDialog.show()
                        }
                    })


                    binding.searchArtikel.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
                        override fun onQueryTextSubmit(query: String?): Boolean {
                            return false
                        }

                        override fun onQueryTextChange(newText: String?): Boolean {
                            val searchList = ArrayList<DataArtikel>()
                            var isDataFound = false
                            for (dataArtikel in dataList){
                                if(dataArtikel.title?.lowercase()?.contains(newText!!.lowercase()) == true ||
                                    dataArtikel.author?.lowercase()?.contains(newText!!.lowercase()) == true){
                                    searchList.add(dataArtikel)
                                    isDataFound = true
                                }
                            }
                            if (!isDataFound) {
                                searchList.clear()
                                binding.noData.visibility = View.VISIBLE
                            }

                            dataAdapter.searchDataList(searchList)
                            return true
                        }

                    })

                    showDataList()
                }else{
                    showEmptyDataView()
                }
            }
        }
    }

    private fun showDataList() {
        binding.emptyDataView.visibility = View.GONE
        binding.rvArticle.visibility = View.VISIBLE
    }

    private fun showEmptyDataView() {
        binding.rvArticle.visibility = View.GONE
        binding.emptyDataView.visibility = View.VISIBLE
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }
}