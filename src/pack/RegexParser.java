package pack;

import java.util.*;

class RegexParser {

    private static List<String> shuntingYard(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Stack<String> stack = new Stack<>();

        // Таблица приоритетов операторов
        Map<String, Integer> precedence = new HashMap<>();
        precedence.put("*", 4);     // Высший приоритет
        precedence.put(".", 3);     // Конкатенация
        precedence.put("||", 2);    // Shuffle
        precedence.put("|", 1);     // Дизъюнкция

        for (String token : tokens) {
            if (isLiteral(token)) { // Литералы (символы)
                output.add(token);
            } else if (token.equals("(")) {
                stack.push(token);
            } else if (token.equals(")")) {
                // Выталкиваем все операторы до открывающей скобки
                while (!stack.peek().equals("(")) {
                    output.add(stack.pop());
                }
                stack.pop(); // Удаляем "("
            } else { // Обработка операторов
                while (!stack.isEmpty() &&
                        !stack.peek().equals("(") &&
                        precedence.getOrDefault(stack.peek(), 0) >= precedence.get(token)) {
                    output.add(stack.pop());
                }
                stack.push(token);
            }
        }

        // Выталкиваем оставшиеся операторы
        while (!stack.isEmpty()) {
            output.add(stack.pop());
        }

        return output;
    }

    private static boolean isLiteral(String token) {
        // Проверяем, является ли токен символом (не оператором или скобкой)
        return !token.matches("[*|.()]") && !token.equals("||");
    }

    private static List<String> generateSteps(List<String> rpn) {
        List<String> steps = new ArrayList<>();
        Stack<String> stack = new Stack<>();

        for (String token : rpn) {
            if (token.matches("[a-zA-Z]+")) { // Поддержка многобуквенных символов
                steps.add("createCharNFA('" + token + "')");
                stack.push(token);
            } else if (isUnaryOperator(token)) {
                String operand = stack.pop();
                String step = applyUnaryOperator(token, operand);
                steps.add(step);
                stack.push("(" + step + ")");
            } else {
                String right = stack.pop();
                String left = stack.pop();
                String step = applyBinaryOperator(token, left, right);
                steps.add(step);
                stack.push("(" + step + ")");
            }
        }
        return steps;
    }

    private static boolean isUnaryOperator(String token) {
        return token.equals("*");
    }

    private static String applyUnaryOperator(String op, String operand) {
        return switch (op) {
            case "*" -> "star(" + operand + ")";
            default -> throw new IllegalArgumentException("Unknown unary operator: " + op);
        };
    }

    private static String applyBinaryOperator(String op, String left, String right) {
        return switch (op) {
            case "." -> "concat(" + left + ", " + right + ")";
            case "||" -> "shuffle(" + left + ", " + right + ")";
            case "|" -> "union(" + left + ", " + right + ")";
            default -> throw new IllegalArgumentException("Unknown operator: " + op);
        };
    }

    private static List<String> tokenize(String regex) {
        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        int i = 0;

        while (i < regex.length()) {
            char c = regex.charAt(i);

            if (Character.isLetterOrDigit(c)) {
                currentToken.append(c);
                i++;
            } else {
                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString());
                    currentToken.setLength(0);
                }

                if (c == '|' && i < regex.length()-1 && regex.charAt(i+1) == '|') {
                    tokens.add("||");
                    i += 2;
                } else if ("*().|".indexOf(c) != -1) {
                    tokens.add(String.valueOf(c));
                    i++;
                } else if (c == '\\') { // Экранирование
                    if (i+1 < regex.length()) {
                        tokens.add(String.valueOf(regex.charAt(i+1)));
                        i += 2;
                    }
                } else {
                    throw new IllegalArgumentException("Invalid character: " + c);
                }
            }
        }

        if (currentToken.length() > 0) {
            tokens.add(currentToken.toString());
        }

        return tokens;
    }

    public static NFA parseRegexToNFA(String regex) {
        List<String> tokens = tokenize(regex);
        List<String> rpn = shuntingYard(tokens);
        return buildNFAFromRPN(rpn);
    }

    private static NFA buildNFAFromRPN(List<String> rpn) {
        Stack<NFA> stack = new Stack<>();

        for (String token : rpn) {
            if (token.matches("[a-zA-Z]+")) {
                stack.push(NFABuilder.createCharNFA(token.charAt(0)));
            } else if (isUnaryOperator(token)) {
                NFA operand = stack.pop();
                stack.push(applyUnaryOperator(token, operand));
            } else {
                NFA right = stack.pop();
                NFA left = stack.pop();
                stack.push(applyBinaryOperator(token, left, right));
            }
        }

        return stack.pop();
    }

    private static NFA applyUnaryOperator(String op, NFA operand) {
        switch (op) {
            case "*": return NFABuilder.star(operand);
            default: throw new IllegalArgumentException("Unknown unary operator: " + op);
        }
    }

    private static NFA applyBinaryOperator(String op, NFA left, NFA right) {
        switch (op) {
            case ".": return NFABuilder.concat(left, right);
            case "||": return NFABuilder.shuffle(left, right);
            case "|": return NFABuilder.union(left, right);
            default: throw new IllegalArgumentException("Unknown operator: " + op);
        }
    }
}