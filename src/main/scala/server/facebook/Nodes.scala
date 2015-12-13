package server.facebook

trait Node

case class UserNode(var id: String,
                    var about: String,
                    var birthday: String,
                    var email: String,
                    var first_name: String,
                    var public_key: String) extends Node

case class PageNode(var id: String,
                    var about: String,
                    var description: String,
                    var name: String,
                    var likes: Int) extends Node

case class PostNode(var id: String,
                    var created_time: String,
                    var description: String,
                    var from: String,
                    var message: String,
                    var to: List[String],
                    var updated_time: String) extends Node

case class PostNodeV2(var id: String,
                      var created_time: String,
                      var description: String,
                      var from: String,
                      var message: String,
                      var encrypted_message: String,
                      var encrypted_private_keys: List[EncryptedPrivateKey],
                      var to: List[String],
                      var updated_time: String) extends Node


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

case class EncryptedPrivateKey(var to: String,
                               var encrypted_private_key: String)

