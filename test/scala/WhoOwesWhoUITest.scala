import java.io.File

import com.splitthebill.{WhoOwesWhoUI, IsSquare, Person, DaoCollection}
import cpframework_jvm.{DaoPlatformJVM, DBRapper, LoggerFactorySystem}
import helpers.SoftAssertions
import org.scalatest.{BeforeAndAfter, FunSuite}

class WhoOwesWhoUIImplTest extends WhoOwesWhoUI {
  var totalOwed = ""
  var changeDue = ""
  var changeBlockVisible = false
  var paidFromError = Map[String, Boolean]()
  var changeError = ""
  var changeErrorShown = false
  var personOwes = Map[String, String]()
  var personChangeOwed = Map[String, String]()
  var tookFromError = Map[String, Boolean]()
  var totalPaid = ""
  var notEnough = false
  var isSquare = Map[String, IsSquare]()
  var summaryToUI = ""

  override def changeBlockVisibleToUI(v: Boolean) = changeBlockVisible = v

  override def totalOwedToUI(v: String): Unit = totalOwed = v

  override def changeDueToUI(v: String): Unit = changeDue = v

  override def paidFromErrorToUI(p: Person, v: Boolean): Unit = paidFromError += (p.uuid.get -> v)

  override def changeErrorToUI(v: String, show: Boolean): Unit = {
    changeError = v
    changeErrorShown = show
  }

  override def personOwesToUI(p: Person, v: String): Unit = personOwes += (p.uuid.get -> v)

  override def personChangeOwedToUI(p: Person, v: String): Unit = personChangeOwed += (p.uuid.get -> v)

  override def tookFromErrorToUI(p: Person, v: Boolean): Unit = tookFromError += (p.uuid.get -> v)

  override def totalPaidToUI(v: String, notEnough: Boolean): Unit = {
    totalPaid = v
    this.notEnough = notEnough
  }

  override def personSquareToUI(v: Person, square: IsSquare) = isSquare += v.uuid.get -> square

  override def personPaidToUI(p: Person, v: String): Unit = {}

  override def personTookChangeToUI(p: Person, v: String): Unit = {}

  override def summaryToUI(v: String): Unit = summaryToUI = v
}

class WhoOwesWhoUITest extends FunSuite with BeforeAndAfter {
  var sa: SoftAssertions = null
  var ui: WhoOwesWhoUIImplTest = null
  var me: Person = null

  before {
    sa = new SoftAssertions
    ui = new WhoOwesWhoUIImplTest
    me = createPerson("Me", 0)
    val people = Array(me)
    ui.setPeople(people)
  }

  after {
    if (!sa.isChecked) {
      assert(sa.checkIsEmptyAndClear() === true)
    }
  }

  def createPerson(name: String, owes: Double): Person = {
    var p = new Person
    p.name = name
    p.uuid = Some(name.toLowerCase)
    p.avatarFilename = "drawable/avatar1"
    p.owesItems = owes
    p
  }

