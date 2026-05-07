import java.util.*;
import java.io.*;

public class NFA {
    public int start;
    public int accept;
    public Map<Integer, List<Pair<String, Integer>>> transitions = new HashMap<>();

    public void addTransition(int from, String symbol, int to) {
        transitions.computeIfAbsent(from, k -> new ArrayList<>()).add(new Pair<>(symbol, to));
    }

    public void addAll(NFA other) {
        for (var entry : other.transitions.entrySet()) {
            transitions.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).addAll(entry.getValue());
        }
    }

    public static void print(NFA nfa) {
        System.out.println("Start state: " + nfa.start);
        System.out.println("Accept state: " + nfa.accept);
        System.out.println("Transitions:");
        for (var from : nfa.transitions.keySet()) {
            for (var edge : nfa.transitions.get(from)) {
                System.out.println("  " + from + " --" + edge.first + "--> " + edge.second);
            }
        }
    }

    /**public static void exportToJFLAP(NFA nfa, String filename) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        writer.write("<structure>\n");
        writer.write("  <type>fa</type>\n");
        writer.write("  <automaton>\n");

        Set<Integer> allStates = new HashSet<>();
        allStates.add(nfa.start);
        allStates.add(nfa.accept);
        for (int from : nfa.transitions.keySet()) {
            allStates.add(from);
            for (var pair : nfa.transitions.get(from)) {
                allStates.add(pair.second);
            }
        }

        int i = 0;
        for (int state : allStates) {
            writer.write("    <state id=\"" + state + "\" name=\"q" + state + "\">\n");
            writer.write("      <x>" + (100 + (i * 80)) + "</x><y>200</y>\n");
            if (state == nfa.start) writer.write("      <initial/>\n");
            if (state == nfa.accept) writer.write("      <final/>\n");
            writer.write("    </state>\n");
            i++;
        }

        for (int from : nfa.transitions.keySet()) {
            for (var pair : nfa.transitions.get(from)) {
                writer.write("    <transition>\n");
                writer.write("      <from>" + from + "</from>\n");
                writer.write("      <to>" + pair.second + "</to>\n");
                writer.write("      <read>" + (pair.first.equals("ε") ? "" : pair.first) + "</read>\n");
                writer.write("    </transition>\n");
            }
        }

        writer.write("  </automaton>\n</structure>\n");
        writer.close();
    }**/

    public static void exportToDot(NFA nfa, String filename) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        writer.write("digraph NFA {\n");
        writer.write("  rankdir=LR;\n");
        writer.write("  node [shape=circle];\n");

        // Invisible start node
        writer.write("  start [shape=point];\n");
        writer.write("  start -> q" + nfa.start + ";\n");

        for (int from : nfa.transitions.keySet()) {
            for (Pair<String, Integer> trans : nfa.transitions.get(from)) {
                String label = trans.first.equals("ε") ? "ε" : trans.first;
                writer.write("  q" + from + " -> q" + trans.second + " [label=\"" + label + "\"];\n");
            }
        }

        // Final state style
        writer.write("  q" + nfa.accept + " [shape=doublecircle];\n");
        writer.write("}\n");
        writer.close();
    }

}
