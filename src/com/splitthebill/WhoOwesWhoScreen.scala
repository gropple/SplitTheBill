package com.splitthebill

import android.app.Activity
import android.content.Intent
import android.graphics.{Bitmap, Rect}
import android.os.Bundle
import android.text.{Editable, InputType, TextWatcher}
import android.util.Log
import android.view.View.{OnClickListener, OnFocusChangeListener}
import android.view.{Menu, View, ViewGroup}
import android.widget._
import com.google.android.gms.analytics.GoogleAnalytics

case class IsSquare(msg: String, ower: Boolean, owee: Boolean)


class WhoOwesWhoScreen extends Activity with WhoOwesWhoUI {

  val TAG = getClass.toString

  var app: DaApp = null
  var paidTable: TableLayout = null
  var tookTable: TableLayout = null
  var summaryTable: TableLayout = null

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.who_owes_who)

    app = getApplicationContext.asInstanceOf[DaApp]

    app.initialise()

    if (app.people.isEmpty) {
      Shared.refetchPeople(app)
      // just for testing
      app.people.filter(_.uuid.get == "me").foreach(_.checked = true)
//      app.people.filter(_.uuid.get == "me").foreach(_.owesItems = 30)
//      app.people.filter(_.name == "Bob").foreach(_.checked = true)
//      app.people.filter(_.name == "Bob").foreach(_.owesItems = 40)
//      app.people.filter(_.name == "GJames").foreach(_.checked = true)
//      app.people.filter(_.name == "GJames").foreach(_.owesItems = 23)
    }

    paidTable = findViewById(R.id.paidTable).asInstanceOf[TableLayout]
    tookTable = findViewById(R.id.tookTable).asInstanceOf[TableLayout]
    summaryTable = findViewById(R.id.summaryTable).asInstanceOf[TableLayout]

    findViewById(R.id.summaryTableToHide).setVisibility(View.GONE)

//    Shared.setHeaderFont(this, R.id.wowsHeader1)
//    Shared.setHeaderFont(this, R.id.wowsHeader2)
//    Shared.setHeaderFont(this, R.id.wowsHeader3)

    findViewById(R.id.wowBack).setOnClickListener(new OnClickListener() {
      def onClick(v: View) {
        val intent: Intent = new Intent(WhoOwesWhoScreen.this, classOf[SplitScreen])
        startActivity(intent)
      }
    })

    while (paidTable.getChildCount > 3) {
      paidTable.removeViewAt(2)
    }

    while (tookTable.getChildCount > 1) {
      tookTable.removeViewAt(1)
    }

    val weightName = Shared.nameWeight
    val weightNumber = Shared.numberWeight

    // Headers and footers
    Shared.setupAvatarTableCell(paidTable.getChildAt(0).asInstanceOf[TableRow].getChildAt(1).asInstanceOf[TextView], false, weightName)
    Shared.setupAvatarTableCell(paidTable.getChildAt(0).asInstanceOf[TableRow].getChildAt(2).asInstanceOf[TextView], false, weightNumber)
    Shared.setupAvatarTableCell(paidTable.getChildAt(0).asInstanceOf[TableRow].getChildAt(3).asInstanceOf[TextView], false, weightNumber)
    Shared.setupAvatarTableCell(paidTable.getChildAt(paidTable.getChildCount - 1).asInstanceOf[TableRow].getChildAt(1).asInstanceOf[TextView], false, weightName)
    Shared.setupAvatarTableCell(paidTable.getChildAt(paidTable.getChildCount - 1).asInstanceOf[TableRow].getChildAt(2).asInstanceOf[TextView], false, weightNumber)
    Shared.setupAvatarTableCell(paidTable.getChildAt(paidTable.getChildCount - 1).asInstanceOf[TableRow].getChildAt(3).asInstanceOf[TextView], false, weightNumber)
    Shared.setupAvatarTableCell(tookTable.getChildAt(0).asInstanceOf[TableRow].getChildAt(1).asInstanceOf[TextView], false, weightName)
    Shared.setupAvatarTableCell(tookTable.getChildAt(0).asInstanceOf[TableRow].getChildAt(2).asInstanceOf[TextView], false, weightNumber)
    Shared.setupAvatarTableCell(tookTable.getChildAt(0).asInstanceOf[TableRow].getChildAt(3).asInstanceOf[TextView], false, weightNumber)

    app.peopleChecked.foreach(p => {
      val lp = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

      val paidAvatar = Shared.createAvatar(p, WhoOwesWhoScreen.this, lp)
      val paidName = Shared.createName(p, this)

      val paidOwes = new TextView(this)
      paidOwes.setTextSize(18)
      paidOwes.setText(Shared.prettifyFloat(p.owes,2))

      val paidPaid = new EditText(this)
      paidPaid.setInputType(InputType.TYPE_CLASS_PHONE)
      paidPaid.addTextChangedListener(new TextWatcher {
        override def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int): Unit = {}
        override def afterTextChanged(s: Editable): Unit = {}
        override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int): Unit = {
          paidFromUI(p, paidPaid.getText.toString)
        }
      })
      paidPaid.setOnFocusChangeListener(new OnFocusChangeListener {
        override def onFocusChange(view: View, hasFocus: Boolean): Unit = {
          paidFocusFromUI(p, hasFocus)
        }
      })


      val paidRow = new TableRow(this)

      Shared.setupAvatarTableCell(paidName, false, weightName)
      Shared.setupAvatarTableCell(paidOwes, false, weightNumber)
      Shared.setupAvatarTableCell(paidPaid, true, weightNumber)

      paidRow.addView(paidAvatar)
      paidRow.addView(paidName)
      paidRow.addView(paidOwes)
      paidRow.addView(paidPaid)


      val tookAvatar = Shared.createAvatar(p, WhoOwesWhoScreen.this, lp)
      val tookName = Shared.createName(p, this)

      val tookOwed = new TextView(this)
      tookOwed.setTextSize(18)

      val tookTook = new EditText(this)
      tookTook.setInputType(InputType.TYPE_CLASS_PHONE)
      tookTook.addTextChangedListener(new TextWatcher {
        override def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int): Unit = {}
        override def afterTextChanged(s: Editable): Unit = {}
        override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int): Unit = {
          tookFromUI(p, tookTook.getText.toString)
        }
      })

      tookTook.setOnFocusChangeListener(new OnFocusChangeListener {
        override def onFocusChange(view: View, hasFocus: Boolean): Unit = {
          tookFocusFromUI(p, hasFocus)
        }
      })

      Shared.setupAvatarTableCell(tookName, false, weightName)
      Shared.setupAvatarTableCell(tookOwed, false, weightNumber)
      Shared.setupAvatarTableCell(tookTook, true, weightNumber)

      val tookRow = new TableRow(this)
      tookRow.addView(tookAvatar)
      tookRow.addView(tookName)
      tookRow.addView(tookOwed)
      tookRow.addView(tookTook)


      val summaryAvatar = Shared.createAvatar(p, WhoOwesWhoScreen.this, lp)

      val summary = new TextView(this)
      summary.setTextSize(18)
      val summaryLP = new TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
