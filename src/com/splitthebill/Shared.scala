package com.splitthebill

import android.app.Activity
import android.content.{Context, Intent}
import android.graphics.Rect
import android.text.TextUtils
import android.util.TypedValue
import android.view.View.OnClickListener
import android.view.{View, ViewGroup, ViewTreeObserver}
import android.widget._
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.doubleclick.{PublisherAdRequest, PublisherAdView}
import expr.{Parser, SyntaxException}

object Shared {
  def refetchPeople(app: DaApp) = {
    val people = app.di.daos.personDao.getAllSorted
    people.foreach(v => {
      val existing = app.people.filter(_.uuid == v.uuid)
      if (existing.nonEmpty) {
        v.copyTransients(existing.head)
      }
    })
    app.people = people
  }

  def prettifyFloat(v: Double, dp: Int): String = {
    s"%1.${dp}f".format(v)
  }

  def createAvatar(v: Person, ctx: Activity, lp: LinearLayout.LayoutParams): ImageView = {
    val rowAvatar = new ImageView(ctx)
    val uri = v.avatarFilename
    val imageResource = ctx.getResources().getIdentifier(uri, null, ctx.getPackageName())

    rowAvatar.setImageResource(imageResource)
  //    val lp = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    val r = rowAvatar.getResources
    lp.width  = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, r.getDisplayMetrics).asInstanceOf[Int]
    lp.height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, r.getDisplayMetrics).asInstanceOf[Int]
    lp.bottomMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, r.getDisplayMetrics).asInstanceOf[Int]
    lp.rightMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, r.getDisplayMetrics).asInstanceOf[Int]
    rowAvatar.setLayoutParams(lp)

    rowAvatar.setOnClickListener(new OnClickListener() {
      def onClick(view: View) {
        val intent = new Intent(ctx, classOf[SelectAvatarScreen])
        intent.putExtra("personUuid", v.uuid.get)
        ctx.startActivityForResult(intent, 1)
      }
    })

    rowAvatar
  }

  def createName(v: Person, ctx: Context): TextView = {
    val nameLP = new TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    val rowName = new TextView(ctx)

    rowName.setText(v.name)
    rowName.setTextSize(18)
    rowName.setMaxLines(1)
    rowName.setEllipsize(TextUtils.TruncateAt.END)

    rowName.setLayoutParams(nameLP)

    rowName
  }

  def evaluateMathsExpr(v: String, mustBePositive: Boolean): Option[Double] = {
    if (v.isEmpty) return Some(0)
    try {
      val expr = Parser.parse(v)
      if (expr.value < -0.00001 && mustBePositive) {
        return None
      }
      Some(expr.value())
    }
    catch {
      case e: SyntaxException => None
    }

  }

  def nameWeight = 1.1f
  def numberWeight = 1.0f

  def setupAvatarTableCell(view: TextView, isEdit: Boolean, weight: Float, maxLines: Int = 3) = {
    val r = view.getResources
    val lp = new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT)
    if (isEdit) {
      lp.bottomMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, r.getDisplayMetrics).asInstanceOf[Int]
    }
    else {
      lp.topMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 9, r.getDisplayMetrics).asInstanceOf[Int]
    }
//    lp.width = 0
    lp.weight = weight
    view.setMaxLines(maxLines)
    view.setEllipsize(TextUtils.TruncateAt.END)
    view.setLayoutParams(lp)
    if (!isEdit) {
      view.setTextSize(18)
    }
  }

  def setupSmartFooterHiding(ctx: Activity, rootViewId: Int, wholeId: Int, bodyId: Int, footerId: Int) = {
    val rootView = ctx.findViewById(rootViewId)
    val whole = ctx.findViewById(wholeId).asInstanceOf[RelativeLayout]
    val body = ctx.findViewById(bodyId).asInstanceOf[LinearLayout]
    val footer = ctx.findViewById(footerId)

    rootView.getViewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      override def onGlobalLayout() = {

        val r = new Rect()
        rootView.getWindowVisibleDisplayFrame(r)
        val screenHeight = rootView.getRootView.getHeight

        // r.bottom is the position above soft keypad or device button.
        // if keypad is shown, the r.bottom is smaller than that before.
        val keypadHeight = screenHeight - r.bottom

//        Log.d(TAG, "keypadHeight = %d".format(keypadHeight))

        if (keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
          // keyboard is opened
          if (whole.indexOfChild(footer) != -1) {
            whole.removeView(footer)
            body.addView(footer, body.getChildCount)
            val lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            footer.setLayoutParams(lp)
          }
          //          footer.setVisibility(View.GONE)
        }
        else {
          // keyboard is closed
          //          footer.setVisibility(View.VISIBLE)
          if (body.indexOfChild(footer) != -1) {
            body.removeView(footer)
            whole.addView(footer, 2)
            val lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            footer.setLayoutParams(lp)
          }
          //          if (footer.getParent == null) {
          //          }
        }
      }
    })

  }

  def setHeaderFont(activity: Activity, id: Int) = {
//    val header = activity.findViewById(id).asInstanceOf[TextView]
//    val font = Typeface.createFromAsset(activity.getAssets(), "OpenSans-CondLight.ttf")
//    header.setTypeface(font)
  }

  def setupAd(activity: Activity, id: Int): Unit = {
    //    val locService = activity.getSystemService(Context.LOCATION_SERVICE).asInstanceOf[LocationManager]

    //    val lastLocation = locService.getLastKnownLocation("NETWORK_PROVIDER")


    val app = activity.getApplicationContext.asInstanceOf[DaApp]

    if (app.disableAdsForTesting) {
      return
    }

    val mAdView = activity.findViewById(id).asInstanceOf[PublisherAdView]

    if (app.screenshotMode) {
      // Ads? What ads?
      mAdView.setVisibility(View.GONE)
    }
    else {
      // Oh. Those ads.
      mAdView.setVisibility(View.VISIBLE)


      // Create an ad request. Check logcat output for the hashed device ID to
      // get test ads on a physical device. e.g.
      // "Use AdRequest.Builder.addTestDevice("ABCDEF012345") to get test ads on this device."
      val adRequest = new PublisherAdRequest.Builder()
        //      .setLocation(lastLocation)
        .addTestDevice(AdRequest.DEVICE_ID_EMULATOR) // All emulators
        //      .addTestDevice("AC98C820A50B4AD8A2106EDE96FB87D4")  // My Galaxy Nexus test phone
        .build()

      // Start loading the ad in the background.
      mAdView.loadAd(adRequest)
    }
  }
}
