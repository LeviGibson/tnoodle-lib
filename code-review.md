# Code Review: `fto3phase`

Reviewer's mood: irritable. Coffee: cold. Patience: spent.

I was asked to review four production files and one test file in `fto3phase`. I did. I have opinions. Most of them are unkind. Below is what I found, grouped by file. Severity labels are mine; deal with it.

---

## Summary

This is a three-phase Thistlethwaite-style solver for the FTO. The math is clever and the design idea is sound. The execution, however, looks like it was written in a single caffeine-fueled weekend and then never audited. The codebase is littered with magic numbers, leaky static singletons, copy-pasted algorithms, dead variables, contradictory javadoc, public mutable arrays, no concurrency story, and a test suite that asserts everything except the things that matter.

If I had to merge-block this, I would. Below is why.

---

## `FtoCoord.java`

### Architecture / Concurrency

- **Static global state masquerading as a class.** Every movetable and every pruning table is a `static` field on `FtoCoord`. There is exactly one FTO solver per JVM, forever, and you have no way to reset it for tests. There is no `FtoCoord instance`, so injecting mocks or alternate configurations is impossible. This is a singleton implemented as a namespace, which is the worst of both worlds (C1, design).
- **The `volatile boolean initialized` + `synchronized init()` pattern is half a double-checked lock.** `getInitialized()` is not synchronized; it reads the `volatile` field, which is fine for visibility, but `init()` performs all the work *inside* the lock. So any thread that calls `init()` blocks every other thread that wants to *check* whether init has started but accidentally stumbles into the monitor. The pattern you actually want — and the one that does not require holding the lock for ~1 second of table generation — is double-checked locking with a local snapshot. Right now this is just "synchronized, badly" (C1, concurrency).
- **`Search.solution` is `synchronized` on the `Search` instance, `FtoCoord.init()` is `synchronized` on `FtoCoord.class`.** The two locks have no happens-before relationship. The `if (!FtoCoord.getInitialized()) FtoCoord.init();` inside `solution` is therefore a check-then-act race against `initialized` that *will* work because `initialized` is volatile, but only by accident. If you ever do anything fancier (e.g. partial-init visibility) this breaks silently (C1, concurrency).

### Magic numbers

The file is a graveyard of unexplained integer literals:

