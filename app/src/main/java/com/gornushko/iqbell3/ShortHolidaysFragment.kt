package com.gornushko.iqbell3

import android.app.DatePickerDialog
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.toast
import java.util.*

@ExperimentalUnsignedTypes
class ShortHolidaysFragment(listener2: MyFragmentListener) : MyListFragment(listener2) {

    override var stringData = Array(16) { " " }

    override fun updateView() {
        if (activity != null) {
            for (i in 0..15) {
                var invalid = false
                val c = GregorianCalendar()
                var t = byteData[i * 2].toUByte()
                val isShort = (t > 127.toUInt())
                t = t and 0x7F.toUByte()
                val month = t.toInt()
                if (month == 0 || month > 12) invalid = true
                t = byteData[i * 2 + 1].toUByte()
                val day = t.toInt()
                if (day == 0 || day > 31) invalid = true
                if (invalid) {
                    stringData[i] = "${i + 1}."
                } else {
                    c.set(2000, month - 1, day)
                    stringData[i] = "${i + 1}.   " + String.format("%1\$te %1\$tB : ", c) +
                            getString(if (isShort) R.string.short_day else R.string.holiday_day)
                }
            }
            adapter?.notifyDataSetChanged()
        }
    }

    override fun clear() {
        for (i in 0..1) byteData[adapter?.selectedItem!! * 2 + i] = 0xFF.toByte()
        listener2.editData(
            byteArrayOf(
                0xFF.toByte(),
                0xFF.toByte()
            ), adapter?.selectedItem!! * 2
        )
        updateView()
    }

    override fun edit() {
        val c = GregorianCalendar()
        DatePickerDialog(
            activity!!, R.style.ThemeOverlay_AppCompat_Dialog,
            DatePickerDialog.OnDateSetListener { _, mYear, mMonth, mDay ->
                if (mYear == GregorianCalendar().get(Calendar.YEAR)) {
                    run {
                        alert(
                            when (GregorianCalendar(
                                mYear,
                                mMonth,
                                mDay
                            ).get(Calendar.DAY_OF_WEEK)) {
                                1, 7 -> "${getString(R.string.you_chose)}: ${GregorianCalendar(
                                    mYear, mMonth, mDay
                                ).getDisplayName(
                                    Calendar.DAY_OF_WEEK, Calendar.LONG,
                                    Locale.getDefault()
                                )}. ${getString(R.string.further_actions)}."
                                else -> "${getString(R.string.you_chose)}: ${GregorianCalendar(
                                    mYear, mMonth, mDay
                                ).getDisplayName(
                                    Calendar.DAY_OF_WEEK, Calendar.LONG,
                                    Locale.getDefault()
                                )}. ${getString(R.string.choose_short_or_holiday)}"
                            }
                        ) {
                            negativeButton(
                                getString(
                                    when (GregorianCalendar(
                                        mYear,
                                        mMonth,
                                        mDay
                                    ).get(Calendar.DAY_OF_WEEK)) {
                                        1, 7 -> (R.string.average_school_day)
                                        else -> (R.string.holiday_day)
                                    }
                                )
                            ) {
                                byteData[adapter?.selectedItem!! * 2] =
                                    (mMonth + 1).toUByte().toByte()
                                byteData[adapter?.selectedItem!! * 2 + 1] = mDay.toUByte().toByte()
                                updateView()
                                listener2.editData(
                                    byteArrayOf(
                                        (mMonth + 1).toUByte().toByte(),
                                        mDay.toUByte().toByte()
                                    ), adapter?.selectedItem!! * 2
                                )
                            }
                            positiveButton(getString(R.string.short_day)) {
                                byteData[adapter?.selectedItem!! * 2] =
                                    ((mMonth + 1).toUByte() or 0x80.toUByte()).toByte()
                                byteData[adapter?.selectedItem!! * 2 + 1] = mDay.toUByte().toByte()
                                updateView()
                                listener2.editData(
                                    byteArrayOf(
                                        ((mMonth + 1).toUByte() or 0x80.toUByte()).toByte(),
                                        mDay.toUByte().toByte()
                                    ), adapter?.selectedItem!! * 2
                                )
                            }
                        }.show()
                    }
                } else toast(getString(R.string.choose_current_year))
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}