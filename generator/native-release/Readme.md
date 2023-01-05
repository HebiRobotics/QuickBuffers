# Deploying binary plugin artifacts

In order to to support the `pluginArtifact` parameter (`"us.hebi.quickbuf:protoc-gen-quickbuf:${version}"`) of the [protoc-jar-maven-plugin](https://github.com/os72/protoc-jar-maven-plugin) we need to create native executables with the same naming convention as [protoc](https://repo1.maven.org/maven2/com/google/protobuf/protoc/3.20.0/) and the [protoc-gen-grpc-java](https://repo1.maven.org/maven2/io/grpc/protoc-gen-grpc-java/1.9.1/) plugin, and upload them to Maven Central. 

The Java bytecode can be compiled to a standalone native executable using [GraalVM](https://www.graalvm.org/). This also gets rid of requiring an installed Java runtime and significantly speeds up execution due to ahead of time compilation and faster startup time. The caveat is that the images need to be compiled on each supported platform, which introduces some manual steps as GitHub Actions don't support macOS aarch64 yet.

### Compiling native executables

**Windows / Linux / macOS [x86_64]**
* push the desired commit to release/native-gen
* download the [workflow artifacts](https://github.com/HebiRobotics/QuickBuffers/actions/workflows/native-plugin.yml) and extract them into `bin`

**macOS [aarch64]**
* setup [GraalVM](https://www.graalvm.org/22.3/docs/getting-started/macos/) and `GRAAL_HOME` 
* run `mvn clean package -Pnative --projects generator -am -DskipTests`
* copy `target/protoc-gen-quickbuf-${version}-osx-aarch_64.exe` to `bin`

### Releasing to Maven Central

* set the appropriate `<version>` in the `pom.xml`
* `mvn clean deploy`