- `96800`, `35200`, `1680`, `11_289_600`, `11520`, `220`, `440`, `6720`, `81`, `160`, `8`, `32`, `4`, `2`.
- `packG1`: `edge * 220 + tris`. Why 220? It's `C(12,3)`. Not named, not declared, just sprinkled in. If the moved universe ever changes you get to play "find the typo across 5 files."
- `packTxE`: `edge * 1680 + tri`. Same complaint.
- `g1InitEdges`: `new int[440][16]`. The `440` is `2 * C(12,3)` and the `16` is the move count. Both are implicit. Compare to `g1InitTriangles`: `new int[220][16]`. Now I have to reverse-engineer whether `220` is the same `220` as in `packG1` (it is) or some other `220` (it isn't, but you made me check).
- Random `52063`, `161`, `100`, `11484`, `5405`, `4_194_993` are *initial capacity hints* for `IntArray` and they are oddly specific. Either they're computed from something and should be expressed as such, or they're arbitrary and should be round numbers (W1).

### `IntArray`

- `IntArray` doubles in size when it grows. Fine. But its constructor takes a capacity that the callers stuff with oddly-specific magic numbers (see above). Then on `clear()` it null-keeps the backing array — fine for reuse, but `frontier = next; next = tmp; next.clear()` is a 3-line buffer-swap trick that recurs *four times* in this file with no helper method. Refactor (W2).
- No bounds-checking on `data[i]` access from outside the class. `frontier.data[0]` is accessed bare inside `g1GeneratePrun` etc. and silently assumes the caller just `add`ed something (W2).

### BFS / pruning generators

Every pruning generator (`g1GeneratePrun`, `g2GenerateTriplePrun`, `g2GenerateTxEPrun`, `g3GenerateCornerPrun`) is the same algorithm copy-pasted with the inner `turnX` swapped out. This is *begging* for a generic `bfs(initial, size, turnFn, moveset)` helper. You wrote it four times instead. Have fun maintaining it (W1, DRY).

Specific problems inside:

- **`g2GenerateTripleFrontier` has a dead `depth` variable.** Look at it: `int depth = 0; … depth++;`. It's only used to compute `prun[nextIdx] = (byte) (depth + 1)` (line 193), but `prun` is local, is never returned, and is used solely as a "visited" flag where only `-1` vs. `not -1` matters. The exact value is irrelevant. So `depth` does nothing meaningful. Remove it (W1, dead code).
- **`g2GenerateTripleFrontier` walks `Search.G3_MOVESET` but indexes into `G2_TRIPLE_MOVES[idx]`.** `G2_TRIPLE_MOVES` is dimensioned `[35200][10]` (sized for `G2_MOVESET`). The G3 moveset IDs happen to fit in `[0,9]` so this works, but it works *by accident*. There is no comment explaining that this is intentional ("G3 moveset is a subset of G2 moveset IDs"). Next time someone reorders the move constants in `FtoCubie`, this silently explodes (C2, fragile coupling).
- **`g2GenerateTriplePrun` allocates `new IntArray(11484)` *inside* the while loop.** Every depth iteration creates a new `IntArray` (and thus a new `int[11484]`). Hoist it. Call me when the GC loves you (W2).
- **`byte` for pruning depth.** Bytes are signed in Java. Max representable pruning distance is 127. If any phase ever needs depth 128 the byte silently underflows to -128 and your BFS treats every newly-discovered state as unvisited, blowing up the table size. Document the assumption, and at minimum `assert` that no value overflows (C2, latent bug).
- **`g1GeneratePrun` is package-private** while `g2InitTriples` is `public` and `g2GenerateTriplePrun` is `private`. Pick a convention. Visibility is currently random (W2).
- **`System.out.println("Time to initialize: …")` inside a library.** No. Library code does not print to stdout. Return the duration, log it via SLF4J, or just delete it (C2, library hygiene).

### `FtoCoord` general

- `import java.util.function.IntPredicate;` is **unused**. Did nobody compile-check this? (W1)
- `FtoCubie solved = new FtoCubie()` is instantiated *six times during init* purely to read its solved packed indices. They're constants — compute them once into `static final int`s and forget about it (W2).
- `g2InitTriples` is `public`. Everything else is `private` or package-private. Why? (W2)

---

## `Search.java`

### The infinite loops

- `g1Iterate`, `g2Iterate`, `g3Iterate` all use `for (int depth = 0; depth < Integer.MAX_VALUE; depth++)`. This is `while (true)` written in a way that pretends to have a bound. When `depth` overflows `Integer.MAX_VALUE` (yes it can — likely never, but the loop *will* wrap to `Integer.MIN_VALUE`, which is `< Integer.MAX_VALUE`, and then continue running with negative depths), the recursion blows up with a StackOverflowError instead of a clean "no solution found." Use `while (true)` or a sane depth cap (C2).

### `g2Search` / `g1Search` / `g3Search`

- `g3Iterate` ends with `throw new RuntimeException();` — no message, no context, no type. If this ever fires the next developer will spend two hours in a debugger. The other two phases throw with messages; this one forgot (W2).
- `g2Search(depth, maxl, …)` has this guard:
  ```java
  if (maxl > 0 && !isValidMove(moves.getLast(), move)) continue;
  ```
  `moves.getLast()` will throw `NoSuchElementException` on an empty deque. The `maxl > 0` guard *implies* the deque is non-empty, but only by an invariant maintained implicitly by recursion. One refactor and this throws at runtime. Either assert or restructure (W2, brittle invariants).
- `g1Search` does `candidates.add(Arrays.copyOf(moves.stream().mapToInt(Integer::intValue).toArray(), maxl))`. You are: streaming the deque → boxing → unboxing → `toArray` → `Arrays.copyOf` to trim (where the trim length already equals the array length because `maxl == moves.size()`). This is two redundant copies. Initial capacity is not specified on the ArrayList<int[]> either (W2).
- `MIN_G1_CANDIDATES = 500` — undocumented threshold. Why 500? Where does it come from? Performance tuning? Quality? (W2, magic number)

### Public mutable arrays

- `G1_MOVESET`, `G2_MOVESET`, `G3_MOVESET` are `public static final int[]` — which means callers can **mutate the contents** of these "constants." `private static final int[] G1_MOVESET` would still be mutable, but at least the reference would be hidden. The correct exposure is a defensive copy or an immutable `List<Integer>`. Right now anyone (including the BFS code in `FtoCoord`) reading these arrays trusts that they will not change mid-search. If somebody tinkers with them, your search silently produces wrong answers (C2, security/correctness hygiene).

### `initInvalidMoveTable`

- The static initializer in `Search` instantiates `new FtoCubie()` twice per loop iteration in `initInvalidMoveTable()` to check commutativity of axes. This runs *exactly once* per JVM, fine. But the algorithm encodes the equivalence-class dedup rule ("if A and B commute, prune B-after-A") with zero comment explaining that's what it does. The next reader will have to derive this from the source of the puzzle. Comment it (W2).
- `invalidMoves` is a packed bitmask table — fine — but uses `int` bitwise ops on `byte`-sized data. Reasonable, but no `BitSet` and no comments. The name `invalidMoves` is also misleading: it's `invalidSuccessorAxes[la]` (W2).

### `validateSolution` / runtime exception

- `solution()` ends with:
  ```java
  if (!validateSolution(fullSolutionStr, randomState)){
      throw new RuntimeException("CRITICAL: …");
  }
  ```
  Using `RuntimeException` for an invariant violation inside your own solver is sloppy. Define an `IllegalStateException` subclass or at least `assert`. Right now callers can't reliably catch-and-retry (W2).

### `solution()` is `synchronized` on the `Search` instance

So two `Search` objects in the same JVM can run searches concurrently, but only if `FtoCoord` is initialized. Fine. But if they both call `init()` they'll both try to generate tables because `init()`'s `if (initialized) return;` check is inside the `FtoCoord` class lock, not the `Search` instance lock — so you're relying on `volatile` again. See the FtoCoord concurrency rant above (C1).

### Misc

- `public Search() {}` — explicit empty constructor. Useless (W2).
- `IntStream.concat(Arrays.stream(g1AndG2Solution), Arrays.stream(g3Solution)).toArray()` — allocates two `IntStream`s and an intermediate array. `System.arraycopy` would have been a quarter of the bytecode and zero boxing (W2).
- `buildStatesFromCandidates` returns `int[candidates.size()][6]`. The `6` is the number of phase-2 coordinates (edges, tris, four triples). It's a magic number with no constant and no comment (W1).

---

## `FtoCubie.java`

### `temps` is not thread-safe

```java
private FtoCubie temps = null; // lazily initialized when turn() is called
public void turn(int move){
    if (temps == null) temps = new FtoCubie();
    turnInto(move, temps);
    temps.copyInto(this);
}
```

So `FtoCubie` instances are **not safe to share across threads** because `turn()` mutates a member field. This isn't documented. Worse, `turnInto` has a `throws IllegalArgumentException("out can not be this")` guard, but there's no equivalent guard for "don't share FtoCubie instances across threads." Either make `turn()` allocate a stack-local buffer, or document the restriction. Right now users get silent data races (C1, thread safety).

### Javadoc lies

- `g2SetEdges` documents:
  ```
  Set the g1 edges based on a compact index generated by g2PackEdges
  ```
  "g1 edges." That's copy-paste from `g1SetEdges`. Wrong (W2, javadoc).
- `g2SetTripleCorners` says `// C(12,3) * 2^3` but the actual index range on the next comment block says `[0, C(6,3) * 2^3 - 1]`. The `C(12,3)` is just wrong (W2).
- `setAllCornerOrientation`'s `@param` description copy-pastes the corner *permutation* range `[0, 359]` while it should be `[0, 31]`. Inconsistent (W2).
- `triple` color documentation in `g2PackTriples(int color)` says `0 = U, 1 = F, 2 = BR, 3 = BL`. But `CORNER_TRIPLE_COLOR_LOOKUP` and `G2_EDGE_COLORS` use a different scheme (TOU=0, TOF=1, TOBR=2, TOBL=3; TOR=0, TOL=1, TOB=2, TOD=3). The naming is overloaded: the "color" parameter to `g2PackTriples` is a *triple* color (U/F/BR/BL), but elsewhere "color" is a triangle color (R/L/B/D). Pick a vocabulary.

### Rotating-a-cycle-with-swaps in `g3SetEdges`

```java
for (int iteration = 0; iteration < 3; iteration++) {
    if (edges[G3_EDGES[axis][0]] == G3_EDGES[axis][digit])
        break;
    for (int i = 0; i < 2; i++) {
        swap(edges, G3_EDGES[axis][0], G3_EDGES[axis][i + 1]);
    }
}
```

I had to read this five times. You're rotating three edges by repeatedly swapping `[0]↔[1]` then `[0]↔[2]`. That's a 3-cycle. There is `System.arraycopy`/rotate idiom, there is `Util.swap` of two pairs to build a 3-cycle, and there is — most importantly — a one-line form: `swap([0],[1]); swap([1],[2]);`. The double-loop here is unreadable and the inner loop with `i+1` is just clever enough to be wrong on the next edit (W1, readability).

### `g3PackEdges`

```java
for (int axis = 0; axis < 4; axis++) {
    for (int i = 0; i < 3; i++) {
        if (G3_EDGES[axis][i] == edges[G3_EDGES[axis][0]]){
            loc[axis] = i;
        }
    }
}
```

You're iterating `i=0,1,2` to find which `G3_EDGES[axis][i] == edges[G3_EDGES[axis][0]]`. `i=0` will *always* satisfy this condition. Because `edges[G3_EDGES[axis][0]] == G3_EDGES[axis][0]` when the edge is solved, but in general — wait, this isn't comparing "edge label", it's comparing the constant edge ID `G3_EDGES[axis][i]` to the *value* stored at position `G3_EDGES[axis][0]`. The value at position `G3_EDGES[axis][0]` is one of `{G3_EDGES[axis][0], G3_EDGES[axis][1], G3_EDGES[axis][2]}`, so this is a linear scan to find which of the three it is. Fine, but the trivial `i=0` case is silently accepted as a fallback and you never `break` after finding. If a state for some reason has two axis values that match (which shouldn't happen but you don't assert against it), `loc[axis]` silently keeps the last match. Add a `break` and validate (C2, latent).

### `equals` / `hashCode`

- `hashCode` XORs five `Arrays.hashCode` values. XOR is commutative and associative, which means two states with swapped arrays (e.g. cornerPerm and cornerOri swapped — impossible in practice but principle) collide. More importantly, XOR mixes poorly; concatenation-style hashCode would mix better. For five arrays you could do `Objects.hash(Arrays.hashCode(a), Arrays.hashCode(b), …)` (W2).
- `equals` is fine but doesn't handle subclasses — `getClass() != obj.getClass()` rules them out. If anyone ever subclasses `FtoCubie` (they shouldn't, but you've made the class `public`), equality breaks asymmetrically. Mark `final` (W2).

### Manual move tables

- 8 moves × (cp + co + ep + tpU + tpR) = 8 × 4-stuff hardcoded into a `static` block of arrays. This is *exactly* the kind of thing that should be test-fuzzed. There's a test that does `R R R == solved` and `R R' == solved`, which is great, but there is no test verifying that each individual `MoveEffect` produces the *biologically correct* FTO permutation — i.e., the cycles on the actual puzzle. If you got `U`'s tpU triangle cycle wrong, your tests only notice if the round-trip pack/unpack fails by accident. Add a test that asserts a known move produces a known configuration (e.g. `R` puts EUR where? assert it) (C1, test coverage).

### Other

- `FtoCubie(FtoCubie other)` — copy constructor with no `@warning` about the `temps` field not being deep-copied. Wait: `temps` is not copied, which is correct (it's lazily allocated), but it's worth noting that the copy's `temps` starts null. The comment "Lazily initialized when turn() is called" is on the field but the copy constructor's behavior is silent (W2).
- `applyMovesInto` is package-private. Why? `turnInto` is public. `copyInto` is public. The access policy in this class is drawn from a hat (W2).
- The `g3SetCorners` comment says `// (6!/2) * 2^5` but the index range is `(6!/2) * 2^5 = 11520`, so the `11520` is fine, but `11520` is hardcoded in the bounds check while the formula `6!/2` and `2^5` aren't linked to it. Extract (W2).
- Constants for `EDF`, `EDBR`, `EDBL`, etc. are declared `private static final`. They're only used inside the class, but the G3 packing logic uses raw indices like `9`, `10`, `11` via `EDF=9`. Fine. But `g1SetEdges` does `int[] perm = {EDF, EDBR, EDBL}` — that's barely even readable without knowing those are D-face edges. Add a comment (W2).
- The javadoc on `FtoCubie` is long, detailed, and well-written. Still: phase boundaries should be summarized in a class-level diagram, not prose paragraphs. Tables of "what pack function does what" would help.

---

## `Util.java`

### `isParity` is doing two jobs

- `isParity(int[] perm)` makes a defensive copy (fine), computes the parity by cycle decomposition (need to verify), and *also* throws `IllegalArgumentException` if `perm` is not a permutation of `0..n-1`. So the method is "isParity, but also isPermutation, and if not I'll throw a surprise exception." Split into `requirePermutation(perm)` + `parity(perm)`, or at least document the dual behavior in the javadoc. Right now the surprise-bomb behavior is only visible by reading the source (C2, contract).

### `packPerm` readability

```java
int lehmerDigit = e - Integer.bitCount(seen >> (size - e));
index += Util.FACTORIAL[(size-1)-i] * lehmerDigit;
```

This is correct Lehmer encoding, but it's the kind of bit-fiddling that gets people in trouble six months later. Add a one-line comment explaining the invariant (`seen` represents which values less-than-or-equal-to `e` we've already passed) (W2).

