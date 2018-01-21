import android.app.Activity
import android.os.Build
import android.widget._
import com.splitthebill.{AddPeopleScreen, DaApp, R, TotalsScreen}
import helpers.{GooglePlayServicesUtilShadow, SoftAssertions}
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.scalatest.{FunSuite, BeforeAndAfter, FlatSpec, RobolectricSuite}

@Config( emulateSdk = 18, reportSdk = 18, resourceDir = "bin/resources/res", shadows=Array(classOf[GooglePlayServicesUtilShadow]) )
class AddPeopleScreenRetainTest extends FunSuite with RobolectricSuite with BeforeAndAfter {
  var sa: SoftAssertions = null

  var peopleTable: TableLayout = null
  var addPerson: Button = null
  var back: Button = null
  var next: Button = null
  var app: DaApp = null

  var activity: Activity = null

  before {
    sa = new SoftAssertions
    activity = Robolectric.setupActivity(classOf[AddPeopleScreen])
    peopleTable = activity.findViewById(R.id.peopleTable).asInstanceOf[TableLayout]
    addPerson = activity.findViewById(R.id.addPerson).asInstanceOf[Button]
    back = activity.findViewById(R.id.addPeopleBack).asInstanceOf[Button]
    next = activity.findViewById(R.id.addPeopleNext).asInstanceOf[Button]
    app = activity.getApplicationContext.asInstanceOf[DaApp]
  }

  after {
    if (!sa.isChecked) {
      assert(sa.checkIsEmptyAndClear() === true)
    }
  }

  test("addPersonInit") {
    addPerson.performClick()
    addPerson.performClick()

    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("addPerson") {
    sa.assertThat(app.people.size).isEqualTo(3)
    sa.assertThat(app.people.count(_.uuid.get == "me")).isEqualTo(1)
    sa.assertThat(app.people.count(_.name == "Person 1")).isEqualTo(1)
    sa.assertThat(peopleTable.getChildCount).isEqualTo(3)

    sa.assertThat(rowCheckbox(0).isChecked).isEqualTo(true)
    sa.assertThat(meName().getText).isEqualTo("Me")

    sa.assertThat(rowCheckbox(1).isChecked).isEqualTo(true)
    sa.assertThat(rowName(1).getText.toString).isEqualTo("Person 1")

    sa.assertThat(rowCheckbox(2).isChecked).isEqualTo(true)
    sa.assertThat(rowName(2).getText.toString).isEqualTo("Person 2")

    assert(sa.checkIsEmptyAndClear() === true)

    app.reset()
  }

  test("checkInit") {
    addPerson.performClick()
    addPerson.performClick()

    rowCheckbox(0).performClick()
    rowCheckbox(2).performClick()

    sa.assertThat(rowCheckbox(0).isChecked).isEqualTo(false)
    sa.assertThat(rowCheckbox(1).isChecked).isEqualTo(true)
    sa.assertThat(rowCheckbox(2).isChecked).isEqualTo(false)

    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("check") {
    sa.assertThat(rowCheckbox(0).isChecked).isEqualTo(false)
    sa.assertThat(rowCheckbox(1).isChecked).isEqualTo(true)
    sa.assertThat(rowCheckbox(2).isChecked).isEqualTo(false)

    assert(sa.checkIsEmptyAndClear() === true)

    app.reset()
  }

  test("removePersonInit") {
    addPerson.performClick() // Add 1
    addPerson.performClick() // Add 2

    rowRemove(1).performClick() // Remove 1

    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("removePerson1") {
    sa.assertThat(app.people.size).isEqualTo(2)
    sa.assertThat(app.people.count(_.uuid.get == "me")).isEqualTo(1)
    sa.assertThat(app.people.count(_.name == "Person 2")).isEqualTo(1)
    sa.assertThat(peopleTable.getChildCount).isEqualTo(2)

    rowRemove(1).performClick() // Remove 2

    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("removePerson2") {
    sa.assertThat(app.people.size).isEqualTo(1)
    sa.assertThat(app.people.count(_.uuid.get == "me")).isEqualTo(1)
    sa.assertThat(peopleTable.getChildCount).isEqualTo(1)

    assert(sa.checkIsEmptyAndClear() === true)

    app.reset()
  }


  test("editPersonInit") {
    addPerson.performClick()
    rowName(1).setText("George")
    assert(sa.checkIsEmptyAndClear() === true)
  }

  test("editPerson") {
    sa.assertThat(rowName(1).getText.toString).isEqualTo("George")
    assert(sa.checkIsEmptyAndClear() === true)
    app.reset()
  }

  def rowCheckbox(row: Int): CheckBox = {
    peopleTable.getChildAt(row).asInstanceOf[TableRow].getChildAt(3).asInstanceOf[CheckBox]
  }

  def rowAvatar(row: Int): ImageView = {
    peopleTable.getChildAt(row).asInstanceOf[TableRow].getChildAt(1).asInstanceOf[ImageView]
  }

  def meName(): TextView = {
    peopleTable.getChildAt(0).asInstanceOf[TableRow].getChildAt(2).asInstanceOf[TextView]
  }

  def rowName(row: Int): EditText = {
    peopleTable.getChildAt(row).asInstanceOf[TableRow].getChildAt(2).asInstanceOf[EditText]
  }

  def rowRemove(row: Int): ImageButton = {
    peopleTable.getChildAt(row).asInstanceOf[TableRow].getChildAt(0).asInstanceOf[ImageButton]
  }


}