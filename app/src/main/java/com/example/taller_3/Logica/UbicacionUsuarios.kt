package com.example.taller_3.Logica

import android.Manifest
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taller_3.Data.Datos
import com.example.taller_3.Data.Usuario
import com.example.taller_3.R

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.taller_3.databinding.ActivityUbicacionUsuariosBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UbicacionUsuarios : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityUbicacionUsuariosBinding

    private var positionMarker: Marker? = null
    private var otherMarker: Marker? = null

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback

    private var latitudUsuario = 0.0
    private var longitudUsuario = 0.0
    private var longitud = 0.0
    private var latitud = 0.0
    private var lastLatitude: Double? = null
    private var lastLongitude: Double? = null
    private var lastPolyline: Polyline? = null


    val PATH_USERS = "users/"
    private val database = FirebaseDatabase.getInstance()
    private lateinit var myRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUbicacionUsuariosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationRequest = createLocationRequest()

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                Log.i("LOCATION", "Location update in the callback: $location")

            }
        }

        var bundle = intent.getBundleExtra("bundle")!!
        longitudUsuario = bundle.getString("longitud")!!.toDouble()
        latitudUsuario = bundle.getString("latitud")!!.toDouble()

        pedirPermiso(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION) , "se necesita de este permiso", Datos.MY_PERMISSION_REQUEST_LOC)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true


    }

    private fun createLocationRequest(): LocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,10000).apply{
        setMinUpdateIntervalMillis(5000)
    }.build()

    fun setLocation(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        mFusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            Log.i("LOCATION", "onSuccess location")
            if (location != null) {
                Log.i("LOCATION", "Longitud: " + location.longitude)
                Log.i("LOCATION", "Latitud: " + location.latitude)

                // Actualiza las coordenadas
                longitud = location.longitude
                latitud = location.latitude

                otherLocation()

                // Verifica si el mapa está listo para usar
                if (::mMap.isInitialized) {
                    // Crea una nueva posición
                    val nuevaPosicion = LatLng(latitud, longitud)
                    val posUsuario = LatLng(latitudUsuario, longitudUsuario)

                    if (positionMarker != null && otherMarker != null) {
                        positionMarker?.position = nuevaPosicion
                        otherMarker?.position = posUsuario
                    } else {
                        // Si no existe, crea un nuevo marcador
                        positionMarker = mMap.addMarker(MarkerOptions().position(nuevaPosicion).title("Estás aquí").icon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_GREEN)) )
                        otherMarker = mMap.addMarker(MarkerOptions().position(posUsuario).title("El otro usuario está aquí"))
                    }

                    lastPolyline?.remove()

                    // Agrega la nueva línea y guarda una referencia
                    lastPolyline = mMap.addPolyline(
                        PolylineOptions()
                            .add(nuevaPosicion, posUsuario)
                            .width(5f)
                            .color(Color.BLUE)
                    )
                    // Mueve la cámara a la nueva posición
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nuevaPosicion, 15f))
                }
            }
        }

    }

    private fun otherLocation() {
        val bundle = intent.getBundleExtra("bundle")!!
        val email = bundle.getString("email")!!
        myRef = database.getReference(PATH_USERS)
        val query = myRef.orderByChild("email").equalTo(email)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    val myUser = singleSnapshot.getValue(Usuario::class.java)

                    val newLatitude = myUser?.latitud
                    val newLongitude = myUser?.longitud

                    // Solo actualiza si hay cambios en latitud o longitud
                    if (newLatitude != lastLatitude || newLongitude != lastLongitude) {
                        lastLatitude = newLatitude
                        lastLongitude = newLongitude

                        if (newLatitude != null && newLongitude != null) {
                            latitudUsuario = newLatitude
                            longitudUsuario = newLongitude
                            updateMapWithNewPosition()
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "error en la consulta", databaseError.toException())
            }
        })
    }

    private fun updateMapWithNewPosition() {
        if (::mMap.isInitialized) {
            val nuevaPosicion = LatLng(latitud, longitud)
            val posUsuario = LatLng(latitudUsuario, longitudUsuario)

            if (positionMarker != null && otherMarker != null) {
                positionMarker?.position = nuevaPosicion
                otherMarker?.position = posUsuario
            } else {
                positionMarker = mMap.addMarker(MarkerOptions().position(nuevaPosicion).title("Estás aquí").icon(BitmapDescriptorFactory.defaultMarker(
                    BitmapDescriptorFactory.HUE_GREEN)) )
                otherMarker = mMap.addMarker(MarkerOptions().position(posUsuario).title("El otro usuario está aquí"))
            }

            lastPolyline?.remove()

            // Agrega la nueva línea y guarda una referencia
            lastPolyline = mMap.addPolyline(
                PolylineOptions()
                    .add(nuevaPosicion, posUsuario)
                    .width(5f)
                    .color(Color.BLUE)
            )
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nuevaPosicion, 15f))
        }
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
}