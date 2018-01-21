import android.app.Activity
import android.widget.{Button, EditText, TextView}
import com.splitthebill.{DaApp, R, TotalsScreen}
import helpers.{GooglePlayServicesUtilShadow, SoftAssertions}
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.scalatest.{BeforeAndAfter, FunSuite, RobolectricSuite}

@Config( emulateSdk = 18, reportSdk = 18, resourceDir = "bin/resources/res", shadows=Array(classOf[GooglePlayServicesUtilShadow]) )
class TotalScreenRetainTest extends FunSuite with RobolectricSuite with BeforeAndAfter {
  var sa: SoftAssertions = null
  var itemsTotal:EditText = null
  var total:EditText = null
  var tipPct:EditText = null
  var tipAmount:EditText = null
  var calcTotal:TextView = null
  var next:Button = null
  var activity: Activity = null
  var app: DaApp = null

  before {
    sa = new SoftAssertions

    activity = Robolectric.setupActivity(classOf[TotalsScreen])
    itemsTotal = activity.findViewById(R.id.itemsTotal).asInstanceOf[EditText]
    total = activity.findViewById(R.id.totalInput).asInstanceOf[EditText]
    tipPct = activity.findViewById(R.id.tipPct).asInstanceOf[EditText]
    tipAmount = activity.findViewById(R.id.tipAmount).asInstanceOf[EditText]
    calcTotal = activity.findViewById(R.id.calcTotal).asInstanceOf[TextView]
    next = activity.findViewById(R.id.totalsNext).asInstanceOf[Button]
    app = activity.getApplicationContext.asInstanceOf[DaApp]

  }

  after {
    if (!sa.isChecked) {
      assert(sa.checkIsEmptyAndClear()===true)
    }
  }

  test ("init") {
    sa.assertThat(total.getText.toString).isEqualTo("")
    sa.assertThat(tipPct.getText.toString).isEqualTo("")
    sa.assertThat(tipAmount.getText.toString).isEqualTo("")
    sa.assertThat(calcTotal.getText.toString).isEqualTo("0.00")

    itemsTotal.setText("110.456789")
    total.setText("123.456789")
    tipPct.setText("23.45656")

    sa.assertThat(calcTotal.getText.toString).isEqualTo("152.42")

    assert (sa.checkIsEmptyAndClear() === true)
  }

  test ("retain") {
    sa.assertThat(itemsTotal.getText.toString).isEqualTo("110.46")
    sa.assertThat(total.getText.toString).isEqualTo("123.46")
    sa.assertThat(tipPct.getText.toString).isEqualTo("23.46")
    sa.assertThat(tipAmount.getText.toString).isEqualTo("")
    sa.assertThat(calcTotal.getText.toString).isEqualTo("152.42")

    assert (sa.checkIsEmptyAndClear() === true)
  }


}
