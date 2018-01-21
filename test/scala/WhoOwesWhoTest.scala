import android.app.Activity
import android.os.Build
import android.view.View
import android.widget._
import com.splitthebill._
import helpers.{GooglePlayServicesUtilShadow, SoftAssertions}
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.scalatest.{FunSuite, BeforeAndAfter, FlatSpec, RobolectricSuite}

@Config( emulateSdk = 18, reportSdk = 18, resourceDir = "bin/resources/res", shadows=Array(classOf[GooglePlayServicesUtilShadow]) )
class WhoOwesWhoTest extends FunSuite with RobolectricSuite with BeforeAndAfter {
  var sa: SoftAssertions = null
  var app: DaApp = null

  var ui: WhoOwesWhoUI = null
  var me: Person = null

  var paidTable: TableLayout = null
  var tookTable: TableLayout = null
  var summaryTable: TableLayout = null
  var changeBlock: LinearLayout = null
//  var next: Button = null
  var back: Button = null

  var activity: Activity = null

  before {
    sa = new SoftAssertions
    activity = Robolectric.setupActivity(classOf[WhoOwesWhoScreen])
    ui = activity.asInstanceOf[WhoOwesWhoUI]
    app = activity.getApplicationContext.asInstanceOf[DaApp]

    me = app.di.daos.personDao.get("me").get

    paidTable = activity.findViewById(R.id.paidTable).asInstanceOf[TableLayout]
    tookTable = activity.findViewById(R.id.tookTable).asInstanceOf[TableLayout]
    summaryTable = activity.findViewById(R.id.summaryTable).asInstanceOf[TableLayout]
    changeBlock = activity.findViewById(R.id.changeBlock).asInstanceOf[LinearLayout]
//    next = activity.findViewById(R.id.changeBlock).asInstanceOf[LinearLayout]
    back = activity.findViewById(R.id.wowBack).asInstanceOf[Button]

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
    checkPaidRow(0, "Me", "0.00", "")
    sa.assertThat(changeBlock.getVisibility).isEqualTo(View.GONE)
    checkSummaryRow(0, "I", ui.squareMsgIsSquare(me))
    assert(sa.checkIsEmptyAndClear() === true)
    app.reset()
  }


  test("retainBasicInit") {
    paidPaid(0).setText("10.00")
    checkPaidRow(0, "Me", "0.00", "10.00")
    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("retainBasic") {
    checkPaidRow(0, "Me", "0.00", "10.00")
    assert(sa.checkIsEmptyAndClear() === true)
    app.reset()
  }

  test("calcBasicRetainInit") {
    paidPaid(0).setText("2+3")
    checkPaidRow(0, "Me", "0.00", "2+3")
    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("calcBasicRetain") {
    checkPaidRow(0, "Me", "0.00", "5.00")
    sa.assertThat(paidPaid(0).requestFocus()).isTrue
    checkPaidRow(0, "Me", "0.00", "2+3")
    assert(sa.checkIsEmptyAndClear() === true)
    app.reset()
  }

  test("calcErrorRetainInit") {
    paidPaid(0).requestFocus()
    paidPaid(0).setText("2+")
    checkPaidRow(0, "Me", "0.00", "2+")
    sa.assertThat(paidPaid(0).getError).isNull()
    back.requestFocus()
    checkPaidRow(0, "Me", "0.00", "2+")
    sa.assertThat(paidPaid(0).getError).isNotNull
    paidPaid(0).requestFocus()
    checkPaidRow(0, "Me", "0.00", "2+")
    sa.assertThat(paidPaid(0).getError).isNotNull
    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("calcErrorRetain") {
    checkPaidRow(0, "Me", "0.00", "2+")
    sa.assertThat(paidPaid(0).getError).isNotNull
    paidPaid(0).requestFocus()
    checkPaidRow(0, "Me", "0.00", "2+")
    sa.assertThat(paidPaid(0).getError).isNotNull
    paidPaid(0).requestFocus()
    paidPaid(0).setText("2+3")
    checkPaidRow(0, "Me", "0.00", "2+3")
    // Almost certain this is broken in Robolectric - error has definitely cleared
//    sa.assertThat(paidPaid(0).getError).isNull
    back.requestFocus()
    checkPaidRow(0, "Me", "0.00", "5.00")
    sa.assertThat(paidPaid(0).getError).isNull
    paidPaid(0).requestFocus()
    checkPaidRow(0, "Me", "0.00", "2+3")

    assert(sa.checkIsEmptyAndClear() === true)
    app.reset()
  }

  def paidName(row: Int): String = {
    paidTable.getChildAt(row + 1).asInstanceOf[TableRow].getChildAt(1).asInstanceOf[TextView].getText.toString
  }

  def paidOwes(row: Int): TextView = {
    paidTable.getChildAt(row + 1).asInstanceOf[TableRow].getChildAt(2).asInstanceOf[TextView]
  }

  def paidPaid(row: Int): EditText = {
    paidTable.getChildAt(row + 1).asInstanceOf[TableRow].getChildAt(3).asInstanceOf[EditText]
  }

  def checkPaidRow(row: Int, name: String, owesVal: String, paidVal: String) = {
    sa.assertThat(paidName(row)).isEqualTo(name)
    sa.assertThat(paidOwes(row).getText.toString).isEqualTo(owesVal)
    sa.assertThat(paidPaid(row).getText.toString).isEqualTo(paidVal)
  }

  def checkSummaryRow(rowIdx: Int, name: String, v: String) = {
    val msg = summaryTable.getChildAt(rowIdx).asInstanceOf[TableRow].getChildAt(1).asInstanceOf[TextView].getText.toString

    sa.assertThat(msg.contains(name)).isEqualTo(true)
    sa.assertThat(msg).isEqualTo(v)
  }

}