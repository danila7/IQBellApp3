package com.gornushko.iqbell3

import android.app.TimePickerDialog
import kotlin.experimental.or

@ExperimentalUnsignedTypes
class TimetableFragment(listener2: MyFragmentListener) : MyListFragment(listener2) {

    override var stringData = Array(24) { " " }


    override fun updateView() {
        for (i in 0..23) {
            val t = byteData[i]
            if (t.toUByte() > 127.toUByte()) stringData[i] = "${i + 1}."
            else stringData[i] =
                "${i + 1}.   ${print2digits(t / 12 + 8)}:${print2digits(t % 12 * 5)}"
        }
        adapter?.notifyDataSetChanged()
    }

    private fun print2digits(num: Int) = if (num < 10) "0$num" else "$num"

    override fun clear() {
        byteData[adapter?.selectedItem!!] =
            byteData[adapter?.selectedItem!!] or 0x80.toUByte().toByte()
        listener2.editData(byteArrayOf(byteData[adapter?.selectedItem!!]), adapter?.selectedItem!!)
        updateView()
    }

    override fun edit() {
        val tt = (byteData[adapter?.selectedItem!!].toUByte() and 0x7F.toUByte()).toInt()
        TimePickerDialog(activity!!, R.style.ThemeOverlay_AppCompat_Dialog,
            TimePickerDialog.OnTimeSetListener { _, mHour, mMinute ->
                run {
                    val t = (((mHour - 8) * 12) + (mMinute / 5)).toUByte()
                    if (t < 128.toUByte()) {
                        byteData[adapter?.selectedItem!!] = t.toByte()
                        updateView()
                        listener2.editData(
                            byteArrayOf(byteData[adapter?.selectedItem!!]),
                            adapter?.selectedItem!!
                        )

                    }
                }
            }, tt / 12 + 8, tt % 12 * 5, true
        ).show()
    }
}
