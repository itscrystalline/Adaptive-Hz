{
  description = "Android workspace for occasion (Rust core linked via cargo-ndk)";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    rust-overlay = {
      url = "github:oxalica/rust-overlay";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  nixConfig = {
    substituters = ["https://cache.nixos.org"];
  };

  outputs = {
    self,
    nixpkgs,
    rust-overlay,
    ...
  }: let
    systems = [
      "x86_64-linux"
      "aarch64-linux"
    ];
    forEachSystem = nixpkgs.lib.genAttrs systems;
    mkPkgs = system:
      import nixpkgs {
        inherit system;
        overlays = [(import rust-overlay)];
        # android-studio ships an unfree (non-redistributable) component.
        config.allowUnfreePredicate = p: nixpkgs.lib.getName p == "android-studio";
      };
  in {
    # `nix run .#android-studio` launches Android Studio with the workspace env.
    packages = forEachSystem (
      system: let
        pkgs = mkPkgs system;
      in
        nixpkgs.lib.optionalAttrs pkgs.stdenv.hostPlatform.isx86_64 {
          default = pkgs.android-studio;
          inherit (pkgs) android-studio;
        }
    );

    devShells = forEachSystem (
      system: let
        pkgs = mkPkgs system;

        # Rust stable plus the Android target std libraries cargo-ndk needs.
        rustToolchain = pkgs.rust-bin.stable.latest.default.override {
          targets = [
            "aarch64-linux-android"
            "armv7-linux-androideabi"
            "x86_64-linux-android"
            "i686-linux-android"
          ];
        };

        jdk = pkgs.jdk21;
        sdkRoot = "$HOME/Android/Sdk";

        mkDevShell = {withStudio}:
          pkgs.mkShell {
            name =
              if withStudio
              then "occasion-android-studio"
              else "occasion-android";

            packages =
              [
                rustToolchain
                pkgs.cargo-ndk
                pkgs.kotlin-language-server
                pkgs.ktlint
                jdk
                pkgs.unzip
                pkgs.zip
                pkgs.curl
                pkgs.git
                pkgs.jq
                pkgs.pkg-config
              ]
              ++ nixpkgs.lib.optional withStudio pkgs.android-studio;

            # Gradle/AGP must run on the Nix JDK; do not leak a foreign JAVA_HOME.
            JAVA_HOME = "${jdk}";

            shellHook = ''
              # Android Studio needs a *writable* SDK, so it lives outside the store.
              export ANDROID_HOME="''${ANDROID_HOME:-${sdkRoot}}"
              export ANDROID_SDK_ROOT="$ANDROID_HOME"
              export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

              if [ -z "''${ANDROID_NDK_HOME:-}" ] && [ -d "$ANDROID_HOME/ndk" ]; then
                export ANDROID_NDK_HOME="$(
                  find "$ANDROID_HOME/ndk" -mindepth 1 -maxdepth 1 -type d | sort | tail -n 1
                )"
              fi

              export GRADLE_USER_HOME="''${GRADLE_USER_HOME:-$PWD/.gradle}"
              export CARGO_TARGET_DIR="''${CARGO_TARGET_DIR:-$PWD/native/target}"

              echo "${
                if withStudio
                then "occasion-android devshell (Studio)"
                else "occasion-android devshell"
              }"
              echo "  JAVA_HOME        = $JAVA_HOME"
              echo "  ANDROID_HOME     = $ANDROID_HOME"
              echo "  ANDROID_NDK_HOME = ''${ANDROID_NDK_HOME:-unset (run ./scripts/bootstrap-android-sdk.sh)}"
              ${nixpkgs.lib.optionalString withStudio ''
                echo "  launch Studio with: android-studio ."
              ''}
            '';
          };
      in {
        default = mkDevShell {withStudio = true;};
        studioless = mkDevShell {withStudio = false;};
      }
    );

    formatter = forEachSystem (system: (mkPkgs system).nixfmt-rfc-style);
  };
}
