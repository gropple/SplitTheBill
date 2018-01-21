import android.app.Activity
import android.os.Build
import android.widget.{Button, TextView, EditText}
import com.splitthebill.{DaApp, R, TotalsScreen}
import helpers.{GooglePlayServicesUtilShadow, SoftAssertions}
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.scalatest.{FunSuite, BeforeAndAfter, FlatSpec, RobolectricSuite}

@Config( emulateSdk = 18, reportSdk = 18, resourceDir = "bin/resources/res", shadows=Array(classOf[GooglePlayServicesUtilShadow]) )
class TotalScreenTest extends FunSuite with RobolectricSuite with BeforeAndAfter {
  var sa: SoftAssertions = null
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
    app.reset()
  }

  test ("total1") {
    total.setText("123.12")
    sa.assertThat(calcTotal.getText.toString).isEqualTo("123.12")
    assert (sa.checkIsEmptyAndClear() === true)
  }

  test ("totalRounding") {
    total.setText("123.12678")
    sa.assertThat(calcTotal.getText.toString).isEqualTo("123.13")
    assert (sa.checkIsEmptyAndClear() === true)
  }

  test ("totalRounding2") {
    total.setText("123.123")
    sa.assertThat(calcTotal.getText.toString).isEqualTo("123.12")
    assert (sa.checkIsEmptyAndClear() === true)
  }

  test ("pct") {
    total.setText("100")
    tipPct.setText("20")
    sa.assertThat(calcTotal.getText.toString).isEqualTo("120.00")
    assert (sa.checkIsEmptyAndClear() === true)
  }

  test ("pctTotalChange") {
    total.setText("100")
    tipPct.setText("20")
    sa.assertThat(calcTotal.getText.toString).isEqualTo("120.00")
    total.setText("200")
    sa.assertThat(calcTotal.getText.toString).isEqualTo("240.00")
    tipPct.setText("10")
    sa.assertThat(calcTotal.getText.toString).isEqualTo("220.00")
    assert (sa.checkIsEmptyAndClear() === true)
  }

  test ("amount") {
    total.setText("100")
    tipAmount.setText("20")
    sa.assertThat(calcTotal.getText.toString).isEqualTo("120.00")
    assert (sa.checkIsEmptyAndClear() === true)
  }

  test ("amountTotalChange") {
    total.setText("100")
    tipAmount.setText("20")
    sa.assertThat(calcTotal.getText.toString).isEqualTo("120.00")
    total.setText("200")
    sa.assertThat(calcTotal.getText.toString).isEqualTo("220.00")
    tipAmount.setText("10")
    sa.assertThat(calcTotal.getText.toString).isEqualTo("210.00")
    assert (sa.checkIsEmptyAndClear() === true)
  }

  test ("pctOfZero") {
    tipPct.setText("20")
    sa.assertThat(calcTotal.getText.toString).isEqualTo("0.00")
    total.setText("0")
    sa.assertThat(calcTotal.getText.toString).isEqualTo("0.00")
    assert (sa.checkIsEmptyAndClear() === true)
  }

  test ("pctSet") {
    total.setText("200")
    tipPct.setText("20")
    tipAmount.setText("20")
    sa.assertThat(tipPct.getText.toString).isEqualTo("")
    sa.assertThat(calcTotal.getText.toString).isEqualTo("220.00")
    assert (sa.checkIsEmptyAndClear() === true)
  }

  test ("amountSet") {
    total.setText("200")
    tipAmount.setText("20")
    tipPct.setText("20")
    sa.assertThat(tipAmount.getText.toString).isEqualTo("")
    sa.assertThat(calcTotal.getText.toString).isEqualTo("240.00")
    assert (sa.checkIsEmptyAndClear() === true)
  }

}
