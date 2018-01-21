import java.io.File
import java.util.Date

import com.splitthebill.{Person, DaoCollection, DepInj}
import cpframework_android.LoggerFactoryAndroid
import cpframework_jvm.{LoggerFactorySystem, DaoPlatformJVM, DBRapper}
import cpframework_shared.{LocDate, JsonWriter, JsonReader}
import org.robolectric.annotation.Config
import org.scalatest.{BeforeAndAfter, FunSuite}
import helpers.SoftAssertions
import org.junit._

class PersonDaoTest extends FunSuite with BeforeAndAfter {
  var sa: SoftAssertions = null
  val logger = new LoggerFactorySystem

  val db = new DBRapper(logger)
  val platform = new DaoPlatformJVM(logger)
  val file = new File("stb_test.db")
  db.open(file)

  val daos = new DaoCollection(db, platform)

  before {
    sa = new SoftAssertions
    daos.dropAndCreateAll()
  }

  after {
    if (!sa.isChecked) {
      assert(sa.checkIsEmptyAndClear() === true)
    }
  }

  def nextPersonAndCreate: String = {
    val next = daos.personDao.nextPersonName
    var p = new Person
    p.name = next
    daos.personDao.insert(p)
    next
  }

  def createAndInsert(name: String) = {
    var p = new Person
    p.name = name
    daos.personDao.insert(p)
  }

  test("nextPersonName1") {
    sa.assertThat(nextPersonAndCreate).isEqualTo("Person 1")
    sa.assertThat(nextPersonAndCreate).isEqualTo("Person 2")
    sa.assertThat(nextPersonAndCreate).isEqualTo("Person 3")
    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("nextPersonName2") {
    createAndInsert("Person 2")

    sa.assertThat(nextPersonAndCreate).isEqualTo("Person 1")
    sa.assertThat(nextPersonAndCreate).isEqualTo("Person 3")
    sa.assertThat(nextPersonAndCreate).isEqualTo("Person 4")
    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("nextPersonName3") {
    createAndInsert("Person 4")

    sa.assertThat(nextPersonAndCreate).isEqualTo("Person 1")
    sa.assertThat(nextPersonAndCreate).isEqualTo("Person 2")
    sa.assertThat(nextPersonAndCreate).isEqualTo("Person 3")
    sa.assertThat(nextPersonAndCreate).isEqualTo("Person 5")
    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("nextPersonName4") {
    ('1' to '9').foreach(v => createAndInsert(s"Person ${v}"))
    sa.assertThat(nextPersonAndCreate).isEqualTo("Person A")
    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("nextPersonName5") {
    ('1' to '9').foreach(v => createAndInsert(s"Person ${v}"))
    createAndInsert("Person Andy")
    createAndInsert("Person Bob")
    createAndInsert("Person Charlie")
    createAndInsert("Person")

    sa.assertThat(nextPersonAndCreate).isEqualTo("Person D")
    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("nextPersonNameAll") {
      ('1' to '9').foreach(v => {
        sa.assertThat(nextPersonAndCreate).isEqualTo("Person " + v)
      })
    ('A' to 'Z').foreach(v => {
      sa.assertThat(nextPersonAndCreate).isEqualTo("Person " + v)
    })
    ('a' to 'z').foreach(v => {
      sa.assertThat(nextPersonAndCreate).isEqualTo("Person " + v)
    })
    sa.assertThat(nextPersonAndCreate).isEqualTo("Person")
    sa.assertThat(nextPersonAndCreate).isEqualTo("Person")

    assert(sa.checkIsEmptyAndClear() === true)
  }


}