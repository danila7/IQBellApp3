package com.gornushko.iqbell3

interface MyFragmentListener {
    fun sendData(data: ByteArray, topic: String)
    fun noEdit()
    fun edit()
}