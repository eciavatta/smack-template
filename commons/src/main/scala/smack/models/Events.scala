package smack.models

object Events {

  case class UserCreating(email: String, password: String, fullName: String)
  case class UserUpdating(fullName: String)
  case class SitesListing(userId: String)
  case class SiteCreating(userId: String, domain: String)
  case class SiteUpdating(domain: String)
  case class SiteDeleting(userId: String)
  case class LogEvent(url: String, ipAddress: String, userAgent: String)
  case class HealthMessage(protocol: String, method: String, uri: String, clientIp: String, hostname: String, hostIp: String)

}
