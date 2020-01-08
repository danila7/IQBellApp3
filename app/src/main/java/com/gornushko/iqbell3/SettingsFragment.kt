package com.gornushko.iqbell3


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_connect.view.*
import org.jetbrains.anko.sdk27.coroutines.onClick

@ExperimentalUnsignedTypes
class SettingsFragment : Fragment() {

    private lateinit var listener: MyListener

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
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        super.onResume()
    }
}