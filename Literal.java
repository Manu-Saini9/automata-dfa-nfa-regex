package Regex;

public class Literal implements RegexNode {
    public String value;
    public Literal(String value) {
        this.value = value;
    }

    @Override
    public RegexNode simplify() {
        return this;
    }

    @Override
    public String toString() {
        return value.equals("") ? "ε" : value;
    }
}
