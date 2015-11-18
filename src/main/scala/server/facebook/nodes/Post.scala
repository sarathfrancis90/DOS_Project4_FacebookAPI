package server.facebook.nodes

class Post(var _id: String,
           var _created_time: String, /*FIXME: a good time*/
           var _description: String,
           var _from: String, /*FIXME: can be User or Page*/
           var _message: String,
           var _to: String, /*FIXME: can be list of Users or Pages*/
           var _updated_time: String /*FIXME: a good time*/) {

  def id = _id

  def id_=(in: String): Unit = _id = in

  def created_time = _created_time

  def created_time_=(in: String): Unit = _created_time = in

  def description = _description

  def description_=(in: String): Unit = _description = in

  def from = _from

  def from_=(in: String): Unit = _from = in

  def message = _message

  def message_=(in: String): Unit = _message = in

  def to = _to

  def to_=(in: String): Unit = _to = in

  def updated_time = _updated_time

  def updated_time_=(in: String): Unit = _updated_time = in
}
