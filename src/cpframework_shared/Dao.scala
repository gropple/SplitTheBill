package cpframework_shared

import java.util.Date

import com.almworks.sqlite4java.{SQLiteConnection, SQLiteJob, SQLiteStatement}
import cpframework_jvm.DBRapper
import cpframework_provided.Dependencies
import shared.models.InMemDb

//import framework.LoggerServer
import scala.collection.mutable.ArrayBuffer

// A Dao knows how to look up a sort of item (e.g. a User) based on its UUID from some sort of backing store - like
// an in-memory or Sqlite database. It can also do stuff like read and write JSON versions of an object, and insert into
// the backing store too. This lets us abstract operations over JS, server and Android.
trait Dao[T<:HasUUID] {
  var platform: DaoPlatform
  //  private val log: Logger = platform.logger.create(getClass.toString)

  // For testing. Only used for db, not inmem.
  var dbHits = 0

  // Create the store (for db, will create the table)
  def create()

  // Remove the store (for db, will remove the table)
  def delete()

  def generateUuid(v: T): String = platform.generateUuid

  // Note: the cache is meant to be a short-lived thing, e.g. during a function scope. There's no effort made to update the cache
  // if anything changes, including dependencies.
  def get(uuid: String, notRemoved: Boolean = true, expanded: Boolean = true, cache: Option[Dependencies] = None): Option[T]  = {

    if (cache.isDefined) {
      var result = getFromCache(uuid, cache.get)
      if (result.isDefined) {
        return result
      }
    }

    var result = getSpecialCase(uuid)

    if (!result.isDefined) {
      result = getByUuid(uuid, notRemoved, expanded, cache)
    }

    if (result.isDefined && cache.isDefined) {
      insertInCache(uuid, result.get, cache.get)
    }

    result
  }
  // Adding this especially for Daos that have some hardcoded values that for performance reasons are too slow to go in the db
  def getSpecialCase(uuid: String): Option[T] = None
  def getFromCache(uuid: String, cache: Dependencies): Option[T]
  def insertInCache(uuid: String, v: T, cache: Dependencies): Unit
  def getByUuid(uuid: String, notRemoved: Boolean = true, expanded: Boolean = true, cache: Option[Dependencies] = None): Option[T]
  def fillContentValues(u:T): Array[(String, Any)]
  def fillDependencies(v: T, f: Dependencies, recurse: Boolean)
  def createNew() : T
  def extract(u: T, cursor: CursorWrapper)
  // Can't use fillDependencies as it returns objects, not ModelProxy
  //  def expandTransitives(u: T, cache: Option[Dependencies] = None)
  // Can't use fillDependencies as it puts uuids in map
  //  def insertTransitives(v:T): Unit
  def setHasChanged(u: T, v: Boolean) = u.hasChanged = v
  def insertImpl(u: T, values: Array[(String, Any)], ifNotExists: Boolean): Unit
  def getTableName: String
  def hasChanged(u: T): Boolean = u.hasChanged
  def setRemoved(u: T, v: Boolean) = u.isRemoved = v
  def isRemoved(u: T): Boolean = u.isRemoved
  def remove(u:T): Unit
  def markAsRemoved(u:T): Unit = u.isRemoved = true
  def getAllColumns(): Array[String]
  //  protected def removeTransitives(v:T): Unit
  //  protected def markTransitivesAsRemoved(v:T): Unit
  def getAll(notRemoved: Boolean = true): Seq[T]
  def update(u:T): Unit
  def count(): Int
  def getAllChangedOrRemoved(): Map[String, T]
  def getTransitives(v:T): Seq[ModelProxy[_]]

  // Provided so we can cache effectively
  def expandTransitives(v:T, cache:Option[Dependencies]) = {
    getTransitives(v).foreach(x => if (x.isDefined()) x.use(cache))
  }

  // Join tables aren't modelled brilliantly yet, this can be improved on
  def getJoins(u: T, notRemoved: Boolean = true, expanded: Boolean = true): T = u
  def updateJoins(u:T) = {}
  def insertJoins(u:T) = {}
  def dropJoins() = {}

