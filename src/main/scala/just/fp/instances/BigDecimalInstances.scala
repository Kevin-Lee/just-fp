package just.fp.instances

import just.fp.Equal

/**
  * @author Kevin Lee
  * @since 2019-07-28
  */
trait BigDecimalEqualInstance {
  implicit val bigDecimalEqual: Equal[BigDecimal] = new Equal[BigDecimal] {
    @SuppressWarnings(Array("org.wartremover.warts.Equals"))
    override def equal(x: BigDecimal, y: BigDecimal): Boolean = x == y
  }
}
