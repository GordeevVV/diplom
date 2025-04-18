package pack;

import java.util.*;
import java.util.stream.Collectors;


public class DFA {
    Set<Character> alphabet = new HashSet<>();
    Set<Set<Integer>> states = new HashSet<>();
    Set<Integer> startState;
    Set<Set<Integer>> acceptStates = new HashSet<>();
    Map<Set<Integer>, Map<Character, Set<Integer>>> transitionTable = new HashMap<>();

    public Set<Character> getAlphabet() {
        return alphabet;
    }

    public Set<Set<Integer>> getStates() {
        return states;
    }

    public Set<Integer> getStartState() {
        return startState;
    }

    public Set<Set<Integer>> getAcceptStates() {
        return acceptStates;
    }

    public Map<Set<Integer>, Map<Character, Set<Integer>>> getTransitionTable() {
        return transitionTable;
    }

    @Override
    public String toString() {
        return "Start: " + startState + "\nAccept: " + acceptStates + "\nTransitions:\n" +
                transitionTable.entrySet().stream()
                        .map(e -> e.getKey() + " -> " + e.getValue())
                        .collect(Collectors.joining("\n"));
    }

    public boolean matches(String input) {
        Set<Integer> currentState = startState;
        for (int i = 0; i < input.length(); i++) {
            char symbol = input.charAt(i);

            // Проверка на недопустимый символ
            if (!alphabet.contains(symbol)) {
                return false;
            }

            // Получение переходов из текущего состояния
            Map<Character, Set<Integer>> stateTransitions = transitionTable.get(currentState);
            if (stateTransitions == null) {
                return false;
            }

            // Получение следующего состояния
            Set<Integer> nextState = stateTransitions.get(symbol);
            if (nextState == null) {
                return false;
            }

            currentState = nextState;
        }

        // Проверка, является ли конечное состояние принимающим
        return acceptStates.contains(currentState);
    }
}
