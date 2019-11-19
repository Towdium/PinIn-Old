package me.towdium.pinin;

import it.unimi.dsi.fastutil.chars.*;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.towdium.pinin.elements.Phoneme;
import me.towdium.pinin.elements.Pinyin;
import me.towdium.pinin.utils.Accelerator;
import me.towdium.pinin.utils.Matcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

/**
 * Author: Towdium
 * Date: 21/04/19
 */
public class TreeSearcher<T> implements Searcher<T> {
    Node<T> root = new NDense<>();
    CharList chars = new CharArrayList();
    List<T> objects = new ObjectArrayList<>();
    List<NAcc<T>> naccs = new ArrayList<>();
    final Accelerator<CharList> acc;
    final PinIn context;
    final boolean suffix;
    static final int THRESHOLD = 256;

    private int charsEnd(int start) {
        for (int i = start; ; i++) {
            if (chars.getChar(i) == '\0') return i;
        }
    }

    private void refresh() {
        naccs.forEach(i -> i.reload(this));
    }

    private String charsStr(int start) {
        return charsStr(start, charsEnd(start));
    }

    private String charsStr(int start, int end) {
        return new String(chars.subList(start, end).toCharArray());
    }

    public TreeSearcher(boolean suffix, PinIn context) {
        this.suffix = suffix;
        this.context = context;
        acc = new Accelerator<CharList>(context) {
            @Override
            protected char get(CharList str, int offset) {
                return str.getChar(offset);
            }

            @Override
            protected boolean end(CharList str, int offset) {
                return str.getChar(offset) == '\0';
            }
        };
        context.listen(this, this::refresh);
    }

    public void put(String name, T identifier) {
        int pos = chars.size();
        for (char c : name.toCharArray()) {
            chars.add(c);
            context.genChar(c);
        }
        chars.add('\0');
        for (int i = 0; i < (suffix ? name.length() : 1); i++)
            root = root.put(this, pos + i, objects.size());
        objects.add(identifier);
    }

    public List<T> search(String s) {
        acc.search(s);
        IntSet ret = new IntRBTreeSet();
        root.get(this, ret, 0);
        return ret.stream().map(i -> objects.get(i))
                .collect(Collectors.toList());
    }

    public PinIn context() {
        return context;
    }

    interface Node<T> {
        void get(TreeSearcher<T> p, IntSet ret, int offset);

        void get(TreeSearcher<T> p, IntSet ret);

        Node<T> put(TreeSearcher<T> p, int name, int identifier);
    }

    public static class NSlice<T> implements Node<T> {
        Node<T> exit = new NMap<>();
        int start, end;

        public NSlice(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public void get(TreeSearcher<T> p, IntSet ret, int offset) {
            get(p, ret, offset, 0);
        }

        @Override
        public void get(TreeSearcher<T> p, IntSet ret) {
            exit.get(p, ret);
        }

        @Override
        public Node<T> put(TreeSearcher<T> p, int name, int identifier) {
            int length = end - start;
            int match = Matcher.strCmp(p.charsStr(start, end), p.charsStr(name), 0, 0, length);
            if (match >= length) exit = exit.put(p, name + length, identifier);
            else {
                cut(p, start + match);
                exit = exit.put(p, name + match, identifier);
            }
            return start == end ? exit : this;
        }

        private void cut(TreeSearcher<T> p, int offset) {
            NMap<T> insert = new NMap<>();
            if (offset + 1 == end) insert.put(p.chars.getChar(offset), exit);
            else {
                NSlice<T> half = new NSlice<>(offset + 1, end);
                half.exit = exit;
                insert.put(p.chars.getChar(offset), half);
            }
            exit = insert;
            end = offset;
        }

        private void get(TreeSearcher<T> p, IntSet ret, int offset, int start) {
            if (this.start + start == end)
                exit.get(p, ret, offset);
            else if (offset == p.acc.search().length()) exit.get(p, ret);
            else {
                char ch = p.chars.getChar(this.start + start);
                p.acc.get(p.context.genChar(ch), offset).foreach(i ->
                        get(p, ret, offset + i, start + 1));
            }
        }
    }

    public static class NDense<T> implements Node<T> {
        // offset, object, offset, object
        IntList data = new IntArrayList();

        @Override
        public void get(TreeSearcher<T> p, IntSet ret, int offset) {
            if (p.acc.search().length() == offset) get(p, ret);
            else {
                for (int i = 0; i < data.size() / 2; i++) {
                    int ch = data.getInt(i * 2);
                    int obj = data.getInt(i * 2 + 1);
                    if (p.chars.getChar(ch) == '\0') continue;
                    if (p.acc.check(offset, p.chars, ch)) ret.add(obj);
                }
            }
        }

