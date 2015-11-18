package server.facebook.nodes

class User(var _id: String,
           var _about: String,
           var _birthday: String,
           var _email: String,
           var _first_name: String) {

  def id = _id

  def id_=(in: String): Unit = _id = in

  def about = _about

  def about_=(in: String): Unit = _about = in

  def birthday = _birthday

  def birthday_=(in: String): Unit = _birthday = in

  def email = _email

  def email_=(in: String): Unit = _email = in

  def first_name = _first_name

  def first_name_=(in: String): Unit = _first_name = in
}
