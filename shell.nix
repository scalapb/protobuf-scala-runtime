{ pkgs ? import <nixpkgs> { } }:

with pkgs;

let
  sbt = pkgs.sbt.override { jre = jdk21; };
in
mkShell {
  buildInputs = [
    sbt
    jdk21
    nodejs
    clang

    # keep this line if you use bash
    bashInteractive
  ];
}
