package com.splitthebill

import java.net.URI

import android.app.{Fragment, Activity}
import android.content.{Context, Intent}
import android.net.Uri
import android.os.Bundle
import android.text.{Editable, TextWatcher}
import android.util.Log
import android.view.ViewGroup.LayoutParams
import android.view.{LayoutInflater, ViewGroup, View}
import android.view.View.OnClickListener
import android.widget.AdapterView.OnItemClickListener
import android.widget._
import com.google.android.gms.analytics.GoogleAnalytics


class ImageAdapter(mContext: Context, imageResIds: Array[Int]) extends BaseAdapter {

  override def getCount: Int = {
    imageResIds.length
  }

  override def getItem(position: Int): Object = imageResIds(position).asInstanceOf[Object]

  override def getItemId(position: Int): Long = position

  override def getView(position: Int, convertView: View , container: ViewGroup ): View = {
    var imageView: ImageView = null
    if (convertView == null) { // if it's not recycled, initialize some attributes
      imageView = new ImageView(mContext)
//      imageView.setScaleType(ImageView.ScaleType.CENTER_CROP)

//      val lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
//      lp.width = 50
//      lp.height = 50
//      imageView.setLayoutParams(lp)

      // Gets rid of gaps between columns
      imageView.setAdjustViewBounds(true)
    } else {
      imageView = convertView.asInstanceOf[ImageView]
    }
    imageView.setImageResource(imageResIds(position)) // Load image into ImageView
    imageView
  }
}



class SelectAvatarScreen extends Activity {
  val TAG = getClass.toString

  var avatars: GridView = null
  var di: DepInj = null
  var app: DaApp = null
  var mAdapter: ImageAdapter = null


  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.select_avatar)

    app = getApplicationContext.asInstanceOf[DaApp]
    app.initialise()
    di = app.di

    var personUuid = "me"
    val extras = getIntent().getExtras()
    if (extras != null) {
      personUuid = extras.getString("personUuid")
    }

    val person = di.daos.personDao.get(personUuid).get

    avatars = findViewById(R.id.avatars).asInstanceOf[GridView]

    val r = new Range(0, di.daos.personDao.maxImage, 1)
    val imageResIds = r.map(v => {
      val uri = s"drawable/avatar$v"
      val rId = getResources().getIdentifier(uri, null, getPackageName())
      rId -> uri
    })

    mAdapter = new ImageAdapter(this, imageResIds.map(_._1).toArray)

//    val v = inflater.inflate(R.layout.image_grid_fragment, container, false)
    avatars.setAdapter(mAdapter)
    avatars.setOnItemClickListener(new OnItemClickListener {
      override def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long): Unit = {
        val uri = imageResIds(position)._2
        person.avatarFilename = uri

        di.daos.personDao.update(person)
        setResult(0)
        finish()
      }
    })

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