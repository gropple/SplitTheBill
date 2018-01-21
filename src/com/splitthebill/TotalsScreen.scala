package com.splitthebill

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.{Editable, TextWatcher}
import android.util.Log
import android.view.View
import android.view.View.{OnClickListener, OnFocusChangeListener}
import android.view.inputmethod.EditorInfo
import android.widget._
import com.google.android.gms.analytics.GoogleAnalytics

class TotalsScreen extends Activity {
  val TAG = getClass.toString

  var app: DaApp = null

  var itemsTotal:EditText = null
  var total:EditText = null
  var tipPct:EditText = null
  var tipAmount:EditText = null
  var calcTotal:TextView = null
  var afterError:TextView = null
  var next:Button = null
  var ignoreEvents: Boolean = false



  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.totals)

    //    if (savedInstanceState == null) {
    //      getSupportFragmentManager().beginTransaction()
    //        .add(R.id.container, new PlaceholderFragment())
    //        .commit();
    //    }


    app = getApplicationContext.asInstanceOf[DaApp]

    itemsTotal = findViewById(R.id.itemsTotal).asInstanceOf[EditText]
    total = findViewById(R.id.totalInput).asInstanceOf[EditText]
    tipPct = findViewById(R.id.tipPct).asInstanceOf[EditText]
    tipAmount = findViewById(R.id.tipAmount).asInstanceOf[EditText]
    afterError = findViewById(R.id.afterError).asInstanceOf[TextView]
    calcTotal = findViewById(R.id.calcTotal).asInstanceOf[TextView]
    next = findViewById(R.id.totalsNext).asInstanceOf[Button]

    ignoreEvents = true

    if (!app.itemsTotalRaw.isEmpty) {
      val eval = Shared.evaluateMathsExpr(app.itemsTotalRaw, true)
      if (eval.isDefined) {
        itemsTotal.setText(prettifyFloat(eval.get, 2)) // ignore
      }
      else {
        itemsTotal.setText(app.itemsTotalRaw) // ignore
        itemsTotal.setError("Please set to a valid positive amount")
      }
    }
    else if (app.itemsTotalValue > 0) {
      itemsTotal.setText(prettifyFloat(app.itemsTotalValue, 2)) // ignore
    }

    if (!app.totalRaw.isEmpty) {
      val eval = Shared.evaluateMathsExpr(app.totalRaw, true)
      if (eval.isDefined) {
        total.setText(prettifyFloat(eval.get, 2)) // ignore fire
      }
      else {
        total.setText(app.totalRaw) // ignore fire
        total.setError("Please set to a valid positive amount")
      }
    }
    else if (app.totalValue > 0) {
      total.setText(prettifyFloat(app.totalValue, 2)) // ignore fire
    }

    if (!app.tipPctRaw.isEmpty) {
      val eval = Shared.evaluateMathsExpr(app.tipPctRaw, true)
      if (eval.isDefined) {
        tipPct.setText(prettifyFloat(eval.get, 2))
      }
      else {
        tipPct.setText(app.tipPctRaw)
        tipPct.setError("Please set to a valid positive amount")
      }
    }
    else if (app.tipPctValue > 0) {
      tipPct.setText(prettifyFloat(app.tipPctValue, 2))
    }



    if (!app.tipAmountRaw.isEmpty) {
      val eval = Shared.evaluateMathsExpr(app.tipAmountRaw, true)
      if (eval.isDefined) {
        tipAmount.setText(prettifyFloat(eval.get, 2))
      }
      else {
        tipAmount.setText(app.tipAmountRaw)
        tipAmount.setError("Please set to a valid positive amount")
      }
    }
    else if (app.tipAmountValue > 0) {
      tipAmount.setText(prettifyFloat(app.tipAmountValue, 2))
    }

    ignoreEvents = false



    itemsTotal.setOnFocusChangeListener(new OnFocusChangeListener {
      override def onFocusChange(view: View, hasFocus: Boolean): Unit = {
        if (ignoreEvents) { return }
        ignoreEvents = true
        if (hasFocus) {
          itemsTotal.setText(app.itemsTotalRaw) // ignore
        }
        else {
          if (!app.itemsTotalRaw.isEmpty) {
            val eval = Shared.evaluateMathsExpr(app.itemsTotalRaw, true)
            if (eval.isDefined) {
              itemsTotal.setText(Shared.prettifyFloat(eval.get, 2)) // ignore
              itemsTotal.setError(null)
            }
            else {
              itemsTotal.setError("Please set to a valid positive amount")
            }
          }
        }
        ignoreEvents = false
      }
    })


    itemsTotal.addTextChangedListener(new TextWatcher {
      override def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int): Unit = {}
      override def afterTextChanged(s: Editable): Unit = {}
      override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int): Unit = {
        if (ignoreEvents) { Log.i(TAG, "Ignoring event"); return }
        app.itemsTotalRaw = itemsTotal.getText.toString
        val asDouble = Shared.evaluateMathsExpr(app.itemsTotalRaw, true)
        if (asDouble.isDefined) {
          app.itemsTotalValue = asDouble.get
        }
        else {
          app.itemsTotalValue = 0
        }

        if (!app.userHasSetTotal) {
          ignoreEvents = true
          app.totalValue = app.itemsTotalValue
          app.totalRaw = Shared.prettifyFloat(app.itemsTotalValue, 2)
          total.setText(app.totalRaw) // ignore
          ignoreEvents = false

          totalChange(app.totalValue)
        }

        itemsTotalChange(app.itemsTotalValue)
      }
    })

    total.setOnFocusChangeListener(new OnFocusChangeListener {
      override def onFocusChange(view: View, hasFocus: Boolean): Unit = {
        if (ignoreEvents) { return }
        if (hasFocus) {
          total.setText(app.totalRaw)
        }
        else {
          ignoreEvents = true
          if (!app.totalRaw.isEmpty) {
            val eval = Shared.evaluateMathsExpr(app.totalRaw, true)
            if (eval.isDefined) {
              total.setText(Shared.prettifyFloat(eval.get, 2))
              total.setError(null)
            }
            else {
              total.setError("Please set to a valid positive amount")
            }
          }
          ignoreEvents = false

        }
      }
    })

    total.addTextChangedListener(new TextWatcher {
      override def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int): Unit = {}
      override def afterTextChanged(s: Editable): Unit = {}
      override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int): Unit = {
        if (ignoreEvents) { Log.i(TAG, "Ignoring event"); return }
        app.userHasSetTotal = true
        app.totalRaw = total.getText.toString
        val asDouble = Shared.evaluateMathsExpr(app.totalRaw, true)
        if (asDouble.isDefined) {
          app.totalValue = asDouble.get
        }
        else {
          app.totalValue = 0
        }
        totalChange(app.totalValue)
      }
    })

    tipPct.setOnFocusChangeListener(new OnFocusChangeListener {
      override def onFocusChange(view: View, hasFocus: Boolean): Unit = {
        if (ignoreEvents) { return }
        if (hasFocus) {
          if (!app.tipPctRaw.isEmpty) {
            tipPct.setText(app.tipPctRaw)
          }
        }
        else {
          ignoreEvents = true
          if (!app.tipPctRaw.isEmpty) {
            val eval = Shared.evaluateMathsExpr(app.tipPctRaw, true)
            if (eval.isDefined) {
              tipPct.setText(Shared.prettifyFloat(eval.get, 2))
              tipPct.setError(null)
            }
            else {
              tipPct.setError("Please set to a valid positive amount")
            }
          }
          ignoreEvents = false
        }
      }
    })


    tipPct.addTextChangedListener(new TextWatcher {
      override def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int): Unit = {}
      override def afterTextChanged(s: Editable): Unit = {}
      override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int): Unit = {
        if (ignoreEvents) { Log.i(TAG, "Ignoring event"); return }
        app.tipPctRaw = tipPct.getText.toString
        app.tipPctValue = Shared.evaluateMathsExpr(app.tipPctRaw, true) match {
          case Some(asD: Double) => asD
          case _ => 0
        }
        tipPctChange(app.tipPctValue)
      }
    })


    tipAmount.setOnFocusChangeListener(new OnFocusChangeListener {
      override def onFocusChange(view: View, hasFocus: Boolean): Unit = {
        if (ignoreEvents) { return }
        if (hasFocus) {
          if (!app.tipAmountRaw.isEmpty) {
            tipAmount.setText(app.tipAmountRaw)
          }
        }
        else {
          ignoreEvents = true
          if (!app.tipAmountRaw.isEmpty) {
            val eval = Shared.evaluateMathsExpr(app.tipAmountRaw, true)
            if (eval.isDefined) {
              tipAmount.setText(Shared.prettifyFloat(eval.get, 2))
              tipAmount.setError(null)
            }
            else {
              tipAmount.setError("Please set to a valid positive amount")
            }
          }
          ignoreEvents = false
        }
      }
    })

    tipAmount.addTextChangedListener(new TextWatcher {
      override def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int): Unit = {}
      override def afterTextChanged(s: Editable): Unit = {}
      override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int): Unit = {
        if (ignoreEvents) { Log.i(TAG, "Ignoring event"); return }
        app.tipAmountRaw = tipAmount.getText.toString
        app.tipAmountValue = Shared.evaluateMathsExpr(app.tipAmountRaw, true) match {
          case Some(asD: Double) => asD
          case _ => 0
        }
        tipAmountChange(app.tipAmountValue)
      }
    })


    next.setOnClickListener(new OnClickListener() {
      def onClick(v: View) {
        val intent: Intent = new Intent(TotalsScreen.this, classOf[AddPeopleScreen])
        startActivity(intent)
      }
    })

    tipAmount.setImeOptions(EditorInfo.IME_ACTION_DONE)
    tipPct.setImeOptions(EditorInfo.IME_ACTION_DONE)
