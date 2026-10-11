{ pkgs ? import <nixpkgs> { } }:

with pkgs;

let
  sbt = pkgs.sbt.override { jre = jdk21; };
in
mkShell {
  # sbt-protoc downloads a Linux executable linked against libstdc++.
  LD_LIBRARY_PATH = lib.makeLibraryPath [ stdenv.cc.cc.lib ];

  buildInputs = [
    sbt
    jdk21
    nodejs
    clang

    # keep this line if you use bash
    bashInteractive
  ];
}
