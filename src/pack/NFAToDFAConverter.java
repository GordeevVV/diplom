package pack;

import java.util.*;

public class NFAToDFAConverter {

    public static DFA convert(NFA nfa) {
        DFA dfa = new DFA();
        collectAlphabet(nfa, dfa);

        // ε-замыкание начального состояния НКА
        Set<Integer> startClosure = epsilonClosure(nfa, Collections.singleton(nfa.getStartState()));
        dfa.startState = startClosure;

        // Очередь для обработки состояний ДКА
        Queue<Set<Integer>> queue = new LinkedList<>();
        queue.add(startClosure);
        dfa.states.add(startClosure);

        // Проверка начального состояния на допуск
        if (isAcceptState(startClosure, nfa)) {
            dfa.acceptStates.add(startClosure);
        }

        while (!queue.isEmpty()) {
            Set<Integer> currentState = queue.poll();

            for (Character symbol : dfa.getAlphabet()) {
                Set<Integer> nextState = epsilonClosure(nfa, move(nfa, currentState, symbol));

                if (nextState.isEmpty()) continue;

                if (!dfa.states.contains(nextState)) {
                    dfa.states.add(nextState);
                    queue.add(nextState);

                    if (isAcceptState(nextState, nfa)) {
                        dfa.acceptStates.add(nextState);
                    }
                }

                dfa.transitionTable
                        .computeIfAbsent(currentState, k -> new HashMap<>())
                        .put(symbol, nextState);
            }
        }

        return dfa;
    }

    // Сбор алфавита из НКА (исключая ε)
    private static void collectAlphabet(NFA nfa, DFA dfa) {
        for (Map<Character, Set<Integer>> transitions : nfa.getTransitions().values()) {
            for (Character symbol : transitions.keySet()) {
                if (symbol != null) {
                    dfa.getAlphabet().add(symbol);
                }
            }
        }
    }

    // Вычисление ε-замыкания
    private static Set<Integer> epsilonClosure(NFA nfa, Set<Integer> states) {
        Set<Integer> closure = new HashSet<>(states);
        Queue<Integer> queue = new LinkedList<>(states);

        while (!queue.isEmpty()) {
            Integer state = queue.poll();

            Set<Integer> epsilonTransitions = nfa.getTransitions()
                    .getOrDefault(state, Collections.emptyMap())
                    .get(null);

            if (epsilonTransitions != null) {
                for (Integer nextState : epsilonTransitions) {
                    if (!closure.contains(nextState)) {
                        closure.add(nextState);
                        queue.add(nextState);
                    }
                }
            }
        }

        return closure;
    }

    // Переход по символу
    private static Set<Integer> move(NFA nfa, Set<Integer> states, Character symbol) {
        Set<Integer> result = new HashSet<>();

        for (Integer state : states) {
            Set<Integer> transitions = nfa.getTransitions()
                    .getOrDefault(state, Collections.emptyMap())
                    .get(symbol);

            if (transitions != null) {
                result.addAll(transitions);
            }
        }

        return result;
    }

    // Проверка на допускающее состояние
    private static boolean isAcceptState(Set<Integer> dfaState, NFA nfa) {
        return dfaState.stream()
                .anyMatch(state -> nfa.getAcceptStates().contains(state));
    }
}
