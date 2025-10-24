import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Viewer {
    public static void main(String[] args) {
        String filePathC = "files/quicksort_c.txt";
        String filePatchJava = "files/quicksort_java.txt";

        //String filePathC = "files/sor_c.txt";
        //String filePatchJava = "files/sor_java.txt";

        Forest<CodeBox> c = new Forest<>();
        Forest<CodeBox> java = new Forest<>();

      makeForest(c, filePathC);
      makeForest(java, filePatchJava);

        System.out.println(c);
        System.out.println(java);
    }

    public static void makeForest(Forest<CodeBox> c, String fileName){
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;

            Node<CodeBox> parent =  c.addTree(new CodeBox());

            while ((line = br.readLine()) != null ) {
                if(!line.startsWith(" ") && !line.startsWith("=")){
                    //  line code

                    if(line.endsWith("}") ){
                        parent = parent.getParent();
                    }

                    String[] tokens = line.split(":");

                    // remove useless lines
                    if (tokens.length != 2) {
                        continue;
                    }

                    CodeBox codeBox = new CodeBox();
                    codeBox.amostras = Integer.parseInt(tokens[0]);
                    codeBox.code = new  String(tokens[1]);

                    Node newNode = parent.addChild(codeBox);


                    if(line.endsWith("{")){
                        parent = newNode;
                    }

                }


            }



        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

interface DiffAndStr<T> {
    String toFixed(int width);
    T diff(T other);
}

class Node<T extends DiffAndStr<T>> {
    private T obj;
    private Node<T> parent;
    private List<Node<T>> children = new ArrayList<>();

    public Node(T obj) {
        this.obj = obj;
    }

    public T getObj() {
        return obj;
    }

    public void setObj(T obj) {
        this.obj = obj;
    }

    public Node<T> getParent() {
        return parent;
    }

    public void setParent(Node<T> parent) {
        this.parent = parent;
    }

    public List<Node<T>> getChildren() {
        return children;
    }

    public Node<T> addChild(T obj) {
        Node<T> child = new Node<>(obj);
        child.parent = this;
        this.children.add(child);
        return child;
    }

    public Node<T> deepCopy() {
        Node<T> copy = new Node<>(obj);
        for (Node<T> c : children) {
            Node<T> cc = c.deepCopy();
            cc.parent = copy;
            copy.children.add(cc);
        }
        return copy;
    }
}

class IntBox implements DiffAndStr<IntBox> {
    private int value;

    public IntBox(int value) {
        this.value = value;
    }

    @Override
    public String toFixed(int width) {
        String s = Integer.toString(value);
        if (s.length() > width) return s.substring(0, width);
        return String.format("%" + width + "s", s);
    }

    @Override
    public IntBox diff(IntBox other) {
        return new IntBox(this.value - other.value);
    }

}

class Forest<T extends DiffAndStr<T>> {
    private final List<Node<T>> roots = new ArrayList<>();

    public Forest() { }

    public Node<T> addTree(T obj) {
        Node<T> root = new Node<>(obj);
        roots.add(root);
        return root;
    }

    public List<Node<T>> getRoots() { return roots; }

    public Forest<T> diff(Forest<T> other) {
        Forest<T> result = new Forest<>();
        int maxRoots = Math.max(this.roots.size(), other.roots.size());
        for (int i = 0; i < maxRoots; i++) {
            Node<T> a = i < this.roots.size() ? this.roots.get(i) : null;
            Node<T> b = i < other.roots.size() ? other.roots.get(i) : null;
            Node<T> node = diffNodes(a, b);
            if (node != null) result.roots.add(node);
        }
        return result;
    }

    private Node<T> diffNodes(Node<T> a, Node<T> b) {
        if (a != null && b != null) {
            T newObj = a.getObj().diff(b.getObj());
            Node<T> root = new Node<>(newObj);
            int maxChildren = Math.max(a.getChildren().size(), b.getChildren().size());
            for (int i = 0; i < maxChildren; i++) {
                Node<T> ca = i < a.getChildren().size() ? a.getChildren().get(i) : null;
                Node<T> cb = i < b.getChildren().size() ? b.getChildren().get(i) : null;
                Node<T> child = diffNodes(ca, cb);
                if (child != null) {
                    child.setParent(root);
                    root.getChildren().add(child);
                }
            }
            return root;
        } else if (a != null) {
            return a.deepCopy();
        } else if (b != null) {
            return b.deepCopy();
        }
        return null;
    }

    @Override
    public String toString() {
        int cellWidth = computeCellWidth();
        StringBuilder sb = new StringBuilder();
        for (int idx = 0; idx < roots.size(); idx++) {
            sb.append("Arbre #").append(idx).append("\n");
            renderNode(roots.get(idx), "", true, cellWidth, sb);
            if (idx < roots.size() - 1) sb.append("\n");
        }
        return sb.toString();
    }

    private void renderNode(Node<T> node, String prefix, boolean isLast, int cellWidth, StringBuilder sb) {
        String connector = isLast ? "└─" : "├─";
        sb.append(prefix).append(connector).append(formatCell(node.getObj(), cellWidth)).append("\n");

        String childPrefix = prefix + (isLast ? "  " : "│ ");
        List<Node<T>> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            renderNode(children.get(i), childPrefix, i == children.size() - 1, cellWidth, sb);
        }
    }

    private String formatCell(T obj, int cellWidth) {
        String text = obj.toFixed(cellWidth);
        if (text == null) return  "[]";
        if (text.length() < cellWidth)
            text = String.format("%-" + cellWidth + "s", text);
        else if (text.length() > cellWidth)
            text = text.substring(0, cellWidth);
        return "[" + text + "]";
    }

    private int computeCellWidth() {
        int max = 1; // au moins 1 pour éviter [] vides
        Deque<Node<T>> stack = new ArrayDeque<>(roots);
        while (!stack.isEmpty()) {
            Node<T> n = stack.pop();
            String s = (n.getObj() == null) ? "null" : n.getObj().toString();
            if (s != null) max = Math.max(max, s.length());
            for (Node<T> c : n.getChildren()) stack.push(c);
        }
        return 55;
    }
}

class CodeBox implements DiffAndStr<CodeBox>{

    public int amostras;
    public String code;
    public List<Integer> amostrasA = new ArrayList<>();
    public List<String> lineA = new ArrayList<>();
    public List<String>  codeA = new ArrayList<>();

    @Override
    public String toFixed(int width) {
        if(code == null) return "";
        return String.format("%0" + 5 + "d|%s", amostras, code);
    }

    @Override
    public CodeBox diff(CodeBox other) {
        return null;
    }

}

/*
    public static void main(String[] args) {
        Forest<IntBox> A = new Forest<>();
        Node<IntBox> a1 = A.addTree(new IntBox(10));
        a1.addChild(new IntBox(5));
        a1.addChild(new IntBox(3)).addChild(new IntBox(1));
        A.addTree(new IntBox(7));

        Forest<IntBox> B = new Forest<>();
        Node<IntBox> b1 = B.addTree(new IntBox(4));
        b1.addChild(new IntBox(2));
        B.addTree(new IntBox(100)).addChild(new IntBox(50));

        System.out.println("--- A ---");
        System.out.println(A);
        System.out.println("--- B ---");
        System.out.println(B);

        Forest<IntBox> D = A.diff(B);
        System.out.println("--- A vs B ---");
        System.out.println(D);
    }





                    else {
                        // assembly code
                        String[] tokens = line.split(":");
                        parent.getObj().amostrasA.add(Integer.parseInt(tokens[0].strip()));
                        parent.getObj().lineA.add(tokens[1].strip());
                        parent.getObj().codeA.add(tokens[2].strip());
                    }
 */