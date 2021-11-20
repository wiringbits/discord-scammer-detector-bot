package detectors
import munit.FunSuite
import net.wiringbits.sdb.detectors.{ProhibitedWordDetector, SimilarToKeyword}

class ProhibitedWordDetectorSpec extends FunSuite {
  test("shouldn't find a match") {
    val username = "wiringbits"
    val prohibitedWordDetector = new ProhibitedWordDetector(Some(List("support", "help")))

    val response = prohibitedWordDetector.findPotentialScammer(username)
    assertEquals(response, None)
  }

  test("should find an exact match") {
    val username = "support"
    val prohibitedWordDetector = new ProhibitedWordDetector(Some(List("support", "help")))

    val response = prohibitedWordDetector.findPotentialScammer(username)
    assertEquals(response, Some(SimilarToKeyword(exactMatch = true, keyword = username)))
  }

  test("should find an almost exact match") {
    val username = "suppor"
    val prohibitedWordDetector = new ProhibitedWordDetector(Some(List("support", "help")))

    val response = prohibitedWordDetector.findPotentialScammer(username)
    assertEquals(response, Some(SimilarToKeyword(exactMatch = false, keyword = "support")))
  }
}
