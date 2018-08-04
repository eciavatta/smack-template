package smack.models

import java.util.Date

object Tweeters {

  // Objects
  case class User(id: Long, username: String, registeredSince: Date)
  case class Tweet(id: Long, message: String, author: User, date: Date)

  // Events
  case class UserCreated(email: String, username: String, password: String)
  case class TweetSent(message: String, userId: Long)

}
