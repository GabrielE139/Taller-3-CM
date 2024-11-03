package com.example.taller_3.Data

data class Usuario(
    var nombre: String,
    var apellido: String,
    var email: String,
    var password: String,
    var num_id: Double,
    var latitud: Double,
    var longitud: Double,
    var imageUrl: String,
    var disponible: Boolean){

    constructor() : this("", "", "", "", 0.0, 0.0, 0.0, "", true)
}
