package cpframework_shared

// These values cannot be placed on stack and are returned only
object JsonType extends Enumeration {
  type Type = Value
  val InsideObject,
  InsideArray,
  Key,
  StartOfKey,
  StartOfStringField,
  StartOfNumericField,
  StartOfArrayField,
  ArrayField,
  Field,
  NullField,
  FieldSeparator,
  EndOfObject,
  EndOfArray,
  ArrayElementSeparator,
  Error,
  End = Value
}

// These values can be placed on stack, i.e. is persistent state
object JsonStackType extends Enumeration {
  type Type = Value
  val InsideObject,
  InsideArray,
  Key = Value
}

// Doesn't seem to be one for ScalaJS, so rolling our own. ScalaJS has no reflection.
// This supports only a limited subset of JSON, basically what I produce
class JsonReader(raw: String) {
  var pos = 0

  val stack = collection.mutable.Stack[JsonStackType.Value]()

  def substring = raw.substring(pos)

  // Peek methods don't change state and don't advance us in the string
  def peekIsEndArray: Boolean = raw(pos) == ']'

  // Newer, better, more accurate parser than nextField
  def nextToken: (String, JsonType.Value) = {
    if (pos >= raw.length) {
      return ("", JsonType.End)
    }

    var c: Char = raw(pos)

    c match {
      case '{' => {pos += 1; stack.push(JsonStackType.InsideObject); return (c.toString, JsonType.InsideObject)}
      case '[' => {pos += 1; stack.push(JsonStackType.InsideArray); return (c.toString, JsonType.InsideArray)}
      case '}' => {pos += 1; stack.pop(); if (!stack.isEmpty && stack.head == JsonStackType.Key) stack.pop(); return (c.toString, JsonType.EndOfObject)}
      case ']' => {pos += 1; stack.pop(); if (!stack.isEmpty && stack.head == JsonStackType.Key) stack.pop(); return (c.toString, JsonType.EndOfArray)}
      case ',' => {
        pos += 1
        stack.head match {
          case JsonStackType.InsideArray => return (c.toString, JsonType.ArrayElementSeparator)
          case JsonStackType.InsideObject => return (c.toString, JsonType.FieldSeparator)
          case _ => assert(false)
        }
      }
      case _ =>
    }

    var state = stack.head match {
      case JsonStackType.InsideArray => JsonType.InsideArray
      case JsonStackType.InsideObject => JsonType.InsideObject
      case JsonStackType.Key => JsonType.Key // Last thing we found was a "key": , so we're looking for an Array, Object or Field
      case _ => assert(false)
    }
    var startPos = pos

    while (true) {

      // [>"graham">,"charlie"]
      if (state == JsonType.InsideArray || state == JsonType.ArrayElementSeparator) {
        c match {
          case '"' => { state = JsonType.StartOfArrayField; startPos = pos + 1 }
          case _ =>
        }
      }
      else if (state == JsonType.InsideObject || state == JsonType.FieldSeparator) {
        c match {
          case '"' => { state = JsonType.StartOfKey; startPos = pos + 1 }
          case _ =>
        }
      }
      else if (state == JsonType.StartOfKey) {
        c match {
          case ':' => {
            pos += 1; stack.push(JsonStackType.Key); return (raw.substring(startPos, pos - 2), JsonType.Key)
          }
          case _ =>
        }
      }
      else if (state == JsonType.Key) {
        c match {
          case '"' => {
            state = JsonType.StartOfStringField; startPos = pos + 1
          }
          case 'n' => {
            assert(raw.substring(pos, pos + 4) == "null")
            stack.pop(); pos += 4; return (null, JsonType.NullField)
          }
          case _ =>
        }
        if (c.isDigit) state = JsonType.StartOfNumericField
      }
      else if (state == JsonType.StartOfStringField) {
        c match {
          case '"' => { stack.pop(); pos += 1; return (raw.substring(startPos, pos - 1), JsonType.Field) }
          case _ =>
        }
      }
      else if (state == JsonType.StartOfNumericField) {
        c match {
          case '}' => { stack.pop(); return (raw.substring(startPos, pos), JsonType.Field) }
          case ',' => { stack.pop(); return (raw.substring(startPos, pos), JsonType.Field) }
          case _ =>
        }
      }
      else if (state == JsonType.StartOfArrayField) {
        c match {
          case '"' => { pos += 1; return (raw.substring(startPos, pos - 1), JsonType.ArrayField) }
          case _ =>
        }
      }

      pos += 1
      c = raw(pos)

    }

    return (s"Error reading json at pos ${pos} state ${stack} json '${raw.substring(pos)}'", JsonType.Error)
  }

  // 0 = Looking for {
  // 1 = Looking for key
  // 2 = Looking for end-of-key :
  // 3 = Looking for start-of-field ' or digit or null
  // 4 = Looking for end-of-string-field  '
  // 5 = Looking for end-of-digit-field , or }
  // 6 = Looking for end-of-null-field
  // 7 = Found start of array [

