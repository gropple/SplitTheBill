package com.splitthebill

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.{TextUtils, InputType, Editable, TextWatcher}
import android.util.Log
import android.view.{ViewGroup, KeyEvent, View}
import android.view.View.{OnFocusChangeListener, OnKeyListener, OnClickListener}
import android.widget._
import com.google.android.gms.analytics.GoogleAnalytics

class SplitScreen extends Activity {
  val TAG = getClass.toString

  var app: DaApp = null

  var ssItemsTotal: TextView = null
  var ssTaxAndServiceTotal: TextView = null
  var splitPeople: TableLayout = null
  var splitItemsError: TextView = null
  var splitOthersError: TextView = null
  var next: Button = null
  var back: Button = null
  var ignoreEvents: Boolean = false

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.split)

    app = getApplicationContext.asInstanceOf[DaApp]

    app.initialise()


    ssItemsTotal = findViewById(R.id.ssItemsTotal).asInstanceOf[TextView]
    ssTaxAndServiceTotal = findViewById(R.id.ssTaxAndServiceTotal).asInstanceOf[TextView]
    splitPeople = findViewById(R.id.splitTable).asInstanceOf[TableLayout]
    splitItemsError = findViewById(R.id.splitItemsError).asInstanceOf[TextView]
    splitOthersError = findViewById(R.id.splitTaxAndServiceError).asInstanceOf[TextView]
    back = findViewById(R.id.splitBack).asInstanceOf[Button]
    next = findViewById(R.id.splitNext).asInstanceOf[Button]

    if (app.people.isEmpty) {
      Shared.refetchPeople(app)
      // just for testing
      app.people.filter(_.uuid.get == "me").foreach(_.checked = true)
    }

    recalc()

