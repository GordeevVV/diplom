package pack;

import java.util.Map;
import java.util.Set;

public class NFATest {
    public static void main(String[] args) {
        String regex = "a|b.c*||d";
        NFA resultNFA = RegexParser.parseRegexToNFA(regex);
        printNFAInfo(resultNFA);

        // Преобразуем в ДКА
        DFA dfa = NFAToDFAConverter.convert(resultNFA);

        // Выводим результат
        System.out.println("DFA:");
        System.out.println(dfa);
    }

    private static void printNFAInfo(NFA nfa) {
        System.out.println("Start state: " + nfa.getStartState());
        System.out.println("States: " + nfa.getStates().size());
        System.out.println("Transitions: " + nfa.getTransitions().size());
        System.out.println("\nTransitions:");

        for (Integer from : nfa.getTransitions().keySet()) {
            Map<Character, Set<Integer>> transitions = nfa.getTransitions().get(from);
            for (Character symbol : transitions.keySet()) {
                String sym = symbol == null ? "ε" : symbol.toString();
                for (Integer to : transitions.get(symbol)) {
                    System.out.printf("%s --%s--> %s%n", from, sym, to);
                }
            }
        }
    }
}
