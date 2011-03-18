package ca.digitalface.jasperoo;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Sample of a command class. The command class is registered by the Roo shell following an
 * automatic classpath scan. You can provide simple user presentation-related logic in this
 * class. You can return any objects from each method, or use the logger directly if you'd
 * like to emit messages of different severity (and therefore different colours on 
 * non-Windows systems).
 * 
 * @since 1.1
 */
@Component // Use these Apache Felix annotations to register your commands class in the Roo container
@Service
public class JasperooCommands implements CommandMarker { // All command types must implement the CommandMarker interface
	
	/**
	 * Get a reference to the JasperooOperations from the underlying OSGi container
	 */
	@Reference private JasperooOperations operations;
	
	/**
	 * This method is optional. It allows automatic command hiding in situations when the command should not be visible.
	 * For example the 'entity' command will not be made available before the user has defined his persistence settings 
	 * in the Roo shell or directly in the project.
	 * 
	 * You can define multiple methods annotated with {@link CliAvailabilityIndicator} if your commands have differing
	 * visibility requirements.
	 * 
	 * @return true (default) if the command should be visible at this stage, false otherwise
	 *  
	 */
	@CliAvailabilityIndicator({"jasperoo setup", "jasperoo add", "jasperoo all" })
	public boolean isCommandAvailable() {
		return operations.isCommandAvailable();
	}
	
	/**
	 * This method registers a command with the Roo shell. It also offers a mandatory command attribute.
	 * 
	 * @param type 
	 */
	@CliCommand(value = "jasperoo add", help = "Generate a report for the entity specified.")
	public void add(@CliOption(key = "type", mandatory = true, help = "The entity to list in this report.") JavaType target,
			@CliOption(key = "id", mandatory = false, help = "If specified, a \"Detail\" report is generated for the entity identified, otherwise a \"List\" report is generated.") Long id,
			@CliOption(key = "finder", mandatory = false, help = "If specified, this finder will produce the resultset to be passed to the report.") String finder,
			@CliOption(key = "entityPackage", mandatory = false, help = "The location of the Entities.", unspecifiedDefaultValue="~.domain") String entityPackage, 
			@CliOption(key = "controllerPackage", mandatory = false, help = "The location of the Web Controllers.", unspecifiedDefaultValue="~.web") String controllerPackage) {
		operations.addReportByType(target, id, finder, entityPackage, controllerPackage);
	}
	
	/**
	 * This method registers a command with the Roo shell. It has no command attribute.
	 * 
	 */
	@CliCommand(value = "jasperoo all", help = "Generate a 'List' report for all project entities. Same as calling \"jasperoo add\" on each entity.")
	public void all(@CliOption(key = "entityPackage", mandatory = false, help = "The location of the Entities.", unspecifiedDefaultValue="~.domain") String entityPackage, 
			@CliOption(key = "controllerPackage", mandatory = false, help = "The location of the Web Controllers.", unspecifiedDefaultValue="~.web") String controllerPackage) {
		operations.addListReportForAll(entityPackage, controllerPackage);
	}
	
	/**
	 * This method registers a command with the Roo shell. It has no command attribute.
	 * 
	 */
	@CliCommand(value = "jasperoo setup", help = "Integrate Jasper Reports into this Roo application.")
	public void setup(@CliOption(key = "controllerPackage", mandatory = false, help = "The location of the Web Controllers.", unspecifiedDefaultValue="~.web") String controllerPackage) {
		operations.setup(controllerPackage);
	}
}