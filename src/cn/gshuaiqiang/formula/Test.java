package cn.gshuaiqiang.formula;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class Test {
    static void test(String infix){
        System.out.println(infix+ "=" +FormulaUtil.calculation(FormulaUtil.toPostfix(FormulaUtil.split(infix))));
    }
    public static void main(String[] args) {


        System.out.println(FormulaUtil.toPostfix(FormulaUtil.split("-1+2*1")));
        System.out.println(FormulaUtil.toPostfix(FormulaUtil.split("-1+2*-1")));
        System.out.println(FormulaUtil.toPostfix(FormulaUtil.split("-1*-2*-3")));
        System.out.println(FormulaUtil.toPostfix(FormulaUtil.split("(-1)*(-2)*(-3)")));

        Map<String,String> map = new HashMap<>();
        map.put("x","2");

        System.out.println(FormulaUtil.canCalculation(FormulaUtil.toPostfix(FormulaUtil.split("(-1)*(-x)*(-3)")),map));
        System.out.println(FormulaUtil.calculation(FormulaUtil.toPostfix(FormulaUtil.split("(-1)*(-x)*(-3)")),map));
        System.out.println(FormulaUtil.calculation(FormulaUtil.toPostfix(FormulaUtil.split("x<1||x>=1.5")),map));

        test("3+2--1");

        /*System.out.println(FormulaUtil.isBalance("(3+5)*((2+3)-1)"));
        System.out.println(FormulaUtil.isBalance("(3+5)*(((2+3)-1)-2"));
        System.out.println(FormulaUtil.isBalance("(3+5))*((2+3)-1)-2"));

        System.out.println(" a b ".replace(" ",""));
        System.out.println(Integer.parseInt("1"));

        System.out.println((int)'a');
        System.out.println(3+-1);

        int a = 1;
        boolean b = !(a == 1);

        System.out.println("a".equals('a'));*/
    }
}

