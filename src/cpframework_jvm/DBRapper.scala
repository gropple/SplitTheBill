package cpframework_jvm

import java.io.File
import java.util.Date

import com.almworks.sqlite4java.{SQLite, SQLiteConnection, SQLiteJob, SQLiteQueue}
import cpframework_shared.LoggerFactory

//import framework.LoggerServer

class MyQueue(logger: LoggerFactory, file: File) extends SQLiteQueue(file) {
  private val log = logger.create(getClass.toString)
}

// Our underlying db is an sqlite4java managed sqlite file.  We're trading off high performance (particularly crucial on
// Android where it's still dog-slow) against server scalability.  One day we're going to strain at the limits, but all the DAO
// code is executing basic SQL so we should be able to sub out a different db on server. For right now, this solution is great.
case class DBRapper(logger: LoggerFactory) {
  def execute(statement: String) = {
    val start = new Date().getTime

    val job = new SQLiteJob[Unit] {
      override def job(conn: SQLiteConnection): Unit = {
        val st = conn.prepare(statement)
        st.step()
        st.dispose()
      }
    }

    queue.execute[Unit, SQLiteJob[Unit]](job).complete()

    log.info(s"${new Date().getTime - start} '${statement}'")
  }

  def executeReturnOneInt(statement: String): Int = {
    val start = new Date().getTime

    val result = queue.execute[Int, SQLiteJob[Int]](new SQLiteJob[Int] {
      override def job(conn: SQLiteConnection): Int = {
        val cursor = conn.prepare(statement)
        cursor.step()
        val result = cursor.columnInt(0)
        cursor.dispose()
        return result
      }
    }).complete()

    log.info(s"${new Date().getTime - start} '${statement}'")
    return result

  }

  private val log = logger.create(getClass.toString)
  //  var db: SQLiteConnection = _
  var queue: SQLiteQueue = _

  def open(file: File, libPath: Option[String] = None) = {

    if (libPath.isDefined) {
      SQLite.setLibraryPath(libPath.get)
    }
    //    val props = System.getProperties();
    //    props.setProperty("", "http://gate.ac.uk/wiki/code-repository");

    // Emable debugging
    // If "database is locked", make sure only one test is running
//    val args = Array("-d")
//    com.almworks.sqlite4java.SQLite.main(args)

    queue = new MyQueue(logger, file)
    queue.start()

    //    db = new SQLiteConnection()1
    //    db.open(true)
  }

  def close(): Unit = {
    //    db.dispose()
    queue.stop(true).join()
  }
}
