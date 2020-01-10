package com.gornushko.iqbell3


import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_connect.view.logout
import kotlinx.android.synthetic.main.fragment_settings.view.*
import org.jetbrains.anko.sdk27.coroutines.onClick

@ExperimentalUnsignedTypes
class SettingsFragment : Fragment() {

    private lateinit var listener: MyListener
    private var offset: Byte? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as MyListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        view.logout.onClick { listener.logout() }
        val list = ArrayList<String>()
        for (i in 2..12) {
            list.add("GMT+$i")
        }
        val adapter =
            ArrayAdapter<String>(context!!, android.R.layout.simple_spinner_dropdown_item, list)
        view.spinner.adapter = adapter
        view.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                listener.editData(byteArrayOf(position.toByte()), 112)
            }

        }
        return view
    }

    fun setStartData(i: Byte) {
        offset = i
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        super.onResume()
        updateView()
    }

    fun updateData(i: Byte) {
        offset = i
        updateView()
    }

    @SuppressLint("SetTextI18n")
    fun updateView() {
        view?.current_timezone?.text =
            "${getString(R.string.current_timezone)}: GMT+${offset!!.toInt() + 2}"
        view?.spinner?.setSelection(offset!!.toInt())
    }
}