package cn.gshuaiqiang.formula;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 算式计算工具类
 * 参考资料:后缀表达式(逆波兰式)
 * @author gaosq
 */
public class FormulaUtil {
    //空变量表
    private static Map<String,String> EMPTY_VARIABLE_MAP = new HashMap<>();
    //编译期操作符;
    private static List<Operator> operatorList = new ArrayList<>();
    //运行期操作符;
    private static Map<String,Operator> operatorMap = new HashMap<>();

    //注意这里,括号的优先级为0,最低,确保不会被其他操作符弹出
    //小括号 用来干涉优先级
    private static Operator leftSmallBracket = new Operator("(");
    private static Operator rightSmallBracket = new Operator(")");
    //大括号 []代表绝对值
    private static Operator leftBigBracket = new Operator("[");
    private static Operator rightBigBracket = new Operator("]");
    //负号优先级最高
    private static Operator minusSign = new Operator("-",99999);

    //初始化支持的符号
    static{
        load(new Operator("||",40,2,(BigDecimal[] bigDecimalArr)->{
            if(BigDecimal.ZERO.equals(bigDecimalArr[0]) && BigDecimal.ZERO.equals(bigDecimalArr[1])){
                return BigDecimal.ZERO;
            }
            return BigDecimal.ONE;
        }));
        load(new Operator("&&",50,2,(BigDecimal[] bigDecimalArr)->{
            if(!BigDecimal.ZERO.equals(bigDecimalArr[0]) && !BigDecimal.ZERO.equals(bigDecimalArr[1])){
                return BigDecimal.ONE;
            }
            return BigDecimal.ZERO;
        }));
        load(new Operator(">",60,2,(BigDecimal[] bigDecimalArr)->{
            if(bigDecimalArr[0].compareTo(bigDecimalArr[1]) > 0){
                return BigDecimal.ONE;
            }
            return BigDecimal.ZERO;
        }));
        load(new Operator(">=",60,2,(BigDecimal[] bigDecimalArr)->{
            if(bigDecimalArr[0].compareTo(bigDecimalArr[1]) >= 0){
                return BigDecimal.ONE;
            }
            return BigDecimal.ZERO;
        }));
        load(new Operator("<",60,2,(BigDecimal[] bigDecimalArr)->{
            if(bigDecimalArr[0].compareTo(bigDecimalArr[1]) < 0){
                return BigDecimal.ONE;
            }
            return BigDecimal.ZERO;
        }));
        load(new Operator("<=",60,2,(BigDecimal[] bigDecimalArr)->{
            if(bigDecimalArr[0].compareTo(bigDecimalArr[1]) <= 0){
                return BigDecimal.ONE;
            }
            return BigDecimal.ZERO;
        }));
        load(new Operator("!=",60,2,(BigDecimal[] bigDecimalArr)->{
            if(bigDecimalArr[0].compareTo(bigDecimalArr[1]) != 0){
                return BigDecimal.ONE;
            }
            return BigDecimal.ZERO;
        }));
        load(new Operator("==",60,2,(BigDecimal[] bigDecimalArr)->{
            if(bigDecimalArr[0].compareTo(bigDecimalArr[1]) == 0){
                return BigDecimal.ONE;
            }
            return BigDecimal.ZERO;
        }));
        load(new Operator("+",70,2,(BigDecimal[] bigDecimalArr)->bigDecimalArr[0].add(bigDecimalArr[1])));
        load(new Operator("-",70,2,(BigDecimal[] bigDecimalArr)->bigDecimalArr[0].subtract(bigDecimalArr[1])));
        load(new Operator("*",80,2,(BigDecimal[] bigDecimalArr)->bigDecimalArr[0].multiply(bigDecimalArr[1])));
        load(new Operator("/",80,2,(BigDecimal[] bigDecimalArr)->new BigDecimal(bigDecimalArr[0].divide(bigDecimalArr[1],16,BigDecimal.ROUND_HALF_UP).stripTrailingZeros().toPlainString())));

        //load(new Operator("^",90,Operator.ORDER_RL));
        //这里按符号长度排个序,在解析 >= 和 > 这种类似符号时,长的先生效
        operatorList.sort((Operator o1, Operator o2)-> o2.getSymbol().length()-o1.getSymbol().length());

        //绝对值; 编译期时绝对值符号为[]; 这个符号只在运行期会用到;
        operatorMap.put("|",new Operator("|",0,1,(BigDecimal[] bigDecimalArr)->bigDecimalArr[0].abs()));
    }

