package com.example.taller_3.Logica

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taller_3.Data.Datos
import com.example.taller_3.R
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import org.json.JSONObject
import java.io.InputStream

data class LocationData(val latitude: Double, val longitude: Double, val name: String)

class Inicio : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapa: SupportMapFragment
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var isCameraMoved = false

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
}