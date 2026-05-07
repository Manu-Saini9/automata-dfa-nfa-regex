import java.util.*;
import Regex.*;

public class DfaToRegex {
    static class Transition {
        int from, to;
        String symbol;
        public Transition(int f, int t, String s) {
            from = f; to = t; symbol = s;
        }
    }

    public static RegexNode convert(
            int n,
            int start,
            Set<Integer> finals,
            List<Transition> transitions
    ) {
        // R[k][i][j]: using intermediate states {0..k-1}
        @SuppressWarnings("unchecked")
        RegexNode[][][] R = new RegexNode[n+1][n][n];

        // -- Base case k=0 --
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) R[0][i][j] = new Literal("");   // ε
                else         R[0][i][j] = null;            // ∅
            }
        }
        // add direct transitions
        for (Transition t : transitions) {
            R[0][t.from][t.to] = union(
                    R[0][t.from][t.to],
                    new Literal(t.symbol)
            );
        }

        // -- Induction for k = 1..n --
        for (int k = 1; k <= n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    RegexNode withoutK = R[k-1][i][j];
                    RegexNode viaK = concat(
                            R[k-1][i][k-1],
                            star(R[k-1][k-1][k-1]),
                            R[k-1][k-1][j]
                    );
                    R[k][i][j] = union(withoutK, viaK);
                }
            }
        }

        // -- Combine all paths from start to any final --
        RegexNode finalRegex = null;
        for (int f : finals) {
            finalRegex = union(finalRegex, R[n][start][f]);
        }
        return finalRegex == null
                ? new Literal("ϕ")  // no string accepted
                : finalRegex.simplify();
    }

    // Helpers: union, concat, star
    private static RegexNode union(RegexNode a, RegexNode b) {
        if (a == null) return b;
        if (b == null) return a;
        return new Union(a, b);
    }
    private static RegexNode concat(RegexNode a, RegexNode b, RegexNode c) {
        return b == null
                ? concat(a, c)
                : concat(concat(a,b), c);
    }
    private static RegexNode concat(RegexNode a, RegexNode b) {
        if (a == null || b == null) return null;
        return new Concat(a,b);
    }
    private static RegexNode star(RegexNode a) {
        if (a == null) return null;
        return new Star(a);
    }

    // Example driver:
    public static void main(String[] args) {
        // DFA: q0 --0--> q1; q1 --1--> q1  (start=0, final={1})
        List<Transition> trans = List.of(
                new Transition(0,1,"0"),
                new Transition(1,1,"1")
        );
        RegexNode regex = convert(
                2,             // number of states
                0,             // start state q0
                Set.of(1),     // final state q1
                trans
        );
        System.out.println("Regex: " + regex);
        // prints: Regex: 0(1)*
    }
}
