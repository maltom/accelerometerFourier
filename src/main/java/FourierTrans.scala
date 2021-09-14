import scala.math.{Pi, abs, cos, cosh, pow, sin, sinh, sqrt}

class FourierTrans {
  def exp(c: Complex) = {
    val r = (cosh(c.re) + sinh(c.re))
    Complex(cos(c.im), sin(c.im)) * r
  }

  def _fft(cSeq: Seq[Complex], direction: Complex, scalar: Int): Seq[Complex] = {
    if (cSeq.length == 1) {
      return cSeq
    }
    val n = cSeq.length
    assume(n % 2 == 0, "The number of samples must be even.")

    val evenOddPairs = cSeq.grouped(2).toSeq
    val evens = _fft(evenOddPairs map (_ (0)), direction, scalar)
    val odds = _fft(evenOddPairs map (_ (1)), direction, scalar)

    def leftRightPair(k: Int): (Complex, Complex) = {
      val base = evens(k) / scalar
      val offset = exp(direction * (Pi * k / n)) * odds(k) / scalar
      (base + offset, base - offset)
    }

    val pairs = (0 until n / 2) map leftRightPair
    val left = pairs map (_._1)
    val right = pairs map (_._2)
    left ++ right
  }

  def fft(cSeq: Seq[Complex]): Seq[Complex] = _fft(cSeq, Complex(0, 2), 1)

  def absfft(cSeq: Seq[Complex]): Seq[Double] = {
    val cmplx = fft(cSeq)

    return cmplx.collect { case in => sqrt(pow(in.re, 2.0) + pow(in.im, 2.0)) }
  }
}