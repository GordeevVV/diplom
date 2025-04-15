import java.util.*;

class State {
    int id;
    Map<Character, List<State>> transitions = new HashMap<>();
    boolean isAccepting;

    State(int id) {
        this.id = id;
        this.isAccepting = false;
    }

    void addTransition(char symbol, State state) {
        transitions.computeIfAbsent(symbol, k -> new ArrayList<>()).add(state);
    }
}

class NFA {
    State startState;
    List<State> acceptStates = new ArrayList<>();
    int stateCount = 0;

    NFA() {
        startState = new State(stateCount++);
    }

    void addAcceptState(State state) {
        state.isAccepting = true;
        acceptStates.add(state);
    }
}

public class RegexToNFA {
    private static int stateCounter = 0;

    public static NFA regexToNFA(String regex) {
        // Удаляем пробелы и обрабатываем регулярное выражение
        regex = regex.replaceAll("\\s+", "");
        Stack<NFA> stack = new Stack<>();
        Stack<Character> operators = new Stack<>();

        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);

            if (Character.isLetter(c)) {
                // Создаем NFA для символа
                stack.push(createNFA(c));
            } else if (c == '(') {
                operators.push(c);
            } else if (c == ')') {
                while (!operators.isEmpty() && operators.peek() != '(') {
                    processOperator(stack, operators.pop());
                }
                operators.pop(); // Убираем '('
            } else if (c == '|' || c == '.' || c == '*' || (c == '|' && i + 1 < regex.length() && regex.charAt(i + 1) == '|')) {
                // Обрабатываем shuffle (||)
                if (c == '|' && i + 1 < regex.length() && regex.charAt(i + 1) == '|') {
                    i++; // Пропускаем следующий символ '|'
                    while (!operators.isEmpty() && precedence("||") <= precedence(String.valueOf(operators.peek()))) {
                        processOperator(stack, operators.pop());
                    }
                    operators.push('|'); // Используем '|' для обозначения shuffle
                } else {
                    while (!operators.isEmpty() && precedence(String.valueOf(c)) <= precedence(String.valueOf(operators.peek()))) {
                        processOperator(stack, operators.pop());
                    }
                    operators.push(c);
                }
            }
        }

        while (!operators.isEmpty()) {
            processOperator(stack, operators.pop());
        }

        if (stack.size() != 1) throw new IllegalStateException("Ошибка в регулярном выражении: остались элементы в стеке");

        return stack.pop();
    }

    private static void processOperator(Stack<NFA> stack, char operator) {
        if (operator == '*') {
            if (stack.isEmpty()) throw new IllegalArgumentException("Стек пуст для операции *");
            NFA nfa = stack.pop();
            stack.push(star(nfa));
        } else {
            if (stack.size() < 2) throw new IllegalArgumentException("Недостаточно элементов в стеке для операции " + operator);
            NFA nfa2 = stack.pop();
            NFA nfa1 = stack.pop();
            if (operator == '|') {
                stack.push(union(nfa1, nfa2));
            } else if (operator == '.') {
                stack.push(concat(nfa1, nfa2));
            }
        }
    }

    private static int precedence(String operator) {
        return switch (operator) {
            case "||" -> 4;
            case "*" -> 3;
            case "." -> 2;
            case "|" -> 1;
            default -> 0;
        };
    }

    private static NFA createNFA(char symbol) {
        NFA nfa = new NFA();
        State acceptState = new State(stateCounter++);
        nfa.startState.addTransition(symbol, acceptState);
        nfa.addAcceptState(acceptState);
        return nfa;
    }

    private static NFA union(NFA nfa1, NFA nfa2) {
        NFA newNFA = new NFA();
        newNFA.startState.addTransition('\0', nfa1.startState);
        newNFA.startState.addTransition('\0', nfa2.startState);
        for (State state : nfa1.acceptStates) {
            newNFA.addAcceptState(state);
        }
        for (State state : nfa2.acceptStates) {
            newNFA.addAcceptState(state);
        }
        return newNFA;
    }

    private static NFA concat(NFA nfa1, NFA nfa2) {
        NFA newNFA = new NFA();
        newNFA.startState = nfa1.startState; // Начальное состояние нового NFA - начальное состояние первого NFA
        for (State state : nfa1.acceptStates) {
            state.addTransition('\0', nfa2.startState); // ε-переход к началу второго NFA
        }
        for (State state : nfa2.acceptStates) {
            newNFA.addAcceptState(state); // Добавляем принимающие состояния второго NFA
        }
        return newNFA;
    }

    private static NFA star(NFA nfa) {
        NFA newNFA = new NFA();
        newNFA.startState.addTransition('\0', nfa.startState); // ε-переход к началу NFA
        newNFA.startState.addTransition('\0', newNFA.startState); // ε-переход для повторения

        for (State state : nfa.acceptStates) {
            state.addTransition('\0', nfa.startState); // ε-переход к началу
            newNFA.addAcceptState(state);
        }
        return newNFA;
    }

    private static NFA shuffle(NFA nfa1, NFA nfa2) {
        NFA newNFA = new NFA();
        newNFA.startState.addTransition('\0', nfa1.startState);
        newNFA.startState.addTransition('\0', nfa2.startState);

        for (State state1 : nfa1.acceptStates) {
            for (State state2 : nfa2.acceptStates) {
                // Создаем переходы между принимающими состояниями двух NFA
                state1.addTransition('\0', state2); // ε-переход от первого принимающего состояния ко второму
                state2.addTransition('\0', state1); // ε-переход от второго принимающего состояния к первому
            }
        }

        for (State state : nfa1.acceptStates) {
            newNFA.addAcceptState(state);
        }
        for (State state : nfa2.acceptStates) {
            newNFA.addAcceptState(state);
        }
        return newNFA;
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

    public static void main(String[] args) {
        String regex = "a|b.c*||d";
        try {
            NFA nfa = regexToNFA(regex);

            System.out.println("Начальное состояние: " + nfa.startState.id);
            System.out.println("Принимающие состояния: ");
            for (State acceptState : nfa.acceptStates) {
                System.out.println(" - " + acceptState.id);
            }

            // Дополнительная информация о переходах
            System.out.println("Переходы из начального состояния:");
            for (Map.Entry<Character, List<State>> entry : nfa.startState.transitions.entrySet()) {
                System.out.print("Символ '" + entry.getKey() + "' переходит в состояния: ");
                for (State state : entry.getValue()) {
                    System.out.print(state.id + " ");
                }
                System.out.println();
            }
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }
}
