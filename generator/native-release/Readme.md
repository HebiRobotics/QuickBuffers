# Deploying binary plugin artifacts

In order to to support the `pluginArtifact` parameter (`"us.hebi.quickbuf:protoc-gen-quickbuf:${version}"`) of the [protoc-jar-maven-plugin](https://github.com/os72/protoc-jar-maven-plugin) we need to create native executables with the same naming convention as [protoc](https://repo1.maven.org/maven2/com/google/protobuf/protoc/3.20.0/) and the [protoc-gen-grpc-java](https://repo1.maven.org/maven2/io/grpc/protoc-gen-grpc-java/1.9.1/) plugin, and upload them to Maven Central. 

The Java bytecode can be compiled to a standalone native executable using [GraalVM](https://www.graalvm.org/). This also gets rid of requiring an installed Java runtime and significantly speeds up execution due to ahead of time compilation and faster startup time. The caveat is that the images need to be compiled on each supported platform, which requires a self-hosted runner (macOS aarch64).

The process could be fully automated, but with native executables it's better to do some extra sanity checking. Maven can't update releases if they break. We can automate more tests if updates ever become more frequent.

### Creating native executables

* set a matching native release version for Conveyor 
* make sure that a self-hosted macOS aarch64 is running. Requires the following setup
  * mvn
  * [GRAALVM]([GraalVM](https://www.graalvm.org/22.3/docs/getting-started/macos/)) and a GRAALVM_HOME environment variable
  * Conveyor CLI
* push the desired commit to the `release/native-gen` branch

### Releasing to Github

* download the [workflow artifacts](https://github.com/HebiRobotics/QuickBuffers/actions/workflows/native-plugin.yml) and append everything to the release
* (manually) confirm that the executables run on different operating systems
* create a Github release and upload the entire site and all executables
* update `icon.png` and `download.html` to `docs/`

### Releasing to Maven Central

* cd into `generator/native-release`
* download the executables and extract them into `bin`
* set the appropriate `<version>` in the `pom.xml`
* `mvn clean deploy`
