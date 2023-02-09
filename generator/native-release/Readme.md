# Deploying binary plugin artifacts

In order to support the `pluginArtifact` parameter (`"us.hebi.quickbuf:protoc-gen-quickbuf:${version}"`) of the [protoc-jar-maven-plugin](https://github.com/os72/protoc-jar-maven-plugin), we need to upload native executables to Maven Central and match the naming convention of [protoc](https://repo1.maven.org/maven2/com/google/protobuf/protoc/3.20.0/) and the [protoc-gen-grpc-java](https://repo1.maven.org/maven2/io/grpc/protoc-gen-grpc-java/1.9.1/) plugin.

The Java bytecode can be compiled to a standalone native executable using [GraalVM](https://www.graalvm.org/). This also gets rid of requiring an installed Java runtime and significantly speeds up execution due to ahead of time compilation and faster startup time. The caveat is that the images need to be compiled on each supported platform, which requires a self-hosted runner (macOS aarch64).

Given that Maven releases can only be released once, we decided to add some manual sanity checking rather than fully automating the release pipeline. The checks can be automated if updates ever become more frequent. The self-hosted runner already has everything setup, so the notes below are only reminders in case it ever breaks.

### Creating native executables

* set a matching native release version for Conveyor 
* make sure that a self-hosted macOS aarch64 is running. Requires the following setup
  * mvn
  * [GRAALVM]([GraalVM](https://www.graalvm.org/22.3/docs/getting-started/macos/)) and a GRAALVM_HOME environment variable
  * Conveyor CLI
* push the desired commit to the `release/native-gen` branch

### Testing the executables

* download the [workflow artifacts](https://github.com/HebiRobotics/QuickBuffers/actions/workflows/native-plugin.yml)
* (manually) confirm that the executables run on different operating systems

### Releasing to Maven Central

* cd into `generator/native-release`
* copy the workflow artifact executables into `bin/`
* set the appropriate `<version>` in the `pom.xml`
* `mvn clean deploy`

### Releasing to Github

* create a Github release and upload the entire site (in the workflow files) and all executables
* update `icon.png` and `download.html` to `docs/`


