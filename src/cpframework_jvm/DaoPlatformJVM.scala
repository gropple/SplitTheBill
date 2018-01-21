package cpframework_jvm

import java.text.SimpleDateFormat
import java.util.{Calendar, Date, TimeZone, UUID}

import cpframework_shared.{DaoPlatform, LoggerFactory}
import cpframework_shared.expr.Parser


class DaoPlatformJVM(logger: LoggerFactory) extends DaoPlatform(logger) {
  def generateUuid: String = {
    return UUID.randomUUID().toString
  }

  // val iso8601Format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  override def formatDate(date: Date): String = {
    val c = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    c.setTime(date)
    //    return new LocDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE))

    return s"${c.get(Calendar.YEAR)}-${"%02d".format(c.get(Calendar.MONTH) + 1)}-${"%02d".format(c.get(Calendar.DATE))} ${"%02d".format(c.get(Calendar.HOUR))}:${"%02d".format(c.get(Calendar.MINUTE))}:${"%02d".format(c.get(Calendar.SECOND))}"
  }

  override def parseDate(v: String): Date = {
    //    val year = v.substring(0, 4).toInt
    //    val month = v.substring(5, 7).toInt
    //    val day = v.substring(8, 10).toInt
    //    val hours = v.substring(11, 13).toInt
    //    val minutes = v.substring(14, 16).toInt
    //    val seconds = v.substring(17, 19).toInt
    //    val c = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    //    c.set(Calendar.YEAR, year)
    //    c.set(Calendar.MONTH, month)
    //    c.set(Calendar.DAY_OF_MONTH, day)
    //    c.set(Calendar.HOUR, hours)
    //    c.set(Calendar.MINUTE, minutes)
    //    c.set(Calendar.SECOND, seconds)
    ////    c.setTimeZone(TimeZone.getTimeZone("UTC"))
    //    //    val date = new Date(Date.UTC(year, month, day, hours, minutes, seconds))
    //    c.getTime

    val isoFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
    val s = isoFormat.parse(v)
    s
  }

  override def convertCurrency(amount: Double, toCurrency: String): String = {
    val currency: java.util.Currency  = java.util.Currency.getInstance(toCurrency)
    val format: java.text.NumberFormat  = java.text.NumberFormat.getCurrencyInstance()
    format.setCurrency(currency)
    return format.format(amount)
  }

  // "3+4"->7
  override def calcStringToNumber(v: String): Option[Double] = {
    if (v.isEmpty) return Some(0)
    try {
      val p = new Parser
      p.allow(null)
      val expr = p.parseString(v)
      Some(expr.value())
    }
    catch {
      case _: Throwable => return None
    }
  }

  // '4.43' -> 4.43
  override def numberToString(amount: String): Option[Double] = {
    try {
      Some(amount.toDouble)
    }
    catch {
      case _: Throwable => None
    }
  }

}
