# Deploying binary plugin artifacts

In order to support the `pluginArtifact` parameter (`"us.hebi.quickbuf:protoc-gen-quickbuf:${version}"`) of the [protoc-jar-maven-plugin](https://github.com/os72/protoc-jar-maven-plugin), we need to upload native executables to Maven Central and match the naming convention of [protoc](https://repo1.maven.org/maven2/com/google/protobuf/protoc/3.20.0/) and the [protoc-gen-grpc-java](https://repo1.maven.org/maven2/io/grpc/protoc-gen-grpc-java/1.9.1/) plugin.

The Java bytecode can be compiled to a standalone native executable using [GraalVM](https://www.graalvm.org/). This also gets rid of requiring an installed Java runtime and significantly speeds up execution due to ahead of time compilation and faster startup time. The caveat is that the images need to be compiled on each supported platform, which currently requires a self-hosted runner for linux-aarch64.

Releasing to Maven Central as well as Conveyor packaging happen automatically in a Github action. It can be triggered manually in the action interface, or by pushing to `release/native-plugins`.

### Release procedure

* Update version
  * `versions:set -DprocessAllModules -DnewVersion=${NEW_VERSION}`
  * update `app.version` in `../conveyor.conf`
  * update version in `../Readme.md`
* Create a tag
* Push to `release/native-plugins`
* Upload `conveyor-site` contents to a Github release
* Copy the `icon.png` and `download.html` site to `../docs/`

### Local setup for creating native executables

You can setup a suitable GraalVM environment locally by following the steps below:

* download [GRAALVM]([GraalVM](https://www.graalvm.org/22.3/docs/getting-started/macos/)) and a GRAALVM_HOME environment variable
* build native images `mvn clean package --projects protoc-gen-quickbuf -am -P"makeNative,useNative"
* test the native images `mvn clean package --projects quickbuf-compat -am -PuseNative
