# fto3phase

`fto3phase` is the Face Turning Octahedron solver used by TNoodle's FTO
scrambler. It generates scrambles by creating a random reachable FTO state and
solving that state with a three-phase IDA* search.

The search uses two kinds of pruning data:

- Small in-memory pruning tables generated at class initialization.
- Bundled binary pruning resources, `edgeprun.dat` and `centerprun.dat`, loaded
  from `src/main/resources`.

The resource files are part of the source distribution and should be committed.
The `build/` directory is Gradle output and should not be committed.

`FullFto` is the mutable internal puzzle representation. `FtoSearch` owns the
three-phase search and returns a TNoodle-compatible algorithm string using the
standard FTO move names: `R L U D F B BR BL` and their inverse moves.
