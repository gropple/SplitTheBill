package com.splitthebill

import android.app.Activity
import android.content.{Context, Intent}
import android.os.Bundle
import android.text.{Editable, InputType, TextWatcher}
import android.util.Log
import android.view.View.OnClickListener
import android.view.inputmethod.InputMethodManager
import android.view.{View, ViewGroup}
import android.widget._
import com.google.android.gms.analytics.GoogleAnalytics


class AddPeopleScreen extends Activity {
  val TAG = getClass.toString

  var peopleTable: TableLayout = null
  var addPerson: Button = null
  var back: Button = null
  var next: Button = null
  var di: DepInj = null
  var app: DaApp = null

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.add_people_2)

    Log.i(TAG, "On create")

    app = getApplicationContext.asInstanceOf[DaApp]
    app.initialise()
    di = app.di

    if (app.people.isEmpty) {
      refetchPeople()
      app.people.filter(_.uuid.get == "me").foreach(_.checked = true)
    }

    peopleTable = findViewById(R.id.peopleTable).asInstanceOf[TableLayout]
    addPerson = findViewById(R.id.addPerson).asInstanceOf[Button]
    back = findViewById(R.id.addPeopleBack).asInstanceOf[Button]
    next = findViewById(R.id.addPeopleNext).asInstanceOf[Button]

//    peopleTable.setColumnShrinkable(2, true)
//    peopleTable.setColumnStretchable(2, true)

    redrawPeople()

    addPerson.setOnClickListener(new OnClickListener {
      override def onClick(v: View): Unit = {
        val person = new Person
        person.name = di.daos.personDao.nextPersonName
        person.avatarFilename = di.daos.personDao.nextPersonAvatarFilename
        di.daos.personDao.insert(person)

        refetchPeople()

        app.people.filter(_.uuid == person.uuid).foreach(_.checked = true)

        redrawPeople(Some(person))
//        scrollToPerson(person)
      }
    })

    back.setOnClickListener(new OnClickListener() {
      def onClick(v: View) {
        val intent: Intent = new Intent(AddPeopleScreen.this, classOf[TotalsScreen])
        startActivity(intent)
      }
    })

    next.setOnClickListener(new OnClickListener() {
      def onClick(v: View) {
        val intent: Intent = new Intent(AddPeopleScreen.this, classOf[SplitScreen])
        startActivity(intent)
      }
    })

    Shared.setupSmartFooterHiding(this, R.id.root, R.id.whole, R.id.body, R.id.footer)
    Shared.setupAd(this, R.id.apsAdView)
  }

  def scrollToPerson(v: Person) = {
    peopleTable.getChildAt(app.people.indexOf(v)).asInstanceOf[TableRow].requestFocus()
//  findViewById(R.id.scroller).asInstanceOf[ScrollView].setFocusableInTouchMode()
  }

  def refetchPeople() = {
    Shared.refetchPeople(app)
  }

  def redrawPeople(focusPerson: Option[Person] = None): Unit = {

    peopleTable.removeAllViews()

    app.people.foreach(v => {
      val row = new TableRow(this)

      val rowCheckbox = new CheckBox(this)
      rowCheckbox.setChecked(v.checked)
      rowCheckbox.setOnClickListener(new OnClickListener {
        override def onClick(view: View): Unit = {
          v.checked = rowCheckbox.isChecked
          app.di.daos.personDao.update(v)
        }
      })

      val lp = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
      val rowAvatar = Shared.createAvatar(v, AddPeopleScreen.this, lp)



      val rowName = new EditText(this)
      rowName.setText(v.name)
      rowName.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME|InputType.TYPE_TEXT_FLAG_CAP_WORDS)
      rowName.addTextChangedListener(new TextWatcher {
        override def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int): Unit = {}
        override def afterTextChanged(s: Editable): Unit = {}
        override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int): Unit = {
          v.name = rowName.getText.toString
          app.di.daos.personDao.update(v)
        }
      })


      val rowRemove = new ImageButton(this)
      val d = getResources().getDrawable(android.R.drawable.ic_delete)
      rowRemove.setImageDrawable(d)
//      val transBack = getResources().getDrawable(android.R.color.transparent)
//      rowRemove.setBackground(transBack)

      rowRemove.setOnClickListener(new OnClickListener {
        override def onClick(view: View): Unit = {
          di.daos.personDao.remove(v.uuid.get)
          refetchPeople()
          redrawPeople()
        }
      })


      if (v.uuid.get != "me") {
        row.addView(rowRemove)
      }
      else {
        row.addView(new View(this))
      }

      Shared.setupAvatarTableCell(rowName, false, 1)

      row.addView(rowAvatar)
      row.addView(rowName)
      row.addView(rowCheckbox)

      peopleTable.addView(row)

      if (focusPerson.isDefined && focusPerson.get.uuid == v.uuid) {
        row.requestFocus()
        rowName.selectAll()
        val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE).asInstanceOf[InputMethodManager]
        imm.showSoftInput(rowName,InputMethodManager.SHOW_IMPLICIT)

      }
    })

  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent ) = {
    Log.i(TAG, s"onActivityResult $requestCode $resultCode")
    refetchPeople()
    redrawPeople()
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