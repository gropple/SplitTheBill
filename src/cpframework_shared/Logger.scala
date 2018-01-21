package cpframework_shared

abstract class Logger(_clazz: String) {
  val clazz = _clazz

  def debug(v: String)
  def info(v: String)
  def warn(v: String)
  def error(v: String)
}

trait LoggerFactory {
  def create(clazz: String): Logger
}