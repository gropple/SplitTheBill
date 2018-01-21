package cpframework_shared

import java.util.{Calendar, Date}

abstract class LocDatePlatform {
  def fromDate(date: Date): LocDate

  def fromDateTime(date: Date): LocDateTime

  def now(): LocDateTime = {
    val date = new Date()
    fromDateTime(date)
  }

}

class LocDatePlatformJVM extends LocDatePlatform {

  def fromDate(date: Date): LocDate = {
    val calendar = Calendar.getInstance
    calendar.setTime(date)
    return new LocDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DATE))
  }

  def fromDateTime(date: Date): LocDateTime = {
    val c = Calendar.getInstance
    c.setTime(date)
    return new LocDateTime(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DATE), c.get(Calendar.HOUR), c.get(Calendar.MINUTE), c.get(Calendar.SECOND))
  }

}


class LocDatePlatformJS extends LocDatePlatform {

  // Remember these aren't really Java Dates, they are JS dates
  def fromDate(date: Date): LocDate = {
    return new LocDate(date.getYear + 1900, date.getMonth + 1, date.getDate)
  }

  def fromDateTime(date: Date): LocDateTime = {
    return new LocDateTime(date.getYear + 1900, date.getMonth + 1, date.getDate, date.getHours, date.getMinutes, date.getSeconds)
  }
}

// This is a backdoor where code will definitely be running on JVM, to avoid having to put the DI everywhere in the test code
// Try to use the classes above instead for cross-platformey goodness.
object LocDatePlatform {
  def fromDate(date: Date): LocDate = {
    val calendar = Calendar.getInstance
    calendar.setTime(date)
    return new LocDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE))
  }
}
