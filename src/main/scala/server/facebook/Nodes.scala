package server.facebook

trait Node

case class UserNode(var id: String,
                    var about: String,
                    var birthday: String,
                    var email: String,
                    var first_name: String) extends Node

case class PageNode(var id: String,
                    var about: String,
                    var description: String,
                    var name: String,
                    var likes: Integer) extends Node

case class PostNode(var id: String,
                    var created_time: String, /*FIXME: a good time*/
                    var description: String,
                    var from: String, /*FIXME: can be User or Page*/
                    var message: String,
                    var to: String, /*FIXME: can be list of Users or Pages*/
                    var updated_time: String /*FIXME: a good time*/) extends Node

case class FriendListNode(var id: String,
                          var name: String,
                          var owner: String) extends Node

case class PhotoNode(var id: String,
                     var album: String, /*FIXME: an Album node*/
                     var created_time: String, /*FIXME: */
                     var from: String, /*FIXME: can be a User profile or Page profile*/
                     var height: Integer,
                     var name: String,
                     var width: String) extends Node

case class Album(var id: String,
                 var count: Integer,
                 var cover_photo: String,
                 var created_time: String, /*FIXME: */
                 var description: String,
                 var from: String, /*FIXME: */
                 var name: String,
                 var album_type: String,
                 var updated_time: String /*FIXME: */) extends Node

