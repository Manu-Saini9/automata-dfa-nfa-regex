import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class DfaToRegexAlgebraic {

    // ---------- Regular Expression AST ----------

    static abstract class RegExp {

        static final class Empty extends RegExp {
            @Override
            public String toString() { return "∅"; }
            @Override
            public boolean equals(Object o) { return o instanceof Empty; }
            @Override
            public int hashCode() { return 1; }
        }

        static final class Epsilon extends RegExp {
            @Override
            public String toString() { return "ε"; }
            @Override
            public boolean equals(Object o) { return o instanceof Epsilon; }
            @Override
            public int hashCode() { return 2; }
        }

        static final class Atom extends RegExp {
            final String symbol;
            Atom(String s) { this.symbol = s; }
            @Override
            public String toString() { return symbol; }
            @Override
            public boolean equals(Object o) {
                if (!(o instanceof Atom)) return false;
                Atom other = (Atom)o;
                return Objects.equals(this.symbol, other.symbol);
            }
            @Override
            public int hashCode() { return Objects.hash(symbol); }
        }

        static final class Union extends RegExp {
            final RegExp left, right;
            Union(RegExp l, RegExp r) { this.left = l; this.right = r; }
            @Override
            public String toString() {
                // internal debug form; final printing uses toPrettyString(...)
                return "Union(" + left + "," + right + ")";
            }
            @Override
            public boolean equals(Object o) {
                if (!(o instanceof Union)) return false;
                Union other = (Union)o;
                return left.equals(other.left) && right.equals(other.right);
            }
            @Override
            public int hashCode() { return 31 * left.hashCode() + right.hashCode(); }
        }

        static final class Concat extends RegExp {
            final RegExp left, right;
            Concat(RegExp l, RegExp r) { this.left = l; this.right = r; }
            @Override
            public String toString() {
                // internal debug form; final printing uses toPrettyString(...)
                return "Concat(" + left + "," + right + ")";
            }
            @Override
            public boolean equals(Object o) {
                if (!(o instanceof Concat)) return false;
                Concat other = (Concat)o;
                return left.equals(other.left) && right.equals(other.right);
            }
            @Override
            public int hashCode() { return 37 * left.hashCode() + right.hashCode(); }
        }

        static final class Star extends RegExp {
            final RegExp inner;
            Star(RegExp in) { this.inner = in; }
            @Override
            public String toString() {
                // internal debug form; final printing uses toPrettyString(...)
                return "Star(" + inner + ")";
            }
            @Override
            public boolean equals(Object o) {
                if (!(o instanceof Star)) return false;
                Star other = (Star)o;
                return inner.equals(other.inner);
            }
            @Override
            public int hashCode() { return 41 * inner.hashCode(); }
        }

        private static final Empty   EMPTY   = new Empty();
        private static final Epsilon EPSILON = new Epsilon();

        static RegExp empty()   { return EMPTY; }
        static RegExp epsilon() { return EPSILON; }
        static RegExp atom(String s) { return new Atom(s); }

        boolean isEmpty()   { return this instanceof Empty; }
        boolean isEpsilon() { return this instanceof Epsilon; }

        static RegExp union(RegExp a, RegExp b) {
            if (a.isEmpty()) return b;
            if (b.isEmpty()) return a;
            if (a.equals(b)) return a;  // r + r = r
            return new Union(a, b);
        }

        static RegExp concat(RegExp a, RegExp b) {
            if (a.isEmpty() || b.isEmpty()) return empty(); // ∅r = r∅ = ∅
            if (a.isEpsilon()) return b;                    // εr = r
            if (b.isEpsilon()) return a;                    // rε = r
            return new Concat(a, b);
        }

        static RegExp star(RegExp r) {
            if (r.isEmpty() || r.isEpsilon()) return epsilon(); // ∅* = ε, ε* = ε
            if (r instanceof Star) return r;                    // (r*)* = r*
            // (ε + X)* → X*
            if (r instanceof Union) {
                Union u = (Union) r;
                if (u.left.isEpsilon() && !u.right.isEpsilon()) {
                    return star(u.right);
                }
                if (u.right.isEpsilon() && !u.left.isEpsilon()) {
                    return star(u.left);
                }
            }
            return new Star(r);
        }

        // ---------- Pretty-printer to avoid bracket explosion ----------

        // Public pretty-printer entry
        static String toPrettyString(RegExp r) {
            return toPrettyString(r, 0);
        }

        // precedence: 1 = union, 2 = concat, 3 = star/atom
        private static String toPrettyString(RegExp r, int parentPrec) {
            int prec;
            String s;

            if (r instanceof Union) {
                prec = 1;
                Union u = (Union) r;
                String left  = toPrettyString(u.left, prec);
                String right = toPrettyString(u.right, prec);
                s = left + "+" + right;
            } else if (r instanceof Concat) {
                prec = 2;
                Concat c = (Concat) r;
                String left  = toPrettyString(c.left, prec);
                String right = toPrettyString(c.right, prec);
                // Concatenation: just stick them together (no symbol)
                s = left + right;
            } else if (r instanceof Star) {
                prec = 3;
                Star st = (Star) r;
                String inner = toPrettyString(st.inner, prec);
                // If inner is union or concat, we need parentheses before *
                if (st.inner instanceof Union || st.inner instanceof Concat) {
                    s = "(" + inner + ")*";
                } else {
                    s = inner + "*";
                }
            } else if (r instanceof Atom) {
                prec = 3;
                s = ((Atom) r).symbol;
            } else if (r instanceof Epsilon) {
                prec = 3;
                s = "ε";
            } else if (r instanceof Empty) {
                prec = 3;
                s = "∅";
            } else {
                prec = 3;
                s = "?";
            }

            // Parenthesize if this operator has lower precedence than its parent
            if (prec < parentPrec) {
                return "(" + s + ")";
            } else {
                return s;
            }
        }
    }

    // ---------- Helper: read next non-empty, non-comment line ----------

    private static String nextNonEmpty(BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("#")) continue;
            return line;
        }
        return null;
    }

    // ---------- MAIN: Algebraic DFA → RE using Arden's theorem ----------

    public static void main(String[] args) {
        // TODO: change this path if your example_dfa.txt is elsewhere
        String filePath = "C:\\Users\\Manu Saini\\OneDrive - Brock University\\Desktop\\COSC3P99\\untitled\\dfa_ex_four.txt";
        System.out.println("Using DFA file: " + filePath);

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            // 1) Read n (states) and m (alphabet size)
            String line = nextNonEmpty(br);
            if (line == null) throw new RuntimeException("Unexpected EOF reading n, m");
            String[] parts = line.split("\\s+");
            int n = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);

            // 2) Read alphabet symbols
            line = nextNonEmpty(br);
            if (line == null) throw new RuntimeException("Unexpected EOF reading symbols");
            parts = line.split("\\s+");
            if (parts.length != m) throw new RuntimeException("Expected " + m + " symbols");
            String[] symbols = parts;

            // 3) Read start state
            line = nextNonEmpty(br);
            if (line == null) throw new RuntimeException("Unexpected EOF reading start state");
            int start = Integer.parseInt(line.trim());

            // 4) Read final states
            line = nextNonEmpty(br);
            if (line == null) throw new RuntimeException("Unexpected EOF reading final states");
            parts = line.split("\\s+");
            int finalCount = Integer.parseInt(parts[0]);
            if (parts.length - 1 != finalCount)
                throw new RuntimeException("Final states line must be: <count> f1 f2 ...");
            boolean[] isFinal = new boolean[n];
            for (int i = 0; i < finalCount; i++) {
                int f = Integer.parseInt(parts[i + 1]);
                isFinal[f] = true;
            }

            // 5) Read transitions
            int[][] transitions = new int[n][m];
            for (int s = 0; s < n; s++) {
                line = nextNonEmpty(br);
                if (line == null) throw new RuntimeException("Unexpected EOF reading transitions");
                parts = line.split("\\s+");
                if (parts.length != m)
                    throw new RuntimeException("Expected " + m + " transitions for state " + s);
                for (int a = 0; a < m; a++) {
                    transitions[s][a] = Integer.parseInt(parts[a]);
                }
            }

            // ---------- Build system: R_i = const[i] + sum_j coeff[i][j] * R_j ----------

            RegExp[] constTerm = new RegExp[n];
            RegExp[][] coeff   = new RegExp[n][n];

            for (int i = 0; i < n; i++) {
                constTerm[i] = RegExp.empty();
                Arrays.fill(coeff[i], RegExp.empty());
            }

            for (int i = 0; i < n; i++) {
                if (isFinal[i]) {
                    constTerm[i] = RegExp.epsilon(); // ε term
                }
                for (int a = 0; a < m; a++) {
                    int j = transitions[i][a];
                    RegExp sym = RegExp.atom(symbols[a]);
                    coeff[i][j] = RegExp.union(coeff[i][j], sym);
                }
            }

            // ---------- Solve system using Arden elimination ----------

            for (int k = 0; k < n; k++) {
                // Equation k: R_k = const_k + sum_j coeff[k][j] R_j
                RegExp Akk = coeff[k][k];

                if (!Akk.isEmpty()) {
                    // R_k = (Akk)* (const_k + sum_{j != k} coeff[k][j] R_j)
                    RegExp Astar = RegExp.star(Akk);

                    // Multiply const term and all other coefficients by A*
                    constTerm[k] = RegExp.concat(Astar, constTerm[k]);
                    for (int j = 0; j < n; j++) {
                        if (j == k) continue;
                        if (!coeff[k][j].isEmpty()) {
                            coeff[k][j] = RegExp.concat(Astar, coeff[k][j]);
                        }
                    }
                    coeff[k][k] = RegExp.empty();
                }

                // Substitute R_k into all other equations
                for (int i = 0; i < n; i++) {
                    if (i == k) continue;
                    RegExp Dik = coeff[i][k];
                    if (Dik.isEmpty()) continue;

                    // Add Dik * const_k to const_i
                    constTerm[i] = RegExp.union(
                            constTerm[i],
                            RegExp.concat(Dik, constTerm[k])
                    );

                    // For j != k: add Dik * coeff[k][j] to coeff[i][j]
                    for (int j = 0; j < n; j++) {
                        if (j == k) continue;
                        if (!coeff[k][j].isEmpty()) {
                            RegExp toAdd = RegExp.concat(Dik, coeff[k][j]);
                            coeff[i][j] = RegExp.union(coeff[i][j], toAdd);
                        }
                    }

                    // eliminate R_k from equation i
                    coeff[i][k] = RegExp.empty();
                }
            }

            // After elimination, R_start should be constTerm[start]
            RegExp regex = constTerm[start];

            // ---- Generic simplification pass (works for ANY DFA) ----
            regex = RegexSimplifier.simplify(regex);

            System.out.println("Regular expression for L(M) via algebraic (Arden) method:");
            System.out.println("r = " + RegExp.toPrettyString(regex));

        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    // ---------- Generic Regex Simplifier (no hardcoding to one DFA) ----------

    static class RegexSimplifier {

        static RegExp simplify(RegExp r) {
            if (r instanceof RegExp.Empty ||
                    r instanceof RegExp.Epsilon ||
                    r instanceof RegExp.Atom) {
                return r;
            }

            if (r instanceof RegExp.Star) {
                RegExp.Star s = (RegExp.Star) r;
                RegExp inner = simplify(s.inner);
                return RegExp.star(inner); // reuse star rules
            }

            if (r instanceof RegExp.Concat) {
                RegExp.Concat c = (RegExp.Concat) r;
                RegExp left  = simplify(c.left);
                RegExp right = simplify(c.right);
                return RegExp.concat(left, right); // reuse concat rules
            }

            if (r instanceof RegExp.Union) {
                RegExp.Union u = (RegExp.Union) r;
                RegExp left  = simplify(u.left);
                RegExp right = simplify(u.right);
                return simplifyUnion(left, right);
            }

            return r;
        }

        /**
         * Union simplification + generic factoring patterns:
         *  - X + X Y  => X(ε + Y)
         *  - X Y + X Z => X(Y + Z)
         *  - Y X + Z X => (Y + Z)X
         */
        private static RegExp simplifyUnion(RegExp a, RegExp b) {
            // basic union rules (∅, duplicate)
            RegExp basic = RegExp.union(a, b);

            if (!(basic instanceof RegExp.Union)) {
                return basic;
            }

            RegExp.Union u = (RegExp.Union) basic;
            RegExp left  = u.left;
            RegExp right = u.right;

            // Pattern 1: X + X Y  =>  X(ε + Y)
            if (left instanceof RegExp.Concat && !(right instanceof RegExp.Concat)) {
                RegExp.Concat lc = (RegExp.Concat) left;
                RegExp X = lc.left;
                RegExp Y = lc.right;
                if (X.equals(right)) {
                    RegExp opt = RegExp.union(RegExp.epsilon(), Y);
                    return RegExp.concat(X, opt);
                }
            }
            if (right instanceof RegExp.Concat && !(left instanceof RegExp.Concat)) {
                RegExp.Concat rc = (RegExp.Concat) right;
                RegExp X = rc.left;
                RegExp Y = rc.right;
                if (X.equals(left)) {
                    RegExp opt = RegExp.union(RegExp.epsilon(), Y);
                    return RegExp.concat(X, opt);
                }
            }

            // Pattern 2: X Y + X Z  =>  X (Y + Z)
            if (left instanceof RegExp.Concat && right instanceof RegExp.Concat) {
                RegExp.Concat lc = (RegExp.Concat) left;
                RegExp.Concat rc = (RegExp.Concat) right;

                // common prefix
                if (lc.left.equals(rc.left)) {
                    RegExp X = lc.left;
                    RegExp Y = lc.right;
                    RegExp Z = rc.right;
                    RegExp inside = RegExp.union(Y, Z);
                    return RegExp.concat(X, inside);
                }

                // common suffix: Y X + Z X => (Y + Z) X
                if (lc.right.equals(rc.right)) {
                    RegExp Y = lc.left;
                    RegExp Z = rc.left;
                    RegExp X = lc.right;
                    RegExp inside = RegExp.union(Y, Z);
                    return RegExp.concat(inside, X);
                }
            }

            return u; // no extra factoring applies
        }
    }
}