//      summary.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE)
      summary.setLayoutParams(summaryLP)

      val summaryRowLP = new TableLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT)

      val summaryRow = new TableRow(this)
      summaryRow.setLayoutParams(summaryRowLP)

      Shared.setupAvatarTableCell(summary, false, 1, 10)

      summaryRow.addView(summaryAvatar)
      summaryRow.addView(summary)


      paidTable.addView(paidRow, paidTable.getChildCount - 2)
      tookTable.addView(tookRow)
      summaryTable.addView(summaryRow)
    })

//    paidTable.setColumnShrinkable(1, true)
//    paidTable.setColumnStretchable(1, true)
//    paidTable.setColumnShrinkable(2, true)
//    paidTable.setColumnStretchable(2, true)
//    paidTable.setColumnShrinkable(3, true)
//    paidTable.setColumnStretchable(3, true)
//
//    tookTable.setColumnShrinkable(1, true)
//    tookTable.setColumnStretchable(1, true)
//    tookTable.setColumnShrinkable(2, true)
//    tookTable.setColumnStretchable(2, true)
//    tookTable.setColumnShrinkable(3, true)
//    tookTable.setColumnStretchable(3, true)
//
//    summaryTable.setColumnShrinkable(1, true)
//    summaryTable.setColumnStretchable(1, true)


    setPeople(app.peopleChecked)

    init()
    recalc()

    Shared.setupAd(this, R.id.wowsAdView)
  }

  def paidRow(v: Person): TableRow = {
    val idx = app.peopleChecked.indexOf(v) + 1
    paidTable.getChildAt(idx).asInstanceOf[TableRow]
  }

  def tookRow(v: Person): TableRow = {
    val idx = app.peopleChecked.indexOf(v) + 1
    tookTable.getChildAt(idx).asInstanceOf[TableRow]
  }

  def summaryRow(v: Person): TableRow = {
    val idx = app.peopleChecked.indexOf(v)
    summaryTable.getChildAt(idx).asInstanceOf[TableRow]
  }

  override def totalOwedToUI(v: String): Unit = findViewById(R.id.totalOwed).asInstanceOf[TextView].setText(v)

  override def personSquareToUI(v: Person, square: IsSquare): Unit = summaryRow(v).getChildAt(1).asInstanceOf[TextView].setText(square.msg)

  override def personChangeOwedToUI(p: Person, v: String): Unit = {
    tookRow(p).getChildAt(2).asInstanceOf[TextView].setText(v)
  }

  override def changeDueToUI(v: String): Unit = findViewById(R.id.changeDue).asInstanceOf[TextView].setText(v)

  override def paidFromErrorToUI(p: Person, v: Boolean): Unit = {
    paidRow(p).getChildAt(3).asInstanceOf[EditText].setError(if (v) "Please set to a valid positive amount" else null)
  }

  override def changeErrorToUI(v: String, show: Boolean): Unit = {
    val changeError = findViewById(R.id.changeError).asInstanceOf[TextView]
    changeError.setVisibility(if (show) View.VISIBLE else View.GONE)
    changeError.setText(v)
  }

  override def personOwesToUI(p: Person, v: String): Unit = paidRow(p).getChildAt(2).asInstanceOf[TextView].setText(v)

  override def changeBlockVisibleToUI(v: Boolean): Unit = findViewById(R.id.changeBlock).setVisibility(if (v) View.VISIBLE else View.GONE)

  override def tookFromErrorToUI(p: Person, v: Boolean): Unit = tookRow(p).getChildAt(3).asInstanceOf[EditText].setError(if(v) "Please set to a valid positive amount" else null)

  override def totalPaidToUI(v: String, notEnough: Boolean): Unit = {
    findViewById(R.id.totalPaid).asInstanceOf[TextView].setText(v)
  }

  override def personPaidToUI(p: Person, v: String) = paidRow(p).getChildAt(3).asInstanceOf[EditText].setText(v)

  override def personTookChangeToUI(p: Person, v: String) = tookRow(p).getChildAt(3).asInstanceOf[EditText].setText(v)

  var mShareActionProvider: ShareActionProvider = null
  var lastMsg = ""

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    val inflater = getMenuInflater

    inflater.inflate(R.menu.wows_menu, menu)

    // Locate MenuItem with ShareActionProvider
    val item = menu.findItem(R.id.menu_item_share)

    // Fetch and store ShareActionProvider
    mShareActionProvider = item.getActionProvider.asInstanceOf[ShareActionProvider]

    summaryToUI(lastMsg)
