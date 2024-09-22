package gecw.cse.utils

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*


class DateUtils {
    companion object{
        fun isoStringToDate(isoString: String): Date {
            val instant = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(isoString))
            return Date.from(instant)
        }
    }
}