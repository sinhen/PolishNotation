package cn.gshuaiqiang.formula;

import java.math.BigDecimal;

/**
 * 计算核心
 * @author gaosq
 */
public interface CalculationCore {
    BigDecimal calculation(BigDecimal[] bigDecimalArr);
}