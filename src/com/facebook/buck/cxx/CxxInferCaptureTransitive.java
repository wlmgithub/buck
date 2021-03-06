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

package com.facebook.buck.cxx;

import com.facebook.buck.model.BuildTargets;
import com.facebook.buck.model.HasBuildTarget;
import com.facebook.buck.rules.AbstractBuildRuleWithResolver;
import com.facebook.buck.rules.BuildContext;
import com.facebook.buck.rules.BuildRuleParams;
import com.facebook.buck.rules.BuildTargetSourcePath;
import com.facebook.buck.rules.BuildableContext;
import com.facebook.buck.rules.HasPostBuildSteps;
import com.facebook.buck.rules.HasRuntimeDeps;
import com.facebook.buck.rules.SourcePath;
import com.facebook.buck.rules.SourcePathResolver;
import com.facebook.buck.step.Step;
import com.facebook.buck.step.fs.MkdirStep;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.nio.file.Path;
import java.util.stream.Stream;

public class CxxInferCaptureTransitive extends AbstractBuildRuleWithResolver
    implements HasRuntimeDeps, HasPostBuildSteps {

  private ImmutableSet<CxxInferCapture> captureRules;
  private Path outputDirectory;

  public CxxInferCaptureTransitive(
      BuildRuleParams params,
      SourcePathResolver pathResolver,
      ImmutableSet<CxxInferCapture> captureRules) {
    super(params, pathResolver);
    this.captureRules = captureRules;
    this.outputDirectory =
        BuildTargets.getGenPath(getProjectFilesystem(), this.getBuildTarget(), "infer-%s");
  }

  public ImmutableSet<CxxInferCapture> getCaptureRules() {
    return captureRules;
  }

  @Override
  public Stream<SourcePath> getRuntimeDeps() {
    return captureRules.stream()
        .map(HasBuildTarget::getBuildTarget)
        .map(BuildTargetSourcePath::new);
  }

  @Override
  public ImmutableList<Step> getBuildSteps(
      BuildContext context, BuildableContext buildableContext) {
    return ImmutableList.<Step>builder()
        .add(new MkdirStep(getProjectFilesystem(), outputDirectory))
        .build();
  }

  @Override
  public Path getPathToOutput() {
    return outputDirectory;
  }

  @Override
  public ImmutableList<Step> getPostBuildSteps() {
    return ImmutableList.<Step>builder()
        .add(new MkdirStep(getProjectFilesystem(), outputDirectory))
        .add(
            CxxCollectAndLogInferDependenciesStep.fromCaptureOnlyRule(
                this,
                getProjectFilesystem(),
                this.outputDirectory.resolve("infer-deps.txt"))
        )
        .build();
  }
}
