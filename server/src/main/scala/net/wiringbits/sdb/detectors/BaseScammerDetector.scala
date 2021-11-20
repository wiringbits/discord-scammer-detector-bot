package net.wiringbits.sdb.detectors

import org.apache.commons.text.similarity.LevenshteinDistance

trait BaseScammerDetector {

  // Similarities smaller that this value lead to potential scammers
  // Higher values means slower runtime, and leads to most matches but anything above 3
  // is likely very annoying.
  val DistanceThreshold = 1
  val distance = new LevenshteinDistance(DistanceThreshold)

  /**
   * For now, just lower case and take everything between the character 20 and 255,
   * ignoring anything else.
   */
  def normalize(username: String): String = {
    username.toLowerCase.filter(c => c >= 20 && c <= 255)
  }
}

abstract class BaseScammer {
  val exactMatch: Boolean
}