  def insert(u:T, ifNotExists: Boolean = false): Unit = {
    var uuid: Option[String] = u.uuid

    if (uuid.isEmpty) {
      u.uuid = Some(generateUuid(u))
    }

    // Not sure about this - involves looksup for UserEntries members
    //    insertTransitives(u)

    setHasChanged(u, v = true)

    val start = new Date().getTime
    var values = fillContentValues(u)
    val namesOnly = values.map(_._1).toList
    val valuesOnly = values.map(_._2).toList

    insertImpl(u, values, ifNotExists)
    insertJoins(u)
  }

  def writeJson(u:Iterable[T], deps: Option[Dependencies]): String = {
    val json = new JsonWriter
    json.startArray
    u.foreach(writeJson(_, json, deps))
    json.endArray()
    return json.toString()
  }

  def writeJsonMapAsFlat(u:Map[String, T], deps: Option[Dependencies]): String = {
    val json = new JsonWriter
    json.startArray
    u.foreach(v => writeJson(v._2, json, deps))
    json.endArray()
    return json.toString()
  }


  def writeJson(u:T, deps: Option[Dependencies]): String = {
    val json = new JsonWriter
    writeJson(u, json, deps)
    return json.toString()
  }

  def writeJson(u:T, json: JsonWriter, deps: Option[Dependencies]) = {
    json.startObject()

    val values = fillContentValues(u)
    values.foreach(v => {
      v._2 match {
        case value: Int => json.writeFieldInt(v._1, value)
        case value: Double => json.writeFieldDouble(v._1, value)
        case value: Boolean => json.writeFieldInt(v._1, if(value) 1 else 0)
        case value: String => json.writeFieldString(v._1, value)
        case value: LocDateTime => json.writeFieldString(v._1, value.toString())
        case value: LocDate => json.writeFieldString(v._1, value.toString())
        case _ => {
          if (v._2 == null) {
            json.writeFieldNull(v._1)
          }
          else {
            //            log.warn(s"Unable to convert field ${v._1} ${v._2}")
          }
        }
      }
    })

    writeJsonHook(u, json, deps)

    json.endObject()

    if (deps.isDefined) {
      fillDependencies(u, deps.get, recurse = true)
    }
  }

  def writeJsonHook(u:T, json: JsonWriter, deps: Option[Dependencies]) = {}
  def readJsonHook(u:T, json: JsonReader) = {}

  def readJson(json: String): T = {
    val jsonReader = new JsonReader(json)
    readJson(jsonReader)
  }

  def readJson(json: JsonReader): T = {
    val u = createNew()
    val cursor = new JsonCursorWrapper(json)
    var t = json.nextToken // step inside the {
    assert (t._2 == JsonType.InsideObject)
    extract(u, cursor)
    readJsonHook(u, json)
    t = json.nextToken // close the }
    u
  }

  def readJsonArray(json: String): Map[String, T] = {
    val jsonReader = new JsonReader(json)
    readJsonArray(jsonReader)
  }

  def readJsonArray(json: JsonReader): Map[String, T] = {
    var ret = Map[String, T]()
    val cursor = new JsonCursorWrapper(json)
    var t = json.nextToken // step inside the [
    assert (t._2 == JsonType.InsideArray)
    while (t._2 != JsonType.EndOfArray) {
      t = json.nextToken

      if (t._2 == JsonType.InsideObject) {
        val u = createNew()
        extract(u, cursor)
        readJsonHook(u, json)
        t = json.nextToken // close the }
        ret += u.uuid.get -> u
      }
    }
    ret
  }
}

abstract class DaoInMem[T<:HasUUID](_db: InMemDb, _platform: DaoPlatform) extends Dao[T] {
  var platform: DaoPlatform = _platform

  def create() = {}

  def delete() = getMap.clear()

  def getMap: collection.mutable.Map[String, T]

  def getAll(notRemoved: Boolean = true): Seq[T] = {
    getMap.map(_._2).toSeq
  }

