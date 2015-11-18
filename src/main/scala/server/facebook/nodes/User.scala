package server.facebook.nodes

class User(var _id:String,
            var _about:String,
            var _birthday:String,
            var _email:String,
            var _first_name:String) {

  // setters
  def id = _id
  def about = _about
  def birthday = _birthday
  def email = _email
  def first_name = _first_name

  // getters
  def id_=(idIn: String): Unit = _id = idIn
  def about_=(aboutIn: String): Unit = _about = aboutIn
  def birthday_=(birthdayIn: String): Unit = _birthday = birthdayIn
  def email_=(emailIn: String): Unit = _email = emailIn
  def first_name_=(first_nameIn: String): Unit = _first_name = first_nameIn
}
