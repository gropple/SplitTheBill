package cpframework_shared

import cpframework_provided.Dependencies


// Because we can't fetch a model while we're in the middle of fetching another, as the sqlite4java job queue only starts
// one thread. So we proxy it and lazy-fetch it later.
// Don't use directly, use a derived class and provide the uuid method.
class ModelProxy[T<:HasUUID] {
  var v: Option[T] = None
  //  protected var _uuid: Option[String] = None

  def set(t: T) = {
    v = Some(t)
  }

  def set(t: Option[T], _uuid: String) = {
    v = t
  }

  def set(t: Option[T], _uuid: Option[String]) = {
    v = t
    //    this._uuid = _uuid
  }

  def set(t: Option[T]) = {
    v = t
    //    this._uuid = None
  }

  // Here so we don't have to fetch the object if we just need UUID.
  def uuid(): Option[String] = v.get.uuid

  def get(cache: Option[Dependencies] = None): Option[T] = v

  def use(cache: Option[Dependencies] = None): T = {
    assert (v.isDefined)
    return v.get
  }

  def refetch(): Option[T] = {
    return v
  }

  def isDefined() = v.isDefined

  def canEqual(other: Any): Boolean = other.isInstanceOf[ModelProxy[T]]

  override def equals(other: Any): Boolean = {
    other match {
      case that: ModelProxy[T] =>
        if (that.isDefined()) that.use()
        (that canEqual this) &&
          v == that.v
      case _ => false
    }
  }

  override def hashCode(): Int = {
    val state = Seq(v)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

// We have a UUID and have a backing store to get it from
class ModelProxyFetchable[T<:HasUUID](Dao: Dao[T], _uuid: String) extends ModelProxy[T] {

  assert(!_uuid.startsWith("Some("))

  // Definitely referring to something, just maybe haven't fetched it yet
  override def isDefined() = true

  override def uuid(): Option[String] = {
    if (v.isDefined) {
      v.get.uuid
    }
    else {
      Some(_uuid)
    }
  }

  override def get(cache: Option[Dependencies] = None): Option[T] = {
    if (!v.isDefined) {
      v = Dao.get(_uuid)
    }
    return v
  }

  // Only use this method if certain that the object being fetched actually exists
  override def use(cache: Option[Dependencies] = None): T = {
    if (!v.isDefined) {
      v = Dao.get(_uuid, true, true, cache)
    }
    if (!v.isDefined) {
//      log.error(s"Failed to get ${_uuid}")
      assert (false)
    }
    return v.get  
  }

  override def refetch(): Option[T] = {
    v = Dao.get(uuid.get)
    return v
  }

}

//class ModelProxyJSON[T<:HasUUID](Dao: Dao[T], _uuid: String) extends ModelProxyServer[T](Dao, _uuid) {
//
//}

class ModelProxyUUID[T<:HasUUID](dao: Dao[T], _uuid: String) extends ModelProxy[T] {
  override def uuid(): Option[String] = Some(_uuid)
}



//case class UserProxyServer(daos: DaoCollection, uuid: String) extends ModelProxyServer[User](daos.userDao, uuid)