  override def getByUuid(uuid: String, notRemoved: Boolean = true, expanded: Boolean = true, cache: Option[Dependencies]): Option[T] = {
    val map = getMap
    if (map.contains(uuid)) {
      return Some(map(uuid))
    }
    None
  }

  override def getAllChangedOrRemoved(): Map[String, T] = {
    getMap.filter(v => v._2.isRemoved || v._2.hasChanged).toMap
  }

  override def insertImpl(u: T, values: Array[(String, Any)], ifNotExists: Boolean): Unit = {
    getMap += (u.uuid.get -> u)
  }

  override def remove(u: T): Unit = getMap -= u.uuid.get

  override def count(): Int = getMap.size

  override def update(u: T): Unit = {
    getMap += (u.uuid.get -> u)
  }
}

abstract class DaoDb[T<:HasUUID](_db: DBRapper, _platform: DaoPlatform) extends Dao[T] {
  private val log: Logger = _platform.logger.create(getClass.toString)

  //  var tableName: String = null
  var db: DBRapper = _db
  //  val iso8601Format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  var platform: DaoPlatform = _platform

  // Deprecated, prefer local cache
  var shouldCache: Boolean = false
  var cache = collection.mutable.Map[String, T]()

  def create() = createTable()

  def delete() = dropTable()


  //  def register(db: DBRapper, platform: DaoPlatform): Unit = {
  //    this.db = db
  //    this.platform = platform
  //    log = platform.logger.create(getClass.toString)
  //  }

  def dropTable(name: String = getTableName): Unit = {
    val start = new Date().getTime
    val statement = s"drop table if exists ${name}"
    db.queue.execute[Unit, SQLiteJob[Unit]](new SQLiteJob[Unit] {
      override def job(conn: SQLiteConnection): Unit = {
        val st = conn.prepare(statement)
        st.step()
        st.dispose()
      }
    }).complete()
    dbHits += 1
    //    db.db.prepare(statement).step()
    log.info(s"${new Date().getTime - start} '${statement}'")
  }

  def initialise(version:Int) = {
    createTable()
  }


  def createTable(): Unit
  def getUuid(u:T) : Option[String] = u.uuid
  def setUuid(u:T, uuid:Option[String]) = u.uuid = uuid
  def getAllColumns() : Array[String]
  //  def insertTransitives(v:T): Unit

  def getDao() : Dao[T]


  def extractMultiple(cursor: CursorWrapper): ArrayBuffer[T] = {
    var results = new ArrayBuffer[T]
    while (cursor.step()) {
      var u = createNew()
      cursor.reset()
      extract(u, cursor)
      //      val uuid = u.uuid.get
      //      val r = cursor.createModelProxy(getDao, uuid)
      //      r.set(Some(u), uuid)
      results += u
    }
    cursor.dispose()
    results
  }

  def extractMultipleAsUuidMap(cursor: CursorWrapper): Map[String, T] = {
    var results = Map[String, T]()
    while (cursor.step()) {
      var u: T = createNew()
      cursor.reset()
      extract(u, cursor)
      val uuid = u.uuid
      //      val r = cursor.createModelProxy(getDao, getUuid(u).get)
      //      r.v = Some(u)
      results += uuid.get -> u
    }
    cursor.dispose()
    results
  }


  def insertImpl(u: T, values: Array[(String, Any)], ifNotExists: Boolean = false) = {
    var start = new Date().getTime
    val namesOnly = values.map(_._1).toList
    val valuesOnly = values.map(_._2).toList
    var statement = s"insert ${if (ifNotExists) "or ignore " else ""}into $getTableName (" + namesOnly.mkString(", ")
    statement += ") values ("
    val sb = new StringBuffer()

    buildInsertOrUpdate(valuesOnly, sb)
    statement += sb.toString

    statement += ")"

    db.queue.execute[Unit, SQLiteJob[Unit]](new SQLiteJob[Unit] {
      override def job(conn: SQLiteConnection): Unit = {
        val st = conn.prepare(statement)
        st.step()
        st.dispose()
      }
    }).complete()
    dbHits += 1
    log.info(s"${new Date().getTime - start} '$statement'")
  }

