package cpframework_android

import android.util.Log
import cpframework_shared.{LoggerFactory, Logger}

case class LoggerAndroid(_clazz: String) extends Logger(_clazz) {
  def debug(v: String) = {
    Log.v(clazz, v)
  }

  def info(v: String) = {
    Log.i(clazz, v)
  }

  def warn(v: String) = {
    Log.w(clazz, v)
  }

  def error(v: String) = {
    Log.e(clazz, v)
  }
}

class LoggerFactoryAndroid extends LoggerFactory {
  def create(clazz: String): Logger = {
    return new LoggerAndroid(clazz)
  }
}
