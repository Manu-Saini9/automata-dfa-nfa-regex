import Regex.*;

public class Main {
    public static void main(String[] args) {
        RegexNode regex = new Union(
                new Union(
                        new Union(
                                new Union(
                                        new Union(
                                                new Union(
                                                        new Union(
                                                                new Union(
                                                                        new Literal("0"),
                                                                        new Literal("1")
                                                                ),
                                                                new Literal("2")
                                                        ),
                                                        new Star(new Literal("0"))
                                                ),
                                                new Star(new Literal("1"))
                                        ),
                                        new Star(new Literal("2"))
                                ),
                                new Concat(new Literal("1"), new Literal("1")) // "11"
                        ),
                        new Concat(new Literal("2"), new Literal("2"))     // "22"
                ),
                new Concat(new Literal("0"), new Literal("0"))         // "00"
        );




        regex = regex.simplify();
        StateCounter.reset();
        NFA nfa = ThompsonBuilder.build(regex);

        NFA.print(nfa);

        try {
           // NFA.exportToJFLAP(nfa, "nfa_01star.jff");
            NFA.exportToDot(nfa, "nfa_0_1_1star.dot");
            System.out.println("Exported both .jff and .dot files.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

