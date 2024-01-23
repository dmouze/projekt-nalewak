package com.kierman.projektnalewak.util

data class UserModel(
    val id: String? = "",
    val name: String? = "",
    val time: List<Double>? = emptyList()
)