package org.http4s

import java.nio.charset.Charset
import scala.collection.JavaConverters._
import org.http4s.util.CaseInsensitiveString

sealed trait CharacterSet extends HttpValue[String] with QHelper {
  def name: CaseInsensitiveString
  def charset: Charset
  def q: Float
  def satisfiedBy(characterSet: CharacterSet): Boolean
  def withQuality(q: Float): CharacterSet

  final def satisfies(characterSet: CharacterSet): Boolean = characterSet.satisfiedBy(this)

  def value: String = {
    if (q == 1.0f) name.toString
    else name + formatq(q)
  }

  override def equals(that: Any): Boolean = that match {
    case that: CharacterSet => that.name == this.name && that.q == this.q
    case _ => false
  }
}

private class CharacterSetImpl(val name: CaseInsensitiveString, val q: Float = 1.0f)
                                    extends CharacterSet {

  val charset: Charset = Charset.forName(name.toString)

  def satisfiedBy(characterSet: CharacterSet): Boolean = {
    this.q != 0.0f  &&  // a q=0.0 means this charset is invalid
    this.name == characterSet.name
  }

  def withQuality(q: Float): CharacterSet = {
    checkQuality(q)
    new CharacterSetImpl(name, q)
  }
}

object CharacterSet extends Resolvable[CaseInsensitiveString, CharacterSet] {

  protected def stringToRegistryKey(s: String): CaseInsensitiveString = s.ci

  protected def fromKey(k: CaseInsensitiveString): CharacterSet = {
    if (k == `*`.value) `*`
    else new CharacterSetImpl(k)
  }

  private def register(name: String): CharacterSet = {
    val characterSet = new CharacterSetImpl(name.ci)
    register(characterSet.name, characterSet)
    for (alias <- characterSet.charset.aliases.asScala) register(alias.ci, characterSet)
    characterSet
  }

  private class AnyCharset(val q: Float) extends CharacterSet {
    def name: CaseInsensitiveString = "*".ci
    def satisfiedBy(characterSet: CharacterSet): Boolean = q != 0.0f
    def charset: Charset = Charset.defaultCharset() // Give the system default
    override def withQuality(q: Float): CharacterSet = {
      checkQuality(q)
      new AnyCharset(q)
    }
  }

  val `*`: CharacterSet = new AnyCharset(1.0f)

  // These six are guaranteed to be on the Java platform. Others are your gamble.
  val `US-ASCII`     = register("US-ASCII")
  val `ISO-8859-1`   = register("ISO-8859-1")
  val `UTF-8`        = register("UTF-8")
  val `UTF-16`       = register("UTF-16")
  val `UTF-16BE`     = register("UTF-16BE")
  val `UTF-16LE`     = register("UTF-16LE")



  // Charset are sorted by the quality value, from greatest to least
  implicit def charactersetOrdering = new Ordering[CharacterSet] {
    def compare(x: CharacterSet, y: CharacterSet): Int = {
      val diff = y.q - x.q
      (diff*1000).toInt       // Will ignore significance below the third decimal place
    }
  }
}
