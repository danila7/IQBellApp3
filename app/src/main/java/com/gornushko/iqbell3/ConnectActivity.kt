package com.gornushko.iqbell3

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import kotlinx.android.synthetic.main.activity_connect.*
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick

@SuppressLint("ApplySharedPref")
@ExperimentalUnsignedTypes
class ConnectActivity : AppCompatActivity() {

    private lateinit var loginData: SharedPreferences

    companion object Const {
        const val KEY = "key"
        const val LOGIN_DATA = "logindata"
        const val URL_N_PORT = "adrp"
        const val USERNAME = "nm"
        const val PASSWORD = "pass"
    }

    private var connected = false

    private fun connected(info: ByteArray, data: ByteArray) {
        connected = true
        startActivity(
            intentFor<MainActivity>(
                MainActivity.START_INFO to info,
                MainActivity.START_DATA to data
            ).newTask().clearTask().clearTop()
        )
    }

    private fun reconnecting() {
        status.text = getText(R.string.no_connection)
    }

    @ExperimentalUnsignedTypes
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)
        setSupportActionBar(toolbar as Toolbar?)
        login.onClick { login() }
        logout.onClick { logout() }
        loginData = getSharedPreferences(LOGIN_DATA, Context.MODE_PRIVATE)
        if (loginData.contains(URL_N_PORT)) {
            status.text = getString(R.string.loading)
            progress.visibility = View.VISIBLE
            logout.visibility = View.VISIBLE
            addressLayout.visibility = View.GONE
            portLayout.visibility = View.GONE
            usernameLayout.visibility = View.GONE
            passwordLayout.visibility = View.GONE
            login.visibility = View.GONE
            when (intent.extras?.getInt(KEY)) {
                IQService.NO_INTERNET -> {
                    startService(
                        intentFor<IQService>(
                            IQService.ACTION to IQService.NEW_PENDING_INTENT,
                            IQService.PENDING_INTENT to createPendingResult(1, intent, 0)
                        )
                    )
                    reconnecting()
                }
                else -> startService(
                    intentFor<IQService>(
                        IQService.ACTION to IQService.START,
                        IQService.PENDING_INTENT to createPendingResult(1, intent, 0)
                    )
                )
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_about -> startActivity(intentFor<AboutActivity>())
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!connected) {
            startService(
                Intent(this, IQService::class.java).putExtra(
                    IQService.ACTION,
                    IQService.STOP_SERVICE
                )
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            IQService.NO_INTERNET -> reconnecting()
            IQService.CONNECTING -> connecting()
            IQService.CONNECTED -> connected(
                data!!.getByteArrayExtra(IQService.INFO)!!,
                data.getByteArrayExtra(IQService.DATA)!!
            )
        }
    }

    private fun connecting() {
        status.text = getText(R.string.loading)
    }

    private fun login() {
        val usr = username.text.toString()
        val pass = password.text.toString()
        val addr = address.text.toString()
        val pr = port.text.toString()
        if (usr.isNotBlank() and pass.isNotBlank() and addr.isNotBlank() and pr.isNotBlank()) {
            if (Patterns.WEB_URL.matcher(addr).matches()) {
                status.text = getString(R.string.loading)
                progress.visibility = View.VISIBLE
                logout.visibility = View.VISIBLE
                addressLayout.visibility = View.GONE
                portLayout.visibility = View.GONE
                usernameLayout.visibility = View.GONE
                passwordLayout.visibility = View.GONE
                login.visibility = View.GONE
                val ed = loginData.edit()
                ed.putString(URL_N_PORT, "tcp://$addr:$pr")
                ed.putString(USERNAME, usr)
                ed.putString(PASSWORD, pass)
                ed.commit()
                startService(
                    intentFor<IQService>(
                        IQService.ACTION to IQService.START,
                        IQService.PENDING_INTENT to createPendingResult(1, intent, 0)
                    )
                )

            } else toast(getString(R.string.not_valid_url_warning))
        } else toast(getString(R.string.not_empty_warning))
    }


    private fun logout() {
        startService(
            Intent(this, IQService::class.java).putExtra(
                IQService.ACTION,
                IQService.STOP_SERVICE
            )
        )
        val ed = loginData.edit()
        ed.clear()
        ed.commit()
        status.text = getString(R.string.welcome)
        progress.visibility = View.GONE
        logout.visibility = View.GONE
        addressLayout.visibility = View.VISIBLE
        portLayout.visibility = View.VISIBLE
        usernameLayout.visibility = View.VISIBLE
        passwordLayout.visibility = View.VISIBLE
        login.visibility = View.VISIBLE
    }
}
