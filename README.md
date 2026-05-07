# Automata Theory — DFA/NFA to Regular Expression Converter

>A Java implementation of three fundamental algorithms in automata theory: converting Deterministic Finite Automata (DFA) to regular expressions using two distinct methods (Theorem 2.4 dynamic programming and Arden's Lemma algebraic elimination), and converting regular expressions to Non-deterministic Finite Automata (NFA) using Thompson's Construction.

---

## Table of Contents

- [Project Overview](#project-overview)
- [Background — Why These Algorithms Matter](#background--why-these-algorithms-matter)
- [Project Structure](#project-structure)
- [Module 1 — DFA → Regex via Theorem 2.4 (Dynamic Programming)](#module-1--dfa--regex-via-theorem-24-dynamic-programming)
- [Module 2 — DFA → Regex via Arden's Lemma (Algebraic Elimination)](#module-2--dfa--regex-via-ardens-lemma-algebraic-elimination)
- [Module 3 — Regex → NFA via Thompson's Construction](#module-3--regex--nfa-via-thompsons-construction)
- [DFA Input File Format](#dfa-input-file-format)
- [Sample DFA Files](#sample-dfa-files)
- [How to Run](#how-to-run)
- [Key Design Decisions](#key-design-decisions)

---

## Project Overview

This project implements the **three core conversion algorithms** that form the backbone of the Kleene theorem — the equivalence of regular expressions and finite automata:

```
Regular Expression  ←────────────────────────→  Finite Automaton
                     Thompson's Construction →
                     ← Theorem 2.4 (DP)
                     ← Arden's Lemma (Algebraic)
```

All three directions are implemented, giving a complete round-trip toolkit for any regular language.

---

## Background — Why These Algorithms Matter

Finite automata and regular expressions are two different ways of describing the same class of languages — the **regular languages**. Proving they are equivalent (Kleene's theorem) requires showing that any automaton can be converted to a regular expression and vice versa. The algorithms in this project make that conversion concrete and computational.

These algorithms are not purely academic. They appear directly in:

- **Compiler design** — lexer generators like `lex`/`flex` compile regular expressions into DFAs using exactly Thompson's Construction internally
- **Text processing** — regex engines in Python, Java, and Perl build automaton-like structures from patterns
- **Formal verification** — model checkers use automata-theoretic methods to verify system properties
- **Network security** — intrusion detection systems match packet patterns against DFAs compiled from rule sets

Understanding how to move between these representations is a foundational skill in computer science theory and systems programming.

---

## Project Structure

```
COSC3P99/
├── DFA/
│   └── src/
│       ├── DfaToRegexFromFile.java      # Theorem 2.4 — dynamic programming method
│       └── DfaToRegexAlgebraic.java     # Arden's Lemma — algebraic elimination method
│   ├── dfa_ex_one.txt                   # DFA: L = {(ab)^n | n ≥ 0}
│   ├── dfa_ex_two.txt                   # DFA: strings with even number of 0s
│   ├── dfa_ex_three.txt                 # DFA: strings over {0,1}
│   ├── dfa_ex_four.txt                  # DFA: complex multi-state example
│   ├── dfa_all.txt                      # DFA: accepts everything (Σ*)
│   ├── dfa_atleastonezero.txt           # DFA: at least one 0
│   ├── dfa_evenzeros.txt                # DFA: even number of zeros
│   └── example_dfa.txt                  # DFA from Theorem 2.4 lecture example
│
└── NFA/
    └── src/
        ├── Main.java                    # Entry point — builds NFA from regex tree
        ├── NFA.java                     # NFA data structure + DOT/JFLAP export
        ├── ThompsonBuilder.java         # Thompson's Construction algorithm
        ├── DfaToRegex.java              # DFA → Regex using dynamic programming
        ├── Theorem.java                 # State elimination method
        ├── StateCounter.java            # Global state ID generator
        ├── Pair.java                    # Generic pair utility
        └── Regex/
            ├── RegexNode.java           # Interface for regex AST nodes
            ├── Literal.java             # Terminal: a single character or ε
            ├── Union.java               # r₁ + r₂
            ├── Concat.java              # r₁r₂
            └── Star.java                # r*
```

---

## Module 1 — DFA → Regex via Theorem 2.4 (Dynamic Programming)

**File:** `DFA/src/DfaToRegexFromFile.java` and `NFA/src/DfaToRegex.java`

### Algorithm

This method is based on the classical recurrence from Theorem 2.4 in automata theory. Define:

> **R(k, i, j)** = the set of strings that take the DFA from state *i* to state *j* using only states **{0, 1, …, k−1}** as intermediates.

**Base case (k = 0):** Direct transitions only — no intermediate states allowed.
- R(0, i, i) = ε (the empty string, staying in place)
- R(0, i, j) = the set of symbols labelling edges from i to j (or ∅ if none)

**Inductive step:** Adding state k as a new allowed intermediate:

```
R(k, i, j) = R(k-1, i, j)  ∪  R(k-1, i, k) · R(k-1, k, k)* · R(k-1, k, j)
```

This reads: *"paths from i to j either avoid state k entirely, or they go from i to k, loop any number of times on k, then go from k to j."*

**Final result:** The language of the DFA is the union of R(n, start, f) over all final states f, where n is the total number of states.

### Regex AST

The implementation builds a proper **Abstract Syntax Tree (AST)** for the regular expression rather than working with strings. The AST nodes are:

| Node | Meaning | Example |
|---|---|---|
| `Literal("")` | Empty string ε | ε |
| `Literal("a")` | Symbol | `a` |
| `Union(l, r)` | Alternation | `a+b` |
| `Concat(l, r)` | Concatenation | `ab` |
| `Star(r)` | Kleene star | `a*` |
| `null` | Empty language ∅ | — |

Working with an AST allows algebraic simplification rules to be applied during construction (e.g., `ε · r → r`, `∅ · r → ∅`, `(r*)* → r*`), keeping the output readable.

---

## Module 2 — DFA → Regex via Arden's Lemma (Algebraic Elimination)

**File:** `DFA/src/DfaToRegexAlgebraic.java`

### Algorithm

This method treats the DFA as a **system of linear equations** over regular expressions and solves it using Arden's Lemma.

**Step 1 — Set up equations.** Each state *i* gives one equation:

```
R_i  =  B_i  +  Σ_j  A_{ij} · R_j
```

where:
- `B_i = ε` if state *i* is a final state, otherwise `∅`
- `A_{ij}` = the union of all symbols labelling transitions from *i* to *j*

**Step 2 — Apply Arden's Lemma.** For each equation with a self-loop (`R_i = A · R_i + B`), Arden's Lemma gives the unique solution:

```
R_i = A* · B
```

**Step 3 — Gaussian elimination.** Substitute the solved `R_i` into all other equations, reducing the system one equation at a time until only `R_start` remains.

### Simplifier

After elimination, a `RegexSimplifier` pass applies algebraic factoring rules to reduce expression size:

| Pattern | Simplified Form |
|---|---|
| `X + XY` | `X(ε + Y)` |
| `XY + XZ` | `X(Y + Z)` |
| `YX + ZX` | `(Y + Z)X` |
| `∅*` | `ε` |
| `(r*)*` | `r*` |
| `(ε + X)*` | `X*` |

### Comparison with Theorem 2.4

| Aspect | Theorem 2.4 (DP) | Arden's Lemma (Algebraic) |
|---|---|---|
| Approach | Bottom-up table filling | System of equations + substitution |
| Space | O(n³) regex objects | O(n²) with in-place elimination |
| Output style | Often more nested | Often more factored/readable |
| Intuition | Paths through intermediate states | Language equations per state |

Both produce equivalent regular expressions for the same DFA.

---

## Module 3 — Regex → NFA via Thompson's Construction

**Files:** `NFA/src/ThompsonBuilder.java`, `NFA/src/NFA.java`, `NFA/src/Main.java`

### Algorithm

Thompson's Construction converts a regular expression into an ε-NFA by structural induction on the regex AST. Each constructor produces a small NFA fragment with exactly **one start state** and **one accept state**, then fragments are composed using ε-transitions.

#### Base Cases

**Literal `a`:**
```
→ (s) --a--> ((f))
```

**Epsilon ε:**
```
→ (s) --ε--> ((f))
```

#### Inductive Cases

**Concatenation `r₁r₂`:**
```
→ [NFA₁] --ε--> [NFA₂]
```
Wire the accept state of NFA₁ to the start state of NFA₂ via ε.

**Union `r₁ + r₂`:**
```
        ε→ [NFA₁] →ε
→ (s) <               > ((f))
        ε→ [NFA₂] →ε
```
New start and accept states; ε-transitions branch into and merge out of both sub-NFAs.

**Kleene Star `r*`:**
```
→ (s) --ε--> [NFA₁] --ε--> ((f))
              ↑←←loop←←↓
(s) --ε--> ((f))    (skip for ε)
```
New start and accept; loop from NFA₁'s accept back to its start, plus direct ε skip for the empty string.

### Export Formats

The resulting NFA can be exported to:

- **`.dot` (Graphviz)** — render with `dot -Tpng nfa.dot -o nfa.png` to get a visual state diagram
- **`.jff` (JFLAP)** — open in the [JFLAP](https://www.jflap.org/) educational tool for interactive exploration

Sample `.dot` files for several example expressions are included in `NFA/`.

---

## DFA Input File Format

DFAs for modules 1 and 2 are specified in a simple plain-text format:

```
# Lines starting with # are comments and are ignored

# Line 1: number_of_states  alphabet_size
3 2

# Line 2: alphabet symbols (space-separated)
0 1

# Line 3: start state index (0-based)
0

# Line 4: final_state_count  f1  f2  ...
2 1 2

# Lines 5 through 5+n-1: transition table — one line per state
# Each line lists target state indices for each alphabet symbol in order
1 2
0 2
1 1
```

States are indexed `0` through `n−1`. Any number of final states are supported.

---

## Sample DFA Files

| File | Language Described |
|---|---|
| `example_dfa.txt` | Binary strings — Theorem 2.4 lecture DFA |
| `dfa_ex_one.txt` | Strings of the form `(ab)ⁿ` over `{a, b}` |
| `dfa_ex_two.txt` | Binary strings with an even number of 0s |
| `dfa_ex_three.txt` | Strings over `{0,1}` — 3-state DFA |
| `dfa_ex_four.txt` | Complex 4-state DFA example |
| `dfa_all.txt` | DFA that accepts all strings (Σ*) |
| `dfa_atleastonezero.txt` | Binary strings containing at least one `0` |
| `dfa_evenzeros.txt` | Binary strings with an even count of `0`s |

---

## How to Run

### Prerequisites
- Java 17+
- IntelliJ IDEA (recommended) or any Java IDE
- Graphviz (optional, for rendering `.dot` NFA diagrams)

### DFA → Regex (Modules 1 & 2)

1. Open `DFA/` as a project in IntelliJ
2. In `DfaToRegexFromFile.java` or `DfaToRegexAlgebraic.java`, update the `filePath` variable in `main()` to point to one of the provided `.txt` DFA files
3. Run the class — the regular expression is printed to stdout

### Regex → NFA (Module 3)

1. Open `NFA/` as a project in IntelliJ
2. Edit `Main.java` to construct the desired regex using `Literal`, `Union`, `Concat`, and `Star`
3. Run `Main.java` — the NFA transitions are printed and a `.dot` file is exported
4. Render the diagram: `dot -Tpng nfa_output.dot -o nfa_output.png`

---

## Key Design Decisions

**Immutable RegExp AST (DFA module).** The `RegExp` hierarchy uses value-based `equals`/`hashCode` throughout, enabling safe structural comparison during simplification without aliasing bugs.

**Algebraic simplification at construction time.** Rather than building an unsimplified expression and post-processing it, both DFA modules apply simplification rules inside the `union()`, `concat()`, and `star()` factory methods. This keeps intermediate expressions small throughout computation.

**Thompson's Construction with a global state counter.** The NFA module uses a shared `StateCounter` singleton to assign unique integer IDs across all NFA fragments during recursive construction, guaranteeing no state ID collisions when fragments are merged.

**Two independent DFA-to-regex implementations.** Implementing both Theorem 2.4 and Arden's Lemma for the same input DFAs allows direct comparison of their outputs — both produce equivalent, though differently structured, regular expressions for any given input.

**Graphviz DOT export.** Exporting to `.dot` makes the NFA human-verifiable — a rendered diagram immediately shows whether the construction is structurally correct without having to trace transitions by hand.
