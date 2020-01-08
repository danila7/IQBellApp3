package com.gornushko.iqbell3

interface MyListener {
    fun editData(data: ByteArray, offset: Int)
    fun sendData(data: ByteArray, topic: String)
    fun noEdit()
    fun edit()
    fun logout()
}