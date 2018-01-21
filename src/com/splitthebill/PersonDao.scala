package com.splitthebill

import cpframework_provided.Dependencies
import cpframework_shared.{CursorWrapper, Dao}

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class PersonShared(daos: DaoCollection) {

  def fillContentValues(u:Person): Array[(String, Any)] = {
    var values: Array[(String, Any)] = Array(
      ("uuid", u.uuid.get),
      ("name", u.name),
      ("avatarFilename", u.avatarFilename)
    )
    return values
  }

  def extract(u: Person, cursor: CursorWrapper) = {
    u.uuid = Some(cursor.nextFieldAsString)
    u.name = cursor.nextFieldAsString
    u.avatarFilename = cursor.nextFieldAsString
  }

  def expandTransitives(u: Person): Unit = {
  }

  def fillDependencies(v: Person, f: Dependencies, recurse: Boolean): Unit = {
  }
}

class PersonDao(daos: DaoCollection) extends Dao[Person](daos.db, daos.platform) {
  def getAllSorted: ArrayBuffer[Person] = {
    val all = getAll().map(_.use())
    var sorted = all.filter(_.uuid.get == "me")
    sorted ++= all.filter(v => v.uuid.get != "me" && !v.name.startsWith("Person ")).sortBy(_.name)
    sorted ++= all.filter(v => v.name.startsWith("Person ")).sortBy(_.name)
    sorted
  }


  // Will generate a name "Person " + a single character, from A-Z, 1-9, a-z, based on avoiding name clashes with existing
  // anonymous people.  If it fails to generate a name after 62 gos, returns "Person".
  def nextPersonName: String = {
    val genericUsers = getAll().map(_.use()).filter(_.name.startsWith("Person ")).filter(_.name.length >= 8).sortBy(_.name) // ['Person C','Person D',...]
    val endings = genericUsers.map(_.name.charAt(7))  // ['C','D',...]

    var c = '1'

    for (idx <- endings.indices) {
      val ending = endings(idx)
      if (ending != c) {
        return "Person " + c
      }

      c = (c + 1).toChar

      // Do it this way so it matchs ASCII chart order
      if (c == ('9' + 1).toChar) {
        c = 'A'
      }
      else if (c == ('Z' + 1).toChar) {
        c = 'a'
      }
      else if (c == ('z' + 1).toChar) {
        return "Person"
      }
    }

    "Person " + c
  }

  def maxImage = 242
  val r = new Random()

  def nextPersonImage: Int = {
//    val users = getAll().map(_.use())
//    val used = users.map(_.avatarFilename).foreach(_.substring(8).toInt)
    r.nextInt(maxImage + 1) // 0-242 inclusive
  }

  def nextPersonAvatarFilename: String = {
    s"drawable/avatar$nextPersonImage"
  }


  val shared = new PersonShared(daos)

  def findByName(email: String): Option[Person] = {
    val statement = s"SELECT ${getAllColumns.mkString(",")} FROM ${tableName} where name='${email}'"
    return getByStatement(statement)
  }

  private val log = daos.platform.logger.create(getClass.toString)

  tableName = "person"

  val allColumns  = Array(
    "uuid",
    "name",
    "avatarFilename"
)

  override def expandTransitives(u: Person, cache: Option[Dependencies]): Unit = shared.expandTransitives(u)

  def getAllColumns() : Array[String] = {
    return allColumns
  }

  def createTable() = {
    val statement = s"create table if not exists ${tableName} (uuid string primary key, " +
      "name string, " +
      "avatarFilename string)"
    db.execute(statement)
  }

  override def fillContentValues(u: Person): Array[(String, Any)] = shared.fillContentValues(u)

  override def extract(u: Person, cursor: CursorWrapper): Unit = shared.extract(u, cursor)

  override def getFetcher() : Dao[Person] = daos.personDao

  override def getUuid(u: Person): Option[String] = u.uuid

  override def createNew(): Person = new Person

  override def setUuid(u: Person, uuid: Option[String]): Unit = u.uuid = uuid

  override def hasChanged(u: Person): Boolean = false

  override def setRemoved(u: Person, v: Boolean): Unit = {}

  override def removeTransitives(v: Person): Unit = {}

  override def cachedItem(u: Person): Person = u


  override def markTransitivesAsRemoved(v: Person): Unit = {
    //    v.connections = ?
  }

  override def setHasChanged(u: Person, v: Boolean): Unit = {}

  override def isRemoved(u: Person): Boolean = false

  override def insertTransitives(v: Person): Unit = {
  }

  override def fillDependencies(v: Person, f: Dependencies, recurse: Boolean): Unit = shared.fillDependencies(v, f, recurse)

  override def getFromCache(uuid: String, cache: Dependencies): Option[Person] = None

  override def insertInCache(uuid: String, v: Person, cache: Dependencies): Unit = {}
}
