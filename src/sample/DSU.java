package sample;

import java.util.Arrays;

public class DSU {

    int[] parents;
    int[] sizes;
    int[] ranks;

    DSU(int size) {
        this.parents = new int[size];
        this.sizes = new int[size];
        this.ranks = new int[size];

        Arrays.fill(sizes, 1);
        for (int i = 0; i < size; ++i) {
            parents[i] = i;
        }
    }

    int get(int v) {
        int parent = parents[v];
        if (parent == v) return v;
        return parents[v] = get(parent);
    }

    boolean union(int a, int b) {
        a = get(a);
        b = get(b);

        if (a == b) return false;

        if (ranks[a] < ranks[b]) {
            int tmp = a;
            a = b;
            b = tmp;
        }

        parents[b] = a;
        sizes[a] += sizes[b];
        if (ranks[a] == ranks[b]) ++ranks[a];

        return true;
    }

    int size(int v) {
        return sizes[get(v)];
    }
}
