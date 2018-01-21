package cpframework_shared

// Because I generally don't want to worry about timezones.  I created an entry at 6:40pm in Vietnam, in Vietnam time - good.
// I don't ever want to convert that to a different timezone, or have it wrap around to the next day if my browser says I'm in US time, or whatever.
// I want to lose all timezone info completely.

object LocDateOrdering extends Ordering[LocDate] {
  // Result of comparing this with operand that. returns x where x < 0 iff this < that x == 0 iff this == that x > 0 iff this > that
  def compare(x: LocDate, y: LocDate): Int = {
    if (x.year < y.year) return -1
    if (x.year > y.year) return 1
    if (x.month < y.month) return -1
    if (x.month > y.month) return 1
    if (x.day < y.day) return -1
    if (x.day > y.day) return 1
    return 0
  }
}

object LocDateTimeOrdering extends Ordering[LocDateTime] {
  // Result of comparing this with operand that. returns x where x < 0 iff this < that x == 0 iff this == that x > 0 iff this > that
  def compare(x: LocDateTime, y: LocDateTime): Int = {
    if (x.year < y.year) return -1
    if (x.year > y.year) return 1
    if (x.month < y.month) return -1
    if (x.month > y.month) return 1
    if (x.day < y.day) return -1
    if (x.day > y.day) return 1
    if (x.hours < y.hours) return -1
    if (x.hours > y.hours) return 1
    if (x.mins < y.mins) return -1
    if (x.mins > y.mins) return 1
    if (x.secs < y.secs) return -1
    if (x.secs > y.secs) return 1
    return 0
  }
}

object LocDate {
  def fromString(s: String) : LocDate = {
    val fields = s.split("/")
    val year = fields(0).toInt
    val month = fields(1).toInt
    val day = fields(2).toInt
    return new LocDate(year, month, day)
  }

  // 30 days has September, April, June, and November...
  // Use getDaysInMonth to account for leap years
  val daysInMonth = Map(1->31, 2->28, 3->31, 4->30, 5->31, 6->30, 7->31, 8->31, 9->30, 10->31, 11->31, 12->31)

  def daysBetween(d1: LocDate, d2: LocDate): Int = {
    if (d1 == d2) return 1
    if (d1 > d2) return daysBetween(d2, d1)

    var day = d2.day
    var month = d2.month
    var year = d2.year

    var days = 0

    while (!(day == d1.day && month == d1.month && year == d1.year)) {
      day -= 1
      if (day <= 0) {
        month -= 1
        if (month <= 0) {
          year -= 1
          month = 12
          day = getDaysInMonth(year, month)
        }
        else {
          day = getDaysInMonth(year, month)
        }
      }
      days += 1
    }

    return days + 1
  }

  def getDaysInMonth(year: Int, month: Int): Int = {
    if (month == 2) {
      if (isLeapYear(year)) {
        return 29
      }
      return 28
    }
    daysInMonth(month)
  }

  def isLeapYear(year: Int): Boolean = {
    ((year % 4) == 0) && (((year % 100) != 0) || ((year % 400) == 0))
  }

  def minusDays(date: LocDate, days: Int): LocDate = {
    var day = date.day
    var month = date.month
    var year = date.year
    var days2 = days

    while(days2 > 0) {
      day -= 1
      days2 -= 1
      if (day <= 0) {
        month -= 1
        if (month <= 0) {
          year -= 1
          month = 12
          day = getDaysInMonth(year, month)
        }
        day = getDaysInMonth(year, month)
      }
    }

    new LocDate(year, month, day)
  }

  def addDays(date: LocDate, days: Int): LocDate = {
    var day = date.day
    var month = date.month
    var year = date.year
    var days2 = days

    while(days2 > 0) {
      day += 1
      days2 -= 1
      if (day > getDaysInMonth(year, month)) {
        month += 1
        if (month > 12) {
          year += 1
          month = 1
        }
        day = 1
      }
    }

    new LocDate(year, month, day)
  }

