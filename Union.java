package Regex;

public class Union implements RegexNode {
    public RegexNode left, right;
    public Union(RegexNode left, RegexNode right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public RegexNode simplify() {
        RegexNode l = left.simplify();
        RegexNode r = right.simplify();
        if (l.toString().equals(r.toString())) return l;
        return new Union(l, r);
    }

    @Override
    public String toString() {
        return "(" + left.toString() + "+" + right.toString() + ")";
    }
}
