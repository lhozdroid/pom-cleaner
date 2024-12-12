package online.lhoz.maven.plugin.pc;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;

import java.io.File;
import java.io.FileWriter;
import java.util.Collections;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reverts the organization of the POM file by replacing property references with actual values.
 */
@Mojo(name = "revert", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class RevertMojo extends AbstractMojo {
	private static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{(.+?)}");
	private @Component MavenProject mavenProject;

	/**
	 * @throws MojoExecutionException
	 */
	@Override
	public void execute() throws MojoExecutionException {
		try {
			// Obtains the POM file
			File file = mavenProject.getFile();

			// Obtains the Maven model
			Model model = this.loadModel(file);

			// Replace properties in dependencies
			this.replaceDependencyVersions(model);

			// Replace properties in plugins
			this.replacePluginVersions(model);

			// Remove used properties
			this.removeUsedProperties(model);

			// Save the updated POM file
			this.saveModel(file, model);

			// Invokes tidy:pom
			this.invokeTidyPomPlugin();
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	/**
	 * Invokes the tidy:pom Maven plugin.
	 *
	 * @throws Exception if an error occurs while invoking the plugin.
	 */
	private void invokeTidyPomPlugin() throws Exception {
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile(new File(mavenProject.getFile().getAbsolutePath()));
		request.setGoals(Collections.singletonList("tidy:pom"));

		Invoker invoker = new DefaultInvoker();
		InvocationResult result = invoker.execute(request);

		if (result.getExitCode() != 0) {
			throw new Exception("Failed to execute tidy:pom: " + result.getExecutionException());
		}
	}

	/**
	 * Loads the Maven model from the pom.xml.
	 *
	 * @param file The POM file.
	 *
	 * @return The Maven model.
	 *
	 * @throws Exception if an error occurs while loading the model.
	 */
	private Model loadModel(File file) throws Exception {
		if (!file.exists()) {
			throw new Exception("POM file for project does not exist.");
		}

		return mavenProject.getOriginalModel();
	}

	/**
	 * Removes properties that have been used in dependencies and plugins.
	 *
	 * @param model The Maven model to update.
	 */
	private void removeUsedProperties(Model model) {
		Properties properties = model.getProperties();

		for (Dependency dependency : model.getDependencies()) {
			String version = dependency.getVersion();
			if (version != null && !version.startsWith("${")) {
				properties.values().remove(version);
			}
		}

		if (model.getBuild() != null && model.getBuild().getPlugins() != null) {
			for (Plugin plugin : model.getBuild().getPlugins()) {
				String version = plugin.getVersion();
				if (version != null && !version.startsWith("${")) {
					properties.values().remove(version);
				}
			}
		}
	}

	/**
	 * Replaces property references in dependency versions with their actual values.
	 *
	 * @param model The Maven model to update.
	 */
	private void replaceDependencyVersions(Model model) {
		Properties properties = model.getProperties();

		for (Dependency dependency : model.getDependencies()) {
			String version = dependency.getVersion();

			if (version != null && version.startsWith("${")) {
				Matcher matcher = PROPERTY_PATTERN.matcher(version);
				if (matcher.matches()) {
					String propertyKey = matcher.group(1);
					String actualVersion = (String) properties.get(propertyKey);
					if (actualVersion != null) {
						dependency.setVersion(actualVersion);
					}
				}
			}
		}
	}

	/**
	 * Replaces property references in plugin versions with their actual values.
	 *
	 * @param model The Maven model to update.
	 */
	private void replacePluginVersions(Model model) {
		Properties properties = model.getProperties();

		if (model.getBuild() != null && model.getBuild().getPlugins() != null) {
			for (Plugin plugin : model.getBuild().getPlugins()) {
				String version = plugin.getVersion();

				if (version != null && version.startsWith("${")) {
					Matcher matcher = PROPERTY_PATTERN.matcher(version);
					if (matcher.matches()) {
						String propertyKey = matcher.group(1);
						String actualVersion = (String) properties.get(propertyKey);
						if (actualVersion != null) {
							plugin.setVersion(actualVersion);
						}
					}
				}
			}
		}
	}

	/**
	 * Saves the updated Maven model back to the pom.xml file.
	 *
	 * @param file  The POM file.
	 * @param model The Maven model to save.
	 *
	 * @throws Exception if an error occurs while saving the model.
	 */
	private void saveModel(File file, Model model) throws Exception {
		try (FileWriter writer = new FileWriter(file)) {
			new MavenXpp3Writer().write(writer, model);
		}
	}
}
