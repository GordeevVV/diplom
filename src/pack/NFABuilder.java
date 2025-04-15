package pack;

import java.util.*;

public class NFABuilder {

    public static NFA createCharNFA(char c) {
        NFA nfa = new NFA();
        Integer start = nfa.createState();
        Integer end = nfa.createState();
        nfa.setStartState(start);
        nfa.addAcceptState(end);
        nfa.addTransition(start, c, end);
        return nfa;
    }

    public static NFA concat(NFA a, NFA b) {
        NFA result = new NFA();
        Map<Integer, Integer> stateMapA = copyStates(a, result);
        Map<Integer, Integer> stateMapB = copyStates(b, result);

        Integer newStart = stateMapA.get(a.getStartState());
        result.setStartState(newStart);

        for (Integer acceptA : a.getAcceptStates()) {
            result.addTransition(stateMapA.get(acceptA), null, stateMapB.get(b.getStartState()));
        }

        for (Integer acceptB : b.getAcceptStates()) {
            result.addAcceptState(stateMapB.get(acceptB));
        }

        copyTransitions(a, stateMapA, result);
        copyTransitions(b, stateMapB, result);

        return result;
    }

    public static NFA union(NFA a, NFA b) {
        NFA result = new NFA();
        Integer newStart = result.createState();
        Integer newEnd = result.createState();
        result.setStartState(newStart);
        result.addAcceptState(newEnd);

        Map<Integer, Integer> stateMapA = copyStates(a, result);
        Map<Integer, Integer> stateMapB = copyStates(b, result);

        result.addTransition(newStart, null, stateMapA.get(a.getStartState()));
        result.addTransition(newStart, null, stateMapB.get(b.getStartState()));

        for (Integer acceptA : a.getAcceptStates()) {
            result.addTransition(stateMapA.get(acceptA), null, newEnd);
        }
        for (Integer acceptB : b.getAcceptStates()) {
            result.addTransition(stateMapB.get(acceptB), null, newEnd);
        }

        copyTransitions(a, stateMapA, result);
        copyTransitions(b, stateMapB, result);

        return result;
    }

    public static NFA star(NFA a) {
        NFA result = new NFA();
        Integer newStart = result.createState();
        Integer newEnd = result.createState();
        result.setStartState(newStart);
        result.addAcceptState(newEnd);

        Map<Integer, Integer> stateMap = copyStates(a, result);
        Integer aStart = stateMap.get(a.getStartState());

        result.addTransition(newStart, null, aStart);
        result.addTransition(newStart, null, newEnd);

        for (Integer acceptA : a.getAcceptStates()) {
            result.addTransition(stateMap.get(acceptA), null, aStart);
            result.addTransition(stateMap.get(acceptA), null, newEnd);
        }

        copyTransitions(a, stateMap, result);

        return result;
    }

    public static NFA shuffle(NFA a, NFA b) {
        NFA result = new NFA();
        Map<Pair<Integer, Integer>, Integer> pairStateMap = new HashMap<>();

        Pair<Integer, Integer> startPair = new Pair<>(a.getStartState(), b.getStartState());
        Integer newStart = getOrCreatePairState(pairStateMap, startPair, result);
        result.setStartState(newStart);

        for (Integer stateA : a.getStates()) {
            for (Integer stateB : b.getStates()) {
                Pair<Integer, Integer> pair = new Pair<>(stateA, stateB);
                getOrCreatePairState(pairStateMap, pair, result);
            }
        }

        for (Map.Entry<Pair<Integer, Integer>, Integer> entry : pairStateMap.entrySet()) {
            Pair<Integer, Integer> pair = entry.getKey();
            Integer stateA = pair.first;
            Integer stateB = pair.second;
            Integer currentState = entry.getValue();

            addTransitions(a, stateA, stateB, currentState, pairStateMap, result, true);
            addTransitions(b, stateB, stateA, currentState, pairStateMap, result, false);
        }

        for (Pair<Integer, Integer> pair : pairStateMap.keySet()) {
            if (a.getAcceptStates().contains(pair.first) && b.getAcceptStates().contains(pair.second)) {
                result.addAcceptState(pairStateMap.get(pair));
            }
        }

        return result;
    }

    private static void addTransitions(NFA nfa, Integer mainState, Integer otherState, Integer currentState, Map<Pair<Integer, Integer>, Integer> pairStateMap, NFA result, boolean isMainA) {
        Map<Character, Set<Integer>> transitions = nfa.getTransitions().getOrDefault(mainState, Collections.emptyMap());
        for (Map.Entry<Character, Set<Integer>> entry : transitions.entrySet()) {
            Character symbol = entry.getKey();
            for (Integer nextMain : entry.getValue()) {
                Pair<Integer, Integer> nextPair = isMainA ? new Pair<>(nextMain, otherState) : new Pair<>(otherState, nextMain);
                Integer nextState = pairStateMap.get(nextPair);
                result.addTransition(currentState, symbol, nextState);
            }
        }
    }

    private static Map<Integer, Integer> copyStates(NFA source, NFA target) {
        Map<Integer, Integer> stateMap = new HashMap<>();
        for (Integer state : source.getStates()) {
            stateMap.put(state, target.createState());
        }
        return stateMap;
    }

    private static void copyTransitions(NFA source, Map<Integer, Integer> stateMap, NFA target) {
        for (Map.Entry<Integer, Map<Character, Set<Integer>>> entry : source.getTransitions().entrySet()) {
            Integer from = entry.getKey();
            for (Map.Entry<Character, Set<Integer>> transEntry : entry.getValue().entrySet()) {
                Character symbol = transEntry.getKey();
                for (Integer to : transEntry.getValue()) {
                    target.addTransition(stateMap.get(from), symbol, stateMap.get(to));
                }
            }
        }
    }

    private static Integer getOrCreatePairState(Map<Pair<Integer, Integer>, Integer> pairStateMap, Pair<Integer, Integer> pair, NFA nfa) {
        return pairStateMap.computeIfAbsent(pair, p -> nfa.createState());
    }

    private static class Pair<T, U> {
        final T first;
        final U second;

        Pair(T first, U second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pair)) return false;
            Pair<?, ?> pair = (Pair<?, ?>) o;
            return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
        }

        @Override
        public int hashCode() {
            return Objects.hash(first, second);
        }
    }
}
