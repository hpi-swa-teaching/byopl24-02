# Analysis


## 4. Output Format & Structure

### Output Type

- [x] Console/Terminal output (location confirmation only)
- [x] File output (specify format: .bgv, .bgv.gz, .cfg)
- [ ] Graphical output (not directly—requires IGV or seafoam for visualization)
- [x] Binary data (BGV files are binary format, protocol version 7.0)

The primary output is BGV (Binary Graph Visualizer) format files containing serialized compiler IR graphs. These are binary files using network-endian (big-endian) byte order. Associated `.cfg` files contain Control Flow Graph data in text format for C1Visualizer (generated when `PrintBackendCFG=true`).

### Output Location

**Default location:** `$PWD/graal-dumps/<timestamp>/` where timestamp is Unix epoch milliseconds (e.g., `./graal-dumps/1499768882600/`). Console output confirms: `Dumping debug output in /Users/demo/graal-dumps/1499768882600`.

**Custom location:** Specified via `-Djdk.graal.DumpPath=/path/to/dumps`

**Folder structure:**
```
graal-dumps/1499768882600/
├── HotSpotCompilation-791[NodeLIRBuilder.matchComplexExpressions(List)].bgv
├── HotSpotCompilation-792[IntegerStamp.foldStamp(Stamp, Stamp)].bgv
├── HotSpotCompilation-791[NodeLIRBuilder.matchComplexExpressions(List)].cfg
└── ...
```

**File naming pattern:** `HotSpotCompilation-<ID>[<method-signature>].bgv`
- `<ID>`: Sequential compilation identifier
- `<method-signature>`: Full method name with parameter types
- `.bgv.gz`: Compressed variant (Seafoam reads natively)
- `.cfg`: Control Flow Graph for LIR analysis

Each compilation unit generates a `.bgv` file containing all graph dumps for that method across optimization phases. Files can be gzipped (`.bgv.gz`) for significant size reduction—the BGV format compresses well (5-10x) due to repeated patterns.

### Output Format Example

**BGV format structure hierarchy:**
```

Graph {
  sint8 token = BEGIN_GRAPH (0x01)
  sint32 id
  String format
  sint32 args_count
  PropObject[args_count] args
  GraphBody body
}

GraphBody {
  Props props (key-value pairs)
  sint32 nodes_count
  Node[nodes_count] nodes
  sint32 blocks_count
  Block[blocks_count] blocks
}

Node {
  sint32 id
  PoolObject node_class
  bool has_predecessor
  Props props
  Edge[input_count] edges_in
  Edge[output_count] edges_out
}
```

**Graph metadata structure:** Each graph within a BGV file has a name composed of `<compilation-id>:<method-signature> / <phase-name>`. Examples:
- `17:Fib.fib(int) / After parsing`
- `17:Fib.fib(int) / Before phase org.graalvm.compiler.phases.common.LoweringPhase`
- `17:Fib.fib(int) / After TruffleTier`

**Phase sequence:** For a Truffle compilation, typical phases include: After parsing → After inlining → After PartialEscape → After TruffleTier → After FrameState assignment → Before LIRGeneration.

---