  def buildInsertOrUpdate(valuesOnly: List[Any], sb: StringBuffer): Unit = {
    val it: Iterator[Any] = valuesOnly.iterator
    while (it.hasNext) {
      val next: Any = it.next()
      next match {
        case null => sb.append("null")
        case v: Boolean =>
          if (v) {
            sb.append(1)
          }
          else {
            sb.append(0)
          }
        case _ =>
          sb.append("\"")
          sb.append(next)
          sb.append("\"")

      }
      if (it.hasNext) {
        sb.append(", ")
      }
    }
  }

  def update(u:T): Unit = {
    val start = new Date().getTime
    val uuid = getUuid(u)
    assert (uuid != null)
    //    insertTransitives(u)

    if (shouldCache) {
      cache(uuid.get) = u
    }

    setHasChanged(u, v = true)

    var values = fillContentValues(u)
    //    val valuesOnly = values.map(_._2).toList
    val len = values.length
    val sb = new StringBuffer()
    sb.append(s"update $getTableName set ")
    var i = 0
    values.foreach(v => {

      //    val it = values.iterator
      //    while (it.hasNext) {
      //      val next: Any = it.next()
      sb.append(s"${v._1}=")
      v._2 match {
        case null => sb.append("null")
        case v: Boolean =>
          if (v) {
            sb.append(1)
          }
          else {
            sb.append(0)
          }
        case _ =>
          sb.append("\"")
          sb.append(v._2)
          sb.append("\"")

      }
      if (i != (len - 1)) {
        sb.append(", ")
      }
      i += 1
    })
    sb.append(s" where uuid = '${getUuid(u).get}'")
    val statement = sb.toString
    db.queue.execute[Unit, SQLiteJob[Unit]](new SQLiteJob[Unit] {
      override def job(conn: SQLiteConnection): Unit = {
        val st = conn.prepare(statement)
        st.step()
        st.dispose()
      }
    }).complete()
    dbHits += 1
    log.info(s"${new Date().getTime - start} '$statement'")

    updateJoins(u)
  }

  def replace(u:T): Unit = {
    beginTransaction()
    try {
      remove(u)
      insert(u)
    }
    finally {
      commitTransaction()
    }
  }

  def beginTransaction(): Unit = {
    val start = new Date().getTime
    val statement = "begin;"
    db.queue.execute[Unit, SQLiteJob[Unit]](new SQLiteJob[Unit] {
      override def job(conn: SQLiteConnection): Unit = {
        val st = conn.prepare(statement)
        st.step()
        st.dispose()
      }
    }).complete()
    dbHits += 1
    log.info(s"${new Date().getTime - start} '${statement}'")
  }

  def commitTransaction(): Unit = {
    val start = new Date().getTime
    val statement = "commit;"
    db.queue.execute[Unit, SQLiteJob[Unit]](new SQLiteJob[Unit] {
      override def job(conn: SQLiteConnection): Unit = {
        val st = conn.prepare(statement)
        st.step()
        st.dispose()
      }
    }).complete()
    dbHits += 1
    log.info(s"${new Date().getTime - start} '${statement}'")
  }


  def getByUuid(uuid: String, notRemoved: Boolean = true, expanded: Boolean = false, cache: Option[Dependencies] = None): Option[T] = {
    val start = new Date().getTime

    val st = getAllColumns().mkString(",")
    val statement = s"SELECT $st FROM $getTableName WHERE uuid='$uuid'"


    val r = db.queue.execute[Option[T], SQLiteJob[Option[T]]](new SQLiteJob[Option[T]] {
      override def job(conn: SQLiteConnection): Option[T] = {
        var result:Option[T] = None
        val cursor = new SqlLiteCursorWrapper(conn.prepare(statement))
        if (cursor.step()) {
          val u = createNew()
          cursor.reset()
          extract(u, cursor)
          result = Some(u)
        }

        cursor.dispose()
        result
      }
    }).complete()
    dbHits += 1

    //    val cursor = db.db.prepare(statement)

    log.info(s"${new Date().getTime - start} '$statement' exists=${r.isDefined}")

    if (r.isDefined) {
      if (notRemoved && r.get.isRemoved) {
        return None
      }
      getJoins(r.get, notRemoved, expanded)
      if (expanded) {
        expandTransitives(r.get, cache)
      }
    }

    r
  }

