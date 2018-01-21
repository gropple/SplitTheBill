import android.app.Activity
import android.view.View
import android.widget._
import com.splitthebill._
import helpers.{GooglePlayServicesUtilShadow, SoftAssertions}
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.scalatest.{BeforeAndAfter, FunSuite, RobolectricSuite}

@Config( emulateSdk = 18, reportSdk = 18, resourceDir = "bin/resources/res", shadows=Array(classOf[GooglePlayServicesUtilShadow]) )
class SplitScreenTest extends FunSuite with RobolectricSuite with BeforeAndAfter {
  var sa: SoftAssertions = null

  var ssItemsTotal: TextView = null
  var ssTaxAndServiceTotal: TextView = null
  var splitPeople: TableLayout = null
  var splitItemsError: TextView = null
  var splitOthersError: TextView = null
  var next: Button = null
  var back: Button = null
  var app: DaApp = null

  var activity: Activity = null

  before {
    sa = new SoftAssertions
    activity = Robolectric.setupActivity(classOf[SplitScreen])

    ssItemsTotal = activity.findViewById(R.id.ssItemsTotal).asInstanceOf[TextView]
    ssTaxAndServiceTotal = activity.findViewById(R.id.ssTaxAndServiceTotal).asInstanceOf[TextView]
    splitPeople = activity.findViewById(R.id.splitTable).asInstanceOf[TableLayout]
    splitItemsError = activity.findViewById(R.id.splitItemsError).asInstanceOf[TextView]
    splitOthersError = activity.findViewById(R.id.splitTaxAndServiceError).asInstanceOf[TextView]
    back = activity.findViewById(R.id.splitBack).asInstanceOf[Button]
    next = activity.findViewById(R.id.splitBack).asInstanceOf[Button]

    app = activity.getApplicationContext.asInstanceOf[DaApp]
  }

  after {
    if (!sa.isChecked) {
      assert(sa.checkIsEmptyAndClear() === true)
    }
  }

  def createAndInsert(name: String, checked: Boolean) = {
    var p = new Person
    p.name = name
    p.checked = checked
    p.avatarFilename = app.di.daos.personDao.nextPersonAvatarFilename
    app.people += p
    app.di.daos.personDao.insert(p)
  }

  test("init") {
    sa.assertThat(app.people.size).isEqualTo(1)
    sa.assertThat(app.people.count(_.uuid.get == "me")).isEqualTo(1)
    sa.assertThat(ssItemsTotal.getText.toString).isEqualTo("0.00")
    sa.assertThat(ssTaxAndServiceTotal.getText.toString).isEqualTo("0.00")
    checkRow(0, "Me", "", "0.00", "", "0.00")
    sa.assertThat(splitItemsError.getVisibility).isEqualTo(View.GONE)
    sa.assertThat(splitOthersError.getVisibility).isEqualTo(View.GONE)
    assert(sa.checkIsEmptyAndClear() === true)
    app.reset()
  }

  test("initFocusShift") {
    checkRow(0, "Me", "", "0.00", "", "0.00")
    rowItem(0).requestFocus()
    checkRow(0, "Me", "", "0.00", "", "0.00")
    rowOthers(0).requestFocus()
    checkRow(0, "Me", "", "0.00", "", "0.00")
    rowItem(0).requestFocus()
    checkRow(0, "Me", "", "0.00", "", "0.00")
    assert(sa.checkIsEmptyAndClear() === true)
    app.reset()
  }

  test("initSetAmount") {
    val me = app.people.filter(_.name == "Me").head
    me.splittingItems = false
    me.owesItems = 1
    activity.asInstanceOf[SplitScreen].refetchAndRedraw()

    checkRow(0, "Me", "1.00", "", "", "0.00")
    sa.assertThat(splitItemsError.getVisibility).isEqualTo(View.VISIBLE)
    sa.assertThat(splitOthersError.getVisibility).isEqualTo(View.GONE)
    assert(sa.checkIsEmptyAndClear() === true)
    app.reset()
  }


  test("initMultiple") {
    createAndInsert("Bob", true)
    app.itemsTotalValue = 100
    app.calcTotalValue = 105
    activity.asInstanceOf[SplitScreen].refetchAndRedraw()

    sa.assertThat(ssItemsTotal.getText.toString).isEqualTo("100.00")
    sa.assertThat(ssTaxAndServiceTotal.getText.toString).isEqualTo("5.00")
    checkRow(0, "Me", "", "50.00", "", "2.50")
    checkRow(1, "Bob", "", "50.00", "", "2.50")
    sa.assertThat(splitItemsError.getVisibility).isEqualTo(View.GONE)
    sa.assertThat(splitOthersError.getVisibility).isEqualTo(View.GONE)
    assert(sa.checkIsEmptyAndClear() === true)
    app.reset()
  }

