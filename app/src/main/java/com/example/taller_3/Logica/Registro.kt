package com.example.taller_3.Logica

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.taller_3.Data.Datos
import com.example.taller_3.R
import com.example.taller_3.Data.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class Registro : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference
    private lateinit var nombreEditText: EditText
    private lateinit var apellidoEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var idEditText: EditText
    private lateinit var latitudEditText: EditText
    private lateinit var longitudEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var currentPhotoPath: String
    private lateinit var botonImagen: ImageButton
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        pedirPermiso(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE), "Se necesita este permiso", Datos.MY_PERMISSION_REQUEST_CAMERA)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

        nombreEditText = findViewById(R.id.editTextNombre)
        apellidoEditText = findViewById(R.id.editTextApellido)
        emailEditText = findViewById(R.id.editTextCorreo)
        passwordEditText = findViewById(R.id.editTextPassword)
        idEditText = findViewById(R.id.editTextIdetificacion)
        latitudEditText = findViewById(R.id.editTextLatitud)
        longitudEditText = findViewById(R.id.editTextLonguitud)
        registerButton = findViewById(R.id.buttonLogin)
        botonImagen = findViewById(R.id.imageButton)

        botonImagen.setOnClickListener {
            escogerImagen()
        }

        registerButton.setOnClickListener {
            if (validateForm()) {
                val email = emailEditText.text.toString()
                val password = passwordEditText.text.toString()
                val nombre = nombreEditText.text.toString()
                val apellido = apellidoEditText.text.toString()
                val id = idEditText.text.toString().toDouble()
                val latitud = latitudEditText.text.toString().toDouble()
                val longitud = longitudEditText.text.toString().toDouble()
                createUserWithImage(email,password,nombre,apellido,id,latitud,longitud)
            }
        }
    }

    private fun createUserWithImage(email: String, password: String, nombre: String, apellido: String, id: Double, latitud: Double, longitud: Double) {
    if (password.length < 6) {
        Toast.makeText(this, "La contraseña debe tener por lo menos 6 caracteres", Toast.LENGTH_SHORT).show()
        return
    }

    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Log.d("FirebaseRegister", "createUserWithEmail:success")
                val user = auth.currentUser
                if (user != null) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName("$nombre $apellido")
                        .build()
                    user.updateProfile(profileUpdates)

                    val imageRef = storageRef.child("images/${user.uid}.jpg")
                    imageUri?.let {
                        imageRef.putFile(it)
                            .addOnSuccessListener {
                                imageRef.downloadUrl.addOnSuccessListener { uri ->
                                    val usuario = Usuario(nombre, apellido, email, password, id, latitud, longitud, uri.toString(), true)
                                    database.child("users").child(user.uid).setValue(usuario)
                                        .addOnCompleteListener { dbTask ->
                                            if (dbTask.isSuccessful) {
                                                Toast.makeText(baseContext, "User created successfully.", Toast.LENGTH_SHORT).show()
                                                updateUI(user)
                                            } else {
                                                Toast.makeText(baseContext, "Database update failed.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Image upload failed.", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            } else {
                Log.w("FirebaseRegister", "createUserWithEmail:failure", task.exception)
                Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
            }
        }
}

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            val intent = Intent(this, Inicio::class.java)
            intent.putExtra("user", currentUser.email)
            startActivity(intent)
        } else {
            nombreEditText.setText("")
            apellidoEditText.setText("")
            emailEditText.setText("")
            passwordEditText.setText("")
            idEditText.setText("")
            latitudEditText.setText("")
            longitudEditText.setText("")
        }
    }

    private fun validateForm(): Boolean {
        var valid = true
        val email = emailEditText.text.toString()
        if (TextUtils.isEmpty(email)) {
            emailEditText.error = "Required."
            valid = false
        } else if (!isEmailValid(email)) {
            emailEditText.error = "Invalid email."
            valid = false
        } else {
            emailEditText.error = null
        }

        val password = passwordEditText.text.toString()
        if (TextUtils.isEmpty(password)) {
            passwordEditText.error = "Required."
            valid = false
        } else {
            passwordEditText.error = null
        }

        val nombre = nombreEditText.text.toString()
        if (TextUtils.isEmpty(nombre)) {
            nombreEditText.error = "Required."
            valid = false
        } else {
            nombreEditText.error = null
        }

        val apellido = apellidoEditText.text.toString()
        if (TextUtils.isEmpty(apellido)) {
            apellidoEditText.error = "Required."
            valid = false
        } else {
            apellidoEditText.error = null
        }

        val id = idEditText.text.toString()
        if (TextUtils.isEmpty(id)) {
            idEditText.error = "Required."
            valid = false
        } else {
            idEditText.error = null
        }

        val latitud = latitudEditText.text.toString()
        if (TextUtils.isEmpty(latitud)) {
            latitudEditText.error = "Required."
            valid = false
        } else {
            latitudEditText.error = null
        }

        val longitud = longitudEditText.text.toString()
        if (TextUtils.isEmpty(longitud)) {
            longitudEditText.error = "Required."
            valid = false
        } else {
            longitudEditText.error = null
        }

        if (imageUri == null) {
            Toast.makeText(this, "Image is required.", Toast.LENGTH_SHORT).show()
            valid = false
        }

        return valid
    }

    private fun isEmailValid(email: String): Boolean {
        return email.contains("@") && email.contains(".") && email.length >= 5
    }

    private fun escogerImagen(){
        val options = arrayOf("Tomar foto", "Seleccionar de galería")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Seleccionar opción")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> openCamera() // Abrir cámara
                1 -> openGallery() // Abrir galería
            }
        }
        builder.show()
    }

    fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            null
        }
        photoFile?.also {
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "com.example.taller_3.fileprovider",
                it
            )
            galleryAddPic()
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(intent, Datos.MY_PERMISSION_REQUEST_CAMERA)
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun galleryAddPic() {
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
            val f = File(currentPhotoPath)
            val contentUri: Uri = Uri.fromFile(f)
            mediaScanIntent.data = contentUri
            sendBroadcast(mediaScanIntent)
        }
    }

    fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, Datos.MY_PERMISSION_REQUEST_GALLERY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val botonImagen = findViewById<ImageButton>(R.id.imageButton)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                Datos.MY_PERMISSION_REQUEST_CAMERA -> {
                    val file = File(currentPhotoPath)
                    if (file.exists()) {
                        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, Uri.fromFile(file))
                        botonImagen.setImageBitmap(bitmap)
                        imageUri = Uri.fromFile(file)
                    }
                }
                Datos.MY_PERMISSION_REQUEST_GALLERY -> {
                    val selectedImageUri = data?.data
                    if (selectedImageUri != null) {
                        botonImagen.setImageURI(selectedImageUri)
                        imageUri = selectedImageUri
                    }
                }
            }
        }
    }

    private fun pedirPermiso(context: Activity, permisos: Array<String>, justificacion: String, idCode: Int) {
        val permisosNoConcedidos = permisos.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permisosNoConcedidos.isNotEmpty()) {
            if (permisosNoConcedidos.any { ActivityCompat.shouldShowRequestPermissionRationale(context, it) }) {
            }
            ActivityCompat.requestPermissions(context, permisosNoConcedidos.toTypedArray(), idCode)
        } else {
            val botonImagen = findViewById<ImageButton>(R.id.imageButton)
            botonImagen.isEnabled = true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (arePermissionsGranted(grantResults)) {
            when (requestCode) {
                Datos.MY_PERMISSION_REQUEST_CAMERA -> {
                    Toast.makeText(this, "Permisos para la cámara concedidos", Toast.LENGTH_SHORT).show()
                    val botonImagen = findViewById<ImageButton>(R.id.imageButton)
                    botonImagen.isEnabled = true
                }
                Datos.MY_PERMISSION_REQUEST_GALLERY -> {
                    Toast.makeText(this, "Permisos para la galería concedidos", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // Otros códigos de permiso
                }
            }
        } else {
            when (requestCode) {
                Datos.MY_PERMISSION_REQUEST_CAMERA -> {
                    Toast.makeText(this, "Permisos para la cámara denegados", Toast.LENGTH_SHORT).show()
                }
                Datos.MY_PERMISSION_REQUEST_GALLERY -> {
                    Toast.makeText(this, "Permisos para la galería denegados", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // Otros permisos denegados
                }
            }
        }
    }

    private fun arePermissionsGranted(grantResults: IntArray): Boolean {
        return grantResults.all { it == PackageManager.PERMISSION_GRANTED }
    }
}