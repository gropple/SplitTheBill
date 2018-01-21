package cpframework_jvm

import cpframework_shared.{LoggerFactory, Logger}

case class LoggerSystem(_clazz: String) extends Logger(_clazz) {
  def debug(v: String) = {
    println(s"${clazz}: ${v}")
  }

  def info(v: String) = {
    println(s"${clazz}: ${v}")
  }

  def warn(v: String) = {
    println(s"${clazz}: ${v}")
  }

  def error(v: String) = {
    println(s"${clazz}: ${v}")
  }
}

class LoggerFactorySystem extends LoggerFactory {
  def create(clazz: String): Logger = {
    return new LoggerSystem(clazz)
  }
}