//    val shareIntent = new Intent()
//    shareIntent.setAction(Intent.ACTION_SEND)
//    val b = takeScreenShot
////    shareIntent.putExtra(Intent.EXTRA_STREAM, b)
////    shareIntent.setType("image/*")
//    shareIntent.putExtra(Intent.EXTRA_TEXT, "This is my text to send.");
//    shareIntent.setType("text/plain");

//    mShareActionProvider.setShareIntent(shareIntent)

    true
  }

  def summaryToUI(v: String) = {
    lastMsg = v

    val shareIntent = new Intent()
    shareIntent.setAction(Intent.ACTION_SEND)
    shareIntent.putExtra(Intent.EXTRA_TEXT, v)
    shareIntent.setType("text/plain")

    if (mShareActionProvider != null) {
      mShareActionProvider.setShareIntent(shareIntent)
    }
  }

  def takeScreenShot(): Bitmap = {
    val view = this.getWindow.getDecorView
    view.setDrawingCacheEnabled(true)
    view.buildDrawingCache()
    val b1 = view.getDrawingCache
    val frame = new Rect()
    this.getWindow.getDecorView.getWindowVisibleDisplayFrame(frame)
    val statusBarHeight = frame.top
    val width = this.getWindowManager.getDefaultDisplay.getWidth()
    val height = this.getWindowManager.getDefaultDisplay.getHeight()

    val b = Bitmap.createBitmap(b1, 0, statusBarHeight, width, height  - statusBarHeight)
    view.destroyDrawingCache()
    b
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent ) = {
    Log.i(TAG, s"onActivityResult $requestCode $resultCode")
    Shared.refetchPeople(app)
    finish();
    startActivity(getIntent());
  }

  override def onStart(): Unit = {
    super.onStart()
    GoogleAnalytics.getInstance(this).reportActivityStart(this)
  }

  override def onStop() = {
    super.onStop()
    GoogleAnalytics.getInstance(this).reportActivityStop(this)
  }


}