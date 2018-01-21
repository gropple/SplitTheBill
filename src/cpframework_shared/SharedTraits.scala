package cpframework_shared

import scala.beans.BeanProperty

trait HasUUID {
  @BeanProperty
  var uuid: Option[String] = None

  var isRemoved: Boolean = false

  var hasChanged: Boolean = false
}