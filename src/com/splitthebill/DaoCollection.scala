package com.splitthebill

import cpframework_jvm.DBRapper
import cpframework_shared.DaoPlatform

case class DaoCollection(db: DBRapper, platform: DaoPlatform)  {
  val personDao = new PersonDao(this)

  def initialise(): Unit = {
    val version = 1
    personDao.initialise(version)
  }

  def dropAndCreateAll(): Unit = {
    personDao.dropTable()
    initialise()
  }
}