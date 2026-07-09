package com.example

import com.example.ui.ExpressionParser
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun parser_evaluatesSimpleExpressions() {
    val parser1 = ExpressionParser("2 + 3 * 4")
    assertEquals(14.0, parser1.parse(), 0.0001)

    val parser2 = ExpressionParser("(2 + 3) * 4")
    assertEquals(20.0, parser2.parse(), 0.0001)
  }

  @Test
  fun parser_evaluatesPercentages() {
    val parser1 = ExpressionParser("10%")
    assertEquals(0.1, parser1.parse(), 0.0001)

    val parser2 = ExpressionParser("50 + 10%")
    assertEquals(50.1, parser2.parse(), 0.0001)
  }

  @Test
  fun parser_evaluatesDivision() {
    val parser = ExpressionParser("10 / 4")
    assertEquals(2.5, parser.parse(), 0.0001)
  }

  @Test
  fun parser_evaluatesUnaryMinus() {
    val parser = ExpressionParser("-5 + 3")
    assertEquals(-2.0, parser.parse(), 0.0001)
  }
}
