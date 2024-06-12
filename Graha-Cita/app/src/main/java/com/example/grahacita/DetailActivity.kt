package com.example.grahacita

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.grahacita.databinding.ActivityDetailBinding
import com.example.grahacita.model.DataGuru
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class DetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailBinding
    var statusBooking = ""
    var uri: Uri? = null
    var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDetailBinding.inflate(layoutInflater)
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

        setValueViews()
        initButton()
    }

    private fun initButton() {
        binding.imageProfile.setOnClickListener {
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setTitle("Ganti Foto Profil")
            alertDialogBuilder.setMessage("Apakah Anda ingin mengganti foto profil?")
            alertDialogBuilder.setPositiveButton("Ya") { _, _ ->
                startActivityForResult(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI), 100)
            }
            alertDialogBuilder.setNegativeButton("Tidak") { dialog, _ ->
                dialog.dismiss()
            }
            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()
        }

        binding.btnUbah.setOnClickListener {
            val nama = binding.txtNama.text.toString()
            val pendidikan = binding.txtPendidikan.text.toString()
            val mapel = binding.txtMapel.text.toString()
            val kota = binding.txtKota.text.toString()
            val nomorHp = binding.txtNoHp.text.toString()
            val email = binding.txtEmail.text.toString()

            if (nama.isEmpty() || pendidikan.isEmpty() || mapel.isEmpty() || kota.isEmpty() || nomorHp.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Mohon lengkapi semua kolom", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else{
                ubahData(
                    intent.getStringExtra("id")!!,
                    binding.txtNama.text.toString(),
                    binding.txtPendidikan.text.toString(),
                    binding.txtMapel.text.toString(),
                    binding.txtKota.text.toString(),
                    binding.txtNoHp.text.toString(),
                    binding.txtEmail.text.toString(),
                    imageUri
                )
            }

        }

        binding.btnDelete.setOnClickListener {
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setTitle("Hapus Data")
            alertDialogBuilder.setMessage("Apakah Anda yakin ingin menghapus data ini?")
            alertDialogBuilder.setPositiveButton("Ya") { _, _ ->
                hapusData(intent.getStringExtra("id")!!, intent.getStringExtra("profileUrl")!!)
            }
            alertDialogBuilder.setNegativeButton("Tidak") { dialog, _ ->
                dialog.dismiss()
            }
            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()
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
//                binding.txtNameFile.setText(path)
                binding.imageProfile.setImageURI(uri)
                imageUri = uri
            } else {
                Toast.makeText(this, "Format Gambar Harus : JPG, JPEG, PNG, DAN WEBP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun ubahData(
        id: String,
        nama: String,
        pendidikan: String,
        mapel: String,
        kota:String,
        nomorHp:String,
        email:String,
        newImageUri: Uri?
    ) {
        val progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Mengubah Data!")
        progressDialog.setMessage("Sedang Memproses")
        progressDialog.show()


        val storageRef = FirebaseStorage.getInstance().getReference()
        val dbRef = FirebaseDatabase.getInstance().getReference("table_guru").child(id)

        if (newImageUri != null) {
            val imageRef = storageRef.child("profileImages").child(id)

            imageRef.putFile(newImageUri)
                .addOnSuccessListener { taskSnapshot ->
                    progressDialog.dismiss()

                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        val dataGuru = DataGuru(id, nama, pendidikan, mapel, kota, nomorHp, email, statusBooking, uri.toString())
                        dbRef.setValue(dataGuru)
                            .addOnSuccessListener {
                                if (!intent.getStringExtra("profileUrl").isNullOrEmpty()) {
                                    storageRef.child(intent.getStringExtra("profileUrl")!!).delete()
                                }
                                Toast.makeText(this, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Gagal memperbarui data: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(this, "Gagal mengunggah gambar baru: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            val dataGuru = DataGuru(id, nama, pendidikan, mapel, kota, nomorHp, email, statusBooking, intent.getStringExtra("profileUrl")!!)
            dbRef.setValue(dataGuru)
                .addOnSuccessListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(this, "Gagal memperbarui data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun hapusData(id: String, profileUrl: String) {
        val progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Menghapus Data!")
        progressDialog.setMessage("Sedang Memproses")
        progressDialog.show()

        val databaseRef = FirebaseDatabase.getInstance().getReference("table_guru").child(id)
        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(profileUrl)

        databaseRef.removeValue()
            .addOnSuccessListener {
                storageRef.delete()
                    .addOnSuccessListener {
                        progressDialog.dismiss()
                        Toast.makeText(this, "Data dan gambar berhasil dihapus", Toast.LENGTH_SHORT).show()
                        onBackPressed()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal menghapus gambar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Gagal menghapus data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun setValueViews(){
        val status = intent.getStringExtra("status")
        val spinner: Spinner = findViewById(R.id.spinnerStatus)
        val statusOptions = arrayOf("Sudah Dibooking", "Belum Dibooking")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        when (status) {
            "Sudah Dibooking" -> spinner.setSelection(0)
            "Belum Dibooking" -> spinner.setSelection(1)
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedStatus = statusOptions[position]
                statusBooking = selectedStatus
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }


        binding.txtNama.setText(intent.getStringExtra("namaGuru"))
        binding.txtPendidikan.setText(intent.getStringExtra("pendidikan"))
        binding.txtMapel.setText(intent.getStringExtra("mapel"))
        binding.txtKota.setText(intent.getStringExtra("kota"))
        binding.txtNoHp.setText(intent.getStringExtra("nomorHp"))
        binding.txtEmail.setText(intent.getStringExtra("email"))
        loadImageFromUrl(intent.getStringExtra("profileUrl")!!) { bitmap ->
            Handler(Looper.getMainLooper()).post {
                binding.imageProfile.setImageBitmap(bitmap)
            }
        }
    }

    private fun loadImageFromUrl(urlString: String, callback: (Bitmap?) -> Unit) {
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
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                connection?.disconnect()
            }
            callback(bitmap)
        }
        thread.start()
    }
}