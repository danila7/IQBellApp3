package com.gornushko.iqbell3

import android.content.Context


@ExperimentalUnsignedTypes
class TimetableContainerFragment : MyContainerFragment() {
    override val one = TimetableFragment(this)
    override val two = TimetableFragment(this)
    override var nameFirst = String()
    override var nameSecond = String()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        nameFirst = getString(R.string.timetables_main_tab)
        nameSecond = getString(R.string.timetables_second_tab)
    }

    override fun setStartData(data: ByteArray) {
        one.setStartData(data.copyOfRange(0, 24))
        two.setStartData(data.copyOfRange(24, 48))
    }

    override fun updateData(data: ByteArray) {
        one.updateData(data.copyOfRange(0, 24))
        two.updateData(data.copyOfRange(24, 48))
    }

    override fun editData(data: ByteArray, offset: Int) {
        listener?.editData(data, offset + if (activeTab == 0) 0 else 24)
    }
}
