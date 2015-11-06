@Grab(group='com.spotify', module='pipeline-conventions', version='1.0.5', changing = true)
import com.spotify.pipeline.Pipeline

new Pipeline(this) {{ build {
  notify.byMail(recipients: 'tools@spotify.com')

  group(name: 'Build & Test') {
    maven.pipelineVersionFromPom()
    maven.run(goal: '-U -e -B -Pcoverage -Pmissinglink verify')
    jenkinsPipeline.inJob {
      publishers {
        jacocoCodeCoverage{}
      }
    }
  }

  group(name: 'Upload artifacts') {
    maven.upload()
  }
}}}
