package helpers

import java.lang.reflect.Method
import java.util
import java.util.logging.Logger
import org.codehaus.groovy.runtime.powerassert.PowerAssertionError

import net.sf.cglib.proxy.{MethodProxy, MethodInterceptor}

import scala.collection.mutable.ArrayBuffer

class ErrorCollector extends MethodInterceptor {
  val errors = new ArrayBuffer[Throwable]
  private var log: Logger = Logger.getLogger("")

  def intercept(obj: AnyRef, method: Method, args: Array[AnyRef], proxy: MethodProxy): AnyRef = {
    try {
      proxy.invokeSuper(obj, args)
    }
    catch {
      case e: AssertionError => {
//        errors.add(e)
//      errors += e

        val sb = new StringBuilder

        val pa = new PowerAssertionError(e.toString)
        errors += e
        for (ste <- e.getStackTrace) {
          if (!ste.toString.contains("org.scalatest")
            && !ste.toString.contains("net.sf.cglib")
            && !ste.toString.contains("scala.collection.immutable.List.foreach")
            && !ste.toString.contains("org.jetbrains.plugins.scala.testingSupport")
            && !ste.toString.contains("helpers.ErrorCollector")
            && !ste.toString.contains("org$scalatest$BeforeAndAfter")
            && !ste.toString.contains(".run(")
            && !ste.toString.contains(".runTest(")
            && !ste.toString.contains("sun.reflect")) {
            sb ++= ste.toString
            sb += '\n'
          }
        }

        System.out.println(pa.toString)
        System.out.println(sb.toString)
        System.out.println("")
//        pa.printStackTrace
//        {
//          val in: Nothing = null
//          while (pa.stackTrace) {
//            if (ste.toString.startsWith("travelpouch")) {
//              log.info("    at ${ste.methodName}(${ste.fileName}:${ste.lineNumber})")
//            }
//            else {
//            }
//          }
//        }
      }
    }
    return obj
  }

  def errorsAsList: util.List[Throwable] = {
    val ret = new util.ArrayList[Throwable]()
    errors.foreach(ret.add(_))
    return ret
  }

//  def errors: List[Throwable] = {
//    return Collections.unmodifiableList(errors)
//  }
}