  test("init") {
    ui.init()
    ui.recalc()

    sa.assertThat(ui.personOwes.size).isEqualTo(1)
    sa.assertThat(ui.personOwes("me")).isEqualTo("0.00")
    sa.assertThat(ui.totalOwed).isEqualTo("0.00")
    sa.assertThat(ui.changeDue).isEqualTo("0.00")
    sa.assertThat(ui.changeBlockVisible).isEqualTo(false)
    sa.assertThat(ui.paidFromError.size).isEqualTo(0)
    sa.assertThat(ui.changeErrorShown).isEqualTo(false)
    sa.assertThat(ui.personChangeOwed.size).isEqualTo(1)
    sa.assertThat(ui.personChangeOwed("me")).isEqualTo("0.00")
    sa.assertThat(ui.tookFromError.size).isEqualTo(0)
    sa.assertThat(ui.totalPaid).isEqualTo("0.00")
    sa.assertThat(ui.notEnough).isEqualTo(false)
    sa.assertThat(ui.isSquare.size).isEqualTo(1)
    sa.assertThat(ui.isSquare.head._2).isEqualTo(new IsSquare(ui.squareMsgIsSquare(me), false, false))

    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("meOwes10") {
    me.owesItems = 10
    ui.init()
    ui.recalc()

    sa.assertThat(ui.personOwes.size).isEqualTo(1)
    sa.assertThat(ui.personOwes("me")).isEqualTo("10.00")
    sa.assertThat(ui.totalOwed).isEqualTo("10.00")
    sa.assertThat(ui.changeDue).isEqualTo("0.00")
    sa.assertThat(ui.changeBlockVisible).isEqualTo(false)
    sa.assertThat(ui.paidFromError.size).isEqualTo(0)
    sa.assertThat(ui.changeErrorShown).isEqualTo(false)
    sa.assertThat(ui.personChangeOwed.size).isEqualTo(1)
    sa.assertThat(ui.personChangeOwed("me")).isEqualTo("0.00")
    sa.assertThat(ui.tookFromError.size).isEqualTo(0)
    sa.assertThat(ui.totalPaid).isEqualTo("0.00")
    sa.assertThat(ui.notEnough).isEqualTo(true)
    sa.assertThat(ui.isSquare.size).isEqualTo(1)
    sa.assertThat(ui.isSquare.head._2).isEqualTo(new IsSquare(ui.squareMsgUnderpaid(me,10), true, false))

    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("meOwes10Paid5") {
    me.owesItems = 10
    ui.init()
    ui.recalc()

    ui.paidFromUI(me, "5")

    sa.assertThat(ui.personOwes.size).isEqualTo(1)
    sa.assertThat(ui.personOwes("me")).isEqualTo("10.00")
    sa.assertThat(ui.totalPaid).isEqualTo("5.00")
    sa.assertThat(ui.totalOwed).isEqualTo("10.00")
    sa.assertThat(ui.changeDue).isEqualTo("0.00")
    sa.assertThat(ui.changeBlockVisible).isEqualTo(false)
    sa.assertThat(ui.paidFromError.size).isEqualTo(0)
    sa.assertThat(ui.changeErrorShown).isEqualTo(false)
    sa.assertThat(ui.personChangeOwed.size).isEqualTo(1)
    sa.assertThat(ui.personChangeOwed("me")).isEqualTo("0.00")
    sa.assertThat(ui.tookFromError.size).isEqualTo(0)
    sa.assertThat(ui.notEnough).isEqualTo(true)
    sa.assertThat(ui.isSquare.size).isEqualTo(1)
    sa.assertThat(ui.isSquare.head._2).isEqualTo(new IsSquare(ui.squareMsgUnderpaid(me, 5), true, false))

    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("meOwes10Paid10") {
    me.owesItems = 10
    ui.init()
    ui.recalc()

    ui.paidFromUI(me, "10")

    sa.assertThat(ui.totalPaid).isEqualTo("10.00")
    sa.assertThat(ui.totalOwed).isEqualTo("10.00")
    sa.assertThat(ui.changeDue).isEqualTo("0.00")
    sa.assertThat(ui.changeBlockVisible).isEqualTo(false)
    sa.assertThat(ui.paidFromError.size).isEqualTo(0)
    sa.assertThat(ui.changeErrorShown).isEqualTo(false)
    sa.assertThat(ui.personChangeOwed.size).isEqualTo(1)
    sa.assertThat(ui.personChangeOwed("me")).isEqualTo("0.00")
    sa.assertThat(ui.tookFromError.size).isEqualTo(0)
    sa.assertThat(ui.notEnough).isEqualTo(false)
    sa.assertThat(ui.isSquare.head._2).isEqualTo(new IsSquare(ui.squareMsgIsSquare(me), false, false))

    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("meOwes10Paid15") {
    me.owesItems = 10
    ui.init()
    ui.recalc()

    ui.paidFromUI(me, "15")

    sa.assertThat(ui.totalPaid).isEqualTo("15.00")
    sa.assertThat(ui.totalOwed).isEqualTo("10.00")
    sa.assertThat(ui.changeDue).isEqualTo("5.00")
    sa.assertThat(ui.changeBlockVisible).isEqualTo(true)
    sa.assertThat(ui.paidFromError.size).isEqualTo(0)
    sa.assertThat(ui.changeErrorShown).isEqualTo(false)
    sa.assertThat(ui.personChangeOwed.size).isEqualTo(1)
    sa.assertThat(ui.personChangeOwed("me")).isEqualTo("5.00")
    sa.assertThat(ui.tookFromError.size).isEqualTo(0)
    sa.assertThat(ui.notEnough).isEqualTo(false)
    sa.assertThat(ui.isSquare.head._2).isEqualTo(new IsSquare(ui.squareMsgOverpaid(me, 5), false, true))

    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("twoPeopleOwe10And0") {
    val bob = createPerson("Bob", 0)
    me.owesItems = 10
    ui.setPeople(Array(me, bob))
    ui.init()
    ui.recalc()

    sa.assertThat(ui.personOwes.size).isEqualTo(2)
    sa.assertThat(ui.personOwes("me")).isEqualTo("10.00")
    sa.assertThat(ui.personOwes("bob")).isEqualTo("0.00")
    sa.assertThat(ui.totalOwed).isEqualTo("10.00")
    sa.assertThat(ui.changeDue).isEqualTo("0.00")
    sa.assertThat(ui.changeBlockVisible).isEqualTo(false)
    sa.assertThat(ui.paidFromError.size).isEqualTo(0)
    sa.assertThat(ui.changeErrorShown).isEqualTo(false)
    sa.assertThat(ui.personChangeOwed.size).isEqualTo(2)
    sa.assertThat(ui.personChangeOwed("me")).isEqualTo("0.00")
    sa.assertThat(ui.personChangeOwed("bob")).isEqualTo("0.00")
    sa.assertThat(ui.tookFromError.size).isEqualTo(0)
    sa.assertThat(ui.totalPaid).isEqualTo("0.00")
    sa.assertThat(ui.notEnough).isEqualTo(true)
    sa.assertThat(ui.isSquare.filter(_._1 == "me").head._2).isEqualTo(new IsSquare(ui.squareMsgUnderpaid(me, 10), true, false))
    sa.assertThat(ui.isSquare.filter(_._1 == "bob").head._2).isEqualTo(new IsSquare(ui.squareMsgIsSquare(bob), false, false))

    assert(sa.checkIsEmptyAndClear() === true)

  }

  test("twoPeopleOwe10And0Paid5And0") {
    val bob = createPerson("Bob", 0)
    me.owesItems = 10
    ui.setPeople(Array(me, bob))
    ui.init()
    ui.recalc()

    ui.paidFromUI(me, "5")

    sa.assertThat(ui.totalPaid).isEqualTo("5.00")
    sa.assertThat(ui.changeDue).isEqualTo("0.00")
    sa.assertThat(ui.paidFromError.size).isEqualTo(0)
    sa.assertThat(ui.changeErrorShown).isEqualTo(false)
    sa.assertThat(ui.personChangeOwed.size).isEqualTo(2)
    sa.assertThat(ui.personChangeOwed("me")).isEqualTo("0.00")
    sa.assertThat(ui.personChangeOwed("bob")).isEqualTo("0.00")
    sa.assertThat(ui.tookFromError.size).isEqualTo(0)
    sa.assertThat(ui.notEnough).isEqualTo(true)
    sa.assertThat(ui.isSquare.filter(_._1 == "me").head._2).isEqualTo(new IsSquare(ui.squareMsgUnderpaid(me, 5), true, false))
    sa.assertThat(ui.isSquare.filter(_._1 == "bob").head._2).isEqualTo(new IsSquare(ui.squareMsgIsSquare(bob), false, false))

    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("bobOverpaysAndTakesChnage") {
    val bob = createPerson("Bob", 0)
    me.owesItems = 10
    me.paid = 5
    bob.owesItems = 20
    bob.paid = 35
    ui.setPeople(Array(me, bob))
    ui.init()

    ui.tookFromUI(bob, "10")

    sa.assertThat(ui.totalPaid).isEqualTo("40.00")
    sa.assertThat(ui.changeDue).isEqualTo("10.00")
    sa.assertThat(ui.paidFromError.size).isEqualTo(0)
    sa.assertThat(ui.changeErrorShown).isEqualTo(false)
    sa.assertThat(ui.personChangeOwed.size).isEqualTo(2)
    sa.assertThat(ui.personChangeOwed("me")).isEqualTo("0.00")
    sa.assertThat(ui.personChangeOwed("bob")).isEqualTo("10.00")
    sa.assertThat(ui.tookFromError.size).isEqualTo(0)
    sa.assertThat(ui.notEnough).isEqualTo(false)
    sa.assertThat(ui.isSquare.filter(_._1 == "me").head._2).isEqualTo(new IsSquare(ui.squareMsgUnderpaid(me, 5), true, false))
    sa.assertThat(ui.isSquare.filter(_._1 == "bob").head._2).isEqualTo(new IsSquare(ui.squareMsgOverpaidAndTookSomeChange(bob, 15, 10), false, true))

    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("bobOverpaysAndTakesChnageSquaringHim") {
    val bob = createPerson("Bob", 0)
    me.owesItems = 10
    me.paid = 10
    bob.owesItems = 20
    bob.paid = 35
    ui.setPeople(Array(me, bob))
    ui.init()
    ui.recalc()

    ui.tookFromUI(bob, "15")

    sa.assertThat(ui.totalPaid).isEqualTo("45.00")
    sa.assertThat(ui.changeDue).isEqualTo("15.00")
    sa.assertThat(ui.paidFromError.size).isEqualTo(0)
    sa.assertThat(ui.changeErrorShown).isEqualTo(false)
    sa.assertThat(ui.personChangeOwed.size).isEqualTo(2)
    sa.assertThat(ui.personChangeOwed("me")).isEqualTo("0.00")
    sa.assertThat(ui.personChangeOwed("bob")).isEqualTo("15.00")
    sa.assertThat(ui.tookFromError.size).isEqualTo(0)
    sa.assertThat(ui.notEnough).isEqualTo(false)
    sa.assertThat(ui.isSquare.filter(_._1 == "me").head._2).isEqualTo(new IsSquare(ui.squareMsgIsSquare(me), false, false))
    sa.assertThat(ui.isSquare.filter(_._1 == "bob").head._2).isEqualTo(new IsSquare(ui.squareMsgIsSquare(bob), false, false))

    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("WOWbobPaysForMe") {
    val bob = createPerson("Bob", 0)
    bob.paid = 5
    me.owesItems = 10
    me.paid = 5
    ui.setPeople(Array(me, bob))
    ui.recalc()

    sa.assertThat(ui.isSquare.filter(_._1 == "me").head._2).isEqualTo(new IsSquare(ui.squareMsgUnderpaid(me, 5), true, false))
    sa.assertThat(ui.isSquare.filter(_._1 == "bob").head._2).isEqualTo(new IsSquare(ui.squareMsgOverpaid(bob, 5), false, true))

    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("WOWtwoPayForMe") {
    val bob = createPerson("Bob", 0)
    val charlie = createPerson("Charlie", 0)
    bob.paid = 2.50
    charlie.paid = 2.50
    me.owesItems = 10
    me.paid = 5
    ui.setPeople(Array(me, bob, charlie))
    ui.recalc()

    sa.assertThat(ui.isSquare.filter(_._1 == "me").head._2).isEqualTo(new IsSquare(ui.squareMsgUnderpaid(me, 5), true, false))
    sa.assertThat(ui.isSquare.filter(_._1 == "bob").head._2).isEqualTo(new IsSquare(ui.squareMsgOverpaid(bob, 2.5), false, true))
    sa.assertThat(ui.isSquare.filter(_._1 == "charlie").head._2).isEqualTo(new IsSquare(ui.squareMsgOverpaid(charlie, 2.5), false, true))

    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("WOWbobPaysForTwo") {
    val bob = createPerson("Bob", 0)
    val charlie = createPerson("Charlie", 0)
    bob.paid = 10
    charlie.owesItems = 5
    me.owesItems = 5
    ui.setPeople(Array(me, bob, charlie))
    ui.recalc()

    sa.assertThat(ui.isSquare.filter(_._1 == "me").head._2).isEqualTo(new IsSquare(ui.squareMsgUnderpaid(me, 5), true, false))
    sa.assertThat(ui.isSquare.filter(_._1 == "bob").head._2).isEqualTo(new IsSquare(ui.squareMsgOverpaid(bob, 10), false, true))
    sa.assertThat(ui.isSquare.filter(_._1 == "charlie").head._2).isEqualTo(new IsSquare(ui.squareMsgUnderpaid(charlie, 5), true, false))

    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("WOWbobPaysForTwoUnequal") {
    val bob = createPerson("Bob", 0)
    val charlie = createPerson("Charlie", 0)
    bob.paid = 10
    charlie.owesItems = 3
    me.owesItems = 7
    ui.setPeople(Array(me, bob, charlie))
    ui.recalc()

    sa.assertThat(ui.isSquare.filter(_._1 == "me").head._2).isEqualTo(new IsSquare(ui.squareMsgUnderpaid(me, 7), true, false))
    sa.assertThat(ui.isSquare.filter(_._1 == "bob").head._2).isEqualTo(new IsSquare(ui.squareMsgOverpaid(bob, 10), false, true))
    sa.assertThat(ui.isSquare.filter(_._1 == "charlie").head._2).isEqualTo(new IsSquare(ui.squareMsgUnderpaid(charlie, 3), true, false))

    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("twoPeopleOwe10And0Paid5And5") {
    val bob = createPerson("Bob", 0)
    me.owesItems = 10
    ui.setPeople(Array(me, bob))
    ui.init()
    ui.recalc()

    ui.paidFromUI(me, "5")
    ui.paidFromUI(bob, "5")

    sa.assertThat(ui.totalPaid).isEqualTo("10.00")
    sa.assertThat(ui.changeDue).isEqualTo("0.00")
    sa.assertThat(ui.changeBlockVisible).isEqualTo(false)
    sa.assertThat(ui.paidFromError.size).isEqualTo(0)
    sa.assertThat(ui.changeErrorShown).isEqualTo(false)
    sa.assertThat(ui.personChangeOwed.size).isEqualTo(2)
    sa.assertThat(ui.personChangeOwed("me")).isEqualTo("0.00")
    sa.assertThat(ui.personChangeOwed("bob")).isEqualTo("0.00")
    sa.assertThat(ui.tookFromError.size).isEqualTo(0)
    sa.assertThat(ui.notEnough).isEqualTo(false)

    sa.assertThat(ui.isSquare.filter(_._1 == "me").head._2).isEqualTo(new IsSquare(ui.squareMsgUnderpaid(me, 5), true, false))
    sa.assertThat(ui.isSquare.filter(_._1 == "bob").head._2).isEqualTo(new IsSquare(ui.squareMsgOverpaid(bob, 5), false, true))

    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("twoPeopleOwe10And0Paid5And15") {
    val bob = createPerson("Bob", 0)
    me.owesItems = 10
    ui.setPeople(Array(me, bob))
    ui.init()
    ui.recalc()

    ui.paidFromUI(me, "5")
    ui.paidFromUI(bob, "15")

    sa.assertThat(ui.totalPaid).isEqualTo("20.00")
    sa.assertThat(ui.changeDue).isEqualTo("10.00")
    sa.assertThat(ui.paidFromError.size).isEqualTo(0)
    sa.assertThat(ui.changeErrorShown).isEqualTo(false)
    sa.assertThat(ui.personChangeOwed.size).isEqualTo(2)
    sa.assertThat(ui.personChangeOwed("me")).isEqualTo("0.00")
    sa.assertThat(ui.personChangeOwed("bob")).isEqualTo("10.00")
    sa.assertThat(ui.tookFromError.size).isEqualTo(0)
    sa.assertThat(ui.notEnough).isEqualTo(false)
    sa.assertThat(ui.isSquare.filter(_._1 == "me").head._2).isEqualTo(new IsSquare(ui.squareMsgUnderpaid(me, 5), true, false))
    sa.assertThat(ui.isSquare.filter(_._1 == "bob").head._2).isEqualTo(new IsSquare(ui.squareMsgOverpaid(bob, 15), false, true))

    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("msgs") {
    sa.assertThat(ui.squareMsgOverpaidAndTookWayTooMuchChange(me,10,5)).contains("owe 5.00")
    sa.assertThat(ui.squareMsgUnderpaidAndTookTooMuchChange(me,10,5)).contains("owe 15.00")
    sa.assertThat(ui.squareMsgOverpaidAndTookTooLittleChange(me,10,5)).contains("owed 15.00")
    sa.assertThat(ui.squareMsgOverpaidAndTookSomeChange(me,10,5)).contains("owed 5.00")
    sa.assertThat(ui.squareMsgIsSquare(me)).contains("I am paid up")
    assert(sa.checkIsEmptyAndClear() === true)
  }


  test("twoPeopleOwe10And0Paid5And5Took5and0") {
    val bob = createPerson("Bob", 0)
    me.owesItems = 10
    ui.setPeople(Array(me, bob))
    ui.init()
    ui.recalc()

    ui.paidFromUI(me, "5")
    ui.paidFromUI(bob, "15")
    ui.tookFromUI(bob, "5")

    sa.assertThat(ui.totalPaid).isEqualTo("20.00")
    sa.assertThat(ui.changeDue).isEqualTo("10.00")
    sa.assertThat(ui.changeBlockVisible).isEqualTo(true)
    sa.assertThat(ui.changeErrorShown).isEqualTo(false)
    sa.assertThat(ui.personChangeOwed.size).isEqualTo(2)
    sa.assertThat(ui.personChangeOwed("me")).isEqualTo("0.00")
    sa.assertThat(ui.personChangeOwed("bob")).isEqualTo("10.00")
    sa.assertThat(ui.tookFromError.size).isEqualTo(0)
    sa.assertThat(ui.notEnough).isEqualTo(false)

    sa.assertThat(ui.isSquare.filter(_._1 == "me").head._2).isEqualTo(new IsSquare(ui.squareMsgUnderpaid(me, 5), true, false))
    sa.assertThat(ui.isSquare.filter(_._1 == "bob").head._2).isEqualTo(new IsSquare(ui.squareMsgOverpaidAndTookSomeChange(bob, 15, 5), false, true))

    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("weirdBug") {
    val charlie = createPerson("Charlie", 0)
    me.owesItems = 28.64
    charlie.owesItems = 36.46
    ui.setPeople(Array(me, charlie))
    ui.init()
    ui.recalc()

    ui.paidFromUI(me, "30")
    ui.paidFromUI(charlie, "40")
    ui.tookFromUI(me, "1.5")
    ui.tookFromUI(charlie, "3")

    sa.assertThat(ui.totalOwed).isEqualTo("65.10")
    sa.assertThat(ui.totalPaid).isEqualTo("70.00")
    sa.assertThat(ui.changeDue).isEqualTo("4.90")
    sa.assertThat(ui.changeBlockVisible).isEqualTo(true)
    sa.assertThat(ui.paidFromError.size).isEqualTo(0)
    sa.assertThat(ui.changeErrorShown).isEqualTo(false)
    sa.assertThat(ui.personChangeOwed.size).isEqualTo(2)
    sa.assertThat(ui.personChangeOwed("me")).isEqualTo("1.36")
    sa.assertThat(ui.personChangeOwed("charlie")).isEqualTo("3.54")
    sa.assertThat(ui.tookFromError.size).isEqualTo(0)
    sa.assertThat(ui.notEnough).isEqualTo(false)
    sa.assertThat(ui.isSquare.filter(_._1 == "me").head._2).isEqualTo(new IsSquare(ui.squareMsgOverpaidAndTookWayTooMuchChange(me, 1.36, 0.14), true, false))
    sa.assertThat(ui.isSquare.filter(_._1 == "charlie").head._2).isEqualTo(new IsSquare(ui.squareMsgOverpaidAndTookSomeChange(charlie, 3.54, 3), false, true))

    assert(sa.checkIsEmptyAndClear() === true)

  }
}