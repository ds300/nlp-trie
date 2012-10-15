package nlp.trie;
import clojure.lang.IPersistentMap;

public class Trie {
  private final int     _freq;
  private final int     _tfreq;
  private final boolean _terminal;
  private final char[]  _tkeys;  // These ones arranged in complete binary tree
  private final Trie[]  _tnodes; // form for lookup.
  private final char[]  _keys;   // These ones in ascending sorted order for
  private final Trie[]  _nodes;  // traversal and merging etc.
  private final Object  _data;

  public Trie () {
    _freq     = 0;
    _tfreq    = 0;
    _terminal = false;
    _tkeys    = new char[0];
    _tnodes   = new Trie[0];
    _keys     = new char[0];
    _nodes    = new Trie[0];
    _data     = null;
  }

  public Trie (Trie t) {
    _freq     = t._freq;
    _tfreq    = t._tfreq;
    _terminal = t._terminal;
    _tkeys    = t._tkeys;
    _tnodes   = t._tnodes;
    _keys     = t._keys;
    _nodes    = t._nodes;
    _data     = t._data;
  }

  public Trie (int freq, int tfreq, boolean terminal, char[] keys, Trie[] nodes, Object data) {
    _freq = freq;
    _tfreq = tfreq;
    _terminal = terminal;
    _tkeys = keys.clone();
    _tnodes = nodes.clone();
    _keys = keys;
    _nodes = nodes;
    _data = data;

    toCompleteBinaryTree(_tkeys, _tnodes);
  }

  public Trie (int freq, int tfreq, boolean terminal, char[] keys, Trie[] nodes, char[] tkeys, Trie[] tnodes, Object data) {
    _freq = freq;
    _tfreq = tfreq;
    _terminal = terminal;
    _tkeys = tkeys;
    _tnodes = tnodes;
    _keys = keys;
    _nodes = nodes;
    _data = data;

    toCompleteBinaryTree(_tkeys, _tnodes);
  }

  public Trie (String word, int freq, Object data) {
    _freq = freq;
    if (word.length() == 0) {
      _terminal = true;
      _tfreq = freq;
      _data = data;
      _keys = new char[0];
      _nodes = new PersistentTrie[0];
      _tkeys = _keys;
      _tnodes = _nodes;
    } else {
      _terminal = 0;
      _data = null;
      _keys = new char[] {word.charAt(0)};
      _nodes = new PersistentTrie[] {new PersistentTrie(word.substring(1), freq, data)};
      _tkeys = _keys;
      _tnodes = _nodes;
    }
  }

  private static void toCompleteBinaryTree(char[] ks, PersistentTrie[] vs) {
    int height = (int) Math.floor(Math.log(ks.length + 1)/Math.log(2));
    int num_on_bottom = ks.length - (int) Math.pow(2, height) + 1;
    int hi = ks.length - 1;
    while (height > 0) {
      for (int lo=num_on_bottom*2-2; lo >= 0; lo-=2) {
        int i = lo;
        while (i < hi) {
          swap(ks, i, i+1);
          swap(vs, i, i+1); 
          i++;
        }
        hi--;
      }
      height--;
      num_on_bottom=(int) Math.pow(2, height);
    }
  }

  public int getSortedChildIndex (char c) {
    // binary search
    int lo = 0;
    int hi = _keys.length - 1;
    while (lo <= hi) {
      int i = lo + (hi - lo) / 2;
      if (c < _keys[i]) {
        hi = i - 1;
      } else if (c > _keys[i]) {
        lo = i + 1;
      } else {
        return i;
      }
    }
    return -1;
  }

  private int getChildIndex (char c) {
    int i = 0;
    while (i < _tkeys.length) {
      if      (_tkeys[i] > c) i = i * 2 + 1;
      else if (_tkeys[i] < c) i = i * 2 + 2;
      else { return i; }
    }
    return -1;
  }

  public PersistentTrie getChild (char c) {
    int i = getChildIndex(c);
    return i == -1 ? null : _tnodes[i];
  }

  public boolean hasChild (char c) {
    return getChildIndex(c) != -1;
  }

