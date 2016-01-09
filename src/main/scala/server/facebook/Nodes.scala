package server.facebook

trait Node

case class UserNode(var id: String,
                    var about: String,
                    var birthday: String,
                    var email: String,
                    var first_name: String,
                    var public_key: String,
                    var encrypted_special_key: String) extends Node

case class PageNode(var id: String,
                    var about: String,
                    var description: String,
                    var name: String,
                    var likes: Int) extends Node

case class PostNode(var id: String,
                    var created_time: String,
                    var description: String,
                    var from: String,
                    var from_name: String,
                    var message: String,
                    var encrypted_secret_keys: List[EncryptedSecretKey],
                    var to: List[String],
                    var updated_time: String,
                    var encrypted: Boolean,
                    var to_all_friends: Boolean,
                    var message_iv: String,
                    var signature: String) extends Node

case class FriendListNode(var id: String,
                          var name: String,
                          var owner: String) extends Node

case class PhotoNode(var id: String,
                     var album: String,
                     var created_time: String,
                     var from: String,
                     var height: Int,
                     var name: String,
                     var width: Int,
                     var caption: String) extends Node

case class AlbumNode(var id: String,
                     var count: Int,
                     var cover_photo: String,
                     var created_time: String,
                     var description: String,
                     var from: String,
                     var name: String,
                     var album_type: String,
                     var updated_time: String) extends Node

case class PublicKeyNode(var id: String,
                         var public_Key: String) extends Node

case class EncryptedSecretKey(var to: String,
                              var encrypted_secret_key: String)