    /**
     * 装载一个运算符
     * @param operator 运算符
     */
    private static void load(Operator operator){
        operatorList.add(operator);
        operatorMap.put(operator.getSymbol(),operator);
    }

    /**
     * 将中缀表达式转换为一个后缀表达式;
     * 1.遇到操作数时,直接将其加入postfixDeque
     * 2.遇到操作符时,弹出symbolDeque到postfixDeque,直至symbolDeque为空或栈顶优先级比当前操作符优先级更低
     * 3.遍历结束,弹出symbolDeque所有元素到postfixDeque
     *
     * 特殊处理:
     * 小括号:左括号时直接symbolDeque入栈,右括号时出栈直到遇到一个左括号
     * 大括号:在小括号的基础上,解析完毕后向postfixDeque追加一个 |(绝对值) 符号
     *
     * -号: 为减号时,按正常逻辑; 为负号时,解析到该符号时向 postfixDeque 追加一个0
     * 比如 -1,正常情况下 后缀表达式为 [1, -]; 是无法计算的,我们希望得到的是 [0,1,-]
     * 若"-"前是一个操作数或者右括号,则认为其代表的是减法,否则为负号
     * 不支持嵌套, --x不会解析成 0-(0-x)
     *
     * @param infixList 中缀表达式
     * @return 后缀表达式
     * @throws IllegalArgumentException infix非法
     */
    public static Deque<String> toPostfix (List<String> infixList){
        Deque<String> postfixDeque = new LinkedList<>();
        Deque<Operator> symbolDeque = new LinkedList<>();
        //对-的特殊处理
        boolean needAddZero = true;
        Operator operator;
        for(String one : infixList){
            //左括号 直接入栈
            if(one.equals(leftSmallBracket.getSymbol())){
                symbolDeque.addLast(leftSmallBracket);
                needAddZero = true;
                continue;
            }
            //右括号,从栈顶弹出,直到遇到一个左括号
            if(one.equals(rightSmallBracket.getSymbol())){
                while(symbolDeque.peekLast().getSymbol() != leftSmallBracket.getSymbol()){
                    postfixDeque.addLast(symbolDeque.pollLast().getSymbol());
                }
                symbolDeque.pollLast();
                needAddZero = false;
                continue;
            }
            //左括号 入栈
            if(one.equals(leftBigBracket.getSymbol())){
                symbolDeque.addLast(leftBigBracket);
                needAddZero = true;
                continue;
            }
            //右括号,从栈顶弹出,直到遇到一个右括号; 并在后面追加一个 | 符号
            if(one.equals(rightBigBracket.getSymbol())){
                while(symbolDeque.peekLast().getSymbol() != leftBigBracket.getSymbol()){
                    postfixDeque.addLast(symbolDeque.pollLast().getSymbol());
                }
                symbolDeque.pollLast();
                postfixDeque.addLast("|");
                needAddZero = false;
                continue;
            }
            operator = operatorMap.get(one);
            if(operator == null){ //是一个操作数
                postfixDeque.addLast(one);
                needAddZero = false;
                continue;
            }

            //注意 -x解析为0-x; 不支持嵌套, --x不会解析成 0-(0-x)
            //这个
            if(needAddZero && "-".equals(operator.getSymbol())){
                postfixDeque.addLast("0");
                symbolDeque.addLast(minusSign);
                needAddZero = false;
                continue;
            }

            needAddZero = true;
            //注意这里,括号的优先级为最低,括号不会被弹出
            while(!symbolDeque.isEmpty() && symbolDeque.peekLast().getPriority() >= operator.getPriority()){
                postfixDeque.addLast(symbolDeque.pollLast().getSymbol());
            }
            symbolDeque.addLast(operator);

        }
        //弹出symbolDeque所有元素到postfixDeque
        while(!symbolDeque.isEmpty()){
            postfixDeque.addLast(symbolDeque.pollLast().getSymbol());
        }
        return postfixDeque;
    }

