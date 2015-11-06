@Grab(group='com.spotify', module='pipeline-conventions', version='1.0.5', changing = true)
import com.spotify.pipeline.Pipeline

new Pipeline(this) {{ build {
  group(name: 'Build & Verify') {
    maven.pipelineVersionFromPom()
    maven.run(goal: '-U -e -B -Pcoverage -Pmissinglink -Dmaven.javadoc.skip=true verify')
  }
}}}
