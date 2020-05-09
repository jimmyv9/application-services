
# Guide to Building a Rust Component

A component is a client-side library that can be integrated with Firefox Application services, such as Firefox Sync or Firefox Accounts.

Some current examples of components include:

- logins: for storage and syncing of a user's saved login credentials
- push: for applications to receive real-time updates via WebPush

## Set up

In order to begin creating a new Rust component, it is important that the proper environment is built for the platform that you are using.

Within ` ~/libs `, one can find files that can build the required libraries for each platform, as well as verify these libraries are up to date if one has previously built an environment

Prior to building it is is important that the build dependencies, GYP, ninja, and Tcl are properly installed. A guide to to do this is located [here][libs].

For example, running: `./build-all-ios.sh` will build the proper libraries for developing components for iOS services. Likewise, `./verify-ios-environment.sh` will determine if your existing build is up to date. 

Once this process is complete, one can test that their build is properly compiling by ensuring the success of...
```sh
$ cargo test -all
```

If one is experiencing trouble visit the building [documentation][build].
## Creating a Component

For each component, there are a few architectural requirements that need to be addressed. Keep in mind during this procces that if one's work introduces new dependencies, please view the [dependency management guidelines][dependency].

### ~/components/component/src

Here lies the source files that implement the specific logic of one's component, as well as how the component interacts with the database, handles errors, or interacts with the [FFI].

For example in `~/components/logins/src/login.rs`, one can find the implementation behind the Logins struct. Functions include the proper getting and setting of ID's and passwords, as well as how logins interacts with the sync and remerge components.

When creating a library that interacts with existing Firefox Services, one can learn how to accomplish this by visiting the [Application Services Product Portal][products].

### FFI support

An [FFI], or Foreign Function Interface, allows for a program written in one language to make use of the services provided in another. This enables a language to interact with the semantics and calling conventions of another language.

In Rust, the keyword `extern` enables interaction with foreign libraries by initiating the ABI, or Application Binary Interface, to access the external, compiled libraries of a desired language. In order to ensure that Rust's guarantees on safety are upheld, all blocks of code involving `extern` are deemed unsafe. 

When using `extern` within a function definition, this allows this function to be called from the desired language, such as:

```sh
pub extern 'C' fn sync15_passwords_get_by_id()
```

An `extern` block allows Rust to use another languages function, such as:
```sh
extern {
    // fn func_wanted_from_the_other_lang() -> i32
}
```

### Kotlin and Swift Bindings

The bindings for Kotlin and Swift allow for components in Rust to properly interact with the APIs for the Android and iOS platforms.

For Android, there exists the proper `build.gradle` that configures the Android environement with the proper dependencies. The `~/components/component/android/src` directory contains all of the Kotlin API that interacts with the component's Rust architecture.

Similarly for iOS, there exists an iOS folder that contains the Swift API. Unlike Android, Swift does not require a `build.gradle` file to ensure configurations. To further read about implementing iOS compatibility, read about [consuming rust components on iOS][iOS].

There exists more specifc instructions in regard to binding to [Kotlin][Android] and [Swift][iOS] code.

## Megazording

During Distribution, all rust code for all of the components are compiled together into one `.so` file, or a shared-object file which holds a compiled library. To learn more about how these megazord libraries work and their importance, read the megazord [documentation][mz].


## Publishing a Client Library

After the creation of proper documentation and testing, a pull request can be made with contributions to the Application Services repository. It's important that testing is comprehensize before attempting to pull one's work. Please look over the testing [guide][test]. From here, the Application Services team will view the work and provide feedback. A more in-depth version of how to contribute to the Application Services repository exists [here][contribute].



[//]: # (These are reference links used in the body of this note and get stripped out when the markdown processor does its job. There is no need to format nicely because it shouldn't be seen. Thanks SO - http://stackoverflow.com/questions/4823468/store-comments-in-markdown-syntax)

   [FFI]: <https://doc.rust-lang.org/nomicon/ffi.html>
   [libs]: <https://github.com/mozilla/application-services/blob/master/libs/README.md>
   [products]: <https://mozilla.github.io/application-services/>
   [mz]: <https://github.com/mozilla/application-services/blob/master/docs/design/megazords.md>
   [iOS]: <https://github.com/mozilla/application-services/blob/master/docs/howtos/consuming-rust-components-on-ios.md>
   [build]: <https://github.com/mozilla/application-services/blob/master/docs/building.md>
   [contribute]: <https://github.com/mozilla/application-services/blob/master/docs/contributing.md>
   [test]: <https://github.com/mozilla/application-services/blob/master/docs/howtos/testing-a-rust-component.md>
   [dependency]: <https://github.com/mozilla/application-services/blob/master/docs/dependency-management.md>
   [Android]: <https://github.com/mozilla/application-services/blob/master/docs/howtos/exposing-rust-components-to-kotlin.md>
   [iOS]: <https://github.com/mozilla/application-services/blob/master/docs/howtos/exposing-rust-components-to-swift.md>
