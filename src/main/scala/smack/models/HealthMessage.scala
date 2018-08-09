package smack.models

case class HealthMessage(protocol: String, method: String, uri: String, clientIp: String, hostname: String, hostIp: String)
