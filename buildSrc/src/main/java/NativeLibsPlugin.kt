/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * This gradle plugin generates a `copyNativeLibs` task in the project it is
 * applied to.
 * This task copies library files in the application-services libs/ folder
 * inside the build dir nativeLibs/ folder and makes sure this folder is
 * included in Android jniLibs sourcesets.
 * It is the responsability of the consumer of this plugin to depend on that
 * newly defined task, for example in the `generateDebugAssets` task.
 * Example of usage:
 * <pre>
 *   apply plugin: NativeLibsPlugin
 *   nativeLibs {
 *     nss {
 *       lib "libnss3.*" // Wildcards are supported, just like the Copy task `include` method.
 *     }
 *   }
 *   tasks["generateDebugAssets"].dependsOn(tasks["copyNativeLibs"])
 * </pre>
 */

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.kotlin.dsl.delegateClosureOf
import java.io.File
// Needed to be able to call `DomainObjectCollection.all` instead of Kotlin's built-in `all` method.
import kotlin.collections.all as ktAll // ktlint-disable no-unused-imports

const val EXTENSION_NAME = "nativeLibs"
val ARCHS_FOLDERS = arrayOf(
    "android/armeabi-v7a",
    "android/arm64-v8a",
    "android/x86",
    "android/x86_64",
    "desktop/linux-x86-64",
    "desktop/darwin",
    "desktop/win32-x86-64"
)

data class NativeLib(
    val name: String,
    var libs: List<String>
) {
    constructor(name: String) : this(name, listOf<String>())

    public fun lib(libName: String): NativeLib {
        this.libs += libName
        return this
    }
}

open class NativeLibsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            val nativeLibs = container(NativeLib::class.java)
            extensions.add(EXTENSION_NAME, nativeLibs)

            nativeLibs.all(delegateClosureOf<NativeLib>({
                val nativeLib = this
                afterEvaluate {
                    var copyNativeLibsTask = tasks.maybeCreate("copyNativeLibs")
                    ARCHS_FOLDERS.forEach { archFolder ->
                        val taskName = archFolder.replace("/", "-")
                        val copyLibsTask = tasks.maybeCreate("copyNativeLibs-$taskName", Copy::class.java).apply {
                            from("${rootProject.rootDir}/libs/$archFolder/${nativeLib.name}/lib/")
                            into("$buildDir/nativeLibs/$archFolder")
                            nativeLib.libs.forEach {
                                include(it)
                            }
                        }
                        copyNativeLibsTask.dependsOn(copyLibsTask)
                    }
                }
            }))
            afterEvaluate {
                plugins.all(delegateClosureOf<Plugin<*>>({
                    when (this) {
                        is AppPlugin -> addToSourceSets<AppExtension>(project)
                        is LibraryPlugin -> addToSourceSets<LibraryExtension>(project)
                    }
                }))
            }
        }
    }
    private inline fun <reified T : BaseExtension> addToSourceSets(project: Project): Unit = with(project) {
        (extensions.findByName("android") as T).apply {
            sourceSets.getByName("main").jniLibs.srcDir(File("$buildDir/nativeLibs/android"))
            sourceSets.getByName("test").resources.srcDir(File("$buildDir/nativeLibs/desktop"))
        }
    }
}