    /**
     * 对后缀表达式做校验
     * 思路:用一个对operandNum判断;
     * 遍历表达式,遇到操作数则对operandNum++;
     * 遇到操作符,则对operandNum-=(numberOfOperands-1); 比如numberOfOperands=2时,是把2个数计算为1个数;
     * 除了初始化阶段,之后的任一时刻operandNum<1,则不可以正常计算;
     * 遍历完毕,若operandNum=1,则说明可以正常计算;
     * @param postfixDeque 表达式
     * @return 该表达式是否可以正常计算
     */
    public static boolean canCalculation(Deque<String> postfixDeque){
        return canCalculation(postfixDeque,EMPTY_VARIABLE_MAP);
    }

    /**
     * 对后缀表达式做校验
     * 思路:用一个对operandNum判断;
     * 遍历表达式,遇到操作数则对operandNum++;
     * 遇到操作符,则对operandNum-=(numberOfOperands-1); 比如numberOfOperands=2时,是把2个数计算为1个数;
     * 除了初始化阶段,之后的任一时刻operandNum<1,则不可以正常计算;
     * 遍历完毕,若operandNum=1,则说明可以正常计算;
     * @param postfixDeque 表达式
     * @param variableMap 局部表量表
     * @return 该表达式是否可以正常计算
     * @throws IllegalArgumentException 存在未知变量时 会抛出一个异常
     */
    public static boolean canCalculation(Deque<String> postfixDeque,Map<String,String> variableMap) throws IllegalArgumentException{
        int operandNum = 0;
        String one;
        Operator operator;
        while(!postfixDeque.isEmpty()){
            operator = operatorMap.get(one = postfixDeque.pollFirst());
            //操作数
            if(operator == null){
                if(parseOperand(one,variableMap) == null){
                    throw new IllegalArgumentException("unknown variable;"+one);
                }
                operandNum ++;
                continue;
            }
            if((operandNum -= (operator.getNumberOfOperands()-1))<1){
                return false;
            }
        }
        return operandNum == 1;
    }

    /**
     * 对后缀表达式做计算
     * 1.遍历后缀表达式,遇到操作数,直接放入deque
     * 2.遇到符号,从堆栈中弹出计算所需的数字个数,并把计算结果放入堆栈
     * 3.运算结束后,若deque的长度不为1,则说明该公式异常,抛出一个IllegalArgumentException
     * @param postfixDeque 后缀表达式
     */
    public static BigDecimal calculation(Deque<String> postfixDeque){
        return calculation(postfixDeque,EMPTY_VARIABLE_MAP);
    }

    /**
     * 对后缀表达式做计算
     * 1.遍历后缀表达式,遇到操作数,直接放入deque
     * 2.遇到符号,从堆栈中弹出计算所需的数字个数,并把计算结果放入堆栈
     * 3.运算结束后,若deque的长度不为1,则说明该公式异常,抛出一个IllegalArgumentException
     * @param postfixDeque 后缀表达式
     * @param variableMap 局部表量表
     */
    public static BigDecimal calculation(Deque<String> postfixDeque,Map<String,String> variableMap){
        Deque<BigDecimal> deque = new LinkedList<>();

        String one;
        Operator operator;
        BigDecimal[] bigDecimalArr;

        while(!postfixDeque.isEmpty()){
            one = postfixDeque.pollFirst();
            operator = operatorMap.get(one);
            //操作数
            if(operator == null){
                if((one = parseOperand(one,variableMap)) == null){
                    throw new IllegalArgumentException("unknown variable;"+one);
                }
                deque.addLast(new BigDecimal(one));
                continue;
            }
            bigDecimalArr = new BigDecimal[operator.getNumberOfOperands()];
            for(int idx = bigDecimalArr.length - 1; idx>=0; idx--){
                bigDecimalArr[idx] = deque.pollLast();
            }
            deque.add(operator.getCalculationCore().calculation(bigDecimalArr));
        }

        if(deque.size() != 1){
            throw new IllegalArgumentException("formula is error");
        }
        return deque.pollLast();
    }

