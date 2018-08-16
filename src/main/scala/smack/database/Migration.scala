package smack.database

trait Migration {

  def up: String
  def down: String
  def tag: String

}