  test("init3") {
    createAndInsert("Bob", true)
    createAndInsert("Charlie", true)
    app.itemsTotalValue = 100
    app.calcTotalValue = 105
    activity.asInstanceOf[SplitScreen].refetchAndRedraw()

    checkRow(0, "Me", "", "33.33", "", "1.67")
    checkRow(1, "Bob", "", "33.33", "", "1.67")
    checkRow(2, "Charlie", "", "33.33", "", "1.67")
    sa.assertThat(splitItemsError.getVisibility).isEqualTo(View.GONE)
    sa.assertThat(splitOthersError.getVisibility).isEqualTo(View.GONE)
    assert(sa.checkIsEmptyAndClear() === true)
    app.reset()
  }

  test("changeAmount") {
    createAndInsert("Bob", true)
    createAndInsert("Charlie", true)
    app.itemsTotalValue = 100
    app.calcTotalValue = 105
    val bob = app.people.filter(_.name == "Bob").head
    bob.splittingItems = false
    bob.owesItems = 0
    activity.asInstanceOf[SplitScreen].refetchAndRedraw()

    sa.assertThat(app.people.filter(_.name == "Me").head.owesItems).isEqualTo(50)
    sa.assertThat(app.people.filter(_.name == "Bob").head.owesItems).isEqualTo(0)
    sa.assertThat(app.people.filter(_.name == "Charlie").head.owesItems).isEqualTo(50)

    checkRow(0, "Me", "", "50.00", "", "2.50")
    checkRow(1, "Bob", "0.00", "", "", "0.00")
    checkRow(2, "Charlie", "", "50.00", "", "2.50")
    sa.assertThat(splitItemsError.getVisibility).isEqualTo(View.GONE)
    sa.assertThat(splitOthersError.getVisibility).isEqualTo(View.GONE)
    assert(sa.checkIsEmptyAndClear() === true)
    app.reset()
  }


  test("changeOther") {
    createAndInsert("Bob", true)
    createAndInsert("Charlie", true)
    app.itemsTotalValue = 100
    app.calcTotalValue = 105
    val bob = app.people.filter(_.name == "Bob").head
    bob.splittingOthers = false
    bob.owesOthers = 3
    activity.asInstanceOf[SplitScreen].refetchAndRedraw()

    sa.assertThat(app.people.filter(_.name == "Me").head.owesOthers).isEqualTo(1)
    sa.assertThat(app.people.filter(_.name == "Bob").head.owesOthers).isEqualTo(3)
    sa.assertThat(app.people.filter(_.name == "Charlie").head.owesOthers).isEqualTo(1)

    checkRow(0, "Me", "", "33.33", "", "1.00")
    checkRow(1, "Bob", "", "33.33", "3.00", "")
    checkRow(2, "Charlie", "", "33.33", "", "1.00")
    sa.assertThat(splitItemsError.getVisibility).isEqualTo(View.GONE)
    sa.assertThat(splitOthersError.getVisibility).isEqualTo(View.GONE)
    assert(sa.checkIsEmptyAndClear() === true)
    app.reset()
  }

  test("overpayAmount") {
    createAndInsert("Bob", true)
    createAndInsert("Charlie", true)
    app.itemsTotalValue = 100
    app.calcTotalValue = 105
    val bob = app.people.filter(_.name == "Bob").head
    val charlie = app.people.filter(_.name == "Charlie").head
    bob.splittingItems = false
    bob.owesItems = 100
    charlie.splittingItems = false
    charlie.owesItems = 100.12323123
    activity.asInstanceOf[SplitScreen].refetchAndRedraw()

    sa.assertThat(app.people.filter(_.name == "Me").head.owesItems).isEqualTo(0)
    sa.assertThat(app.people.filter(_.name == "Bob").head.owesItems).isEqualTo(100)
    sa.assertThat(app.people.filter(_.name == "Charlie").head.owesItems).isEqualTo(100.12323123)
    sa.assertThat(app.people.filter(_.name == "Me").head.owesOthers).isEqualTo(0)
    sa.assertThat(app.people.filter(_.name == "Bob").head.owesOthers).isEqualTo(2.50)
    sa.assertThat(app.people.filter(_.name == "Charlie").head.owesOthers).isEqualTo(2.50)

    checkRow(0, "Me", "", "0.00", "", "0.00")
    checkRow(1, "Bob", "100.00", "", "", "2.50")
    checkRow(2, "Charlie", "100.12", "", "", "2.50")
    sa.assertThat(splitItemsError.getVisibility).isEqualTo(View.VISIBLE)
    sa.assertThat(splitItemsError.getText.toString).isEqualTo(s"${Shared.prettifyFloat(100+100.12323123, 2)} is more than item total 100.00.")

    sa.assertThat(splitOthersError.getVisibility).isEqualTo(View.GONE)
    assert(sa.checkIsEmptyAndClear() === true)
    app.reset()
  }

