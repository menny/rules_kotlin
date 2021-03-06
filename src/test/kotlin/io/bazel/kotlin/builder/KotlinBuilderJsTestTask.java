/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bazel.kotlin.builder;

import io.bazel.kotlin.builder.toolchain.KotlinToolchain;
import io.bazel.kotlin.model.CompilationTaskInfo;
import io.bazel.kotlin.model.JsCompilationTask;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public final class KotlinBuilderJsTestTask extends KotlinBuilderResource<JsCompilationTask> {
  private static final List<String> PASSTHROUGH_FLAGS =
      Arrays.asList("-source-map", "-meta-info", "-module-kind", "commonjs", "-target", "v5");
  private static final JsCompilationTask.Builder taskBuilder = JsCompilationTask.newBuilder();
  private static final KotlinBuilderComponent component =
      DaggerKotlinBuilderComponent.builder().toolchain(KotlinToolchain.createToolchain()).build();

  @Override
  CompilationTaskInfo.Builder infoBuilder() {
    return taskBuilder.getInfoBuilder();
  }

  @Override
  JsCompilationTask buildTask() {
    return taskBuilder.build();
  }

  @Override
  protected final void before() throws Throwable {
    taskBuilder.clear();
    super.before();

    taskBuilder.addAllPassThroughFlags(PASSTHROUGH_FLAGS);
    taskBuilder
        .getOutputsBuilder()
        .setJar(instanceRoot().resolve(label() + ".jar").toAbsolutePath().toString())
        .setSrcjar(instanceRoot().resolve(label() + "-sources.jar").toAbsolutePath().toString())
        .setJs(instanceRoot().resolve(label() + ".js").toAbsolutePath().toString());
  }

  public void addSource(String filename, String... lines) {
    Path sourcePath = super.writeSourceFile(filename, lines);
    taskBuilder.getInputsBuilder().addKotlinSources(sourcePath.toString());
  }

  public void runCompilationTask() {
    runCompileTask(
        (taskContext, task) -> {
          component.jsTaskExecutor().execute(taskContext, task);
          String jsFile = task.getOutputs().getJs();
          assertFilesExist(
              jsFile,
              jsFile + ".map",
              jsFile.substring(0, jsFile.length() - 3) + ".meta.js",
              task.getOutputs().getJar(),
              task.getOutputs().getSrcjar());
          return null;
        });
  }
}