  def getAll(notRemoved: Boolean = true): Seq[T] = {
    val statement = s"SELECT ${getAllColumns().mkString(",")} FROM $getTableName"
    getMultiple(statement, notRemoved)
  }

  def getMultiple(statement: String, notRemoved: Boolean = true): ArrayBuffer[T] = {
    val start = new Date().getTime

    var results = db.queue.execute[ArrayBuffer[T], SQLiteJob[ArrayBuffer[T]]](new SQLiteJob[ArrayBuffer[T]] {
      override def job(conn: SQLiteConnection): ArrayBuffer[T] = {
        val cursor = new SqlLiteCursorWrapper(conn.prepare(statement))
        extractMultiple(cursor)
      }
    }).complete()
    dbHits += 1

    log.info(s"${new Date().getTime - start} '$statement' results=${results.size}")
    if (notRemoved) {
      results = results.filter(v => !isRemoved(v))
    }

    results.foreach(r => getJoins(r, notRemoved))

    results
  }

  def getByStatement(statement: String, notRemoved: Boolean = true, expanded: Boolean = false): Option[T] = {
    val start = new Date().getTime

    val r = db.queue.execute[Option[T], SQLiteJob[Option[T]]](new SQLiteJob[Option[T]] {
      override def job(conn: SQLiteConnection): Option[T] = {
        var result: Option[T] = None
        val cursor = new SqlLiteCursorWrapper(conn.prepare(statement))

        if(cursor.step()) {
          var u = createNew()
          cursor.reset()
          extract(u, cursor)
          result = Option(u)
        }

        cursor.dispose()

        return result
      }
    }).complete()
    dbHits += 1

    log.info(s"${new Date().getTime - start} '${statement}' exists=${r.isDefined}")

    if (r.isDefined) {
      if (notRemoved && r.get.isRemoved) {
        return None
      }
      getJoins(r.get, notRemoved, expanded)
      //      if (expanded) {
      //        expandTransitives(r.get)
      //      }
    }

    return r
  }

  def getMultipleAsUuidMap(statement: String, notRemoved: Boolean = true): Map[String, T] = {
    val start = new Date().getTime

    var results = db.queue.execute[Map[String, T], SQLiteJob[Map[String, T]]](new SQLiteJob[Map[String, T]] {
      override def job(conn: SQLiteConnection):  Map[String, T] = {
        val cursor = new SqlLiteCursorWrapper(conn.prepare(statement))
        extractMultipleAsUuidMap(cursor)
      }
    }).complete()
    dbHits += 1

    log.info(s"${new Date().getTime - start} '${statement}' count=${results.size}")
    if (notRemoved) {
      results = results.filter(v => !isRemoved(v._2))
    }

    results.foreach(r => getJoins(r._2, notRemoved))

    results
  }

  def extractMultiple(statement: SQLiteStatement): ArrayBuffer[T] = {
    val cursor = new SqlLiteCursorWrapper(statement)
    extractMultiple(cursor)
  }


  def getAllChangedOrRemoved(): Map[String, T] = {
    val start = new Date().getTime
    val statement = s"SELECT ${getAllColumns.mkString(",")} FROM ${getTableName} where hasChanged=1 or isRemoved=1"

    val results = db.queue.execute[Map[String, T], SQLiteJob[Map[String, T]]](new SQLiteJob[Map[String, T]] {
      override def job(conn: SQLiteConnection): Map[String, T] = {
        val cursor = new SqlLiteCursorWrapper(conn.prepare(statement))
        return extractMultipleAsUuidMap(cursor)
      }
    }).complete()
    dbHits += 1


    log.info(s"${new Date().getTime - start} '${statement}' count=${results.size}")
    return results
  }

