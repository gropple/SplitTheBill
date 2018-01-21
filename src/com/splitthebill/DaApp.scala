package com.splitthebill

import com.google.android.gms.analytics.{GoogleAnalytics, Tracker}
import cpframework_android.LoggerFactoryAndroid
import cpframework_jvm.{DBRapper, DaoPlatformJVM}

import scala.collection.mutable.ArrayBuffer
import scala.sys.process.processInternal.File

class DepInj(context: Context) {
  val logger = new LoggerFactoryAndroid

  val db = new DBRapper(logger)
  val platform = new DaoPlatformJVM(logger)
  val file = new File(context.getFilesDir(), "stb.db")
  db.open(file)

  val daos = new DaoCollection(db, platform)
}

class DaApp extends Application {
  var analytics: GoogleAnalytics = null
  var tracker: Tracker = null

  var di:DepInj = null
  var initialised:Boolean = false
  var screenshotMode = false
  var testingMode = false
//  var disableAdsForTesting = testingMode
  var disableAdsForTesting = true

  var itemsTotalValue: Double = 0
  var itemsTotalRaw = ""
  var totalValue: Double = 0
  var totalRaw = ""
  var userHasSetTotal = false
  var tipPctValue: Double = 0
  var tipPctRaw = ""
  var tipAmountValue: Double = 0
  var tipAmountRaw = ""
  var calcTotalValue: Double = 0

  var people = ArrayBuffer[Person]()

  def peopleChecked = people.filter(_.checked)

  def reset(): Unit = {
    itemsTotalValue = 0
    totalValue = 0
    tipPctValue = 0
    tipAmountValue = 0
    calcTotalValue = 0
    people = ArrayBuffer[Person]()
    if (initialised) {
      di.daos.dropAndCreateAll()
      createMe()
    }
  }

  def initialise() = {
    if (!initialised) {
      di = new DepInj(this)
      di.daos.initialise()

      var user = di.daos.personDao.get("me")
      if (user.isEmpty) {
        createMe()
      }

      initialised = true
    }
  }

  def createMe() = {
    val user = Some(new Person())
    user.get.name = "Me"
    user.get.uuid = Some("me")
    user.get.avatarFilename = di.daos.personDao.nextPersonAvatarFilename

    di.daos.personDao.insert(user.get)
  }

  override def onCreate() {
    if (!testingMode) {
      analytics = GoogleAnalytics.getInstance(this)
      // How often data is sent
      analytics.setLocalDispatchPeriod(15)
//      analytics.setDebug(true)

      tracker = analytics.newTracker("UA-52042668-3")
      //    tracker.enableExceptionReporting(true);
      tracker.enableAdvertisingIdCollection(true)
      tracker.enableAutoActivityTracking(true)
      tracker.enableExceptionReporting(true)
      // Enabling Advertising Features in Google Analytics allows you to take advantage of Remarketing, Demographics & Interests reports, and more.
      tracker.enableAdvertisingIdCollection(true)
      tracker.enableAutoActivityTracking(true)
    }
  }

}
