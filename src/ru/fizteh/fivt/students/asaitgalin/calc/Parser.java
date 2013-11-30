package ru.fizteh.fivt.students.asaitgalin.calc;

public class Parser {
    private Lexer scanner;
    private Token look;
    private boolean endOfStream;

    public Parser(Lexer lexer) throws IllegalExpressionException {
        scanner = lexer;
        move();
    }

    public void move() throws IllegalExpressionException {
        look = scanner.getNext();
        endOfStream = look == null;
    }

    int safeAdd(int a, int b) throws ArithmeticException {
        if (a > 0 && b > 0) {
            if (a > (Integer.MAX_VALUE - b)) {
                throw new ArithmeticException("Integer overflow error");
            }
        } else if (a < 0 && b < 0) {
            if (a < Integer.MIN_VALUE - b) {
                throw new ArithmeticException("Integer overflow error");
            }
        }
        return a + b;
    }

    int safeSubtract(int a, int b) throws ArithmeticException {
        if ((a > 0 && b < 0) || (a < 0 && b > 0)) {
            if (Math.abs(a) > Math.abs(Integer.MAX_VALUE - b)) {
                throw  new ArithmeticException("Integer overflow");
            }
        }
        return a - b;
    }

    int safeMultiply(int a, int b) throws ArithmeticException {
        if (a == 0) {
            return 0;
        }
        if (Math.abs(b) > (Integer.MAX_VALUE / Math.abs(a))) {
            throw new ArithmeticException("Integer overflow error");
        }
        return a * b;
    }

    public int parseExpr() throws IllegalExpressionException {
        int x = parseTerm();
        Operator op = (Operator) look;
        while (!endOfStream &&
                (op.opType == Operator.OperatorType.ADDITION || op.opType == Operator.OperatorType.SUBTRACTION)) {
            if (op.opType == Operator.OperatorType.ADDITION) {
                move();
                x = safeAdd(x, parseTerm());
            } else {
                move();
                x = safeSubtract(x, parseTerm());
            }
            op = (Operator) look;
        }
        return x;
    }

    public int parseTerm() throws IllegalExpressionException {
        int x = parseUnary();
        Operator op = (Operator) look;
        while (!endOfStream &&
                (op.opType == Operator.OperatorType.MULTIPLICATION || op.opType == Operator.OperatorType.DIVISION)) {
            if (op.opType == Operator.OperatorType.MULTIPLICATION) {
                move();
                x = safeMultiply(x, parseUnary());
            } else {
                move();
                int y = parseUnary();
                if (y == 0) {
                    throw new ArithmeticException("Division by zero");
                }
                x = x / y;
            }
            op = (Operator) look;
        }
        return x;
    }

    public int parseUnary() throws IllegalExpressionException {
        if (endOfStream) {
            throw new IllegalExpressionException();
        }
        if (look.type == Token.TokenType.OPERATOR) {
            if (((Operator)look).opType == Operator.OperatorType.SUBTRACTION) {
                move();
                return -parseFactor();
            }
        }
        return parseFactor();
    }

    public int parseFactor() throws IllegalExpressionException {
        int ret = 0;
        if (look.type == Token.TokenType.NUMBER) {
            ret = ((Number)look).value;
            move();
        } else if (look.type == Token.TokenType.OPERATOR) {
            Operator op = (Operator)look;
            if (op.opType == Operator.OperatorType.LBRACKET) {
                move();
                ret = parseExpr();
                // Check matching bracket
                if (look != null && ((Operator)look).opType == Operator.OperatorType.RBRACKET) {
                    move();
                } else {
                    throw new IllegalExpressionException("Missing matching bracket");
                }
            } else {
                throw new IllegalExpressionException();
            }
        }
        return ret;
    }

}
