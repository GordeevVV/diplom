package pack;

public class NFATest {
    public static void main(String[] args) {
        String regex = "(a|b)*";
        //Преобразуем в НКА
        NFA resultNFA = RegexParser.parseRegexToNFA(regex);
        System.out.println(resultNFA.toString());

        // Преобразуем в ДКА
        DFA dfa = NFAToDFAConverter.convert(resultNFA);

        System.out.println("DFA:");
        System.out.println(dfa);

        String[] testCases = {"", "a", "ab", "aba", "ba", "b", "abc"};
        for (String test : testCases) {
            boolean accepted = dfa.matches(test);
            System.out.printf("Строка \"%s\" %s%n", test, accepted ? "принята" : "отклонена");
        }
    }
}