  test("overpayAmountTogether") {
    createAndInsert("Bob", true)
    createAndInsert("Charlie", true)
    app.itemsTotalValue = 100
    app.calcTotalValue = 105
    val bob = app.people.filter(_.name == "Bob").head
    val charlie = app.people.filter(_.name == "Charlie").head
    bob.splittingItems = false
    bob.owesItems = 80
    charlie.splittingItems = false
    charlie.owesItems = 80
    activity.asInstanceOf[SplitScreen].refetchAndRedraw()

    sa.assertThat(app.people.filter(_.name == "Me").head.owesItems).isEqualTo(0)
    sa.assertThat(app.people.filter(_.name == "Bob").head.owesItems).isEqualTo(80)
    sa.assertThat(app.people.filter(_.name == "Charlie").head.owesItems).isEqualTo(80)
    sa.assertThat(app.people.filter(_.name == "Me").head.owesOthers).isEqualTo(0)
    sa.assertThat(app.people.filter(_.name == "Bob").head.owesOthers).isEqualTo(2.50)
    sa.assertThat(app.people.filter(_.name == "Charlie").head.owesOthers).isEqualTo(2.50)

    checkRow(0, "Me", "", "0.00", "", "0.00")
    checkRow(1, "Bob", "80.00", "", "", "2.50")
    checkRow(2, "Charlie", "80.00", "", "", "2.50")
    sa.assertThat(splitItemsError.getVisibility).isEqualTo(View.VISIBLE)
    sa.assertThat(splitItemsError.getText.toString).isEqualTo(s"${Shared.prettifyFloat(160, 2)} is more than item total 100.00.")

    sa.assertThat(splitOthersError.getVisibility).isEqualTo(View.GONE)
    assert(sa.checkIsEmptyAndClear() === true)
    app.reset()
  }

  test("overpayBoth") {
    createAndInsert("Bob", true)
    createAndInsert("Charlie", true)
    app.itemsTotalValue = 100
    app.calcTotalValue = 105
    val bob = app.people.filter(_.name == "Bob").head
    val charlie = app.people.filter(_.name == "Charlie").head
    bob.splittingItems = false
    bob.owesItems = 200
    charlie.splittingOthers = false
    charlie.owesOthers = 10.12323123
    activity.asInstanceOf[SplitScreen].refetchAndRedraw()

    sa.assertThat(app.people.filter(_.name == "Me").head.owesItems).isEqualTo(0)
    sa.assertThat(app.people.filter(_.name == "Bob").head.owesItems).isEqualTo(200)
    sa.assertThat(app.people.filter(_.name == "Charlie").head.owesItems).isEqualTo(0)
    sa.assertThat(app.people.filter(_.name == "Me").head.owesOthers).isEqualTo(0)
    sa.assertThat(app.people.filter(_.name == "Bob").head.owesOthers).isEqualTo(0)
    sa.assertThat(app.people.filter(_.name == "Charlie").head.owesOthers).isEqualTo(10.12323123)

    checkRow(0, "Me", "", "0.00", "", "0.00")
    checkRow(1, "Bob", "200.00", "", "", "0.00")
    checkRow(2, "Charlie", "", "0.00", "10.12", "")
    sa.assertThat(splitItemsError.getVisibility).isEqualTo(View.VISIBLE)
    sa.assertThat(splitItemsError.getText.toString).isEqualTo(s"${Shared.prettifyFloat(200, 2)} is more than item total 100.00.")

    sa.assertThat(splitOthersError.getVisibility).isEqualTo(View.VISIBLE)
    sa.assertThat(splitOthersError.getText.toString).isEqualTo(s"${Shared.prettifyFloat(10.12323123, 2)} is more than others total 5.00.")
    assert(sa.checkIsEmptyAndClear() === true)
    app.reset()
  }


