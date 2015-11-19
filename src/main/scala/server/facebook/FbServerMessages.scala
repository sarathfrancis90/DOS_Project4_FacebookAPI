package server.facebook

case class CreateFbNodeReq(nodeType: String, node: Node)
case class CreateFbNodeRsp(result: Boolean, id: String)
