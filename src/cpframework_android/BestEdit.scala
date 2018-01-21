package cpframework_android

import android.content.Context
import android.text.{Editable, TextWatcher}
import android.util.AttributeSet
import android.widget.{TextView, EditText}
import android.widget.TextView.BufferType

// Almost every Android widget does not fire its events on programmatically setting it - except EditText and TextWatcher.
// This class is a simple EditText wrapper to avoid firing events on setText.
class BestEdit(context: Context, attrs: AttributeSet = null) extends EditText(context, attrs) {

  var ignoreEvents = false

  def setTextChangedListener(textChanged: (String) => Unit) {
    addTextChangedListener(new TextWatcher {
      override def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int): Unit = {}

      override def afterTextChanged(s: Editable): Unit = {}

      override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int): Unit = {
        if (ignoreEvents) return
        val text = getText.toString
        textChanged(text)
      }
    })
  }


  override def setText(text: CharSequence, `type`: TextView.BufferType) {
    ignoreEvents = true
    super.setText(text, BufferType.EDITABLE)
    ignoreEvents = false
  }
}