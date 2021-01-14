package pt.isec.amovtp2.geometrygo.data

import android.annotation.SuppressLint
import pt.isec.amovtp2.geometrygo.data.constants.UtilsConstants
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.atan2

object UtilsFunctions {
    @SuppressLint("SimpleDateFormat")
    fun convertDateToStr(date: Date) : String {
        return SimpleDateFormat(UtilsConstants.DATE_FORMAT).format(date)
    }

    @SuppressLint("SimpleDateFormat")
    fun convertToDate(date: String) : Date {
        return SimpleDateFormat(UtilsConstants.DATE_FORMAT).parse(date)!!
    }

    fun calculateAngle(actual: Player, other: Player) : Double {
        return atan2(
            actual.longitude - other.longitude,
            actual.latitude - other.latitude
        )
    }

    fun convertToFirstQuadrant(angle: Double): Double {
        if (0 < angle && angle < 90)
            return angle
        else if (90 < angle && angle < 180)
            return (180 - angle)
        else if (180 < angle && angle < 270)
            return (angle - 180)
        else if (270 < angle && angle < 360)
            return (360 - angle)
        return angle
    }

}