### `unpackPerm` parity fixup

```java
if (parity && Util.isParity(arr)){
    Util.swap(arr, size-2, size-1);
}
```

This is the move that "fixes" parity by swapping the last two — but it implicitly *assumes* that the even-parity representative has the last two in increasing order. That's an ordering convention that is documented nowhere. Also: `isParity` allocates a defensive copy on every call, just to find out two bits of information. In `g2SetEdges` this gets called inside a setter that's called from a hot loop during init's movetable generation. Profile, then optimize (W2).

### `packSubset` validates on every call

```java
for (int i = 0; i < idx.length-1; i++) {
    if (idx[i] >= idx[i+1]){
        throw new IllegalArgumentException("idx must be in ascending order");
    }
}
```

This runs *every single time* `packSubset` is called, including in the BFS that performs ~100k calls during pruning-table generation. The validation is helpful in dev; it's a tax in prod. Strip via `assert` or provide a non-validating `packSubsetUnchecked` (W2).

### Dead code

- `public static boolean contains(int[] array, int key)` — not called anywhere in the production code I can see. Delete it (W1).
- `private static synchronized int[][] computeChooseTable()` — `synchronized` is meaningless here. The static initializer that assigns `Cnk` runs exactly once, single-threaded, under the JVM's class-init lock. Remove the `synchronized` (W1).