  override def markAsRemoved(u:T): Unit = {
    val start = new Date().getTime
    val uuid = getUuid(u)
    assert(uuid.isDefined)

    //    if (shouldCache) {
    //      cache -= uuid.get
    //    }

    //    markTransitivesAsRemoved(u)

    val statement = s"update ${getTableName} set isRemoved=1 where uuid='${uuid.get}'"

    db.queue.execute[Unit, SQLiteJob[Unit]](new SQLiteJob[Unit] {
      override def job(conn: SQLiteConnection): Unit = {
        val cursor = conn.prepare(statement)
        cursor.step()
        cursor.dispose()
      }
    }).complete()
    dbHits += 1

    setRemoved(u, true)

    log.info(s"${new Date().getTime - start} '${statement}'")
  }

  def remove(u:T): Unit = {
    if (!isRemoved(u)) {
      log.warn(s"Removing ${getUuid(u).get} without marking as removed first. This change can't be synced.")
    }

    //    removeTransitives(u)

    val uuid = u.getUuid
    assert(uuid.isDefined)

    val statement = s"delete from ${getTableName} where uuid='${uuid.get}'"
    db.execute(statement)
    dbHits += 1
  }

  def remove(uuid:String): Unit = {
    val u = getByUuid(uuid)
    if (u.isDefined)
      remove(u.get)
  }

  def count(): Int = {
    val start = new Date().getTime
    val statement = s"select count(*) from ${getTableName}"

    val result = db.queue.execute[Int, SQLiteJob[Int]](new SQLiteJob[Int] {
      override def job(conn: SQLiteConnection): Int = {
        val cursor = conn.prepare(statement)
        cursor.step()
        val result = cursor.columnInt(0)
        cursor.dispose()
        return result
      }
    }).complete()
    dbHits += 1

    log.info(s"${new Date().getTime - start} '${statement}' count=${result}")
    return result
  }

  // Called on notification from server that it has successfully synced from the client
  def syncClientFromServer(uuid:String): Unit = {
    val u = getByUuid(uuid)
    if (u.isDefined) {
      if (isRemoved(u.get)) {
        log.info(s"${uuid} server successfully removed, removing from local db")
        remove(u.get)
      }
      else if (hasChanged(u.get)) {
        log.info(s"${uuid} server successfully updated, clearing hasChanged flag from local db")
        setHasChanged(u.get, false)
        update(u.get)
      }
      else {
        log.warn(s"${uuid} server successfully updated, but nothing needed doing...")
      }
    }
  }

  def syncServerFromClient(changedOrRemoved: Seq[T]): Unit = {
    changedOrRemoved.foreach(v => {
      syncServerFromClient(v)
    })
  }

  def syncServerFromClient(u:T): Unit = {
    val uuid = getUuid(u)
    if (isRemoved(u)) {
      log.info(s"${uuid.get} client removed, removing from server db")
      remove(u)
    }
    else if (hasChanged(u)) {
      log.info(s"${uuid.get} client updated, updating local db")
      update(u)
    }
    else {
      log.warn(s"${uuid.get} client signalled change, but nothing needed doing...")
    }
  }

  case class DbCol(idx: Int, name: String, typ: String)

  def getExistingColumns(tableName: String): Seq[DbCol] = {
    val start = new Date().getTime
    val statement = s"PRAGMA table_info(${tableName});"
    var results = new ArrayBuffer[DbCol]

    db.queue.execute[Unit, SQLiteJob[Unit]](new SQLiteJob[Unit] {
      override def job(conn: SQLiteConnection): Unit = {
        val cursor = new SqlLiteCursorWrapper(conn.prepare(statement))
        while (cursor.step()) {
          cursor.reset()
          val idx = cursor.nextFieldAsInt
          val name = cursor.nextFieldAsString
          val typ = cursor.nextFieldAsString
          results += new DbCol(idx, name, typ)
        }
        cursor.dispose()
      }
    }).complete()
    dbHits += 1

    log.info(s"${new Date().getTime - start} '${statement}' count=${results.size}")
    results
  }

}
