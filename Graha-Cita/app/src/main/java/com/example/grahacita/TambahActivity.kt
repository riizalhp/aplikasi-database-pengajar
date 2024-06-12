package com.example.grahacita

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.TextUtils
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.grahacita.databinding.ActivityTambahBinding
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.Locale

class TambahActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTambahBinding

    var uri: Uri? = null
    var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTambahBinding.inflate(layoutInflater)
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

        binding.buttonBrowse.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI), 100)
        }


        binding.txtKota.setText(intent.getStringExtra("kota"))
//        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
//
//        if (ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.ACCESS_FINE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.ACCESS_COARSE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//
//            return
//        }
//        locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, object :
//            LocationListener {
//            override fun onLocationChanged(location: Location) {
//                val geocoder = Geocoder(this@TambahActivity, Locale.getDefault())
//                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
//                Log.e("location", addresses.toString())
//                if (addresses!!.isNotEmpty()) {
//                    val cityName = addresses?.get(0)?.adminArea
//
//                    binding.txtKota.setText(cityName)
//                }
//            }
//
//            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
//
//            override fun onProviderEnabled(provider: String) {}
//
//            override fun onProviderDisabled(provider: String) {}
//        }, null)

        binding.buttonUpload.setOnClickListener {
            val nama = binding.txtnama.text.toString()
            val pendidikan = binding.txtPendidikan.text.toString()
            val mapel = binding.txtMapel.text.toString()
            val kota = binding.txtKota.text.toString()
            val nomorHp = binding.txtNoHp.text.toString()
            val email = binding.txtEmail.text.toString()

            if (nama.isEmpty() || pendidikan.isEmpty() || mapel.isEmpty() || kota.isEmpty() || nomorHp.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Mohon lengkapi semua kolom", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Format email tidak valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!TextUtils.isDigitsOnly(nomorHp)) {
                Toast.makeText(this, "Nomor HP harus berupa angka", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(imageUri != null){
                tambahData()
            }else{
                Toast.makeText(this, "Please select an image file", Toast.LENGTH_SHORT).show()
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            uri = data?.data
            var path = ""
            uri?.let {
                val cursor = contentResolver.query(it, null, null, null, null)
                cursor?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val column = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                        path = cursor.getString(column)
                    }
                }
            }

            if (path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png") || path.endsWith(".webp")) {
                binding.txtNameFile.setText(path)
                imageUri = uri
            } else {
                Toast.makeText(this, "Format Gambar Harus : JPG, JPEG, PNG, DAN WEBP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun tambahData(){
        if(imageUri != null){
            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle("Menambahkan Data!")
            progressDialog.setMessage("Sedang Memproses")
            progressDialog.show()

            val databaseRef = FirebaseDatabase.getInstance().getReference("table_guru").push()
            val idData = databaseRef.key!!
            val ref = FirebaseStorage.getInstance().getReference("profileImages").child(idData)
            ref.putFile(imageUri!!).addOnSuccessListener {
                progressDialog.dismiss()

                ref.downloadUrl.addOnSuccessListener { uri ->
                    // Simpan informasi di Realtime Database
                    val dataMap = HashMap<String, Any>()
                    dataMap["id"] = idData
                    dataMap["nama_guru"] = binding.txtnama.text.toString()
                    dataMap["pendidikan"] = binding.txtPendidikan.text.toString()
                    dataMap["mapel"] = binding.txtMapel.text.toString()
                    dataMap["kota"] = binding.txtKota.text.toString()
                    dataMap["nomor_hp"] = binding.txtNoHp.text.toString()
                    dataMap["email"] = binding.txtEmail.text.toString()
                    dataMap["status"] = "Belum Dibooking"
                    dataMap["profile_url"] = uri.toString()

                    databaseRef.setValue(dataMap).addOnSuccessListener {
                        Toast.makeText(this, "Data Berhasil Ditambahkan", Toast.LENGTH_LONG).show()
                        clearFields()
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to Upload Data: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }.addOnFailureListener{
                progressDialog.dismiss()
                Toast.makeText(this, "Gagal Menambahkan Data", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun clearFields() {
        binding.txtNameFile.setText("")
        binding.txtnama.setText("")
        binding.txtPendidikan.setText("")
        binding.txtMapel.setText("")
        binding.txtKota.setText("")
        binding.txtNoHp.setText("")
        binding.txtEmail.setText("")
    }

}