//    tipAmount.setNextFocusForwardId(next.getId)
//    tipPct.setNextFocusForwardId(next.getId)

    recalc()

    Shared.setupSmartFooterHiding(this, R.id.root, R.id.whole, R.id.body, R.id.footer)
    Shared.setupAd(this, R.id.tsAdView)
  }

  def itemsTotalChange(total: Double): Unit = {
    app.itemsTotalValue = total
  }

  def totalChange(total: Double): Unit = {
    app.totalValue = total
    recalc()
  }

  def tipPctChange(v: Double): Unit = {
    app.tipPctValue = v
    app.tipAmountValue = 0
    app.tipAmountRaw = ""
    tipAmount.setError(null)
    ignoreEvents = true
    tipAmount.setText("")
    ignoreEvents = false
    recalc()
  }

  def tipAmountChange(v: Double): Unit = {
    app.tipAmountValue = v
    app.tipPctValue = 0
    app.tipPctRaw = ""
    tipPct.setError(null)
    ignoreEvents = true
    tipPct.setText("")
    ignoreEvents = false
    recalc()
  }

  def recalc() = {
    app.calcTotalValue = app.totalValue + ((app.tipPctValue / 100) * app.totalValue) + app.tipAmountValue
    calcTotal.setText(prettifyFloat(app.calcTotalValue, 2))
    val totalTooLow = app.totalValue < app.itemsTotalValue - 0.001
    afterError.setVisibility(if (totalTooLow) View.VISIBLE else View.GONE)
  }

  def redraw() = {
  }

  def prettifyFloat(v: Double, dp: Int): String = {
    Shared.prettifyFloat(v, dp)
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