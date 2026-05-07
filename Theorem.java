import java.util.*;
import Regex.*;

public class Theorem {
    static class Transition {
        int from, to;
        String sym;
        Transition(int f, int t, String s) { from = f; to = t; sym = s; }
    }

    private static RegexNode eps()        { return new Literal(""); }
    private static RegexNode union(RegexNode a, RegexNode b){
        if(a==null) return b;
        if(b==null) return a;
        if(a.toString().equals(b.toString())) return a;
        return new Union(a,b);
    }
    private static RegexNode concat(RegexNode a, RegexNode b){
        if(a==null||b==null) return null;
        return new Concat(a,b);
    }
    private static RegexNode star(RegexNode a){
        if(a==null) return null;
        return new Star(a);
    }

    public static RegexNode convertDFAtoRegex(int n, int start,
                                              List<Integer> finals,
                                              List<Transition> trans) {
        RegexNode[][] R = new RegexNode[n][n];
        // R⁰ init
        for(int i=0;i<n;i++){
            for(int j=0;j<n;j++){
                R[i][j] = (i==j ? eps() : null);
            }
        }
        for(var t: trans){
            R[t.from][t.to] = union(R[t.from][t.to], new Literal(t.sym));
        }
        // eliminate all non-start, non-final
        for(int k=0;k<n;k++){
            if(k==start || finals.contains(k)) continue;
            RegexNode[][] N = new RegexNode[n][n];
            for(int i=0;i<n;i++){
                for(int j=0;j<n;j++){
                    if(i==k||j==k) continue;
                    RegexNode via = concat(concat(R[i][k], star(R[k][k])), R[k][j]);
                    N[i][j] = union(R[i][j], via);
                }
            }
            for(int i=0;i<n;i++)
                for(int j=0;j<n;j++)
                    if(i!=k&&j!=k)
                        R[i][j] = N[i][j];
        }
        // union of start→final
        RegexNode result = null;
        for(int f: finals) result = union(result, R[start][f]);
        return result;
    }

    public static void main(String[] args) {
        // —— hard-coded DFA from Theorem 2.4 ——
        int numStates = 3;
        int startState = 0;              // q0 is start
        List<Integer> finalStates = List.of(1, 2);  // q1, q2 are finals

        List<Transition> transitions = List.of(
                new Transition(0,1,"0"),  // q0—0→q1
                new Transition(0,2,"1"),  // q0—1→q2
                new Transition(1,0,"0"),  // q1—0→q0
                new Transition(1,2,"1"),  // q1—1→q2
                new Transition(2,1,"0"),  // q2—0→q1
                new Transition(2,1,"1")   // q2—1→q1
        );

        RegexNode raw = convertDFAtoRegex(numStates, startState, finalStates, transitions);
        RegexNode simp = raw.simplify();

        System.out.println("Raw:       " + raw);
        System.out.println("Simplified:" + simp);
    }
}
