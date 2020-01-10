package com.gornushko.iqbell3


import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.experimental.and


private val df = DateFormat.getDateInstance(DateFormat.LONG)
private val tf = DateFormat.getTimeInstance(DateFormat.DEFAULT)
private lateinit var byteData: ByteArray
private lateinit var extraByteData: ByteArray

@ExperimentalUnsignedTypes
class HomeFragment : Fragment() {

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
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        view.assembly_button.onClick { listener.sendData(byteArrayOf(0x3), "m") }
        view.workshop_button.onClick { listener.sendData(byteArrayOf(0x2), "m") }
        view.ring_button.onClick { listener.sendData(byteArrayOf(0x1), "m") }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        super.onResume()
        updateView()
    }

    fun setStartData(data: ByteArray) {
        byteData = data
    }

    fun setStartExtraData(data: ByteArray) {
        extraByteData = data
    }

    @SuppressLint("SetTextI18n")
    private fun updateView() {
        val iqTime: Calendar = GregorianCalendar.getInstance()
        iqTime.timeInMillis = localToUTC(getLongFromByteArray(byteData) * 1_000)
        date.text = df.format(iqTime.time)
        time.text = "${tf.format(iqTime.time)}, ${iqTime.getDisplayName(
            Calendar.DAY_OF_WEEK, Calendar.LONG,
            Locale.getDefault()
        )}"
        clock.setTime(
            iqTime.get(Calendar.HOUR),
            iqTime.get(Calendar.MINUTE),
            iqTime.get(Calendar.SECOND)
        )
        val iqMode = (byteData[4] and 0x7F).toInt()
        mode.text = getString(
            when (iqMode) {
                0 -> R.string.classes
                1 -> R.string.sun_sat
                2 -> R.string.holidays
                3 -> R.string.day_off
                4 -> R.string.not_started
                else -> R.string.finished
            }
        )
        val shortTimetable = byteData[4].toUByte().toInt() > 127
        val nextBellByte = byteData[7].toUByte().toInt()
        when (iqMode) {
            0, 4, 5 -> {
                timetable.visibility = View.VISIBLE
                timetable.text =
                    getString(if (shortTimetable) R.string.short_day else R.string.normal_classes)
            }
            else -> {
                timetable.visibility = View.GONE
            }
        }
        if (nextBellByte != 255 && iqMode == 0) {
            next_bell.visibility = View.VISIBLE
            time_till_next_bell.visibility = View.VISIBLE
            val nextBellNum = nextBellByte + if (shortTimetable) 24 else 0
            val nextBell = extraByteData[nextBellNum].toUByte().toInt()
            val nextBellHour = nextBell / 12 + 8
            val nextBellMinute = nextBell % 12 * 5
            next_bell.text =
                getString(R.string.next_bell) + ": \n ${print2digits(nextBellHour)}:${print2digits(
                    nextBellMinute
                )}"
            var secondsTillNextBell =
                (nextBellHour * 3600 + nextBellMinute * 60) - (iqTime.get(Calendar.HOUR_OF_DAY) * 3600 + iqTime.get(
                    Calendar.MINUTE
                ) * 60 + iqTime.get(Calendar.SECOND))
            if (secondsTillNextBell < 0) secondsTillNextBell = 0
            time_till_next_bell.text =
                getString(R.string.time_till_next_bell) + ": \n ${print2digits(secondsTillNextBell / 60)}:${print2digits(
                    secondsTillNextBell % 60
                )}"
        } else {
            next_bell.visibility = View.GONE
            time_till_next_bell.visibility = View.GONE
        }
        ringing_state.text = when (byteData[5].toInt()) {
            0 -> getString(R.string.not_ringing)
            1 -> getString(R.string.lesson_ringing) + ": ${byteData[6].toInt()}"
            2 -> getString(R.string.workshop_ringing) + ": ${byteData[6].toInt()}"
            else -> getString(R.string.assembly_ringing) + ": ${byteData[6].toInt()}"
        }
        if (byteData[5].toInt() > 0 || iqMode > 0) {
            workshop_button.visibility = View.GONE
            assembly_button.visibility = View.GONE
            ring_button.visibility = View.GONE
            info.text = getString(R.string.manual_bells_control_na)
        } else {
            workshop_button.visibility = View.VISIBLE
            assembly_button.visibility = View.VISIBLE
            ring_button.visibility = View.VISIBLE
            info.text = getString(R.string.manual_bells_control)
        }
    }

    private fun print2digits(num: Int) = if (num < 10) "0$num" else "$num"

    fun updateData(data: ByteArray) {
        byteData = data
        updateView()
    }

    fun updateExtraData(data: ByteArray) {
        extraByteData = data
        updateView()
    }

    private fun getLongFromByteArray(data: ByteArray): Long {
        var result = 0u
        for (i in 3 downTo 0) result = (result shl 8) + data[i].toUByte()
        return result.toLong()
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @SuppressLint("SimpleDateFormat")
    fun localToUTC(time: Long): Long {
        try {
            val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss")
            val date = Date(time)
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val strDate: String = dateFormat.format(date)
            val dateFormatLocal = SimpleDateFormat("yyyy.MM.dd HH:mm:ss")
            val utcDate: Date = dateFormatLocal.parse(strDate)
            return utcDate.time
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return time
    }
}