    /**
     * 将一个中缀表达式分割,把符号和元素放入放开 依次放入一个集合中
     * 示例:
     *  1. aaa*(b-(c+d)) 转换为["aaa","*","(","b","-","(","c","+","d",")",")"];
     *  2. a>=b 转换为["a",">=","b"]
     * @return 操作数和运算符的集合;
     * @throws IllegalArgumentException infix非法
     */
    public static List<String> split(String infix){
        //加强版trim
        infix = infix.replaceAll(" ","");
        List<String> returnList = new ArrayList<>();
        char[] charArr = infix.toCharArray();
        StringBuilder operandSb = new StringBuilder();
        String operatorSymbol;
        for(int i=0; i<charArr.length;){
            //操作数
            if(isOperand(charArr[i])){
                operandSb.append(charArr[i]);
                if(i == charArr.length-1){
                    returnList.add(operandSb.toString());
                }
                i++;
                continue;
            }

            //非操作数时 前面的操作数放到list中并重置
            if(operandSb.length() != 0){
                returnList.add(operandSb.toString());
                operandSb = new StringBuilder();
            }
            //括弧
            if(isBrackets(charArr[i])){
                returnList.add(new String(new char[]{charArr[i]}));
                i++;
                continue;
            }
            //操作符
            operatorSymbol = getOperatorSymbol(charArr,i);
            i+=operatorSymbol.length();
            returnList.add(operatorSymbol);
        }

        return returnList;
    }

    /**
     * 从charArr第idx个元素开始,找到开头的符号;
     * 如getOperatorSymbol(["a>=b"],1)返回值为">=";
     * 非一个已知符号时,会抛出一个IllegalArgumentException;
     * @param charArr 字符数组
     * @param idx 从这个下标开始比较符号
     * @return 符号
     */
    private static String getOperatorSymbol(char[] charArr,int idx){
        char[] symbolCharArr;
        for (Operator operator : operatorList){
            symbolCharArr = operator.getSymbolCharArr();

            if(idx + symbolCharArr.length > charArr.length){ //校验长度,防止越界;
                continue;
            }

            for (int i = 0; i < symbolCharArr.length; i++) {
                if(symbolCharArr[i] != charArr[idx+i]){ //不一样 则不是这个操作符
                    break;
                }
                if(i == symbolCharArr.length - 1) { //完全一样,则是当前操作符
                    return operator.getSymbol();
                }
            }

        }

        throw new IllegalArgumentException("unknown symbol:"+charArr[idx]);
    }

    /**
     * 字符是括弧
     * @param ch 字符
     * @return 是否括弧
     */
    private static boolean isBrackets(char ch){
        return ch == '(' || ch == ')' || ch == '[' || ch == ']';
    }

    /**
     * 解析一个操作数
     * 数字则直接返回; 变量(非数字)则从变量表取出
     * @param operand 操作数
     * @param variableMap 局部变量表
     * @return 操作数(数字)
     */
    private static String parseOperand(String operand,Map<String,String> variableMap){
        //数字
        if(Pattern.matches("^\\d+(\\.\\d+)?$",operand)){
            return operand;
        }
        return variableMap.get(operand);
    }

    /**
     * 判断ch是否为操作数的组成字符(数字或变量)
     * 组成: 0~9 A~Z a~z .
     * @return 是否为操作数的组成字符
     */
    private static boolean isOperand(Character ch){
        int ascii = (int)ch;
        //0~9
        if(ascii >= 0x30 && ascii <= 0x39){
            return true;
        }
        //A~Z
        if(ascii >= 0x41 && ascii <= 0x5A){
            return true;
        }
        //a~z
        if(ascii >= 0x61 && ascii <= 0x7A){
            return true;
        }
        //.
        return ascii == 0x2E;
    }
}