package Regex;

public class Concat implements RegexNode {
    public RegexNode left, right;
    public Concat(RegexNode left, RegexNode right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public RegexNode simplify() {
        RegexNode l = left.simplify();
        RegexNode r = right.simplify();
        if (l instanceof Literal && ((Literal) l).value.equals("")) return r;
        if (r instanceof Literal && ((Literal) r).value.equals("")) return l;
        return new Concat(l, r);
    }

    @Override
    public String toString() {
        return left.toString() + right.toString();
    }
}
