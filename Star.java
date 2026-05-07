package Regex;

public class Star implements RegexNode {
    public RegexNode node;
    public Star(RegexNode node) {
        this.node = node;
    }

    @Override
    public RegexNode simplify() {
        RegexNode simp = node.simplify();
        if (simp instanceof Literal && ((Literal) simp).value.equals("")) return new Literal("");
        if (simp instanceof Star) return simp;
        return new Star(simp);
    }

    @Override
    public String toString() {
        if (node instanceof Literal && ((Literal) node).value.length() == 1) {
            return ((Literal) node).value + "*";
        }
        return "(" + node.toString() + ")*";
    }
}