        @Override
        public void get(TreeSearcher<T> p, IntSet ret) {
            for (int i = 0; i < data.size() / 2; i++)
                ret.add(data.getInt(i * 2 + 1));
        }

        @Override
        public Node<T> put(TreeSearcher<T> p, int name, int identifier) {
            if (data.size() >= THRESHOLD) {
                int pattern = data.getInt(0);
                int common = Integer.MAX_VALUE;
                for (int i = 0; i < data.size() / 2; i++) {
                    int offset = data.getInt(i * 2);
                    common = Math.min(common, Matcher.strCmp(p.chars, pattern, offset));
                    if (common == 0) break;
                }
                Node<T> ret = new NSlice<>(pattern, pattern + common);
                for (int j = 0; j < data.size() / 2; j++)
                    ret.put(p, data.getInt(j * 2), data.getInt(j * 2 + 1));
                ret.put(p, name, identifier);
                return ret;
            } else {
                data.add(name);
                data.add(identifier);
                return this;
            }
        }
    }

    public static class NMap<T> implements Node<T> {
        Char2ObjectMap<Node<T>> children;
        IntSet leaves = new IntArraySet(1);

        @Override
        public void get(TreeSearcher<T> p, IntSet ret, int offset) {
            if (p.acc.search().length() == offset) get(p, ret);
            else if (children != null) {
                children.forEach((c, n) -> p.acc.get(p.context.genChar(c), offset)
                        .foreach(i -> n.get(p, ret, offset + i)));
            }
        }

        @Override
        public void get(TreeSearcher<T> p, IntSet ret) {
            ret.addAll(leaves);
            if (children != null) children.forEach((c, n) -> n.get(p, ret));
        }

        @Override
        public NMap<T> put(TreeSearcher<T> p, int name, int identifier) {
            if (p.chars.getChar(name) == '\0') {
                if (leaves.size() >= THRESHOLD && leaves instanceof IntArraySet)
                    leaves = new IntOpenHashSet(leaves);
                leaves.add(identifier);
            } else {
                init();
                char ch = p.chars.getChar(name);
                Node<T> sub = children.get(ch);
                if (sub == null) put(ch, sub = new NDense<>());
                sub = sub.put(p, name + 1, identifier);
                children.put(ch, sub);
            }
            return !(this instanceof NAcc) && children != null && children.size() > 32 ?
                    new NAcc<>(p, this) : this;
        }

        private void put(char ch, Node<T> n) {
            init();
            if (children.size() >= THRESHOLD && children instanceof Char2ObjectArrayMap)
                children = new Char2ObjectOpenHashMap<>(children);
            children.put(ch, n);
        }

        private void init() {
            if (children == null) children = new Char2ObjectArrayMap<>();
        }
    }

    public static class NAcc<T> extends NMap<T> {
        Map<Phoneme, CharSet> index = new Object2ObjectArrayMap<>();

        private NAcc(TreeSearcher<T> p, NMap<T> n) {
            children = n.children;
            leaves = n.leaves;
            reload(p);
            p.naccs.add(this);
        }

        @Override
        public void get(TreeSearcher<T> p, IntSet ret, int offset) {
            if (p.acc.search().length() == offset) get(p, ret);
            else {
                Node<T> n = children.get(p.acc.search().charAt(offset));
                if (n != null) n.get(p, ret, offset + 1);
                index.forEach((k, v) -> {
                    if (!k.match(p.acc.search(), offset).isEmpty()) {
                        v.forEach((IntConsumer) i -> p.acc.get(p.context.genChar((char) i), offset)
                                .foreach(j -> children.get((char) i).get(p, ret, offset + j)));
                    }
                });
            }
        }

        @Override
        public NAcc<T> put(TreeSearcher<T> p, int name, int identifier) {
            super.put(p, name, identifier);
            index(p, p.chars.getChar(name));
            return this;
        }

        public void reload(TreeSearcher<T> p) {
            index.clear();
            children.keySet().forEach((IntConsumer) i -> index(p, (char) i));
        }

        private void index(TreeSearcher<T> p, char c) {
            if (!Matcher.isChinese(c)) return;
            for (Pinyin py : Pinyin.get(c, p.context)) {
                index.compute(py.phonemes()[0], (j, cs) -> {
                    if (cs == null) return new CharArraySet();
                    else if (cs instanceof CharArraySet && cs.size() >= THRESHOLD && !cs.contains(c))
                        return new CharOpenHashSet(cs);
                    else return cs;
                }).add(c);
            }
        }
    }
}