  public static void swap(char[] arr, int i, int j) {
    char tmp = arr[i];
    arr[i] = arr[j];
    arr[j] = tmp;
  }

  public static void swap(Trie[] arr, int i, int j) {
    Trie tmp = arr[i];
    arr[i] = arr[j];
    arr[j] = tmp;
  }

  private static int countUnique (final char[] a, final char[] b) {
    int i = 0;
    int j = 0;
    int count = 0;
    while (i < a.length && j < b.length) {
      count++;
      if (a[i] < b[j]) { i++; }
      else if (a[i] > b[j]) { j++; }
      else { i++; j++; }
    }
    return count + (a.length - i) + (b.length - j);
  }

  public Trie merge (Trie t) {

    final int freq = _freq + t._freq;
    final int tfreq = _tfreq + t._tfreq;
    final boolean terminal = _terminal || t._terminal;
    final Object data = t._data != null ? t._data : _data;

    if (_keys.length == 0) {
      return new Trie(count, tfreq, terminal, t._keys, t._nodes, t._tkeys, t._tnodes data);
    } else if (t._keys.length == 0) {
      return new Trie(count, tfreq, terminal, _keys, _nodes, _tkeys, _tnodes, data);
    } else {
      // clone and sort original data structures

      // count unique keys
      int unique = countUnique(_keys, t._keys);
      
      // create return arrays
      char[] rks = new char[unique];
      Trie[] rns = new Trie[unique];
      
      int i = 0;
      int j = 0;
      int k = 0;

      while (i < _keys.length && j < t._keys.length) {
        if (_keys[i] < t._keys[j]) {
          rks[k] = _keys[i];
          rns[k] = _nodes[i];
          i++;
        } else if (_keys[i] > t._keys[j]) {
          rks[k] = t._keys[j];
          rns[k] = t._nodes[j];
          j++;
        } else {
          rks[k] = _keys[i];
          rns[k] = merge(_nodes[i], t._nodes[j]);
          i++;
          j++;
        }
        k++;
      }
      while (i < _keys.length) {
        rks[k] = _keys[i];
        rns[k] = _nodes[i];
        i++;
        k++;
      }
      while (j < t._keys.length) {
        rks[k] = t._keys[j];
        rns[k] = t._nodes[j];
        j++;
        k++;
      }
      return new Trie(freq, tfreq, terminal, rks, rns, data);
    }
  }

  public Trie endNode (String s) {
    Trie node = this;
    for (int i = 0; i < word.length(); i++) {
      node = node.getChild(word.charAt(i));
      if (node == null) {
        return null;
      }
    }
    return node;
  }

  public boolean contains (String s) {
    ImmutableTrie node = this;
    for (int i = 0; i < word.length(); i++) {
      node = node.getChild(word.charAt(i));
      if (node == null) {
        return false;
      }
    }
    return node.terminal;
  }

  private Trie remove (String s, int freq) {
    if (s.length() == 0) {
      if (_keys.length == 0) {
        // this node is useless and can be removed
        return new Trie();
      } else {
        // there's stuff coming off from this node, so just make it not terminal
        return new Trie(_freq - freq, 0, false, _keys, _nodes, _tkeys, _tnodes, null);
      }
    } else {
      int x = getSortedChildIndex(s.charAt(0));
      int y = getChildIndex(s.charAt(0));
      Trie replacement = _nodes[x].remove(s.substring(1), freq);
      boolean include_replacement = replacement._terminal || replacement._keys.length > 0; // YES?? DUNNO MATE
      if (include_replacement) {
        Trie[] rns = _nodes.clone();
        Trie[] rtns = _tnodes.clone();
        rns[x] = replacement;
        rtns[y] = replacement;
        return new Trie(_freq - freq, _tfreq, _terminal, _keys, rns, _tkeys, rtns, _data);
      } else {
        // do interesting shit
      }

    }
  }

  @Override
  public Trie without(Object key) {
    Trie end = endNode((String) key);
    if (end == null || !end.terminal) {
      return this;
    } else {
      return remove((String) key, end._tfreq);
    }
  }


}