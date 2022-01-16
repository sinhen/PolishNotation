package cn.gshuaiqiang.formula;

import java.util.Objects;

/**
 * 运算符类
 * @author gaosq
 */
public class Operator {
    //符号
    private String symbol;
    //符号str的char
    private char[] symbolCharArr;
    //运算的优先级 优先级越高,越先计算
    private int priority;
    //优先级相同时,运算的顺序
    private int operationOrder;
    //计算核心所依赖的操作数个数
    private int numberOfOperands;
    //计算核心
    CalculationCore calculationCore;
    //从左至右
    public static final int ORDER_LR = 1;
    //从右至左
    public static final int ORDER_RL = 2;

    public Operator(String symbol, int priority, int operationOrder, int numberOfOperands, CalculationCore calculationCore) {
        this(symbol,priority);
        this.operationOrder = ORDER_LR;
        this.numberOfOperands = numberOfOperands;
        this.calculationCore = calculationCore;

    }

    public Operator(String symbol, int priority, int numberOfOperands, CalculationCore calculationCore) {
        this(symbol,priority,ORDER_LR,numberOfOperands,calculationCore);
    }

    public Operator(String symbol, int priority) {
        this(symbol);
        this.priority = priority;
    }

    public Operator(String symbol) {
        this.symbol = symbol;
        this.symbolCharArr = symbol.toCharArray();
    }

    public int getNumberOfOperands() {
        return numberOfOperands;
    }

    public void setNumberOfOperands(int numberOfOperands) {
        this.numberOfOperands = numberOfOperands;
    }

    public CalculationCore getCalculationCore() {
        return calculationCore;
    }

    public void setCalculationCore(CalculationCore calculationCore) {
        this.calculationCore = calculationCore;
    }

    public String getSymbol() {
        return symbol;
    }

    public Operator setSymbol(String symbol) {
        this.symbol = symbol;
        return this;
    }

    public int getPriority() {
        return priority;
    }

    public Operator setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public char[] getSymbolCharArr() {
        return symbolCharArr;
    }

    public void setSymbolCharArr(char[] symbolCharArr) {
        this.symbolCharArr = symbolCharArr;
    }

    public int getOperationOrder() {
        return operationOrder;
    }

    public void setOperationOrder(int operationOrder) {
        this.operationOrder = operationOrder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Operator operator = (Operator) o;
        return Objects.equals(symbol, operator.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol);
    }
}