  test("adjustTip") {
    createAndInsert("Bob", true)
    createAndInsert("Charlie", true)
    app.itemsTotalValue = 100
    app.calcTotalValue = 105
    val bob = app.people.filter(_.name == "Bob").head
    val charlie = app.people.filter(_.name == "Charlie").head
    bob.splittingItems = false
    bob.owesItems = 10
    bob.splittingOthers = false
    bob.owesOthers = 1
    charlie.splittingItems = false
    charlie.owesItems = 10
    activity.asInstanceOf[SplitScreen].refetchAndRedraw()

    sa.assertThat(app.people.filter(_.name == "Me").head.owesItems).isEqualTo(80)
    sa.assertThat(app.people.filter(_.name == "Bob").head.owesItems).isEqualTo(10)
    sa.assertThat(app.people.filter(_.name == "Charlie").head.owesItems).isEqualTo(10)
    sa.assertThat(app.people.filter(_.name == "Me").head.owesOthers).isEqualTo(3.55555555555555555555555) // Me paid 80% of meal, 4 left on tip.  Paid 80/90 % of people that haven't set an others amount.
    sa.assertThat(app.people.filter(_.name == "Bob").head.owesOthers).isEqualTo(1)
    sa.assertThat(app.people.filter(_.name == "Charlie").head.owesOthers).isEqualTo(0.444444444444444444444)

    sa.assertThat(splitItemsError.getVisibility).isEqualTo(View.GONE)
    sa.assertThat(splitOthersError.getVisibility).isEqualTo(View.GONE)
    assert(sa.checkIsEmptyAndClear() === true)
    app.reset()
  }

