package net.wiringbits.sdb.detectors

import munit.FunSuite

class ForbiddenWordsDetectorSpec extends FunSuite {
  test("shouldn't find a match") {
    val username = "wiringbits"
    val prohibitedWordDetector = new ForbiddenWordsDetector(List("support", "help"))

    val response = prohibitedWordDetector.findMatch(username)
    assertEquals(response, None)
  }

  test("should find an exact match") {
    val username = "support"
    val prohibitedWordDetector = new ForbiddenWordsDetector(List("support", "help"))

    val response = prohibitedWordDetector.findMatch(username)
    assertEquals(response, Some(ForbiddenWordMatched(username)))
  }

  test("should find an almost exact match") {
    val username = "suppor"
    val prohibitedWordDetector = new ForbiddenWordsDetector(List("support", "help"))

    val response = prohibitedWordDetector.findMatch(username)
    assertEquals(response, Some(ForbiddenWordMatched("support")))
  }

  test("check if contains") {
    val username = "I'm here to help"
    val prohibitedWordDetector = new ForbiddenWordsDetector(List("support", "help"))

    val response = prohibitedWordDetector.findMatch(username)
    assertEquals(response, Some(ForbiddenWordMatched("help")))
  }
}
