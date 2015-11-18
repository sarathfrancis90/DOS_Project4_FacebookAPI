package server.facebook.nodes

class FriendList(var _id: String,
                 var _name: String,
                 var _owner: String) {

  def id = _id

  def id_=(in: String): Unit = _id = in

  def name = _name

  def name_=(in: String): Unit = _name = in

  def owner = _owner

  def owner_=(in: String): Unit = _owner = in
}
