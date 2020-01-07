package com.gornushko.iqbell3

import android.content.Intent
import android.os.Bundle
import android.util.Log.e
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.*

@ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity(), MyListener {

    companion object Const {
        const val START_INFO = "start_d"
        const val START_DATA = "start_x"
    }

    private var goingBack = false
    private lateinit var dialog: AlertDialog
    private lateinit var lastData: ByteArray
    private lateinit var lastTopic: String
    private lateinit var toSend: ByteArray
    private val homeFragment = HomeFragment()
    private val timetableContainerFragment = TimetableContainerFragment()
    private val holidaysContainerFragment = HolidaysContainerFragment()
    private var active: Fragment = homeFragment
    private val fm = supportFragmentManager
    private var edit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar as Toolbar?)
        bottomNavigation.setOnNavigationItemSelectedListener(navListener)
        fm.beginTransaction().add(R.id.fragment_container, holidaysContainerFragment, "3")
            .hide(holidaysContainerFragment).commit()
        fm.beginTransaction().add(R.id.fragment_container, timetableContainerFragment, "2")
            .hide(timetableContainerFragment).commit()
        fm.beginTransaction().add(R.id.fragment_container, homeFragment, "1").commit()
        val info = intent.getByteArrayExtra(START_INFO)!!
        val data = intent.getByteArrayExtra(START_DATA)!!
        toSend = data
        homeFragment.setStartData(info)
        homeFragment.setStartExtraData(data.copyOfRange(0, 48))
        timetableContainerFragment.setStartData(data.copyOfRange(0, 48))
        holidaysContainerFragment.setStartData(data.copyOfRange(48, 112))
        startService(
            intentFor<IQService>(
                IQService.ACTION to IQService.NEW_PENDING_INTENT,
                IQService.PENDING_INTENT to createPendingResult(1, intent, 0)
            )
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        when (active) {
            is HomeFragment -> menuInflater.inflate(R.menu.menu_toolbar, menu)
            is TimetableContainerFragment -> menuInflater.inflate(
                if (edit) R.menu.menu_toolbar_send_edit else R.menu.menu_toolbar_send,
                menu
            )
            is HolidaysContainerFragment -> menuInflater.inflate(
                if (edit) R.menu.menu_toolbar_send_edit else R.menu.menu_toolbar_send,
                menu
            )
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_about -> startActivity(intentFor<AboutActivity>())
            R.id.action_send -> {
                sendData(toSend, "s")
            }
            R.id.action_edit -> when (active) {
                is TimetableContainerFragment -> timetableContainerFragment.edit()
                is HolidaysContainerFragment -> holidaysContainerFragment.edit()
            }
            R.id.action_clear -> when (active) {
                is TimetableContainerFragment -> timetableContainerFragment.clear()
                is HolidaysContainerFragment -> holidaysContainerFragment.clear()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private val navListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.action_home -> {
                fm.beginTransaction().hide(active).show(homeFragment).commit()
                active = homeFragment
            }
            R.id.action_holidays -> {
                fm.beginTransaction().hide(active).show(holidaysContainerFragment).commit()
                active = holidaysContainerFragment
            }
            R.id.action_timetable -> {
                fm.beginTransaction().hide(active).show(timetableContainerFragment).commit()
                active = timetableContainerFragment
            }
        }
        timetableContainerFragment.resetSelectedState()
        holidaysContainerFragment.resetSelectedState()
        noEdit()
        invalidateOptionsMenu()
        return@OnNavigationItemSelectedListener true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            IQService.NEW_INFO -> {
                val state = data!!.getByteArrayExtra(IQService.DATA)!!
                homeFragment.updateData(state)
            }
            IQService.NO_INTERNET, IQService.CONNECTING -> {
                goingBack = true
                startActivity(intentFor<ConnectActivity>(ConnectActivity.KEY to resultCode).newTask().clearTask().clearTop())
            }
            IQService.NEW_DATA -> {
                val extra = data!!.getByteArrayExtra(IQService.DATA)
                homeFragment.updateExtraData(extra!!.copyOfRange(0, 48))
                timetableContainerFragment.updateData(extra.copyOfRange(0, 48))
                holidaysContainerFragment.updateData(extra.copyOfRange(48, 112))
                alert(R.string.data_updated) {
                    okButton {}
                }.show()

            }
            IQService.ERROR -> {
                dialog.dismiss()
                alert(R.string.error_sending) {
                    title = getString(R.string.error)
                    positiveButton(getString(R.string.repeat)) { sendData(lastData, lastTopic) }
                    negativeButton(getString(R.string.cancel)) {}
                }.show()
            }
            IQService.OK -> {
                dialog.dismiss()
                alert(R.string.data_was_sent) {
                    okButton {}
                }.show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!goingBack) startService(intentFor<IQService>(IQService.ACTION to IQService.STOP_SERVICE))
    }

    override fun editData(data: ByteArray, offset: Int) {
        for (i in offset until offset + data.size) {
            toSend[i] = data[i - offset]
        }
        e("MAIN ACTIVITY", "DATA EDITED")
    }

    override fun sendData(data: ByteArray, topic: String) {
        startService(
            intentFor<IQService>(
                IQService.ACTION to IQService.SEND_DATA,
                IQService.DATA to data,
                IQService.TOPIC to topic
            )
        )
        lastData = data
        lastTopic = topic
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setView(R.layout.layout_loading_dialog)
        dialog = builder.create()
        dialog.show()
    }


    override fun noEdit() {
        edit = false
        invalidateOptionsMenu()
    }

    override fun edit() {
        edit = true
        invalidateOptionsMenu()
    }
}
