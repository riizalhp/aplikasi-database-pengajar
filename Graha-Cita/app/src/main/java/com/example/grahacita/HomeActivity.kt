package com.example.grahacita

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.grahacita.adapter.DataAdapter
import com.example.grahacita.data.Variabel
import com.example.grahacita.databinding.ActivityHomeBinding
import com.example.grahacita.model.DataGuru
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var datarecycleView: RecyclerView
    private lateinit var dataList: ArrayList<DataGuru>

    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        firebaseAuth = FirebaseAuth.getInstance()

        datarecycleView = findViewById(R.id.rvList)
        datarecycleView.layoutManager = LinearLayoutManager(this)
        datarecycleView.setHasFixedSize(true)

        dataList = arrayListOf<DataGuru>()
        getDataGuru()
        getLocation()

        binding.btnArticle.setOnClickListener {
            val intent = Intent(this, ArtikelActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnTambah.setOnClickListener {
            if(Variabel.Kota != ""){
                val intent = Intent(this, TambahActivity::class.java)
                intent.putExtra("kota", Variabel.Kota)
                startActivity(intent)
                finish()
            }
        }

        binding.btnLogout.setOnClickListener {
            firebaseAuth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun getDataGuru(){
        showLoading()
        dbRef = FirebaseDatabase.getInstance().getReference("table_guru")

        dbRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                hideLoading()
                dataList.clear()
                if(snapshot.exists()){
                    for (dataSnap in snapshot.children){
                        val tmpData = dataSnap.getValue(DataGuru::class.java)
                        dataList.add(tmpData!!)
                    }
                    val dataAdapter = DataAdapter(applicationContext, dataList)
                    datarecycleView.adapter = dataAdapter

                    dataAdapter.setOnItemClickListener(object : DataAdapter.OnItemClickListener{
                        override fun onItemClick(position: Int) {
                            val intent = Intent(this@HomeActivity, DetailActivity::class.java)

                            intent.putExtra("id", dataList[position].id)
                            intent.putExtra("namaGuru", dataList[position].nama_guru)
                            intent.putExtra("pendidikan", dataList[position].pendidikan)
                            intent.putExtra("mapel", dataList[position].mapel)
                            intent.putExtra("kota", dataList[position].kota)
                            intent.putExtra("nomorHp", dataList[position].nomor_hp)
                            intent.putExtra("email", dataList[position].email)
                            intent.putExtra("status", dataList[position].status)
                            intent.putExtra("profileUrl", dataList[position].profile_url)
                            startActivity(intent)
                            finish()
                        }
                    })

                    binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
                        override fun onQueryTextSubmit(query: String?): Boolean {
                            return false
                        }

                        override fun onQueryTextChange(newText: String?): Boolean {
                            val searchList = ArrayList<DataGuru>()
                            var isDataFound = false
                            for (dataGuru in dataList){
                                if(dataGuru.nama_guru?.lowercase()?.contains(newText!!.lowercase()) == true ||
                                    dataGuru.mapel?.lowercase()?.contains(newText!!.lowercase()) == true ||
                                    dataGuru.kota?.lowercase()?.contains(newText!!.lowercase()) == true){
                                    searchList.add(dataGuru)
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

            override fun onCancelled(error: DatabaseError) {
                hideLoading()
            }

        })
    }

    private fun getLocation(){
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, object :
            LocationListener {
            override fun onLocationChanged(location: Location) {
                val geocoder = Geocoder(this@HomeActivity, Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (addresses!!.isNotEmpty()) {
                    val cityName = addresses?.get(0)?.adminArea
                    Variabel.Kota = cityName!!
                }
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

            override fun onProviderEnabled(provider: String) {}

            override fun onProviderDisabled(provider: String) {}
        }, null)
    }

    private fun showDataList() {
        binding.emptyDataView.visibility = View.GONE
        binding.rvList.visibility = View.VISIBLE
    }

    private fun showEmptyDataView() {
        binding.rvList.visibility = View.GONE
        binding.emptyDataView.visibility = View.VISIBLE
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }

}