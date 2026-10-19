package com.prabhat.alarm

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan
import kotlin.math.roundToLong

/**
 * Sunrise estimate using the NOAA sunrise equations (90.833 degree zenith).
 * For normal inhabited latitudes this is typically close to published sunrise
 * tables; local terrain and atmospheric conditions can move observed sunrise.
 */
object SolarCalculator {
    private fun radians(degrees: Double) = Math.toRadians(degrees)
    private fun degrees(radians: Double) = Math.toDegrees(radians)
    private fun normalize(value: Double, range: Double): Double {
        var result = value % range
        if (result < 0) result += range
        return result
    }

    fun sunrise(date: LocalDate, latitude: Double, longitude: Double, zone: ZoneId = ZoneId.systemDefault()): ZonedDateTime? {
        if (latitude !in -89.8..89.8 || longitude !in -180.0..180.0) return null

        val n = date.dayOfYear.toDouble()
        val longitudeHour = longitude / 15.0
        val t = n + ((6.0 - longitudeHour) / 24.0)
        val meanAnomaly = (0.9856 * t) - 3.289
        val trueLongitude = normalize(
            meanAnomaly + 1.916 * sin(radians(meanAnomaly)) +
                0.020 * sin(2.0 * radians(meanAnomaly)) + 282.634,
            360.0
        )

        var rightAscension = degrees(atan(0.91764 * tan(radians(trueLongitude))))
        rightAscension = normalize(rightAscension, 360.0)
        rightAscension += floor(trueLongitude / 90.0) * 90.0 - floor(rightAscension / 90.0) * 90.0
        rightAscension /= 15.0

        val sinDeclination = 0.39782 * sin(radians(trueLongitude))
        val cosDeclination = cos(asin(sinDeclination))
        val denominator = cosDeclination * cos(radians(latitude))
        if (denominator == 0.0) return null
        val cosHourAngle = (cos(radians(90.833)) - sinDeclination * sin(radians(latitude))) / denominator
        if (cosHourAngle !in -1.0..1.0) return null // Polar day/night: no sunrise on this date.

        var hourAngle = 360.0 - degrees(acos(cosHourAngle))
        hourAngle /= 15.0
        val localMeanTime = hourAngle + rightAscension - (0.06571 * t) - 6.622
        val utcHour = normalize(localMeanTime - longitudeHour, 24.0)
        val utcMillis = (utcHour * 60.0 * 60_000.0).roundToLong()
        var instant: Instant = date.atStartOfDay(ZoneOffset.UTC).toInstant().plusMillis(utcMillis)
        var local = instant.atZone(zone)

        // The NOAA UTC hour is normalized to [0, 24). For time zones far east
        // or west, choose the adjacent UTC day that maps to the requested local date.
        if (local.toLocalDate().isAfter(date)) {
            instant = instant.minusSeconds(86_400)
            local = instant.atZone(zone)
        } else if (local.toLocalDate().isBefore(date)) {
            instant = instant.plusSeconds(86_400)
            local = instant.atZone(zone)
        }
        return local
    }

    fun info(date: LocalDate, latitude: Double, longitude: Double, offsetMinutes: Int = 72,
             zone: ZoneId = ZoneId.systemDefault()): MuhurtaInfo? {
        val sunrise = sunrise(date, latitude, longitude, zone) ?: return null
        return MuhurtaInfo(
            sunrise = sunrise,
            windowStart = sunrise.minusMinutes(96),
            windowEnd = sunrise.minusMinutes(48),
            recommended = sunrise.minusMinutes(offsetMinutes.coerceIn(48, 96).toLong())
        )
    }

    fun nextBrahmaInstant(latitude: Double, longitude: Double, offsetMinutes: Int,
                          zone: ZoneId = ZoneId.systemDefault(), now: ZonedDateTime = ZonedDateTime.now(zone)): ZonedDateTime? {
        var date = now.toLocalDate()
        var info = info(date, latitude, longitude, offsetMinutes, zone) ?: return null
        if (!info.recommended.isAfter(now.plusSeconds(2))) {
            date = date.plusDays(1)
            info = info(date, latitude, longitude, offsetMinutes, zone) ?: return null
        }
        return info.recommended
    }
}
