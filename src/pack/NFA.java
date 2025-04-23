package pack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NFA {
    private int stateCounter = 0;
    private Set<Integer> states = new HashSet<>();
    private Integer startState;
    private Set<Integer> acceptStates = new HashSet<>();
    private Map<Integer, Map<Character, Set<Integer>>> transitions = new HashMap<>();

    public Integer createState() {
        Integer state = stateCounter++;
        states.add(state);
        return state;
    }

    public void addTransition(Integer from, Character symbol, Integer to) {
        transitions.putIfAbsent(from, new HashMap<>());
        transitions.get(from).putIfAbsent(symbol == null ? null : symbol, new HashSet<>());
        transitions.get(from).get(symbol == null ? null : symbol).add(to);
    }

    public void setStartState(Integer state) {
        startState = state;
    }

    public void addAcceptState(Integer state) {
        acceptStates.add(state);
    }

    public Integer getStartState() {
        return startState;
    }

    public Set<Integer> getAcceptStates() {
        return acceptStates;
    }

    public Set<Integer> getStates() {
        return states;
    }

    public Map<Integer, Map<Character, Set<Integer>>> getTransitions() {
        return transitions;
    }

    @Override
    public String toString() {
        String res = "";
        res += "Start state: " + startState + "\n";
        res += "States: " + states.size() + "\n";
        res += "Transitions: " + transitions.size() + "\n";
        res += "Transitions:" + "\n";

        for (Integer from : transitions.keySet()) {
            Map<Character, Set<Integer>> transitions = getTransitions().get(from);
            for (Character symbol : transitions.keySet()) {
                String sym = symbol == null ? "ε" : symbol.toString();
                for (Integer to : transitions.get(symbol)) {
                    res += STR."""
                        \{from}--\{sym}-->\{to}
                        """;
                }
            }
        }
        return res;
    }
}