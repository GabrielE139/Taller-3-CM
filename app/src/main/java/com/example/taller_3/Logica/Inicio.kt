package com.example.taller_3.Logica

import android.Manifest
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.MatrixCursor
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taller_3.Data.Datos
import com.example.taller_3.Data.Usuario
import com.example.taller_3.R
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.json.JSONObject
import java.io.InputStream

data class LocationData(val latitude: Double, val longitude: Double, val name: String)

class Inicio : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapa: SupportMapFragment
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var isCameraMoved = false
    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance()
    private lateinit var myRef: DatabaseReference
    private val PATH_USERS = "users/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)



        mapa = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapa.view?.visibility = View.GONE

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        pedirPermiso(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), "se necesita de este permiso", Datos.MY_PERMISSION_REQUEST_LOC)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if(location != null) {
                    val locationUser = LatLng(location.latitude, location.longitude)
                    mMap.addMarker(
                        MarkerOptions()
                            .position(locationUser)
                            .title("Estás aquí")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    )
                    if (!isCameraMoved) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationUser, 15f))
                        isCameraMoved = true
                    }
                }
            }
        }
        escucharCambiosdeDisponibilidad()
    }

    private fun pedirPermiso(context: Activity, permisos: Array<String>, justificacion: String, idCode: Int) {
        val permisosNoConcedidos = permisos.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permisosNoConcedidos.isNotEmpty()) {
            if (permisosNoConcedidos.any { ActivityCompat.shouldShowRequestPermissionRationale(context, it) }) {
                // Mostrar justificación adicional si es necesario.
            }
            ActivityCompat.requestPermissions(context, permisosNoConcedidos.toTypedArray(), idCode)
        } else {
            mapa.view?.visibility = View.VISIBLE
            setLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            Datos.MY_PERMISSION_REQUEST_LOC -> {
                var permisosConcedidos = true
                for (i in grantResults.indices) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        permisosConcedidos = false
                        break
                    }
                }

                if (permisosConcedidos) {
                    Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT).show()
                    mapa.view?.visibility = View.VISIBLE
                    setLocation()
                } else {
                    Toast.makeText(this, "Permisos denegados", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                // Manejar otras solicitudes de permisos si es necesario.
            }
        }
    }

    private fun setLocation() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        // Add markers for points of interest
        val locations = readLocationsFromJson(this)
        for (location in locations) {
            val position = LatLng(location.latitude, location.longitude)
            mMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(location.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
        }

        // Add marker for user's current location
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

        fusedLocationClient.requestLocationUpdates(
            LocationRequest.create().apply {
                interval = 10000
                fastestInterval = 5000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            },
            locationCallback,
            null
        )
    }

    fun readLocationsFromJson(context: Context): List<LocationData> {
        val locations = mutableListOf<LocationData>()
        val inputStream: InputStream = context.assets.open("locations.json")
        val json = inputStream.bufferedReader().use { it.readText() }
        val jsonArray = JSONObject(json).getJSONArray("locationsArray")

        for (i in 0 until jsonArray.length()) {
            val jsonObject: JSONObject = jsonArray.getJSONObject(i)
            val lat = jsonObject.getDouble("latitude")
            val lng = jsonObject.getDouble("longitude")
            val name = jsonObject.getString("name")
            locations.add(LocationData(lat, lng, name))
        }

        return locations
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.menuLogOut -> {

                FirebaseApp.initializeApp(this)
                auth = FirebaseAuth.getInstance()
                auth.signOut()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                true
            }
            R.id.EstablecerEstado -> {
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    val database = FirebaseDatabase.getInstance().reference
                    val userStatusRef = database.child("users").child(userId).child("disponible")

                    userStatusRef.get().addOnSuccessListener { snapshot ->
                        val isAvailable = snapshot.getValue(Boolean::class.java) ?: false
                        val newStatus = !isAvailable
                        userStatusRef.setValue(newStatus).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val message = if (newStatus) {
                                    "Ahora estás disponible"
                                } else {
                                    "Ahora estás desconectado"
                                }
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Error al actualizar el estado", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                true
            }
            R.id.IrAUsuarios -> {
                val intent = Intent(this, UsuariosDisponibles::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    //inflate para el menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.activity_menu, menu)
        return true
    }


    fun escucharCambiosdeDisponibilidad() {
        myRef = database.getReference(PATH_USERS)
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val cursor = MatrixCursor(arrayOf("_id", "nombre", "apellido", "imagen", "email", "latitud", "longitud"))
                var i = 0

                for (singleSnapshot in dataSnapshot.children) {
                    val myUser = singleSnapshot.getValue(Usuario::class.java)
                    if (myUser != null) {
                        val userKey = singleSnapshot.key
                        val isAvailable = myUser.disponible
                        if (isAvailable) {
                            cursor.addRow(
                                arrayOf(
                                    i,
                                    myUser.nombre,
                                    myUser.apellido,
                                    myUser.imageUrl,
                                    myUser.email,
                                    myUser.latitud,
                                    myUser.longitud
                                )
                            )
                            Toast.makeText(
                                this@Inicio, "${myUser.nombre} se ha conectado", Toast.LENGTH_SHORT).show()
                            i++
                        } else {
                            Toast.makeText(
                                this@Inicio, "${myUser.nombre} se ha desconectado", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "error en la consulta", databaseError.toException())
            }
        })
    }
}