### `pow`

- `public static int pow(int a, int b)` is a hand-rolled loop. There's `Math.pow` for double, but for ints you'd commonly precompute small powers into a table. Used in `FtoCubie.g3SetEdges` for `pow(3, 3-axis)` — a max of 81. Trivial, but the loop is needless allocation-free sugar. Either way, treat the values as constants or use `Math.pow` and cast (W2).

### `fromAlg`

- `Objects.equals(alg, "")` is a circumlocution for `alg.isEmpty()` (or, better, `alg.isBlank()` if you want to also reject whitespace). Minor (W2).
- `alg.trim().split("\\s+")` will return a non-empty array whose first element is `""` when the input is `" "` (after trim it's empty, so split yields `[""]`). Your guard above handles exactly the empty-string case but not the all-whitespace case, so `" ".trim().split("\\s+")` returns `[""]` and then `parseMove("")` throws `IllegalArgumentException("Unrecognized move: ")`. Validate (W2).

### `moveArrayToInvertedString`

- Fine. Could use `String.join(" ", …)` instead of manual `StringBuilder` + conditional space (W2).

---

## `FtoCubieTest.java`

### Wrong comment, wrong moveset

```java
int[] safeMoves = {0, 1, 2, 3, 4, 5, 6, 7}; // R,RP,L,LP,B,BP,D,DP,U,UP
```

Move IDs `0..7` are `R, RP, L, LP, B, BP, U, UP`. You wrote `R,RP,L,LP,B,BP,D,DP,U,UP` in the comment — that's nine moves, and the IDs say `D` is `8`, `DP` is `9`. So the comment is wrong **and** the moveset omits the D/DP moves that the actual `Search.G3_MOVESET` uses. Either the test is using `U/UP` when it should use `D/DP` (matching G3_MOVESET), or the comment is wrong. Pick one and align. As written, the test exercises states that are not reachable in phase 3, so the round-trip is testing the wrong thing (C2, test correctness).

### No test for `g3PackEdges`

There is `testPhaseThreeCorners` but no `testPhaseThreeEdges`. The most error-prone packing function in the codebase — `g3PackEdges` with its double-loop scan + `g3SetEdges` with its 3-cycle-via-swaps — has zero direct test coverage. Add one immediately (C1, coverage).

### `performanceTest` doesn't actually assert anything

It runs 100 solves, prints them to stdout, and computes an average. It does not assert that the solution length is below some bound, that the solve succeeded in a reasonable time, or that the solved-state trajectory is correct. Rename to `benchmarkSolves` and add an assertion (`assertTrue(sollen <= 30)` or whatever your real bound is). Otherwise this is just a `println`-wrapped benchmark (W2, test hygiene).

### Missing tests

- No test for `Util.packSubset` / `Util.unpackSubset` round-trip directly.
- No test for `Util.isParity` directly.
- No test for `Util.packPerm` / `unpackPerm` round-trip directly.
- No test for `Util.fromAlg` / `moveArrayToInvertedString` round-trip ("apply a scramble, decode its inverse, assert solved").
- No test for `Search.solution` actually inverting — only the `validateSolution` inside `solution()` does that, and *it throws RuntimeException on failure*. So your "correctness test" is "doesn't crash." Weak (C1, coverage).

### Copy-paste between tests

Every "index round trip" test has the same shape:
```java
Random r = new Random(42);
for (int i = 0; i < N; i++) {
    FtoCubie ftoCubie = new FtoCubie();
    for (int j = 0; j < 100; j++) ftoCubie.turn(r.nextInt(16));
    int idx = ftoCubie.<pack>();
    FtoCubie testCube = new FtoCubie();
    testCube.<set>(idx);
    assertEquals(idx, testCube.<pack>());
}
```

Factor this into a parameterized test (JUnit 5 has `@ParameterizedTest` + `@MethodSource`) or a helper method. As-is, you have ~10 copies of the same harness (W2).

### `assertEquals(new FtoCubie(), cube, …)` in `testAllMoves3ReturnToSolved`

`assertEquals(expected, actual, message)`. Yes, this works. But you're relying on `FtoCubie.equals` being correct — which is fine, but the test for `equals` correctness lives nowhere. Unit-test `equals`/`hashCode` directly so failures are diagnosable (W2).

---

## Cross-cutting problems

1. **No centralized place for "what is the size of each phase's state space."** Days of debugging will be spent reverse-engineering constants like `96800`, `35200`, `369600`, `11520`, `81`. Create a `PhaseSizes` class with named constants, or at least `static final` fields per phase.
2. **`Search.G1_MOVESET` etc. are public and mutable.** They are *the* most dangerous public surface in the package. Make defensive copies or expose immutable views.
3. **`FtoCoord` is a singleton-as-namespace.** It cannot be reset between tests, cannot be mocked, and cannot be parallelized per-instance. There is no unit-test seam.
4. **No package-private accessor for `FtoCoord`'s tables.** Tests can warm-start it via `init()`, but cannot inspect or assert against table contents.
5. **The breadth of javadoc on `FtoCubie` is admirable but inconsistent.** `Util` and `Search` are barely commented; `FtoCubie` is commented to the point of redundancy. Pick a bar and meet it everywhere.
6. **No `module-info.java` or package-level javadoc.** This is a library intended to be consumed by `tnoodle-lib`'s scramble infrastructure. Document the public surface (`Search`, `FtoCubie.randomCube`, `FtoCubie.turn`) at the package level.
7. **No `null`-checks on inputs** in `Util.fromAlg`, `Search.solution`, `FtoCubie.applyMovesInto` etc. A `null` move array throws `NullPointerException` deep inside an enhanced-for. Validate at the boundary (W2).

---

## Severity rollup

| Severity | Count |
| -------- | ----- |
| C1 (must fix before merge) | 9 |
| C2 (should fix before merge) | 11 |
| W1 (style / dead code) | 8 |
| W2 (clarity / refactor / hygiene) | ~28 |

---

## Things I will give you credit for

Since I'm being mean: the documentation on `FtoCubie`'s phase boundaries is genuinely good once you read it. The idea of using a single shared `FtoCubie` as a reusable buffer (`turnInto`) is the right call for memory pressure. The lazy `temps` allocation is reasonable for an instance that mostly uses `turnInto`. The BFS-swap-buffer trick is correct, just badly factored. The pruning-table concept is sound. The round-trip tests are well-thought-out *when they test what they claim to test*.

But none of that excuses the public-mutable-moveset arrays. Fix those first.

---

## Required actions before I would approve this PR

1. Make `G1/G2/G3_MOVESET` immutable or expose defensive copies.
2. Add `g3PackEdges` / `g3SetEdges` round-trip test; align `testPhaseThreeCorners`' moveset with `G3_MOVESET`.
3. Fix the twisted javadoc on `g2SetEdges`, `g2SetTripleCorners`, and `setAllCornerOrientation`.
4. Extract the four copy-pasted BFS generators into one parameterized function.
5. Remove `System.out.println` from `FtoCoord.init()`.
6. Turn `Search.g3Iterate()`'s bare `throw new RuntimeException()` into a meaningful exception with a message.
7. Add a thread-safety note to `FtoCubie` documenting that instances are not safe to share across threads (or fix `temps`).
8. Delete dead code: `Util.contains`, unused `IntPredicate` import, `g2GenerateTripleFrontier`'s dead `depth`.
9. Add a `module-info.java` or, at minimum, a `package-info.java` documenting the public surface.

Do those and I'll look at it again. Until then, request changes.