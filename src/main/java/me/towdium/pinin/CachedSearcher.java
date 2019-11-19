package me.towdium.pinin;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CachedSearcher<T> extends SimpleSearcher<T> {
    IntSet all = new IntOpenHashSet();
    int amount;
    LinkedHashMap<String, IntSet> cache = new LinkedHashMap<String, IntSet>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() >= amount;
        }
    };

    public CachedSearcher(boolean suffix, PinIn context) {
        this(suffix, context, 128);
    }

    public CachedSearcher(boolean suffix, PinIn context, int amount) {
        super(suffix, context);
        this.amount = amount;
        context.listen(this, this::reset);
    }

    public void put(String name, T identifier) {
        reset();
        all.add(strs.size());
        super.put(name, identifier);
    }

    public List<T> search(String name) {
        return generate(name).stream().map(i -> objs.get(i)).collect(Collectors.toList());
    }

    private IntSet generate(String name) {
        IntSet ret;
        if (name.isEmpty()) return all;
        else if ((ret = cache.get(name)) == null) {
            ret = new IntRBTreeSet();
            IntSet is = generate(name.substring(0, name.length() - 1));
            acc.search(name);
            for (int i : is) if (acc.contains(strs.get(i), suffix)) ret.add(i);
        }
        cache.remove(name);
        cache.put(name, ret);
        return ret;
    }

    public void reset() {
        cache.clear();
    }
}