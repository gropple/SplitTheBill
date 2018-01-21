package cpframework_shared

import com.almworks.sqlite4java.SQLiteStatement


abstract class CursorWrapper {
  def nextFieldAsString: String
  def nextFieldAsInt: Int
  def nextFieldAsBoolean: Boolean = nextFieldAsInt == 1
  def nextFieldAsDouble: Double

  // If next field is null, it will also advance to the field beyond that. Else it will stay where it is (so value can be read)
  def isNextFieldNull: Boolean

  // These don't use the idx field, they're just to avoid rewriting a lot of code
  def columnString(idx: Int) = nextFieldAsString
  def columnInt(idx: Int) = nextFieldAsInt
  def columnDouble(idx: Int) = nextFieldAsDouble
  def columnNull(idx: Int) = isNextFieldNull

  // To allow abstracting over JSON and JS objects
  def openNamedArray(name: String)
  // If next field is end of the array, it will also advance. Else it stays where it is.
  def endOfArray(): Boolean
  def openObjectInsideArray()
  def closeObject()
  // E.g. is it JSON or JS where we have the data directly available to read now
  def isInlineData(): Boolean

  def dispose()
  def step(): Boolean
  def reset()

  def createModelProxy[T<:HasUUID](Dao: Dao[T], uuid: String): ModelProxy[T] = {
    val result = new ModelProxyFetchable[T](Dao, uuid)
    //    result.set(Some(dao.readJson(reader)), uuid)
    return result
  }
}

class SqlLiteCursorWrapper(cursor: SQLiteStatement) extends CursorWrapper {
  def dispose() = {
    cursor.dispose()
  }

  def step(): Boolean = {
    cursor.step()
  }

  def reset() = {
    columnIdx = 0
  }

  var columnIdx = 0

  override def nextFieldAsString: String = {
    var result = cursor.columnString(columnIdx)
    if (result == null) {
      result = ""
    }
    columnIdx += 1
    return result
  }

  override def nextFieldAsInt: Int = {
    var result = cursor.columnInt(columnIdx)
    columnIdx += 1
    return result
  }

  override def nextFieldAsDouble: Double = {
    var result = cursor.columnDouble(columnIdx)
    columnIdx += 1
    return result
  }

  override def isNextFieldNull: Boolean = {
    if (cursor.columnNull(columnIdx)) {
      columnIdx += 1
      return true
    }
    return false
  }

  // Unimplemented as these will never be used
  override def openNamedArray(name: String): Unit = ???

  override def closeObject(): Unit = ???

  override def isInlineData(): Boolean = false

  override def openObjectInsideArray(): Unit = ???

  override def endOfArray(): Boolean = ???
}



class JsonCursorWrapper(reader: JsonReader) extends CursorWrapper {
  var storedToken: (String, JsonType.Value) = null

  override def nextFieldAsString: String = {
    return reader.nextFieldAsString
  }

  override def nextFieldAsInt: Int = {
    return reader.nextFieldAsInt
  }

  override def nextFieldAsDouble: Double = {
    return reader.nextFieldAsDouble
  }

  override def isNextFieldNull: Boolean = {
    return reader.isNextFieldNull
  }

  def dispose() = {
  }

  def step(): Boolean = {
    return true
  }

  def reset() = {}

  override def openNamedArray(name: String) = {
    var t = reader.nextToken
    if (t._1 == ",") {
      t = reader.nextToken
    }
    assert(t._1 == name)
    t = reader.nextToken;     assert(t._1 == "[")
  }

  override def endOfArray(): Boolean = {
    val end = reader.peekIsEndArray
    if (end) {
      var t = reader.nextToken; assert(t._1 == "]")
    }
    end
  }

  override def openObjectInsideArray() = {
    var t = reader.nextToken
    if (t._1 == ",") {
      t = reader.nextToken
    }
    assert(t._1 == "{")
  }

  override def closeObject() = {
    var t = reader.nextToken; assert(t._1 == "}")
  }

  override def isInlineData(): Boolean = true

}