  /*def nextField: Tuple2[String, String] = {
    var c: Char = raw(pos)
    var mode: Int = 1

    while (c == '{' || c == ',') {
      pos += 1
      c = raw(pos)
    }

    mode = 2

    val startOfKey = pos

    while (c != ':') {
      pos += 1
      c = raw(pos)
    }

    mode = 3

    val endOfKey = pos
    val key = raw.substring(startOfKey, pos)


    var keepGoing = true
    while (keepGoing) {

      pos += 1
      c = raw(pos)

      if (c == '\'') {
        mode = 4
        keepGoing = false
        pos += 1
        c = raw(pos)
      }
      else if (c.isDigit) {
        mode = 5
        keepGoing = false
      }
      else if (c == 'n') {
        mode = 6
        keepGoing = false
      }
      else if (c == '[') {
        mode = 7
        keepGoing = false
      }
      else assert(false)
    }

    val startOfField = pos

    if (mode == 4) {
      while (c != '\'') {
        pos += 1
        c = raw(pos)
      }

      val field = raw.substring(startOfField, pos)

      pos += 1 // Skip the '

      return new Tuple2(key, field)
    }
    else if (mode == 5) {
      while (c != ',' && c != '}') {
        pos += 1
        c = raw(pos)
      }

      val field = raw.substring(startOfField, pos)
      return new Tuple2(key, field)

    }
    else {
      assert (mode == 6)
      assert (raw.substring(pos, pos + 4) == "null")
      pos += 4
      c = raw(pos)

      val field = null
      return new Tuple2(key, field)

    }

  }
*/

  def nextField: Tuple2[String, String] = {
    var t1 = nextToken
    while (t1._2 != JsonType.Key) {
      t1 = nextToken
    }
    var t2 = nextToken
    assert(t2._2 == JsonType.Field || t2._2 == JsonType.NullField)
    return (t1._1, t2._1)
  }

  def isNextFieldNull: Boolean = {
    val curPos = pos
    var t1 = nextToken
    while (t1._2 != JsonType.Key) {
      t1 = nextToken
    }
    var t2 = nextToken
    val result = (t2._2 == JsonType.NullField)
    if (!result) {
      pos = curPos
    }
    return result
  }

  def nextFieldAsString(): String = {
    val t = nextField
    t._2
  }

  def nextFieldAsInt(): Int = {
    val t = nextField
    t._2.toInt
  }

  def nextFieldAsDouble(): Double = {
    val t = nextField
    t._2.toDouble
  }

  def nextFieldAsBoolean(): Boolean = {
    nextFieldAsInt() == 1
  }

}


class JsonWriter {
  def writeFieldBoolean(s: String, v: Boolean) = {
    val value = if (v) 1 else 0
    writeFieldInt(s, value)
  }

  val sb = new StringBuilder

  // 0 = Started
  // 1 = Inside { or [, not written a field
  // 2 = Inside { or [, written a field
  // 3 = Outside } or ], written a {}
  var mode = 0

  def writeFieldString(key: String, field: String) = {
    if (mode == 2) {
      sb += ','
    }
    sb += '"'
    sb ++= key
    sb ++= "\":\""
    sb ++= field
    sb += '"'
    mode = 2
  }

  def writeFieldNull(key: String) = {
    if (mode == 2) {
      sb += ','
    }
    sb += '"'
    sb ++= key
    sb ++= "\":null"
    mode = 2
  }

  def writeFieldInt(key: String, field: Int) = {
    if (mode == 2) {
      sb += ','
    }
    sb += '"'
    sb ++= key
    sb ++= "\":"
    sb ++= field.toString
    mode = 2
  }

  def writeFieldDouble(key: String, field: Double) = {
    if (mode == 2) {
      sb += ','
    }
    sb += '"'
    sb ++= key
    sb ++= "\":"
    sb ++= field.toString
    mode = 2
  }

  def writeArrayValueString(field: String) = {
    if (mode == 2) {
      sb += ','
    }
    sb += '"'
    sb ++= field
    sb += '"'
    mode = 2
  }


  def startObject() = {
    if (mode == 3) {
      sb += ','
    }
    sb += '{'
    mode = 1
  }


  def startNamedObject(v: String) = {
    if (mode == 3) {
      sb += ','
    }
    sb += '"'
    sb ++= v
    sb ++= "\":{"
    mode = 1
  }

  def startArray = {
    if (mode == 3) {
      sb += ','
    }
    sb ++= s"["
    mode = 1
  }

  def startNamedArray(v: String) = {
    if (mode == 2 || mode == 3) {
      sb += ','
    }
    sb += '"'
    sb ++= v
    sb ++= "\":["
    mode = 1
  }

  def endArray() = {
    sb += ']'
    mode = 3
  }

  def endObject() = {
    sb += '}'
    mode = 3
  }

  override def toString(): String = {
    return sb.toString()
  }
}