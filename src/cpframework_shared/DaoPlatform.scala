package cpframework_shared

import java.util.Date

abstract case class DaoPlatform(logger: LoggerFactory) {
  // 3.40 -> "3.40", 3 -> "3.00", 3.45435 -> "3.45". Assumes 2 d.p. which is bad (Russia, Vietnam, etc.)
  def prettifyCurrency(v: Double): String = {
    prettifyFloat(v, 2)
  }

  def prettifyFloat(v: Double, dp: Int): String = {
    var r = s"%1.${dp}f".format(v)
    // 0.00 -> 0, 0.30 -> 0.3
    if (dp > 0 && r.contains(".")) {
      while (true) {
        if (r.substring(r.length - 2) == ".0") return r.substring(0, r.length - 2)
        else if (r.substring(r.length - 1) == "0") r = r.substring(0, r.length - 1)
        else {
          return r
        }
      }
    }
    r
  }

  def generateUuid: String

  // Want this:
  // val iso8601Format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  // but don't have that on ScalaJS
  def formatDate(date: Date): String

  def parseDate(v: String): Date

  // "3+4"->7
  def calcStringToNumber(v: String): Option[Double]

  // 5.43, USD, user's locale is GBP - '$5.43'
  def convertCurrency(amount: Double, toCurrency: String): String

  // '4.43' -> 4.43
  def numberToString(amount: String): Option[Double]
}
