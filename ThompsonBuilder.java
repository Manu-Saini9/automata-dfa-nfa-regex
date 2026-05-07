import Regex.*;

public class ThompsonBuilder {
    public static NFA build(RegexNode node) {
        if (node instanceof Literal) {
            Literal lit = (Literal) node;
            int s = StateCounter.newState();
            int f = StateCounter.newState();
            NFA nfa = new NFA();
            nfa.start = s;
            nfa.accept = f;
            if (!lit.value.equals("ϕ")) {
                nfa.addTransition(s, lit.value.equals("") ? "ε" : lit.value, f);
            }
            return nfa;
        }

        if (node instanceof Concat) {
            Concat c = (Concat) node;
            NFA nfa1 = build(c.left);
            NFA nfa2 = build(c.right);
            NFA result = new NFA();
            result.start = nfa1.start;
            result.accept = nfa2.accept;
            result.addAll(nfa1);
            result.addAll(nfa2);
            result.addTransition(nfa1.accept, "ε", nfa2.start);
            return result;
        }

        if (node instanceof Union) {
            Union u = (Union) node;
            NFA nfa1 = build(u.left);
            NFA nfa2 = build(u.right);
            int s = StateCounter.newState();
            int f = StateCounter.newState();
            NFA result = new NFA();
            result.start = s;
            result.accept = f;
            result.addAll(nfa1);
            result.addAll(nfa2);
            result.addTransition(s, "ε", nfa1.start);
            result.addTransition(s, "ε", nfa2.start);
            result.addTransition(nfa1.accept, "ε", f);
            result.addTransition(nfa2.accept, "ε", f);
            return result;
        }

        if (node instanceof Star) {
            Star s = (Star) node;
            NFA nfa1 = build(s.node);
            int start = StateCounter.newState();
            int accept = StateCounter.newState();
            NFA result = new NFA();
            result.start = start;
            result.accept = accept;
            result.addAll(nfa1);
            result.addTransition(start, "ε", nfa1.start);
            result.addTransition(start, "ε", accept);
            result.addTransition(nfa1.accept, "ε", nfa1.start);
            result.addTransition(nfa1.accept, "ε", accept);
            return result;
        }

        throw new IllegalArgumentException("Unknown RegexNode subclass");
    }
}