package com.example.taller_3.Logica

import android.content.ContentValues.TAG
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taller_3.Data.Usuario
import com.example.taller_3.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UsuariosDisponibles : AppCompatActivity() {

    var mCursor: Cursor? = null
    var mUsuariosAdapter: UsuariosAdapter? = null
    var mlista: ListView? = null
    val PATH_USERS = "users/"

    private val database = FirebaseDatabase.getInstance()
    private lateinit var myRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usuarios_disponibles)
        initView()
    }

    fun initView() {
        mlista = findViewById(R.id.lista)
        mCursor = createCursor()
        mUsuariosAdapter = UsuariosAdapter(this, mCursor!!)
        mlista?.adapter = mUsuariosAdapter
    }

    private fun createCursor() : MatrixCursor {
        val cursor = MatrixCursor(arrayOf("_id","nombre", "apellido", "imagen", "email", "latitud", "longitud"))
        var i = 0;
        myRef = database.getReference(PATH_USERS)
        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    val myUser = singleSnapshot.getValue(Usuario::class.java)
                    if(myUser!!.disponible){
                        cursor.addRow(arrayOf(
                            i,
                            myUser.nombre,
                            myUser.apellido,
                            myUser.imageUrl,
                            myUser.email,
                            myUser.latitud,
                            myUser.longitud
                        ))
                        i++
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "error en la consulta", databaseError.toException())
            }
        })
        return cursor
    }


}