package online.lhoz.maven.plugin.pc;


import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Organizes the POM file
 */
@Mojo(name = "organize", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class OrganizeMojo extends AbstractMojo {

	private @Component MavenProject mavenProject;

	/**
	 * @throws MojoExecutionException
	 */
	public void execute() throws MojoExecutionException {
		try {
			// Obtains the POM file
			File file = mavenProject.getFile();

			// Obtains the maven model
			Model model = this.loadModel(file);

			// Extracts versions of dependencies
			this.extractDependencyVersions(model);

			// Extracts the versions of plugins
			this.extractPluginVersions(model);

			// Sorts the dependencies by scope and then alphabetically
			this.sortDependencies(model);

			// Saves the POM
			this.saveModel(file, model);

			// Invokes tidy:pom
			this.invokeTidyPomPlugin();
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}

	}

	/**
	 * Extracts the dependencies' versions and puts them in the properties,
	 * leaving existing properties as they are.
	 *
	 * @param model The Maven model to update.
	 */
	public void extractDependencyVersions(Model model) {
		// Get existing properties or initialize a new map
		Map<String, String> properties = model.getProperties() == null ? new HashMap<>() : new HashMap<>((Map) model.getProperties());

		// Get dependencies explicitly defined in the current POM
		List<Dependency> dependencies = model.getDependencies();

		// Update dependencies and extract versions
		for (Dependency dependency : dependencies) {
			if (dependency.getVersion() != null) {
				// Create the property key
				String propertyKey = "%s.%s".formatted(dependency.getGroupId(), dependency.getArtifactId());

				// Check if the property already exists
				if (!properties.containsKey(propertyKey)) {
					// Add the version to properties if it doesn't exist
					properties.put(propertyKey, dependency.getVersion());

					// Replace the version in the dependency with a property reference
					dependency.setVersion("${%s}".formatted(propertyKey));
				} else {
					// If the property exists, ensure the dependency references it
					dependency.setVersion("${%s}".formatted(propertyKey));
				}
			}
		}

		// Replace the model's properties with the updated map
		model.getProperties().putAll(properties);
	}


	/**
	 * Moves plugin versions to the <properties> section, leaving existing properties as they are.
	 *
	 * @param model Maven model
	 */
	private void extractPluginVersions(Model model) {
		// Get existing properties or initialize a new map
		Map<String, String> properties = model.getProperties() == null ? new HashMap<>() : new HashMap<>((Map) model.getProperties());

		// Check if the build section exists
		if (model.getBuild() != null && model.getBuild().getPlugins() != null) {
			// Update plugins and extract versions
			model.getBuild().getPlugins().forEach(plugin -> {
				if (plugin.getVersion() != null) {
					// Create the property key
					String propertyKey = "%s.%s".formatted(plugin.getGroupId(), plugin.getArtifactId());

					// Check if the property already exists
					if (!properties.containsKey(propertyKey)) {
						// Add the version to properties if it doesn't exist
						properties.put(propertyKey, plugin.getVersion());

						// Replace the version in the plugin with a property reference
						plugin.setVersion("${%s}".formatted(propertyKey));
					} else {
						// If the property exists, ensure the plugin references it
						plugin.setVersion("${%s}".formatted(propertyKey));
					}
				}
			});
		}

		// Replace the model's properties with the updated map
		model.getProperties().putAll(properties);
	}


	/**
	 * @throws Exception
	 */
	private void invokeTidyPomPlugin() throws Exception {
		// Set up the Maven invoker
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile(new File(mavenProject.getFile().getAbsolutePath()));
		request.setGoals(Collections.singletonList("tidy:pom"));

		Invoker invoker = new DefaultInvoker();

		// Execute the request
		InvocationResult result = invoker.execute(request);

		if (result.getExitCode() != 0) {
			throw new Exception("Failed to execute tidy:pom: " + result.getExecutionException());
		}
	}

	/**
	 * Loads the maven model from the pom.xml
	 *
	 * @param file
	 *
	 * @return
	 *
	 * @throws Exception
	 */
	private Model loadModel(File file) throws Exception {
		// Checks if the file exists
		if (!file.exists()) {
			throw new Exception("POM file for project does not exist.");
		}

		// Obtains the maven model
		Model model = mavenProject.getOriginalModel();

		// Returns
		return model;
	}


	/**
	 * Write the updated model back to pom.xml
	 *
	 * @param file
	 * @param model
	 *
	 * @throws Exception
	 */
	private void saveModel(File file, Model model) throws Exception {
		try (FileWriter writer = new FileWriter(file)) {
			new MavenXpp3Writer().write(writer, model);
		}
	}

	/**
	 * Sorts the dependencies of the model first by scope then alphabetically
	 *
	 * @param model
	 */
	private void sortDependencies(Model model) {
		// Sorts the dependencies by scope and then alphabetically
		List<Dependency> sortedDependencies = model.getDependencies().stream() //
				.sorted(Comparator //
						.comparing((Dependency d) -> d.getScope() == null ? "" : d.getScope()) //
						.thenComparing(Dependency::getGroupId) //
						.thenComparing(Dependency::getArtifactId)) //
				.collect(Collectors.toList());

		// Replaces the dependencies
		model.setDependencies(sortedDependencies);
	}

}