  test("retainInit") {
    createAndInsert("Bob", true)
    createAndInsert("Charlie", true)
    app.itemsTotalValue = 100
    app.calcTotalValue = 105
    val bob = app.people.filter(_.name == "Bob").head
    val charlie = app.people.filter(_.name == "Charlie").head
    bob.splittingItems = false
    bob.owesItems = 10
    bob.splittingOthers = false
    bob.owesOthers = 1
    charlie.splittingItems = false
    charlie.owesItems = 10
    activity.asInstanceOf[SplitScreen].refetchAndRedraw()

    sa.assertThat(app.people.filter(_.name == "Me").head.owesItems).isEqualTo(80)
    sa.assertThat(app.people.filter(_.name == "Bob").head.owesItems).isEqualTo(10)
    sa.assertThat(app.people.filter(_.name == "Charlie").head.owesItems).isEqualTo(10)
    sa.assertThat(app.people.filter(_.name == "Me").head.owesOthers).isEqualTo(3.55555555555555555555555) // Me paid 80% of meal, 4 left on tip.  Paid 80/90 % of people that haven't set an others amount.
    sa.assertThat(app.people.filter(_.name == "Bob").head.owesOthers).isEqualTo(1)
    sa.assertThat(app.people.filter(_.name == "Charlie").head.owesOthers).isEqualTo(0.444444444444444444444)

    sa.assertThat(splitItemsError.getVisibility).isEqualTo(View.GONE)
    sa.assertThat(splitOthersError.getVisibility).isEqualTo(View.GONE)
    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("retain") {
    checkRow(0, "Me", "", "80.00", "", "3.56")
    checkRow(1, "Bob", "10.00", "", "1.00", "")
    checkRow(2, "Charlie", "10.00", "", "", "0.44")

    sa.assertThat(splitItemsError.getVisibility).isEqualTo(View.GONE)
    sa.assertThat(splitOthersError.getVisibility).isEqualTo(View.GONE)
    assert(sa.checkIsEmptyAndClear() === true)
    app.reset()
  }

  test("mathsExprAmount") {
    val me = app.people.filter(_.name == "Me").head
    me.splittingItems = false
    me.owesItemsRaw = "1+2"
    me.owesItems = 3
    activity.asInstanceOf[SplitScreen].refetchAndRedraw()

    checkRow(0, "Me", "3.00", "", "", "0.00")
    sa.assertThat(splitItemsError.getVisibility).isEqualTo(View.VISIBLE)
    sa.assertThat(splitOthersError.getVisibility).isEqualTo(View.GONE)
    sa.assertThat(rowItem(0).getError == null).isEqualTo(true)

    sa.assertThat(rowItem(0).requestFocus()).isEqualTo(true)
    sa.assertThat(rowItem(0).getText.toString).isEqualTo("1+2")
    sa.assertThat(rowItem(0).getError == null).isEqualTo(true)
    rowOthers(0).requestFocus()
    sa.assertThat(rowItem(0).getError == null).isEqualTo(true)
    sa.assertThat(rowItem(0).getText.toString).isEqualTo("3.00")

    rowItem(0).requestFocus()
    rowItem(0).setText("3*4")
    sa.assertThat(rowItem(0).getError == null).isEqualTo(true)
    checkRow(0, "Me", "3*4", "", "", "0.00")

    rowOthers(0).requestFocus()
    sa.assertThat(rowItem(0).getError == null).isEqualTo(true)
    checkRow(0, "Me", "12.00", "", "", "0.00")
    sa.assertThat(splitItemsError.getVisibility).isEqualTo(View.VISIBLE)

    rowItem(0).requestFocus()
    rowItem(0).setText("3*")
    sa.assertThat(rowItem(0).getError).isNull()
    checkRow(0, "Me", "3*", "", "", "0.00")

    rowOthers(0).requestFocus()

    sa.assertThat(rowItem(0).getError).isNotEmpty
    checkRow(0, "Me", "3*", "", "", "0.00")
    sa.assertThat(splitItemsError.getVisibility).isEqualTo(View.GONE)

    assert(sa.checkIsEmptyAndClear() === true)
    app.reset()
  }


  test("mathsExprAmountRetainInit") {
    val me = app.people.filter(_.name == "Me").head
    me.splittingItems = false
    me.owesItemsRaw = "1+2"
    me.owesItems = 3
    activity.asInstanceOf[SplitScreen].refetchAndRedraw()
    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("mathsExprAmountRetain") {
    checkRow(0, "Me", "3.00", "", "", "0.00")
    sa.assertThat(splitItemsError.getVisibility).isEqualTo(View.VISIBLE)
    sa.assertThat(splitOthersError.getVisibility).isEqualTo(View.GONE)
    sa.assertThat(rowItem(0).getError == null).isEqualTo(true)
    sa.assertThat(rowItem(0).requestFocus()).isEqualTo(true)
    sa.assertThat(rowItem(0).getText.toString).isEqualTo("1+2")
    sa.assertThat(rowItem(0).getError == null).isEqualTo(true)

    assert(sa.checkIsEmptyAndClear() === true)
    app.reset()
  }

  test("mathsExprAmountRetainErrorInit") {
    val me = app.people.filter(_.name == "Me").head
    me.splittingItems = false
    me.owesItemsRaw = "1+"
    me.owesItems = 0
    activity.asInstanceOf[SplitScreen].refetchAndRedraw()
    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("mathsExprAmountRetainError") {
    checkRow(0, "Me", "1+", "", "", "0.00")
    sa.assertThat(splitItemsError.getVisibility).isEqualTo(View.GONE)
    sa.assertThat(splitOthersError.getVisibility).isEqualTo(View.GONE)
    sa.assertThat(rowItem(0).getError == null).isEqualTo(false)
    sa.assertThat(rowItem(0).requestFocus()).isEqualTo(true)
    sa.assertThat(rowItem(0).getText.toString).isEqualTo("1+")
    sa.assertThat(rowItem(0).getError == null).isEqualTo(false)

    assert(sa.checkIsEmptyAndClear() === true)
    app.reset()
  }



  test("mathsExprOthers") {
    val me = app.people.filter(_.name == "Me").head
    me.splittingOthers = false
    me.owesOthersRaw = "1+2"
    me.owesOthers = 3
    activity.asInstanceOf[SplitScreen].refetchAndRedraw()

    checkRow(0, "Me", "", "0.00", "3.00", "")
    sa.assertThat(splitItemsError.getVisibility).isEqualTo(View.GONE)
    sa.assertThat(splitOthersError.getVisibility).isEqualTo(View.VISIBLE)
    sa.assertThat(rowOthers(0).getError == null).isEqualTo(true)

    sa.assertThat(rowOthers(0).requestFocus()).isEqualTo(true)
    sa.assertThat(rowOthers(0).getText.toString).isEqualTo("1+2")
    sa.assertThat(rowOthers(0).getError == null).isEqualTo(true)
    rowItem(0).requestFocus()
    sa.assertThat(rowOthers(0).getError == null).isEqualTo(true)
    sa.assertThat(rowOthers(0).getText.toString).isEqualTo("3.00")

    rowOthers(0).requestFocus()
    rowOthers(0).setText("3*4")
    sa.assertThat(rowOthers(0).getError == null).isEqualTo(true)
    checkRow(0, "Me", "", "0.00", "3*4", "")

    rowItem(0).requestFocus()
    sa.assertThat(rowOthers(0).getError == null).isEqualTo(true)
    checkRow(0, "Me", "", "0.00", "12.00", "")
    sa.assertThat(splitOthersError.getVisibility).isEqualTo(View.VISIBLE)

    rowOthers(0).requestFocus()
    rowOthers(0).setText("3*")
    sa.assertThat(rowOthers(0).getError).isNull()
    checkRow(0, "Me", "", "0.00", "3*", "")

    rowItem(0).requestFocus()

    sa.assertThat(rowOthers(0).getError).isNotEmpty
    checkRow(0, "Me", "", "0.00", "3*", "")
    sa.assertThat(splitOthersError.getVisibility).isEqualTo(View.GONE)

    assert(sa.checkIsEmptyAndClear() === true)
    app.reset()
  }


  test("mathsExprOthersRetainInit") {
    val me = app.people.filter(_.name == "Me").head
    me.splittingOthers = false
    me.owesOthersRaw = "1+2"
    me.owesOthers = 3
    activity.asInstanceOf[SplitScreen].refetchAndRedraw()
    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("mathsExprOthersRetain") {
    checkRow(0, "Me", "", "0.00", "3.00", "")
    sa.assertThat(splitItemsError.getVisibility).isEqualTo(View.GONE)
    sa.assertThat(splitOthersError.getVisibility).isEqualTo(View.VISIBLE)
    sa.assertThat(rowOthers(0).getError == null).isEqualTo(true)
    sa.assertThat(rowOthers(0).requestFocus()).isEqualTo(true)
    sa.assertThat(rowOthers(0).getText.toString).isEqualTo("1+2")
    sa.assertThat(rowOthers(0).getError == null).isEqualTo(true)

    assert(sa.checkIsEmptyAndClear() === true)
    app.reset()
  }

  test("mathsExprOthersRetainErrorInit") {
    val me = app.people.filter(_.name == "Me").head
    me.splittingOthers = false
    me.owesOthersRaw = "1+"
    me.owesOthers = 0
    activity.asInstanceOf[SplitScreen].refetchAndRedraw()
    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("mathsExprOthersRetainError") {
    checkRow(0, "Me", "", "0.00", "1+", "")
    sa.assertThat(splitItemsError.getVisibility).isEqualTo(View.GONE)
    sa.assertThat(splitOthersError.getVisibility).isEqualTo(View.GONE)
    sa.assertThat(rowOthers(0).getError == null).isEqualTo(false)
    sa.assertThat(rowOthers(0).requestFocus()).isEqualTo(true)
    sa.assertThat(rowOthers(0).getText.toString).isEqualTo("1+")
    sa.assertThat(rowOthers(0).getError == null).isEqualTo(false)

    assert(sa.checkIsEmptyAndClear() === true)
    app.reset()
  }


  def rowName(row: Int): String = {
    splitPeople.getChildAt(row + 2).asInstanceOf[TableRow].getChildAt(1).asInstanceOf[TextView].getText.toString
  }

  def rowItem(row: Int): EditText = {
    splitPeople.getChildAt(row + 2).asInstanceOf[TableRow].getChildAt(2).asInstanceOf[EditText]
  }

  def rowOthers(row: Int): EditText = {
    splitPeople.getChildAt(row + 2).asInstanceOf[TableRow].getChildAt(3).asInstanceOf[EditText]
  }

  def checkRow(row: Int, name: String, itemVal: String, itemHint: String, othersVal: String, othersHint: String) = {
    sa.assertThat(rowName(row)).isEqualTo(name)
    sa.assertThat(rowItem(row).getText.toString).isEqualTo(itemVal)
    sa.assertThat(rowItem(row).getHint.toString).isEqualTo(itemHint)
    sa.assertThat(rowOthers(row).getText.toString).isEqualTo(othersVal)
    sa.assertThat(rowOthers(row).getHint.toString).isEqualTo(othersHint)
  }

}
