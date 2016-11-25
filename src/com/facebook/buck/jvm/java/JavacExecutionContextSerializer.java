/*
 * Copyright 2016-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.jvm.java;

import com.facebook.buck.io.ProjectFilesystem;
import com.facebook.buck.jvm.core.JavaPackageFinder;
import com.facebook.buck.util.ClassLoaderCache;
import com.facebook.buck.util.ProcessExecutor;
import com.facebook.buck.util.Verbosity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JavacExecutionContextSerializer {

  private JavacExecutionContextSerializer() {
  }

  private static final String VERBOSITY = "verbosity";
  private static final String JAVA_PACKAGE_FINDER = "java_package_finder";
  private static final String PROJECT_FILE_SYSTEM_ROOT = "project_file_system_root";
  private static final String CLASS_USAGE_FILE_WRITER = "class_usage_file_writer";
  private static final String ENVIRONMENT = "env";
  private static final String ABSOLUTE_PATHS_FOR_INPUTS = "absolute_paths_for_inputs";
  private static final String DIRECT_TO_JAR_SETTINGS = "direct_to_jar_settings";

  public static ImmutableMap<String, Object> serialize(JavacExecutionContext context) {
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

    builder.put(VERBOSITY, context.getVerbosity().toString());
    builder.put(
        JAVA_PACKAGE_FINDER,
        JavaPackageFinderSerializer.serialize(context.getJavaPackageFinder()));
    builder.put(PROJECT_FILE_SYSTEM_ROOT, context.getProjectFilesystem().getRootPath().toString());
    builder.put(
        CLASS_USAGE_FILE_WRITER,
        ClassUsageFileWriterSerializer.serialize(context.getUsedClassesFileWriter()));
    builder.put(ENVIRONMENT, context.getEnvironment());
    builder.put(
        ABSOLUTE_PATHS_FOR_INPUTS,
        ImmutableList.copyOf(
            context.getAbsolutePathsForInputs().stream().map(Path::toString).iterator()));
    if (context.getDirectToJarOutputSettings().isPresent()) {
      builder.put(
          DIRECT_TO_JAR_SETTINGS,
          DirectToJarOutputSettingsSerializer.serialize(
              context.getDirectToJarOutputSettings().get()));
    }

    return builder.build();
  }

  @SuppressWarnings("unchecked")
  public static JavacExecutionContext deserialize(
      Map<String, Object> data,
      JavacEventSink eventSink,
      PrintStream stdErr,
      ClassLoaderCache classLoaderCache,
      ObjectMapper objectMapper,
      ProcessExecutor processExecutor) {

    Preconditions.checkArgument(data.containsKey(VERBOSITY));
    Verbosity verbosity = Verbosity.valueOf((String) data.get(VERBOSITY));

    Preconditions.checkArgument(data.containsKey(JAVA_PACKAGE_FINDER));
    JavaPackageFinder javaPackageFinder = JavaPackageFinderSerializer.deserialize(
        (Map<String, Object>) data.get(JAVA_PACKAGE_FINDER));

    Preconditions.checkArgument(data.containsKey(PROJECT_FILE_SYSTEM_ROOT));
    ProjectFilesystem projectFilesystem = new ProjectFilesystem(Paths.get(
        (String) data.get(PROJECT_FILE_SYSTEM_ROOT)));

    Preconditions.checkArgument(data.containsKey(CLASS_USAGE_FILE_WRITER));
    ClassUsageFileWriter classUsageFileWriter = ClassUsageFileWriterSerializer.deserialize(
        (Map<String, Object>) data.get(CLASS_USAGE_FILE_WRITER));

    Preconditions.checkArgument(data.containsKey(ENVIRONMENT));

    Preconditions.checkArgument(data.containsKey(ABSOLUTE_PATHS_FOR_INPUTS));
    ImmutableList<Path> absolutePathsForInputs =
        ImmutableList.copyOf(
            ((List<String>) data.get(ABSOLUTE_PATHS_FOR_INPUTS)).stream()
                .map(s -> Paths.get(s)).iterator());

    Optional<DirectToJarOutputSettings> directToJarOutputSettings = Optional.empty();
    if (data.containsKey(DIRECT_TO_JAR_SETTINGS)) {
      directToJarOutputSettings = Optional.of(
          DirectToJarOutputSettingsSerializer.deserialize(
              (Map<String, Object>) data.get(DIRECT_TO_JAR_SETTINGS)));
    }

    return JavacExecutionContext.of(
        eventSink,
        stdErr,
        classLoaderCache,
        objectMapper,
        verbosity,
        javaPackageFinder,
        projectFilesystem,
        classUsageFileWriter,
        (Map<String, String>) data.get(ENVIRONMENT),
        processExecutor,
        absolutePathsForInputs,
        directToJarOutputSettings);
  }
}