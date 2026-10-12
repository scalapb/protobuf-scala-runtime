# 0.9.0 (2026-10-11)

This release fixes byte-slice handling, malformed-input validation, and output
buffer edge cases. The wire format for valid messages is unchanged.

The minor version bump reflects the `ByteString.bytes` source incompatibility
described below. Published metadata declares `early-semver`; 0.9.x patch releases
will preserve source and binary compatibility within the 0.9 line.

## Behavior changes and migration

- `ByteString` indexing, equality, hashing, and coded input now respect the
  visible slice. Out-of-range indices and invalid copy ranges fail consistently,
  including ranges whose end would overflow an integer.
- The deprecated `ByteString.bytes` accessor returns a fresh copy of the visible
  slice. Mutating that array no longer changes the `ByteString`. Replace uses
  with `toByteArray()`. Its JVM getter signature is retained, but changing a
  Scala `val` to a `def` is a source incompatibility for stable-path/singleton-type
  uses; bind the returned array to a local `val` when a stable value is needed.
- `isValidUtf8` and `readStringRequireUtf8` reject malformed UTF-8, including
  overlong encodings, encoded surrogates, and code points above U+10FFFF.
  `readString()` continues to use replacement decoding.
- Stream inputs enforce the default 64 MiB size limit even for prefetched bytes,
  varints, and large reads. Use `setSizeLimit` when larger messages are expected.
  Array inputs remain bounded by their supplied offset and length.
- Unknown-group skipping enforces the default recursion limit of 100. Use
  `setRecursionLimit` to change it. This limit does not govern recursion inside
  ScalaPB-generated message parsers.
- Embedded limits and length arithmetic reject overflow. Very short stream reads
  no longer grow the call stack; invalid `InputStream.read` results are rejected.
- `CodedInputStream.getLastTag()` exposes the last tag read, matching the Google
  protobuf API.
- Streaming `CodedOutputStream` buffers must be positive in size. Zero or negative
  sizes now throw `IllegalArgumentException` at construction. Empty flat-array
  outputs remain valid for empty messages. This fixes a hang when writing a
  nonempty direct/read-only `ByteBuffer` with a zero-size streaming buffer.
- Array output methods validate the complete source range before writing any
  tag, length prefix, or payload. I/O exceptions still propagate; a failed stream
  write may have emitted a prefix, and retrying on the same encoder is not
  guaranteed safe.

## Compatibility and validation

The build checks JVM binary compatibility against 0.8.16 for each supported Scala
line. A downstream consumer is compiled against 0.8.16 and its unchanged classes
are run with the candidate runtime on Java 8. The same source is compiled against
the candidate as part of the integration suites. These checks do not promise
source compatibility for the stable-path case described above.

Managed ScalaPB-generated integration messages exercise nested messages, packed
and unpacked fields, maps, oneofs, unknown-field preservation, and malformed
payloads on JVM, Scala.js, and Scala Native. Wire fixtures from Google's Java
runtime and the exhaustive bounded slice/UTF-8 and stream regression suites also
run across all nine compiler/platform combinations. Generated sources are not
committed or published.

The Java compatibility stubs remain intentionally incomplete. This release does
not add Google's reflection or Java generated-message runtime. Do not combine
this library with `protobuf-java` on a JVM application classpath.

## Build and maintenance

- Scala 2.12.21, 2.13.18, and 3.3.8; Scala Steward keeps Scala 3 on the 3.3 line.
- Scala.js 1.22.0 and Scala Native 0.5.12.
- sbt 2.0.10 and JDK 21 for building, with Java 8 as the published JVM target.
- Publishing uses Maven Central's Central Portal through sbt-ci-release.
- Release CI requires formatting, runtime/integration tests, and compatibility
  checks before publishing. Test helpers are excluded from publication.
