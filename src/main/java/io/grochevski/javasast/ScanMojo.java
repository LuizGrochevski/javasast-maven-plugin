package io.grochevski.javasast;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the javasast-rs binary against the project's Java sources as part
 * of the Maven build. This Mojo is intentionally a thin wrapper: all the
 * actual static-analysis logic lives in the Rust binary; this plugin's
 * only job is to invoke it with the right arguments, surface its output
 * through the Maven log, and fail the build when the binary's own exit
 * code signals findings at or above the configured severity.
 */
@Mojo(name = "scan", defaultPhase = LifecyclePhase.VERIFY)
public class ScanMojo extends AbstractMojo {

    /**
     * Path to the javasast-rs binary. Defaults to "javasast-rs", assuming
     * it's on the PATH; override with an absolute path if it isn't installed
     * system-wide (e.g. a locally built target/release/javasast-rs).
     */
    @Parameter(property = "javasast.binaryPath", defaultValue = "javasast-rs")
    private String binaryPath;

    /**
     * Directory to scan. Defaults to the project's main Java source root.
     */
    @Parameter(property = "javasast.sourceDirectory", defaultValue = "${project.build.sourceDirectory}")
    private String sourceDirectory;

    /**
     * Fail the build if any finding at or above this severity is present.
     * One of: low, medium, high. Set to "none" to never fail the build
     * (report-only mode).
     */
    @Parameter(property = "javasast.failOn", defaultValue = "high")
    private String failOn;

    /**
     * Optional export format for a report file: json, markdown, or sarif.
     */
    @Parameter(property = "javasast.outputFormat")
    private String outputFormat;

    /**
     * Where to write the exported report, if outputFormat is set.
     * Defaults to the project's build directory.
     */
    @Parameter(property = "javasast.outputFile", defaultValue = "${project.build.directory}/javasast-report")
    private String outputFile;

    /**
     * Skip the scan entirely without failing the build. Useful for
     * temporarily disabling the plugin without removing it from the POM.
     */
    @Parameter(property = "javasast.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("javasast-rs scan skipped (javasast.skip=true)");
            return;
        }

        List<String> command = new ArrayList<>();
        command.add(binaryPath);
        command.add("--path");
        command.add(sourceDirectory);

        if (!"none".equalsIgnoreCase(failOn)) {
            command.add("--fail-on");
            command.add(failOn);
        }

        if (outputFormat != null && !outputFormat.isBlank()) {
            command.add("--output");
            command.add(outputFormat);
            command.add("--out-file");
            command.add(outputFile + "." + (outputFormat.equals("markdown") ? "md" : outputFormat));
        }

        getLog().info("Running: " + String.join(" ", command));

        int exitCode;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    getLog().info(line);
                }
            }

            exitCode = process.waitFor();
        } catch (IOException e) {
            throw new MojoExecutionException(
                "Failed to run javasast-rs — is it installed and on the PATH? " +
                "Set the javasast.binaryPath property to an explicit path if not. Error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("javasast-rs scan was interrupted", e);
        }

        if (exitCode != 0) {
            throw new MojoFailureException(
                "javasast-rs found findings at or above severity '" + failOn +
                "' — see the log above for details, or set javasast.failOn=none to report without failing the build.");
        }

        getLog().info("javasast-rs scan passed (no findings at or above '" + failOn + "')");
    }
}
