import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class DfaToRegexFromFile {

    /**
     * Abstract syntax tree (AST) for regular expressions.
     */
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
            public int hashCode() {
                return Objects.hash(symbol);
            }
        }

        static final class Union extends RegExp {
            final RegExp left, right;
            Union(RegExp l, RegExp r) { this.left = l; this.right = r; }

            @Override
            public String toString() {
                return "(" + left.toString() + "+" + right.toString() + ")";
            }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof Union)) return false;
                Union other = (Union)o;
                return Objects.equals(this.left, other.left)
                        && Objects.equals(this.right, other.right);
            }

            @Override
            public int hashCode() {
                return 31 * left.hashCode() + right.hashCode();
            }
        }

        static final class Concat extends RegExp {
            final RegExp left, right;
            Concat(RegExp l, RegExp r) { this.left = l; this.right = r; }

            @Override
            public String toString() {
                return "(" + left.toString() + ")" + "(" + right.toString() + ")";
            }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof Concat)) return false;
                Concat other = (Concat)o;
                return Objects.equals(this.left, other.left)
                        && Objects.equals(this.right, other.right);
            }

            @Override
            public int hashCode() {
                return 37 * left.hashCode() + right.hashCode();
            }
        }

        static final class Star extends RegExp {
            final RegExp inner;
            Star(RegExp inner) { this.inner = inner; }

            @Override
            public String toString() {
                return "(" + inner.toString() + ")*";
            }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof Star)) return false;
                Star other = (Star)o;
                return Objects.equals(this.inner, other.inner);
            }

            @Override
            public int hashCode() {
                return 41 * inner.hashCode();
            }
        }

        // --- singletons for ∅ and ε ---

        private static final Empty EMPTY = new Empty();
        private static final Epsilon EPSILON = new Epsilon();

        static RegExp empty()   { return EMPTY; }
        static RegExp epsilon() { return EPSILON; }
        static RegExp atom(String s) { return new Atom(s); }

        boolean isEmpty()   { return this instanceof Empty; }
        boolean isEpsilon() { return this instanceof Epsilon; }

        // union with basic simplification
        static RegExp union(RegExp a, RegExp b) {
            if (a.isEmpty()) return b;
            if (b.isEmpty()) return a;
            if (a.equals(b)) return a; // r + r = r
            return new Union(a, b);
        }

        // concat with basic simplification
        static RegExp concat(RegExp a, RegExp b) {
            if (a.isEmpty() || b.isEmpty()) return empty(); // ∅r = r∅ = ∅
            if (a.isEpsilon()) return b;                    // εr = r
            if (b.isEpsilon()) return a;                    // rε = r
            return new Concat(a, b);
        }

        // star with basic simplification
        static RegExp star(RegExp r) {
            if (r.isEpsilon() || r.isEmpty()) {
                return epsilon(); // ε* = ε, ∅* = ε
            }
            if (r instanceof Star) {
                return r; // (r*)* = r*
            }
            // (ε + X)* -> X*
            if (r instanceof Union) {
                Union u = (Union) r;
                RegExp L = u.left;
                RegExp R = u.right;
                if (L.isEpsilon() && !R.isEpsilon()) {
                    return star(R);
                }
                if (R.isEpsilon() && !L.isEpsilon()) {
                    return star(L);
                }
            }
            return new Star(r);
        }
    }

    // --- util: read next non-empty, non-comment line ---

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

    // --- build R^0 from DFA ---

    static RegExp[][] buildBaseR0(int n, int[][] transitions, String[] symbols) {
        RegExp[][] R0 = new RegExp[n][n];

        for (int i = 0; i < n; i++) {
            Arrays.fill(R0[i], RegExp.empty());
        }

        // R_ii^0 includes ε
        for (int i = 0; i < n; i++) {
            R0[i][i] = RegExp.union(R0[i][i], RegExp.epsilon());
        }

        // add direct transitions
        for (int i = 0; i < n; i++) {
            for (int a = 0; a < symbols.length; a++) {
                int j = transitions[i][a];
                RegExp letter = RegExp.atom(symbols[a]);
                R0[i][j] = RegExp.union(R0[i][j], letter);
            }
        }

        return R0;
    }

    // --- apply Theorem 2.4 recursion to get R^n ---

    static RegExp[][] computeAllR(RegExp[][] R0) {
        int n = R0.length;

        RegExp[][] prev = R0;
        RegExp[][] curr = new RegExp[n][n];

        for (int k = 1; k <= n; k++) {
            int m = k - 1; // new allowed intermediate state index

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    RegExp term1 = prev[i][j];        // R_ij^(k-1)
                    RegExp rik   = prev[i][m];        // R_i,m^(k-1)
                    RegExp rkk   = prev[m][m];        // R_m,m^(k-1)
                    RegExp rkj   = prev[m][j];        // R_m,j^(k-1)

                    RegExp viaM = RegExp.concat(
                            RegExp.concat(rik, RegExp.star(rkk)),
                            rkj
                    );
                    curr[i][j] = RegExp.union(term1, viaM);
                }
            }

            prev = curr;
            if (k < n) {
                curr = new RegExp[n][n];
            }
        }

        return prev; // this is R^n
    }

    // --- MAIN: read DFA from file, compute regex, simplify ---

    public static void main(String[] args) {
        // Hard-coded path so you don't fight with IntelliJ args:
        String filePath = "C:\\Users\\Manu Saini\\OneDrive - Brock University\\Desktop\\COSC3P99\\untitled\\example_dfa.txt";

        System.out.println("Using DFA file: " + filePath);

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // 1) n and m
            String line = nextNonEmpty(br);
            if (line == null) throw new RuntimeException("Unexpected EOF reading n and m.");
            String[] parts = line.split("\\s+");
            int n = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);

            // 2) alphabet symbols
            line = nextNonEmpty(br);
            if (line == null) throw new RuntimeException("Unexpected EOF reading symbols.");
            parts = line.split("\\s+");
            if (parts.length != m) {
                throw new RuntimeException("Expected " + m + " symbols, got " + parts.length);
            }
            String[] symbols = parts;

            // 3) start state
            line = nextNonEmpty(br);
            if (line == null) throw new RuntimeException("Unexpected EOF reading start state.");
            int startState = Integer.parseInt(line.trim());

            // 4) final states
            line = nextNonEmpty(br);
            if (line == null) throw new RuntimeException("Unexpected EOF reading final states.");
            parts = line.split("\\s+");
            int finalCount = Integer.parseInt(parts[0]);
            if (parts.length - 1 != finalCount) {
                throw new RuntimeException("Final states line should be: <count> f1 f2 ...");
            }
            int[] finalStates = new int[finalCount];
            for (int i = 0; i < finalCount; i++) {
                finalStates[i] = Integer.parseInt(parts[i + 1]);
            }

            // 5) transitions: n lines, each with m integers
            int[][] transitions = new int[n][m];
            for (int s = 0; s < n; s++) {
                line = nextNonEmpty(br);
                if (line == null) {
                    throw new RuntimeException("Unexpected EOF reading transitions for state " + s);
                }
                parts = line.split("\\s+");
                if (parts.length != m) {
                    throw new RuntimeException("Expected " + m + " transitions for state " + s +
                            " but got " + parts.length);
                }
                for (int a = 0; a < m; a++) {
                    transitions[s][a] = Integer.parseInt(parts[a]);
                }
            }

            // --- Build R^0 and compute R^n ---
            RegExp[][] R0 = buildBaseR0(n, transitions, symbols);
            RegExp[][] Rn = computeAllR(R0);

            // Combine R_(start,f)^n for all final states
            RegExp result = RegExp.empty();
            for (int f : finalStates) {
                result = RegExp.union(result, Rn[startState][f]);
            }

            // Print R^n (debug / understanding)
            System.out.println("R^n matrix (n = " + n + "):");
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    System.out.println("R[" + i + "][" + j + "]^" + n + " = " + Rn[i][j]);
                }
                System.out.println();
            }

            // Simplify final regex with separate class
            RegExp simplified = RegexSimplifier.simplify(result);

            System.out.println("Raw regular expression for L(M):");
            System.out.println("r_raw = " + result);
            System.out.println();
            System.out.println("Simplified regular expression for L(M):");
            System.out.println("r_simplified = " + simplified);

        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Separate regex simplifier class.
     */
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

        private static RegExp simplifyUnion(RegExp a, RegExp b) {
            // basic union rules
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
