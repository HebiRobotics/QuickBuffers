## Why Protobuf v2 instead of the newer v3?

Both proto2 and proto3 use the same wire format, so the messages are binary compatible and only differ in semantics. Unfortunately, there were some major issues with the initial proto3 design, and the protobuf team ended up adding several workarounds that reproduce the proto2 semantics. They have stated that they will keep supporting both versions indefinitely, so we recommend sticking with proto2 whenever possible. Even Google's internal protobuf communication is [96% based on proto2](https://dl.acm.org/doi/pdf/10.1145/3466752.3480051).

For comparison, the main changes were

* No field presence

Field presence checks and non-zero defaults were originally removed to simplify implementing protobufs as [plain structs](https://stackoverflow.com/a/33229024/3574093) in languages without accessors. Unfortunately, not having field presence turned out to be a major design flaw (e.g. [#272](https://github.com/protocolbuffers/protobuf/issues/272), [#1606](https://github.com/protocolbuffers/protobuf/issues/1606)).

It was initially addressed by adding [slow wrapper types](https://github.com/protocolbuffers/protobuf/blob/f75fd051d68136ce366c464cea4f3074158cd141/src/google/protobuf/wrappers.proto) with special semantics, and more recently by adding [synthetic oneof fields](https://github.com/protocolbuffers/protobuf/blob/f75fd051d68136ce366c464cea4f3074158cd141/docs/implementing_proto3_presence.md) that add [explicit presence](https://github.com/protocolbuffers/protobuf/blob/main/docs/field_presence.md) as in proto2. As a result, even though proto3 was supposed to simplify 3rd party implementations, supporting the required workarounds actually makes it more complex than proto2.

* No non-zero defaults

Not having useful defaults requires field presence checks like `return hasValue ? getValue() : nan`, which was part of the reason why not having field presence turned out to be such a big issue.

* No zero values on the wire

Not sending default values was done to save space on the wire, but it further exacerbates the problem of field presence and lack of defaults. In the original design there is no way to tell whether something reported a valid value of zero or doesn't even know about the protocol field. This made proto3 absolutely unusable for many use cases (e.g. [#359](https://github.com/protocolbuffers/protobuf/issues/359#issuecomment-497746377)).

If needed, the same benefits could be achieved by adding generator flags or a method that clears the has bits of all fields that are set to their default values.

* No unknown field retention

This also turned out to be a major flaw and was reverted to proto2 behavior in [version 3.5](https://developers.google.com/protocol-buffers/docs/proto3#unknowns).

* Any instead of Extensions

The [Any](https://github.com/protocolbuffers/protobuf/blob/f75fd051d68136ce366c464cea4f3074158cd141/src/google/protobuf/any.proto) type is essentially a binary blob with a type identifier. It seems  simpler to implement and use than extensions, but we don't have a use case for either one and therefore can't compare.

* No required fields

This is a just formalization of what has already been recommended practice in proto2.