//    splitPeople.setColumnShrinkable(1, true)
//    splitPeople.setColumnStretchable(1, true)
//    splitPeople.setColumnShrinkable(2, true)
//    splitPeople.setColumnStretchable(2, true)
//    splitPeople.setColumnShrinkable(3, true)
//    splitPeople.setColumnStretchable(3, true)

    back.setOnClickListener(new OnClickListener() {
      def onClick(v: View) {
        val intent: Intent = new Intent(SplitScreen.this, classOf[AddPeopleScreen])
        startActivity(intent)
      }
    })

    next.setOnClickListener(new OnClickListener() {
      def onClick(v: View) {
        val intent: Intent = new Intent(SplitScreen.this, classOf[WhoOwesWhoScreen])
        startActivity(intent)
      }
    })

    drawInit()
    redrawTotals()

    Shared.setupSmartFooterHiding(this, R.id.root, R.id.whole, R.id.body, R.id.ssFooter)
    Shared.setupAd(this, R.id.ssAdView)
  }

  def refetchAndRedraw(): Unit = {
    Shared.refetchPeople(app)
    redrawTotals()
    recalc()
    drawInit()
  }

  // RC1: If users are over-paying, don't want others to be overpaid too
  def recalc(): Unit = {
    val splittingItemsCount = app.peopleChecked.count(_.splittingItems)
    val splittingOthersCount = app.peopleChecked.count(_.splittingOthers)

    val alreadySplitItemTotal: Double = app.peopleChecked.filter(!_.splittingItems).foldLeft(0.0)((b,a) => b +a.owesItems)
    val alreadySplitOthersTotal: Double = app.peopleChecked.filter(!_.splittingOthers).foldLeft(0.0)((b,a) => b +a.owesOthers)

    if (splittingItemsCount > 0) {
      val leftToSplitItems = app.itemsTotalValue - alreadySplitItemTotal
      val leftToSplitItemsPerPerson = Math.max(leftToSplitItems / splittingItemsCount, 0)

      app.peopleChecked.filter(_.splittingItems).foreach(_.owesItems = leftToSplitItemsPerPerson)
    }

    val setItemTotal = app.peopleChecked.foldLeft(0.0)((b,a) => b +a.owesItems)
    val isOverpayingAmount = setItemTotal > app.itemsTotalValue + 0.01
    val taxAndServiceTotal = app.calcTotalValue - app.itemsTotalValue

    if (splittingOthersCount > 0) {
      val leftToSplitOthers = taxAndServiceTotal - alreadySplitOthersTotal
      var leftToSplitOthersEqualSplit = 0.0
      if (isOverpayingAmount) {
        val splittingOthersAndOverpayingCount = app.peopleChecked.count(v => v.splittingOthers && v.owesItems > 1)
        leftToSplitOthersEqualSplit = if (splittingOthersAndOverpayingCount > 0) leftToSplitOthers / splittingOthersAndOverpayingCount else 0
      }
      else {
        leftToSplitOthersEqualSplit = leftToSplitOthers / splittingOthersCount
      }
      val atLeastOnePersonNotSplittingOthers = splittingOthersCount != app.peopleChecked.size
      val setItemTotalByPeopleSplittingOthers = app.peopleChecked.filter(_.splittingOthers).foldLeft(0.0)((b,a) => b +a.owesItems)

      app.peopleChecked.filter(_.splittingOthers).foreach(v => {
        var split = 0.0
        if (atLeastOnePersonNotSplittingOthers) {
          split = if (setItemTotalByPeopleSplittingOthers > 0) v.owesItems / setItemTotalByPeopleSplittingOthers * leftToSplitOthers else 0
        }
        else if (isOverpayingAmount && v.owesItems > 0) {
          split = leftToSplitOthersEqualSplit
        }
        else {
          split = if (app.itemsTotalValue > 0) (v.owesItems / app.itemsTotalValue) * leftToSplitOthers else 0
        }
        v.owesOthers = Math.max(split, 0)
      })
    }

    val setOthersTotal = app.peopleChecked.foldLeft(0.0)((b,a) => b +a.owesOthers)

    val showSplitItemsError = setItemTotal - app.itemsTotalValue > 0.01
    val showSplitOthersError = setOthersTotal - taxAndServiceTotal > 0.01

    if (showSplitItemsError) {
      val errText = s"${Shared.prettifyFloat(setItemTotal, 2)} is more than item total ${Shared.prettifyFloat(app.itemsTotalValue, 2)}."
      splitItemsError.setText(errText)
    }

    if (showSplitOthersError) {
      val errText = s"${Shared.prettifyFloat(setOthersTotal, 2)} is more than others total ${Shared.prettifyFloat(taxAndServiceTotal, 2)}."
      splitOthersError.setText(errText)
    }

    splitItemsError.setVisibility(if (showSplitItemsError) View.VISIBLE else View.GONE)
    splitOthersError.setVisibility(if (showSplitOthersError) View.VISIBLE else View.GONE)
  }

  def drawInit(): Unit = {
    while (splitPeople.getChildCount > 2) {
      splitPeople.removeViewAt(2)
    }

    val weightName = Shared.nameWeight
    val weightNumber = Shared.numberWeight

    Shared.setupAvatarTableCell(splitPeople.getChildAt(0).asInstanceOf[TableRow].getChildAt(1).asInstanceOf[TextView], false, weightName)
    Shared.setupAvatarTableCell(splitPeople.getChildAt(0).asInstanceOf[TableRow].getChildAt(2).asInstanceOf[TextView], false, weightNumber)
    Shared.setupAvatarTableCell(splitPeople.getChildAt(0).asInstanceOf[TableRow].getChildAt(3).asInstanceOf[TextView], false, weightNumber)
    Shared.setupAvatarTableCell(splitPeople.getChildAt(1).asInstanceOf[TableRow].getChildAt(1).asInstanceOf[TextView], false, weightName)
    Shared.setupAvatarTableCell(splitPeople.getChildAt(1).asInstanceOf[TableRow].getChildAt(2).asInstanceOf[TextView], false, weightNumber)
    Shared.setupAvatarTableCell(splitPeople.getChildAt(1).asInstanceOf[TableRow].getChildAt(3).asInstanceOf[TextView], false, weightNumber)

    app.peopleChecked.foreach(v => {
      val row = new TableRow(this)
      val rowAvatarLp = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
      val rowAvatar = Shared.createAvatar(v, SplitScreen.this, rowAvatarLp)
      val rowName = Shared.createName(v, this)
      val rowOwesItem = new EditText(this)
      val rowOwesOther = new EditText(this)

      Shared.setupAvatarTableCell(rowName, false, weightName)
      Shared.setupAvatarTableCell(rowOwesItem, true, weightNumber)
      Shared.setupAvatarTableCell(rowOwesOther, true, weightNumber)

      row.addView(rowAvatar)
      row.addView(rowName)
      row.addView(rowOwesItem)
      row.addView(rowOwesOther)

      splitPeople.addView(row)


      val summaryRowLP = new TableLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT)
      row.setLayoutParams(summaryRowLP)

      redrawRow(row, v, true)

      rowOwesItem.setInputType(InputType.TYPE_CLASS_PHONE)

      rowOwesItem.setOnFocusChangeListener(new OnFocusChangeListener {
        override def onFocusChange(view: View, hasFocus: Boolean): Unit = {
          ignoreEvents = true
          if (hasFocus) {
            rowOwesItem.setText(v.owesItemsRaw)
          }
          else {
            if (!v.owesItemsRaw.isEmpty) {
              val eval = Shared.evaluateMathsExpr(v.owesItemsRaw, true)
              if (eval.isDefined) {
                rowOwesItem.setText(Shared.prettifyFloat(eval.get, 2))
              }
              else {
                rowOwesItem.setError("Please set to a valid positive amount")
              }
            }
          }
          ignoreEvents = false
        }
      })

      rowOwesItem.addTextChangedListener(new TextWatcher {
        override def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int): Unit = {}
        override def afterTextChanged(s: Editable): Unit = {}
        override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int): Unit = {
          if (ignoreEvents) { Log.i(TAG, "Ignoring event"); return }
          v.owesItemsRaw = rowOwesItem.getText.toString
          if (rowOwesItem.getText.toString.isEmpty) {
            v.splittingItems = true
          }
          else {
            val asDouble = Shared.evaluateMathsExpr(rowOwesItem.getText.toString, true)
            if (asDouble.isDefined) {
              v.owesItems = asDouble.get
            }
            else {
              v.owesItems = 0
            }
            v.splittingItems = false
          }

          recalc()
          redraw()
        }
      })

      rowOwesOther.setInputType(InputType.TYPE_CLASS_PHONE)

      rowOwesOther.setOnFocusChangeListener(new OnFocusChangeListener {
        override def onFocusChange(view: View, hasFocus: Boolean): Unit = {
          ignoreEvents = true
          if (hasFocus) {
            rowOwesOther.setText(v.owesOthersRaw)
          }
          else {
            if (!v.owesOthersRaw.isEmpty) {
              val eval = Shared.evaluateMathsExpr(v.owesOthersRaw, true)
              if (eval.isDefined) {
                rowOwesOther.setText(Shared.prettifyFloat(eval.get, 2))
              }
              else {
                rowOwesOther.setError("Please set to a valid positive amount")
              }
            }
          }
          ignoreEvents = false
        }
      })



      rowOwesOther.addTextChangedListener(new TextWatcher {
        override def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int): Unit = {}
        override def afterTextChanged(s: Editable): Unit = {}
        override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int): Unit = {
          if (ignoreEvents) { Log.i(TAG, "Ignoring event"); return }
          v.owesOthersRaw = rowOwesOther.getText.toString
          if (v.owesOthersRaw.isEmpty) {
            v.splittingOthers = true
          }
          else {
            val asDouble = Shared.evaluateMathsExpr(v.owesOthersRaw, true)
            if (asDouble.isDefined) {
              v.owesOthers = asDouble.get
            }
            else {
              v.owesOthers = 0
            }
            v.splittingOthers = false
          }
          recalc()
          redraw()
        }
      })

    })
  }

  def redrawRow(row: TableRow, v: Person, fresh: Boolean): Unit = {
    ignoreEvents = true
    val rowOwesItem = row.getChildAt(2).asInstanceOf[EditText]
    val rowOwesOther = row.getChildAt(3).asInstanceOf[EditText]

    if (v.splittingItems) {
      rowOwesItem.setHint(Shared.prettifyFloat(v.owesItems, 2))
      rowOwesItem.setText("")
    }
    else {
      rowOwesItem.setHint("")
      if (fresh) {
        if (!v.owesItemsRaw.isEmpty) {
          val eval = Shared.evaluateMathsExpr(v.owesItemsRaw, true)
          if (eval.isDefined) {
            rowOwesItem.setText(Shared.prettifyFloat(eval.get, 2))
          }
          else {
            rowOwesItem.setError("Please set to a valid positive amount")
            rowOwesItem.setText(v.owesItemsRaw)
          }
        }
        else {
          rowOwesItem.setText(Shared.prettifyFloat(v.owesItems, 2))
        }
      }
    }
    if (v.splittingOthers) {
      rowOwesOther.setText("")
      rowOwesOther.setHint(Shared.prettifyFloat(v.owesOthers, 2))
    }
    else {
      rowOwesOther.setHint("")
      if (fresh) {
        if (!v.owesOthersRaw.isEmpty) {
          val eval = Shared.evaluateMathsExpr(v.owesOthersRaw, true)
          if (eval.isDefined) {
            rowOwesOther.setText(Shared.prettifyFloat(eval.get, 2))
          }
          else {
            rowOwesOther.setError("Please set to a valid positive amount")
            rowOwesOther.setText(v.owesOthersRaw)
          }
        }
        else {
          rowOwesOther.setText(Shared.prettifyFloat(v.owesOthers, 2))
        }
      }
    }
    ignoreEvents = false
  }

  def redrawTotals() {
    ssItemsTotal.setText(Shared.prettifyFloat(app.itemsTotalValue, 2))
    val taxAndServiceTotal = app.calcTotalValue - app.itemsTotalValue
    ssTaxAndServiceTotal.setText(Shared.prettifyFloat(taxAndServiceTotal, 2))
  }

  def redraw(): Unit = {
    redrawTotals()

    var rowIdx = 2

    app.peopleChecked.foreach(v => {
      val row = splitPeople.getChildAt(rowIdx).asInstanceOf[TableRow]
      redrawRow(row, v, false)
      rowIdx += 1
    })
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