  // Tries to keep the day as close as possible to original
  def minusMonths(date: LocDate, months: Int): LocDate = {
    var originalDay = date.day
    var day = date.day
    var month = date.month
    var year = date.year
    var months2 = months

    while(months2 > 0) {
      month -= 1
      months2 -= 1
      if (month <= 0) {
        year -= 1
        month = 12
      }
      day = Math.min(getDaysInMonth(year, month), originalDay)
    }

    new LocDate(year, month, day)
  }

  // Tries to keep the day as close as possible to original
  def addMonths(date: LocDate, months: Int): LocDate = {
    var originalDay = date.day
    var day = date.day
    var month = date.month
    var year = date.year
    var months2 = months

    while(months2 > 0) {
      month += 1
      months2 -= 1
      if (month > 12) {
        year += 1
        month = 1
      }
      day = Math.min(getDaysInMonth(year, month), originalDay)
    }

    new LocDate(year, month, day)
  }

  // Tries to keep the day as close as possible to original
  def minusYears(date: LocDate, years: Int): LocDate = {
    val newYear = date.year - years
    new LocDate(newYear, date.month, Math.min(date.day, getDaysInMonth(newYear, date.month)))
  }

  // Tries to keep the day as close as possible to original
  def addYears(date: LocDate, years: Int): LocDate = {
    val newYear = date.year + years
    new LocDate(newYear, date.month, Math.min(date.day, getDaysInMonth(newYear, date.month)))
  }

  def dayOfYear(date: LocDate): Int = {
    var days = date.day
    var m = date.month - 1
    while (m > 0) {
      days += getDaysInMonth(date.year, m)
      m -= 1
    }
    days
  }

  // Epoch time
  def getMsecsSince1970(date: LocDate): Long = {
    // http://stackoverflow.com/questions/7960318/convert-seconds-since-1970-into-date-and-vice-versa
    val tm_sec = 0
    val tm_min = 0
    val tm_hour = 0
    val tm_yday = dayOfYear(date) - 1
    val tm_year = date.year - 1900
    val term1 = tm_sec + tm_min*60 + tm_hour*3600 + tm_yday*86400
    val term2 = (tm_year-70)*31536000
    val term3 = ((tm_year-69)/4)*86400
    val term4 = ((tm_year-1)/100)*86400
    val term5 = ((tm_year+299)/400)*86400
    val result = (term1 + term2 + term3 - term4 + term5) * 1000L
    result
  }



}

object LocDateTime {
  def fromString(v: String): LocDateTime = {
    val year = v.substring(0, 4).toInt
    val month = v.substring(5, 7).toInt
    val day = v.substring(8, 10).toInt
    val hours = v.substring(11, 13).toInt
    val minutes = v.substring(14, 16).toInt
    val seconds = v.substring(17, 19).toInt
    new LocDateTime(year, month, day, hours, minutes, seconds)
  }
}


case class LocDate(year: Int, month: Int, day: Int) extends Ordered[LocDate] {
  def toLocDateTime: LocDateTime = new LocDateTime(year, month, day, 0, 0, 0)
  override def toString = s"$year/$month/$day"
  def toZeroPaddedString = s"$year/${"%02d".format(month)}/${"%02d".format(day)}"
  def compare(that: LocDate) = LocDateOrdering.compare(this, that)

  def dayOfWeek: Int = {
    val t = Array(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    var y = year
    var m = month
    var d = day
    if (m < 3) {
      y -= 1
    }
    ( y + y/4 - y/100 + y/400 + t(m-1) + d) % 7
  }

}

case class LocDateTime(year: Int, month: Int, day: Int, hours: Int, mins: Int, secs: Int) extends Ordered[LocDateTime] {

  override def toString() = s"${"%04d".format(year)}-${"%02d".format(month)}-${"%02d".format(day)} ${"%02d".format(hours)}:${"%02d".format(mins)}:${"%02d".format(secs)}"

  def toLocDate = new LocDate(year, month, day)

  def compare(that: LocDateTime) = LocDateTimeOrdering.compare(this, that)
}
