package server.facebook.nodes

class Page(var _id: String,
           var _about: String,
           var _description: String,
           var _likes: Integer) {

  def id = _id

  def id_=(in: String): Unit = _id = in

  def about = _about

  def about_=(in: String): Unit = _about = in

  def description = _description

  def description_=(in: String): Unit = _description = in

  def likes = _likes

  def likes_=(in: Integer): Unit = _likes = in
}
