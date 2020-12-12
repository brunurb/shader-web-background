/*
 * Copyright 2020  Kazimierz Pogoda
 *
 * This file is part of shader-web-background.
 *
 * shader-web-background is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * shader-web-background is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with shader-web-background.  If not, see <https://www.gnu.org/licenses/>.
 */

val closureCompilerVersion = "v20201102"

plugins {
  java
}

dependencies {
  runtimeOnly("com.google.javascript:closure-compiler:$closureCompilerVersion")
}

repositories {
  jcenter()
}

val charset = Charsets.UTF_8

fun getMapJsBackPath(path: String) =
    path.count { char -> char == '/' }
        .let { count -> "../".repeat(count) }

fun updateEmbeddedLib(
    libOccurrence: Regex,
    code: String,
    distDir: String,
    mapJsPath: String,
    path: String
) = File(path).let { file ->
  file.writeText(
      file.readText(charset)
          .replaceFirst(
              libOccurrence,
              code.replaceFirst(
                  mapJsPath,
                  "${getMapJsBackPath(path)}$distDir/$mapJsPath"
              )
          )
      ,
      charset
  )
}

fun updateEmbeddedLibs(
    libOccurrence: Regex,
    distDir: String,
    distJs: String,
    mapJsPath: String,
    vararg paths: String
) = File(distDir, distJs).readText(charset).let { code ->
  paths.forEach { path ->
    updateEmbeddedLib(
        libOccurrence,
        code,
        distDir,
        mapJsPath,
        path
    )
  }
}


task("compileJs", JavaExec::class) {
  group = "js"
  main = "com.google.javascript.jscomp.CommandLineRunner"

  val sourceDir = "src/main/js"
  val outputDir = "dist"
  val outputJs = "shader-web-background.min.js"
  val outputJsMap = "$outputJs.map"
  val namespace = "shaderWebBackground"
  val wrapperBegin = "// -- https://xemantic.github.io/shader-web-background/"
  val wrapperEnd = "//# sourceMappingURL=$outputJsMap"

  args = listOf(
      "--compilation_level", "ADVANCED",
      "--js", "$sourceDir/*.js",
      "--js_output_file", "$outputDir/$outputJs",
      "--create_source_map", "$outputDir/$outputJs.map",
      "--source_map_location_mapping", "$sourceDir|../$sourceDir",
      "--language_in", "ECMASCRIPT6",
      "--language_out", "ECMASCRIPT6",
      "--output_wrapper",
      "$wrapperBegin\nconst $namespace={};(()=>{%output%})()\n$wrapperEnd",
      "--jscomp_warning=accessControls",
      "--jscomp_warning=checkRegExp",
      "--jscomp_warning=constantProperty",
      "--jscomp_warning=const",
      "--jscomp_warning=deprecatedAnnotations",
      "--jscomp_warning=deprecated",
      "--jscomp_warning=missingProperties",
      "--jscomp_warning=missingReturn",
      "--jscomp_warning=reportUnknownTypes",
      "--jscomp_warning=strictCheckTypes",
      "--jscomp_warning=typeInvalidation",
      "--jscomp_warning=undefinedNames",
      "--jscomp_warning=unusedLocalVariables",
      "--jscomp_warning=unusedPrivateMembers",
      "--jscomp_warning=visibility"
  )
  classpath = sourceSets["main"].runtimeClasspath

  val libOccurrence = (
      "(?s)"
          + wrapperBegin.replace(".", "\\.")
          + ".*"
          + outputJsMap.replace(".", "\\.") + "\n"
      ).toRegex()

  doLast {
    updateEmbeddedLibs(
        libOccurrence,
        outputDir,
        outputJs,
        outputJsMap,
        "index.html",
        "demo/minimal.html"
    )
  }

}
