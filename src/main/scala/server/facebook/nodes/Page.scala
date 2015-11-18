package server.facebook.nodes

class Page(var _id:String,
            var _about:String,
            var _description:String,
            var _likes:Integer) {

  // setters
  def id = _id
  def about = _about
  def description = _description
  def likes = _likes

  //getters
  def id_=(idIn: String): Unit = _id = idIn
  def about_=(aboutIn: String): Unit = _about = aboutIn
  def description_=(descriptionIn: String): Unit = _description = descriptionIn
  def likes_=(likesIn: Integer): Unit = _likes = likesIn
}
