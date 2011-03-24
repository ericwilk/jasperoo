package ca.digitalface.jasperoo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jvnet.inflector.Noun;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.PhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MutableClassOrInterfaceTypeDetails;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ca.digitalface.jasperoo.utils.TokenReplacementFileCopyUtils;

/**
 * Implementation of Jasperoo Operations.
 * 
 * @since 1.1
 */
@Component
@Service
public class JasperooOperationsImpl implements JasperooOperations {

	/**
	 * MetadataService offers access to Roo's metadata model, use it to retrieve
	 * any available metadata by its MID
	 */
	@Reference
	private MetadataService metadataService;

	/**
	 * Use the PhysicalTypeMetadataProvider to access information about a
	 * physical type in the project
	 */
	@Reference
	private PhysicalTypeMetadataProvider physicalTypeMetadataProvider;

	/**
	 * Use ProjectOperations to install new dependencies, plugins, properties,
	 * etc into the project configuration
	 */
	@Reference
	private ProjectOperations projectOperations;

	/**
	 * Use TypeLocationService to find types which are annotated with a given
	 * annotation in the project
	 */
	@Reference
	private TypeLocationService typeLocationService;

	/**
	 * Use the FileManager to manipulate the files in the target project.
	 */
	@Reference
	private FileManager fileManager;
	
	/**
	 * Use the PathResolver to facilitate access to the various project source areas of the target project.
	 */
	@Reference
	private PathResolver pathResolver;
	
	/**
	 * The is the Roo shell.
	 */
	@Reference
	private Shell shell;

	private static char separator = File.separatorChar;

	private ComponentContext context;

	protected void activate(ComponentContext context) {
		this.context = context;
	}

	/** 
	 * Entity types not in this list will be converted to String by default in the report.
	 */
	private static String[] jasperFacetValidTypes = 
		new String[]{"java.lang.Boolean", "java.lang.Byte", "java.util.Date",
					"java.sql.Timestamp", "java.sql.Time", "java.lang.Double",
					"java.lang.Float", "java.lang.Integer", "java.lang.Long",
					"java.lang.Short", "java.math.BigDecimal", "java.lang.Number",
					"java.lang.String"};
	
	private static String[] validReportFormats = new String[]{"csv", "html", "odt", "pdf", "rtf", "xls", "xml"};

	/** {@inheritDoc} */
	public boolean isCommandAvailable(String commandName) {
		boolean bReady = false;
		bReady = projectOperations.isProjectAvailable();
		return bReady;
	}

	/** {@inheritDoc} */
	public void addReportByType(JavaType javaType, String finder, String entityPackage, String controllerPackage) {
		// Use Roo's Assert type for null checks
		Assert.notNull(javaType, "Java type required");
		
		String id = physicalTypeMetadataProvider.findIdentifier(javaType);
		if (id == null) {
			throw new IllegalArgumentException("Cannot locate source for '" + javaType.getFullyQualifiedTypeName() + "'");
		}

		// Obtain the physical type and itd mutable details
		PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(id);
		Assert.notNull(ptm, "Java source code unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(id));
		PhysicalTypeDetails ptd = ptm.getMemberHoldingTypeDetails();
		Assert.notNull(ptd, "Java source code details unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(id));
		Assert.isInstanceOf(MutableClassOrInterfaceTypeDetails.class, ptd, "Java source code is immutable for type " + PhysicalTypeIdentifier.getFriendlyName(id));
		MutableClassOrInterfaceTypeDetails mutableTypeDetails = (MutableClassOrInterfaceTypeDetails) ptd;

		if (null == MemberFindingUtils.getAnnotationOfType(mutableTypeDetails.getAnnotations(), new JavaType(RooJasperoo.class.getName()))) {
			JavaType rooJasperoo = new JavaType(RooJasperoo.class.getName());
			if (!mutableTypeDetails.getAnnotations().contains(rooJasperoo)) {
				AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(rooJasperoo);
				mutableTypeDetails.addTypeAnnotation(annotationBuilder.build());
			}
		}
		
		// Compute the Fully Qualified Name for the source JavaType
		String fqEntityName = physicalTypeMetadataProvider.findIdentifier(javaType);
		if (fqEntityName == null) {
			throw new IllegalArgumentException("Cannot locate source for '"
					+ javaType.getFullyQualifiedTypeName() + "'");
		}

		// The entity name is used all over.
		String entityName = fqEntityName.substring(fqEntityName.lastIndexOf(".") + 1);

		shell.executeCommand("focus --class "+entityPackage+"."+entityName);
		shell.executeCommand("finder add --finderName find"+getPlural(entityName)+"ById");

		// copy templates, copy jrxml, update ReportController, update
		// views.properties, insert i18n messages
		copyAddFilesIntoProject(entityName, finder, entityPackage, controllerPackage);

		//manipulate report templates
		modifyListReportTemplate(javaType, entityName);
		modifyDetailReportTemplate(javaType, entityName);
		
		// generate menu entry
		// Let's see if this is even needed.
		// addMenuEntry(entityName.toLowerCase());
		
		insertI18nAddMessages(entityName);

	}
	
	/** {@inheritDoc} */
	public void addReportsForAll(String entityPackage,
			String controllerPackage) {
		// Use the TypeLocationService to scan project for all types with a
		// specific annotation
		for (JavaType type : typeLocationService.findTypesWithAnnotation(
				new JavaType("org.springframework.roo.addon.javabean.RooJavaBean"))) {
			addReportByType(type, null, entityPackage, controllerPackage);
		}
	}

	/** {@inheritDoc} */
	public void extend(String formats){
		if(StringUtils.hasText(formats)){
			StringTokenizer stFormats = new StringTokenizer(formats,",", false);
			while(stFormats.hasMoreTokens()){
				String format = stFormats.nextToken().trim();
				if(Arrays.asList(validReportFormats).contains(format)){
					modifyListTagx(format);
					modifyShowTagx(format);
				} else {
					System.out.println("JASPEROO: '"+format + "' is not a valid report format, it will be ignored.");
				}
			}
		}
	}
	
	/** {@inheritDoc} */
	public void setup(String controllerPackage, String formats) {
		
		// Add all new dependencies to pom.xml
		projectOperations.addDependency(new Dependency(
				"org.springframework", "spring-context", "${spring.version}"));
		projectOperations.addDependency(new Dependency(
				"jasperreports", "jasperreports", "3.5.3"));
		projectOperations.addDependency(new Dependency(
				"org.apache.poi", "poi", "3.2-FINAL"));
		projectOperations.addDependency(new Dependency(
				"org.springframework.roo.wrapping", "org.springframework.roo.wrapping.inflector","0.7.0.0001"));
		
		String currentJasperooVersion = context.getBundleContext().getBundle().getHeaders().get("Bundle-Version").toString();
		projectOperations.addDependency(new Dependency("ca.digitalface", "ca.digitalface.jasperoo", currentJasperooVersion));

		//add pluginNode to pom
		String pomPath = pathResolver.getIdentifier(Path.ROOT, "pom.xml");

		MutableFile mutablePomXml = null;
		Document pomDoc;
		try {
			if (fileManager.exists(pomPath)) {
				mutablePomXml = fileManager.updateFile(pomPath);
				pomDoc = XmlUtils.getDocumentBuilder().parse(mutablePomXml.getInputStream());
			} else {
				throw new IllegalStateException("Could not acquire " + pomPath);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element pluginNode = new XmlElementBuilder("plugin", pomDoc)
			.addChild(new XmlElementBuilder("groupId", pomDoc)
				.setText("org.codehaus.mojo").build())
			.addChild(new XmlElementBuilder("artifactId", pomDoc)
				.setText("jasperreports-maven-plugin").build())
			.addChild(new XmlElementBuilder("configuration", pomDoc)
				.addChild(new XmlElementBuilder("sourceDirectory", pomDoc)
					.setText("src/main/webapp/WEB-INF/reports").build())
				.addChild(new XmlElementBuilder("outputDirectory", pomDoc)
					.setText("src/main/webapp/WEB-INF/reports").build())
				.build())
			.addChild(new XmlElementBuilder("executions", pomDoc)
				.addChild(new XmlElementBuilder("execution", pomDoc)
					.addChild(new XmlElementBuilder("goals", pomDoc)
						.addChild(new XmlElementBuilder("goal", pomDoc)
							.setText("compile-reports")
							.build())
						.build())
					.build())
				.build())
			.addChild(new XmlElementBuilder("dependencies", pomDoc)
				.addChild(new XmlElementBuilder("dependency", pomDoc)
					.addChild(new XmlElementBuilder("groupId", pomDoc)
						.setText("jasperreports")
						.build())
					.addChild(new XmlElementBuilder("artifactId", pomDoc)
						.setText("jasperreports")
						.build())
					.addChild(new XmlElementBuilder("version", pomDoc)
						.setText("3.5.3")
						.build())
					.build())
				.addChild(new XmlElementBuilder("dependency", pomDoc)
					.addChild(new XmlElementBuilder("groupId", pomDoc)
						.setText("org.apache.log4j")
						.build())
					.addChild(new XmlElementBuilder("artifactId", pomDoc)
						.setText("com.springsource.org.apache.log4j")
						.build())
					.addChild(new XmlElementBuilder("version", pomDoc)
						.setText("1.2.15")
						.build())
					.build())
				.build())
			.build();
		
		Node pluginsNode = pomDoc.getDocumentElement().getElementsByTagName("plugins").item(0);
		pluginsNode.appendChild(pluginNode);
		
		XmlUtils.writeXml(mutablePomXml.getOutputStream(), pomDoc);
		
		//add jasperReportsMultiFormatView to applicationContext
		String applicationContext = pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES,
				"META-INF/spring/applicationContext.xml");

		MutableFile mutableConfigXml = null;
		Document webConfigDoc;
		try {
			if (fileManager.exists(applicationContext)) {
				mutableConfigXml = fileManager.updateFile(applicationContext);
				webConfigDoc = XmlUtils.getDocumentBuilder().parse(mutableConfigXml.getInputStream());
			} else {
				throw new IllegalStateException("Could not acquire " + applicationContext);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element jasperReportsMultiFormatView = new XmlElementBuilder("bean", webConfigDoc)
			.addAttribute("id", "jasperReportsMultiFormatView")
			.addAttribute("name", "jasperReportsMultiFormatViewBean")
			.addAttribute("class", "ca.digitalface.jasperoo.reports.CustomJasperReportsMultiFormatView")
			.build();
		
		webConfigDoc.getDocumentElement().appendChild(jasperReportsMultiFormatView);
		
		XmlUtils.writeXml(mutableConfigXml.getOutputStream(), webConfigDoc);

		String webMVCConfig = pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES,
		"META-INF/spring/applicationContext.xml");

		MutableFile mutableWebMVCConfig = null;
		Document webMVCConfigDoc;
		try {
			if (fileManager.exists(webMVCConfig)) {
				mutableWebMVCConfig = fileManager.updateFile(webMVCConfig);
				webMVCConfigDoc = XmlUtils.getDocumentBuilder().parse(mutableWebMVCConfig.getInputStream());
			} else {
				throw new IllegalStateException("Could not acquire " + webMVCConfig);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		
		Element viewResolver = new XmlElementBuilder("bean", webMVCConfigDoc)
			.addAttribute("id", "viewResolver")
			.addAttribute("class", "org.springframework.web.servlet.view.ResourceBundleViewResolver")
			.addChild(new XmlElementBuilder("property", webMVCConfigDoc)
					.addAttribute("name", "basename")
					.addAttribute("value", "views").build()
					)
			.build();
		
		webMVCConfigDoc.getDocumentElement().appendChild(viewResolver);
		
		XmlUtils.writeXml(mutableWebMVCConfig.getOutputStream(), webMVCConfigDoc);

		
		//create Report Controller
		shell.executeCommand("class --class ~.web.ReportController");
		
		//copy templates/files
		String classesFolder = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/classes");
		if(!fileManager.exists(classesFolder)){
			fileManager.createDirectory(classesFolder);
		}

		String viewsProperties = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/classes/views.properties");
		if(!fileManager.exists(viewsProperties)){
			fileManager.createFile(viewsProperties);
		}
		
		String reportsFolder = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/reports");
		if(!fileManager.exists(reportsFolder)){
			fileManager.createDirectory(reportsFolder);
		}

		copySetupFilesIntoProject(controllerPackage);
		
		setupListTagx();
		extend(formats);
		
	}

	/**
	 * Populates the host project with the files required to setup the addon, and calls for the insertion of the requires i18n messages.
	 * 
	 * @param controllerPackage The location of the Web Controllers. Default: "~.web"
	 */
	private void copySetupFilesIntoProject(String controllerPackage) {

		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService
				.get(ProjectMetadata.getProjectIdentifier());
		JavaPackage topLevelPackage = projectMetadata.getTopLevelPackage();

		String finalControllerPackage = controllerPackage.replace("~",
				topLevelPackage.getFullyQualifiedPackageName());

		String reportsJavaFolder = "ca.digitalface.jasperoo.reports";

		Properties properties = new Properties();
		properties.put("__TOP_LEVEL_PACKAGE__",
				topLevelPackage.getFullyQualifiedPackageName());
		properties.put("__CONTROLLER_PACKAGE__", finalControllerPackage);

		Map<String, String> templateMap = new HashMap<String, String>();

		templateMap.put(pathResolver.getIdentifier(Path.SRC_MAIN_JAVA,
				reportsJavaFolder.replace('.', separator) + separator
						+ "CustomJasperReportsMultiFormatView.java"),
				"CustomJasperReportsMultiFormatView.java-template");

		templateMap.put(
				pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF"
						+ separator + "classes" + separator
						+ "jasperoo.properties"),
				"jasperoo.properties-template");

		templateMap.put(pathResolver.getIdentifier(Path.SRC_MAIN_JAVA,
				finalControllerPackage.replace('.', separator) + separator
						+ "ReportController.java"),
				"ReportController.java-template");

		for (Entry<String, String> template : templateMap.entrySet()) {

			MutableFile mutableFile = null;

			String path = template.getKey();
			String file = template.getValue();
			try {

				if (fileManager.exists(path))
					mutableFile = fileManager.updateFile(path);
				else
					mutableFile = fileManager.createFile(path);

				TokenReplacementFileCopyUtils.replaceAndCopy(
						TemplateUtils.getTemplate(getClass(), file),
						mutableFile.getOutputStream(), properties);

			} catch (IOException ioe) {
				throw new IllegalStateException(ioe);
			}
		}
		
		insertI18nSetupMessages();

		Map<String, String> imageMap = new HashMap<String, String>();

		imageMap.put(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images"	+ separator 
						+ "report-csv.png"),
						"images" + separator + "report-csv.png");

		imageMap.put(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images"	+ separator 
				+ "report-html.png"),
				"images" + separator + "report-html.png");

		imageMap.put(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images"	+ separator 
				+ "report-list.png"),
				"images" + separator + "report-list.png");

		imageMap.put(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images"	+ separator 
				+ "report-odt.png"),
				"images" + separator + "report-odt.png");

		imageMap.put(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images"	+ separator 
				+ "report-pdf.png"),
				"images" + separator + "report-pdf.png");

		imageMap.put(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images"	+ separator 
				+ "report-rtf.png"),
				"images" + separator + "report-rtf.png");

		imageMap.put(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images"	+ separator 
				+ "report-xls.png"),
				"images" + separator + "report-xls.png");

		imageMap.put(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images"	+ separator 
				+ "report-xml.png"),
				"images" + separator + "report-xml.png");

		imageMap.put(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images"	+ separator 
				+ "report.png"),
				"images" + separator + "report.png");

		for (Entry<String, String> image : imageMap.entrySet()) {
			String path = image.getKey();
			String file = image.getValue();
			try {
				File target = new File(path);
				FileOutputStream fosTarget = new FileOutputStream(target);
				
				TokenReplacementFileCopyUtils.replaceAndCopy(
						TemplateUtils.getTemplate(getClass(), file),
						fosTarget);

			} catch (IOException ioe) {
				throw new IllegalStateException(ioe);
			}
		}
		
	}

	/**
	 * Copies the files required for the requested report, updates the 
	 * ReportController declares the new views required by the requested report 
	 * and inserted the required i18n messages.
	 * 
	 * @param entityName The name of the entity being added.
	 * @param finder The finder to use to generate the recordset.
	 * @param entityPackage The location of the Entities. Default: "~.domain".
	 * @param controllerPackage The location of the Web Controllers. Default: "~.web"
	 */
	private void copyAddFilesIntoProject(String entityName, String finder, String entityPackage, String controllerPackage) {

		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService
				.get(ProjectMetadata.getProjectIdentifier());
		JavaPackage topLevelPackage = projectMetadata.getTopLevelPackage();

		String finalEntityPackage = entityPackage.replace("~",
				topLevelPackage.getFullyQualifiedPackageName());

		String listReportTitle = "label_" + finalEntityPackage.replace(".", "_")
				+ "_" + entityName.toLowerCase() + "_plural";

		String detailReportTitle = "label_" + finalEntityPackage.replace(".", "_")
		+ "_" + entityName.toLowerCase();

		String finalControllerPackage = controllerPackage.replace("~",
				topLevelPackage.getFullyQualifiedPackageName());

		Properties properties = new Properties();
		properties.put("__TOP_LEVEL_PACKAGE__",
				topLevelPackage.getFullyQualifiedPackageName());
		properties.put("__ENTITY_LEVEL_PACKAGE__", finalEntityPackage);
		properties.put("__ENTITY_NAME__", entityName);
		properties.put("__ENTITY_NAME_LOWER__", entityName.toLowerCase());
		properties.put("__LIST_REPORT_TITLE__", listReportTitle);
		properties.put("__DETAIL_REPORT_TITLE__", detailReportTitle);
		properties.put("__CONTROLLER_PACKAGE__", finalControllerPackage);

		Map<String, String> templateMap = new HashMap<String, String>();

		templateMap.put(pathResolver.getIdentifier(
				Path.SRC_MAIN_WEBAPP,
				"WEB-INF" + separator + "reports" + separator
						+ entityName.toLowerCase() + "ReportList.jrxml"),
				"reportList.jrxml-template");

		templateMap.put(pathResolver.getIdentifier(
				Path.SRC_MAIN_WEBAPP,
				"WEB-INF" + separator + "reports" + separator
						+ entityName.toLowerCase() + "ReportDetail.jrxml"),
				"reportDetail.jrxml-template");

		for (Entry<String, String> entry : templateMap.entrySet()) {

			MutableFile mutableFile = null;

			String path = entry.getKey();
			String file = entry.getValue();
			try {

				if (fileManager.exists(path))
					mutableFile = fileManager.updateFile(path);
				else
					mutableFile = fileManager.createFile(path);

				TokenReplacementFileCopyUtils.replaceAndCopy(
						TemplateUtils.getTemplate(getClass(), file),
						mutableFile.getOutputStream(), properties);

			} catch (IOException ioe) {
				throw new IllegalStateException(ioe);
			}
		}

		insertViewDeclarations(entityName);
		insertJasperooMessages(listReportTitle, getPlural(entityName));
		insertJasperooMessages(detailReportTitle, entityName);
		
		modifyReportController(entityName, finder, finalEntityPackage, finalControllerPackage);

	}

	/**
	 * Each report is a new view, here, the required views are declared in the views.properties.
	 * 
	 * @param entityName The Object upon which the report is based.
	 */
	private void insertViewDeclarations(String entityName) {
		String applicationProperties = pathResolver.getIdentifier(
				Path.SRC_MAIN_WEBAPP, "WEB-INF/classes/views.properties");

		MutableFile mutableApplicationProperties = null;

		try {
			if (fileManager.exists(applicationProperties)) {
				mutableApplicationProperties = fileManager
						.updateFile(applicationProperties);
				String originalData = convertStreamToString(mutableApplicationProperties
						.getInputStream());

				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
						mutableApplicationProperties.getOutputStream()));

				out.write(originalData);
				out.write(entityName.toLowerCase()
						+ "ReportList.url=/WEB-INF/reports/"
						+ entityName.toLowerCase() + "ReportList.jasper\n");
				out.write(entityName.toLowerCase()
						+ "ReportList.(class)=ca.digitalface.jasperoo.reports.CustomJasperReportsMultiFormatView\n");

				out.write(entityName.toLowerCase()
						+ "ReportDetail.url=/WEB-INF/reports/"
						+ entityName.toLowerCase() + "ReportDetail.jasper\n");
				out.write(entityName.toLowerCase()
						+ "ReportDetail.(class)=ca.digitalface.jasperoo.reports.CustomJasperReportsMultiFormatView\n");

				out.close();

			} else {
				throw new IllegalStateException("Could not acquire "
						+ applicationProperties);
			}
		} catch (Exception e) {
			System.out.println("---> " + e.getMessage());
			throw new IllegalStateException(e);
		}

	}
	
	/**
	 * Modifies the "List" report template to add all of the fields in the 
	 * entity being listed.
	 * 
	 * @param javaType The JavaType of the entity being listed.
	 * @param entityName The name of the entity being listed.
	 */
	private void modifyDetailReportTemplate(JavaType javaType, String entityName){
		String reportPath = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, 
				"WEB-INF/reports/"+entityName.toLowerCase()+"ReportDetail.jrxml");

		MutableFile mutableReportXml = null;
		Document reportDoc;
		try {
			if (fileManager.exists(reportPath)) {
				mutableReportXml = fileManager.updateFile(reportPath);
				reportDoc = XmlUtils.getDocumentBuilder().parse(mutableReportXml.getInputStream());
			} else {
				throw new IllegalStateException("Could not acquire " + reportPath);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element detailNode = (Element)reportDoc.getDocumentElement().getElementsByTagName("detail").item(0);
		Node detailBand = XmlUtils.findFirstElementByName("band", detailNode);

		// The id field is 20 px high, so we start there and add the field width on each pass.
		int offsetY = 20;
		
		// determine the total number of fields
		int fieldCount = getJavaTypeDetails(javaType).getDeclaredFields().size();
		MutableClassOrInterfaceTypeDetails details = getJavaTypeDetails(javaType);
		List<JavaType> extendsTypes = details.getExtendsTypes();
		
		// We need to know how many fields can be accessed from the hierarchy of this class.
		// Unfortunately, the list that comes back from getDeclaredFields is an "unmodifiableList", 
		// so we can't just concatenate all lists and iterate through a super-list.
		boolean bExtender = extendsTypes != null && extendsTypes.size() > 0;
		while(bExtender){
			JavaType tempType = details.getExtendsTypes().get(0);
			List<? extends FieldMetadata> fields = getJavaTypeDetails(tempType).getDeclaredFields();
			fieldCount = fieldCount + fields.size();
			
			details = getJavaTypeDetails(tempType);
			extendsTypes = details.getExtendsTypes();
			bExtender = extendsTypes != null && extendsTypes.size() > 0;
		}

		details = getJavaTypeDetails(javaType);
		List<? extends FieldMetadata> fields = getJavaTypeDetails(javaType).getDeclaredFields();
		bExtender = true;
		while(bExtender){
			int fieldHeight = (641 - 20)/ fieldCount;
			if(fieldHeight > 20){
				fieldHeight = 20;
			}
			
			for (FieldMetadata fieldMetadata : fields) {
				JavaType fieldType = fieldMetadata.getFieldType();
				boolean bValidType = Arrays.binarySearch(jasperFacetValidTypes, fieldType.getFullyQualifiedTypeName()) >= 0;
				
				JavaSymbolName fieldName = fieldMetadata.getFieldName();
				insertJasperooMessages("jasperoo."+getPlural(entityName)+"."+fieldName.getSymbolName(), fieldName.getReadableSymbolName());
				
				Element pageHeaderNode = XmlUtils.findFirstElementByName("pageHeader", reportDoc.getDocumentElement());

				Element fieldNode = new XmlElementBuilder("field", reportDoc)
					.addAttribute("name", fieldName.getSymbolName())
					.addAttribute("class", fieldType.getFullyQualifiedTypeName())
					.build();
		
				reportDoc.getDocumentElement().insertBefore(fieldNode, pageHeaderNode);

				if(!bValidType){
					Element firstFieldNode = XmlUtils.findFirstElementByName("field", reportDoc.getDocumentElement());
	
					Element importNode = new XmlElementBuilder("import", reportDoc)
						.addAttribute("value", fieldType.getFullyQualifiedTypeName())
						.build();
			
					reportDoc.getDocumentElement().insertBefore(importNode, firstFieldNode);
				}

				Element headerTextfield = new XmlElementBuilder("textField", reportDoc)
					.addChild(new XmlElementBuilder("reportElement", reportDoc)
						.addAttribute("x", "0")
						.addAttribute("y", String.valueOf(offsetY))
						.addAttribute("width", "125")
						.addAttribute("height", String.valueOf(fieldHeight))
						.build())
					.addChild(new XmlElementBuilder("textElement", reportDoc).build())
					.addChild(new XmlElementBuilder("textFieldExpression", reportDoc)
						.addAttribute("class", "java.lang.String")
						.setText("$R{jasperoo."+getPlural(entityName)+"."+fieldName.getSymbolName()+"}")
						.build())
					.build();
			
				detailBand.appendChild(headerTextfield);

				Element detailTextfield = null;
				if(bValidType){
					detailTextfield = new XmlElementBuilder("textField", reportDoc)
					.addChild(new XmlElementBuilder("reportElement", reportDoc)
						.addAttribute("x", "126")
						.addAttribute("y", String.valueOf(offsetY))
						.addAttribute("width", "446")
						.addAttribute("height", String.valueOf(fieldHeight))
						.build())
					.addChild(new XmlElementBuilder("textElement", reportDoc).build())
					.addChild(new XmlElementBuilder("textFieldExpression", reportDoc)
						.addAttribute("class", fieldType.getFullyQualifiedTypeName())
						.setText("$F{"+fieldName.getSymbolName()+"}")
						.build())
					.build();
				} else {
					detailTextfield = new XmlElementBuilder("textField", reportDoc)
						.addChild(new XmlElementBuilder("reportElement", reportDoc)
						.addAttribute("x", "126")
						.addAttribute("y", String.valueOf(offsetY))
						.addAttribute("width", "446")
						.addAttribute("height", String.valueOf(fieldHeight))
						.build())
					.addChild(new XmlElementBuilder("textElement", reportDoc).build())
					.addChild(new XmlElementBuilder("textFieldExpression", reportDoc)
						.addAttribute("class", "java.lang.String")
						.setText("$F{"+fieldName.getSymbolName()+"}.toString()")
						.build())
					.build();
				}
		
				detailBand.appendChild(detailTextfield);
				
				offsetY += fieldHeight;
			}
			
			// Do we keep going?
			extendsTypes = details.getExtendsTypes();
			bExtender = extendsTypes != null && extendsTypes.size() > 0;
			if(bExtender){
				JavaType tempType = details.getExtendsTypes().get(0);
				details = getJavaTypeDetails(tempType);
				fields = details.getDeclaredFields();
			}
		}

		XmlUtils.writeXml(mutableReportXml.getOutputStream(), reportDoc);		
		
	}

	/**
	 * Modifies the "List" report template to add all of the fields in the 
	 * entity being listed.
	 * 
	 * @param javaType The JavaType of the entity being listed.
	 * @param entityName The name of the entity being listed.
	 */
	private void modifyListReportTemplate(JavaType javaType, String entityName){
		String reportPath = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, 
				"WEB-INF/reports/"+entityName.toLowerCase()+"ReportList.jrxml");

		MutableFile mutableReportXml = null;
		Document reportDoc;
		try {
			if (fileManager.exists(reportPath)) {
				mutableReportXml = fileManager.updateFile(reportPath);
				reportDoc = XmlUtils.getDocumentBuilder().parse(mutableReportXml.getInputStream());
			} else {
				throw new IllegalStateException("Could not acquire " + reportPath);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element columnHeaderNode = (Element)reportDoc.getDocumentElement().getElementsByTagName("columnHeader").item(0);
		Element columnHeaderBand = XmlUtils.findFirstElementByName("band", columnHeaderNode);
		Element detailNode = (Element)reportDoc.getDocumentElement().getElementsByTagName("detail").item(0);
		Node detailBand = XmlUtils.findFirstElementByName("band", detailNode);

		// The id field is 30 px wide, so we start there and add the field width on each pass.
		int offsetX = 30;
		
		// determine the total number of fields
		int fieldCount = getJavaTypeDetails(javaType).getDeclaredFields().size();
		MutableClassOrInterfaceTypeDetails details = getJavaTypeDetails(javaType);
		List<JavaType> extendsTypes = details.getExtendsTypes();
		
		// We need to know how many fields can be accessed from the hierarchy of this class.
		// Unfortunately, the list that comes back from getDeclaredFields is an "unmodifiableList", 
		// so we can't just concatenate all lists and iterate through a super-list.
		boolean bExtender = extendsTypes != null && extendsTypes.size() > 0;
		while(bExtender){
			JavaType tempType = details.getExtendsTypes().get(0);
			List<? extends FieldMetadata> fields = getJavaTypeDetails(tempType).getDeclaredFields();
			fieldCount = fieldCount + fields.size();
			
			details = getJavaTypeDetails(tempType);
			extendsTypes = details.getExtendsTypes();
			bExtender = extendsTypes != null && extendsTypes.size() > 0;
		}

		details = getJavaTypeDetails(javaType);
		List<? extends FieldMetadata> fields = getJavaTypeDetails(javaType).getDeclaredFields();
		bExtender = true;
		while(bExtender){
			int fieldWidth = (752 - 30)/ fieldCount;
			
			for (FieldMetadata fieldMetadata : fields) {
				JavaType fieldType = fieldMetadata.getFieldType();
				boolean bValidType = Arrays.binarySearch(jasperFacetValidTypes, fieldType.getFullyQualifiedTypeName()) >= 0;
				
				JavaSymbolName fieldName = fieldMetadata.getFieldName();
				insertJasperooMessages("jasperoo."+getPlural(entityName)+"."+fieldName.getSymbolName(), fieldName.getReadableSymbolName());
				
				Element pageHeaderNode = XmlUtils.findFirstElementByName("pageHeader", reportDoc.getDocumentElement());

				Element fieldNode = new XmlElementBuilder("field", reportDoc)
					.addAttribute("name", fieldName.getSymbolName())
					.addAttribute("class", fieldType.getFullyQualifiedTypeName())
					.build();
		
				reportDoc.getDocumentElement().insertBefore(fieldNode, pageHeaderNode);

				if(!bValidType){
					Element firstFieldNode = XmlUtils.findFirstElementByName("field", reportDoc.getDocumentElement());
	
					Element importNode = new XmlElementBuilder("import", reportDoc)
						.addAttribute("value", fieldType.getFullyQualifiedTypeName())
						.build();
			
					reportDoc.getDocumentElement().insertBefore(importNode, firstFieldNode);
				}

				Element headerTextfield = new XmlElementBuilder("textField", reportDoc)
					.addChild(new XmlElementBuilder("reportElement", reportDoc)
						.addAttribute("x", String.valueOf(offsetX))
						.addAttribute("y", "0")
						.addAttribute("width", String.valueOf(fieldWidth))
						.addAttribute("height", "20")
						.build())
					.addChild(new XmlElementBuilder("textElement", reportDoc).build())
					.addChild(new XmlElementBuilder("textFieldExpression", reportDoc)
						.addAttribute("class", "java.lang.String")
						.setText("$R{jasperoo."+getPlural(entityName)+"."+fieldName.getSymbolName()+"}")
						.build())
					.build();
			
				columnHeaderBand.appendChild(headerTextfield);

				Element detailTextfield = null;
				if(bValidType){
					detailTextfield = new XmlElementBuilder("textField", reportDoc)
					.addChild(new XmlElementBuilder("reportElement", reportDoc)
						.addAttribute("x", String.valueOf(offsetX))
						.addAttribute("y", "0")
						.addAttribute("width", String.valueOf(fieldWidth))
						.addAttribute("height", "20")
						.build())
					.addChild(new XmlElementBuilder("textElement", reportDoc).build())
					.addChild(new XmlElementBuilder("textFieldExpression", reportDoc)
						.addAttribute("class", fieldType.getFullyQualifiedTypeName())
						.setText("$F{"+fieldName.getSymbolName()+"}")
						.build())
					.build();
				} else {
					detailTextfield = new XmlElementBuilder("textField", reportDoc)
					.addChild(new XmlElementBuilder("reportElement", reportDoc)
						.addAttribute("x", String.valueOf(offsetX))
						.addAttribute("y", "0")
						.addAttribute("width", String.valueOf(fieldWidth))
						.addAttribute("height", "20")
						.build())
					.addChild(new XmlElementBuilder("textElement", reportDoc).build())
					.addChild(new XmlElementBuilder("textFieldExpression", reportDoc)
						.addAttribute("class", "java.lang.String")
						.setText("$F{"+fieldName.getSymbolName()+"}.toString()")
						.build())
					.build();
				}
		
				detailBand.appendChild(detailTextfield);
				
				offsetX += fieldWidth;
			}
			
			// Do we keep going?
			extendsTypes = details.getExtendsTypes();
			bExtender = extendsTypes != null && extendsTypes.size() > 0;
			if(bExtender){
				JavaType tempType = details.getExtendsTypes().get(0);
				details = getJavaTypeDetails(tempType);
				fields = details.getDeclaredFields();
			}
		}

		XmlUtils.writeXml(mutableReportXml.getOutputStream(), reportDoc);		
		
	}
	
	/**
	 * Inserts the required request handling method into the ReportController for a "List" report.
	 * 
	 * @param entityName The Object upon which the report is based.
	 * @param finalEntityPackage The properly formatted package of the entity in question.
	 * @param finalControllerPackage The properly formatted package of the ReportController.
	 */
	private void modifyReportController(String entityName, String finder, String finalEntityPackage, String finalControllerPackage) {
		String controllerFile = pathResolver.getIdentifier(Path.SRC_MAIN_JAVA,
				finalControllerPackage.replace('.', separator) + separator
						+ "ReportController.java");

		MutableFile mutableControllerFile = null;

		try {
			if (fileManager.exists(controllerFile)) {
				mutableControllerFile = fileManager.updateFile(controllerFile);
				String originalData = convertStreamToString(mutableControllerFile
						.getInputStream());
				originalData = originalData.substring(0,
						originalData.lastIndexOf("}"));
				String closingBrace = "}";

				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
						mutableControllerFile.getOutputStream()));

				if(originalData.contains("report"+ entityName+ "List") || originalData.contains("report"+ entityName+ "Detail")){
					System.out.println("\nJASPEROO MANUAL INTERVENTION NEEDED!\n");
					if(originalData.contains("report"+ entityName+ "List")){
						System.out.println("-ReportController.java has more than one method named report"+ entityName+ "List()\n");
					}
					if(originalData.contains("report"+ entityName+ "Detail")){
						System.out.println("-ReportController.java has more than one method named report"+ entityName+ "Detail()\n");
					}
				}
				out.write(originalData);
				out.write("\t@RequestMapping(value =\"/"
						+ entityName.toLowerCase()
						+ "List/{format}\", method = RequestMethod.GET)\n");
				out.write("\tpublic String report"
						+ entityName
						+ "List(ModelMap modelMap, @PathVariable(\"format\") String format) {\n");
				if(finder == null){
					out.write("\t\tJRBeanCollectionDataSource jrDataSource = new JRBeanCollectionDataSource("
							+ finalEntityPackage + "." + entityName + ".findAll" + getPlural(entityName) + "(),false);\n");
				} else {
					out.write("\t\tJRBeanCollectionDataSource jrDataSource = new JRBeanCollectionDataSource("
							+ finalEntityPackage + "." + entityName + "." + finder + "(),false);\n");
				}
				out.write("\t\tmodelMap.put(\"reportData\", jrDataSource);\n");
				out.write("\t\tmodelMap.put(\"format\", format);\n");
				out.write("\t\treturn \"" + entityName.toLowerCase()
						+ "ReportList\";\n");
				out.write("\t}\n\n");
				
				out.write("\t@RequestMapping(value =\"/"+entityName.toLowerCase()+"Detail/{id}/{format}\", method = RequestMethod.GET)\n");
				out.write("\tpublic String report"
						+ entityName
						+ "Detail(ModelMap modelMap, @PathVariable(\"id\") Long id, @PathVariable(\"format\") String format) {\n");
				out.write("\t\tJRBeanCollectionDataSource jrDataSource = new JRBeanCollectionDataSource("
							+ finalEntityPackage + "." + entityName + ".find" + getPlural(entityName) + "ById(id).getResultList(),false);\n");
				out.write("\t\tmodelMap.put(\"reportData\", jrDataSource);\n");
				out.write("\t\tmodelMap.put(\"format\", format);\n");
				out.write("\t\treturn \""+entityName.toLowerCase()+"ReportDetail\";\n");
				out.write("\t}\n\n");
				
				out.write(closingBrace);

				out.close();

			} else {
				throw new IllegalStateException("Could not acquire "
						+ controllerFile);
			}
		} catch (Exception e) {
			System.out.println("---> " + e.getMessage());
			throw new IllegalStateException(e);
		}

	}

	/**
	 * Modifies the "List" report template to add all of the fields in the 
	 * entity being listed.
	 * 
	 * @param javaType The JavaType of the entity being listed.
	 * @param entityName The name of the entity being listed.
	 */
	private void modifyListTagx(String reportFormat){
		String docPath = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, 
				"WEB-INF/tags/form/list.tagx");

		MutableFile mutableDocXml = null;
		Document targetDoc;
		try {
			if (fileManager.exists(docPath)) {
				mutableDocXml = fileManager.updateFile(docPath);
				targetDoc = XmlUtils.getDocumentBuilder().parse(mutableDocXml.getInputStream());
			} else {
				throw new IllegalStateException("Could not acquire " + docPath);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Node target = XmlUtils.findFirstElementByName("jsp:doBody", targetDoc.getDocumentElement()).getParentNode();

		/*
		 *	<c:catch>
		 *	    <c:if test="${not empty object.reportable}">
		 *           <span>
		 *               <spring:url value="/reports/${fn:toLowerCase(typeName)}List/${itemId}/pdf" var="report_url" />
		 *               <spring:url value="/resources/images/report.png" var="report_image_url" />
		 *               <spring:message arguments="${typeName}" code="entity_list_report" var="report_label" htmlEscape="false" />
		 *               <a href="${fn:escapeXml(report_url)}" alt="${fn:escapeXml(report_label)}" title="${fn:escapeXml(report_label)}">
		 *                 <img alt="${fn:escapeXml(report_label)}" class="image" src="${fn:escapeXml(report_image_url)}" title="${fn:escapeXml(report_label)}" />
		 *               </a>
		 *           </span>
		 *	    </c:if>
		 *   </c:catch>
		 */

		Element catchNode = new XmlElementBuilder("c:catch", targetDoc)
			.addChild(new XmlElementBuilder("c:if", targetDoc)
				.addAttribute("test", "${not empty items[0].reportable}")
				.addChild(new XmlElementBuilder("span", targetDoc)
					.addAttribute("style", "text-align:right;")
					.build())
					.addChild(new XmlElementBuilder("spring:url", targetDoc)
						.addAttribute("value", "/reports/${fn:toLowerCase(label)}List/"+reportFormat)
						.addAttribute("var", "report_url")
						.build())
					.addChild(new XmlElementBuilder("spring:url", targetDoc)
						.addAttribute("value", "/resources/images/report-"+reportFormat+".png")
						.addAttribute("var", "report_image_url")
						.build())
					.addChild(new XmlElementBuilder("spring:message", targetDoc)
						.addAttribute("arguments", "${label}")
						.addAttribute("code", "entity_list_report")
						.addAttribute("var", "report_label")
						.addAttribute("htmlEscape", "false")
						.build())
					.addChild(new XmlElementBuilder("a", targetDoc)
						.addAttribute("href", "${fn:escapeXml(report_url)}")
						.addAttribute("alt", "${fn:escapeXml(report_label)}")
						.addAttribute("title", "${fn:escapeXml(report_label)}")
						.addChild(new XmlElementBuilder("img", targetDoc)
							.addAttribute("class", "image")
							.addAttribute("alt", "${fn:escapeXml(report_label)}")
							.addAttribute("title", "${fn:escapeXml(report_label)}")
							.addAttribute("src", "${fn:escapeXml(report_image_url)}")
							.build())
						.build())
				.build())
			.build();

		target.appendChild(catchNode);		
		

		XmlUtils.writeXml(mutableDocXml.getOutputStream(), targetDoc);		
		
	}
	
	/**
	 * Sets up the "List" tag.
	 */
	private void setupListTagx(){
		String docPath = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, 
				"WEB-INF/tags/form/list.tagx");

		MutableFile mutableDocXml = null;
		Document targetDoc;
		try {
			if (fileManager.exists(docPath)) {
				mutableDocXml = fileManager.updateFile(docPath);
				targetDoc = XmlUtils.getDocumentBuilder().parse(mutableDocXml.getInputStream());
			} else {
				throw new IllegalStateException("Could not acquire " + docPath);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Node target = XmlUtils.findFirstElementByName("jsp:doBody", targetDoc.getDocumentElement()).getParentNode();

		/*
		 *	<c:catch>
		 *		<c:if test="${not empty object.reportable}"/>
		 *			<spring:message code="jasperoo_reports" htmlEscape="false" var="reports_label"/>
		 *			<b><c:out value="${reports_label} "/></b>
		 *		</c:if>
		 *	</c:catch>
		 */

		Element catchNode = new XmlElementBuilder("c:catch", targetDoc)
			.addChild(new XmlElementBuilder("c:if", targetDoc)
				.addAttribute("test", "${not empty items[0].reportable}")
				.addChild(new XmlElementBuilder("spring:message", targetDoc)
					.addAttribute("code", "jasperoo_reports")
					.addAttribute("htmlEscape", "false")
					.addAttribute("var", "reports_label")
					.build())
				.addChild(new XmlElementBuilder("b", targetDoc)
					.addChild(new XmlElementBuilder("c:out", targetDoc)
						.addAttribute("value", "${reports_label} ")
						.build())
					.build())
				.build())
			.build();
		target.appendChild(catchNode);		
			
		XmlUtils.writeXml(mutableDocXml.getOutputStream(), targetDoc);		
		
	}
	
	/**
	 * Modifies the "List" report template to add all of the fields in the 
	 * entity being listed.
	 * 
	 * @param javaType The JavaType of the entity being listed.
	 * @param entityName The name of the entity being listed.
	 */
	private void modifyShowTagx(String reportFormat){
		String docPath = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, 
				"WEB-INF/tags/form/show.tagx");

		MutableFile mutableDocXml = null;
		Document targetDoc;
		try {
			if (fileManager.exists(docPath)) {
				mutableDocXml = fileManager.updateFile(docPath);
				targetDoc = XmlUtils.getDocumentBuilder().parse(mutableDocXml.getInputStream());
			} else {
				throw new IllegalStateException("Could not acquire " + docPath);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element targetDiv = XmlUtils.findFirstElementByName("div", targetDoc.getDocumentElement());

		/*
		 *	<c:catch>
		 *	    <c:if test="${not empty object.reportable}">
		 *           <span>
		 *               <spring:url value="/reports/${fn:toLowerCase(typeName)}Detail/${itemId}/pdf" var="report_url" />
		 *               <spring:url value="/resources/images/report.png" var="report_image_url" />
		 *               <spring:message arguments="${typeName}" code="entity_detail_report" var="report_label" htmlEscape="false" />
		 *               <a href="${fn:escapeXml(report_url)}" alt="${fn:escapeXml(report_label)}" title="${fn:escapeXml(report_label)}">
		 *                 <img alt="${fn:escapeXml(report_label)}" class="image" src="${fn:escapeXml(report_image_url)}" title="${fn:escapeXml(report_label)}" />
		 *               </a>
		 *           </span>
		 *	    </c:if>
		 *   </c:catch>
		 */

		Element catchNode = new XmlElementBuilder("c:catch", targetDoc)
			.addChild(new XmlElementBuilder("c:if", targetDoc)
				.addAttribute("test", "${not empty object.reportable}")
				.addChild(new XmlElementBuilder("span", targetDoc)
					.build())
					.addChild(new XmlElementBuilder("spring:url", targetDoc)
						.addAttribute("value", "/reports/${fn:toLowerCase(typeName)}Detail/${itemId}/"+reportFormat)
						.addAttribute("var", "report_url")
						.build())
					.addChild(new XmlElementBuilder("spring:url", targetDoc)
						.addAttribute("value", "/resources/images/report-"+reportFormat+".png")
						.addAttribute("var", "report_image_url")
						.build())
					.addChild(new XmlElementBuilder("spring:message", targetDoc)
						.addAttribute("arguments", "${typeName}")
						.addAttribute("code", "entity_detail_report")
						.addAttribute("var", "report_label")
						.addAttribute("htmlEscape", "false")
						.build())
					.addChild(new XmlElementBuilder("a", targetDoc)
						.addAttribute("href", "${fn:escapeXml(report_url)}")
						.addAttribute("alt", "${fn:escapeXml(report_label)}")
						.addAttribute("title", "${fn:escapeXml(report_label)}")
						.addChild(new XmlElementBuilder("img", targetDoc)
							.addAttribute("class", "image")
							.addAttribute("alt", "${fn:escapeXml(report_label)}")
							.addAttribute("title", "${fn:escapeXml(report_label)}")
							.addAttribute("src", "${fn:escapeXml(report_image_url)}")
							.build())
						.build())
				.build())
			.build();

		targetDiv.appendChild(catchNode);		
		

		XmlUtils.writeXml(mutableDocXml.getOutputStream(), targetDoc);		
		
	}
	
	/**
	 * Manipulates the menu.jspx file to add the entries required for this report.
	 * 
	 * @param lowerCaseEntityName The name of the entity in question, in lower case.
	 */
	private void addMenuEntry(String lowerCaseEntityName) {

		// add jasperReportsMultiFormatView to applicationContext
		String menuJSPX = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/views/menu.jspx");

		MutableFile mutableMenuJSPX = null;
		Document menuDoc;
		try {
			if (fileManager.exists(menuJSPX)) {
				mutableMenuJSPX = fileManager.updateFile(menuJSPX);
				menuDoc = XmlUtils.getDocumentBuilder().parse(
						mutableMenuJSPX.getInputStream());
				if (menuDoc == null) {
					throw new IllegalStateException("Could not acquire menuDoc");
				}
			} else {
				throw new IllegalStateException("Could not acquire " + menuJSPX);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element menuItemPDF = new XmlElementBuilder("menu:item", menuDoc)
				.addAttribute("id", "i_" + lowerCaseEntityName + "_list_pdf")
				.addAttribute("messageCode", "global_menu_list")
				.addAttribute("url", "/reports/" + lowerCaseEntityName + "List/pdf").build();

		Element reportsNode = XmlUtils.findFirstElement("menu/category[@id = 'c_reports']", menuDoc.getDocumentElement());

		
		if (reportsNode == null) {
			reportsNode = new XmlElementBuilder("menu:category", menuDoc)
					.addAttribute("id", "c_reports")
					.addChild(menuItemPDF)
					.build();
			Element menu = XmlUtils.findFirstElementByName("menu:menu", menuDoc.getDocumentElement());
			menu.appendChild(reportsNode);
		} else {
			reportsNode.appendChild(menuItemPDF);
		}

		Element menuItemXLS = new XmlElementBuilder("menu:item", menuDoc)
				.addAttribute("id", "i_" + lowerCaseEntityName + "_list_xls")
				.addAttribute("messageCode", "global_menu_list")
				.addAttribute("url", "/reports/" + lowerCaseEntityName + "List/xls").build();

		reportsNode.appendChild(menuItemXLS);

		XmlUtils.writeXml(mutableMenuJSPX.getOutputStream(), menuDoc);

	}

	/**
	 * Modify application.properties by adding the i18n messages that apply to all reports.
	 */
	private void insertI18nSetupMessages() {
		String applicationProperties = pathResolver.getIdentifier(
				Path.SRC_MAIN_WEBAPP, "WEB-INF/i18n/application.properties");

		MutableFile mutableApplicationProperties = null;

		try {
			if (fileManager.exists(applicationProperties)) {
				mutableApplicationProperties = fileManager
						.updateFile(applicationProperties);
				String originalData = convertStreamToString(mutableApplicationProperties
						.getInputStream());

				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
						mutableApplicationProperties.getOutputStream()));

				out.write(originalData);
				out.write("label_ca_digitalface_jasperoo_report=Report\n");
				out.write("label_ca_digitalface_jasperoo_report_plural=Reports\n");
				out.write("label_ca_digitalface_jasperoo_detail=Detail\n");
				out.write("label_ca_digitalface_jasperoo_list=List\n");
				out.write("menu_category_reports_label=Reports\n");

				out.close();

			} else {
				throw new IllegalStateException("Could not acquire "
						+ applicationProperties);
			}
		} catch (Exception e) {
			System.out.println("---> " + e.getMessage());
			throw new IllegalStateException(e);
		}

		String messagesProperties = pathResolver.getIdentifier(
				Path.SRC_MAIN_WEBAPP, "WEB-INF/i18n/messages.properties");

		MutableFile mutableMessagesProperties = null;

		try {
			if (fileManager.exists(messagesProperties)) {
				mutableMessagesProperties = fileManager.updateFile(messagesProperties);
				String originalData = convertStreamToString(mutableMessagesProperties.getInputStream());

				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
						mutableMessagesProperties.getOutputStream()));

				out.write(originalData);
				out.write("\n\n");
				out.write("#reports\n");
				out.write("jasperoo_reports=Reports:\n");
				out.write("entity_list_report={0} List Report\n");
				out.write("entity_detail_report={0} Detail Report\n");

				out.close();

			} else {
				throw new IllegalStateException("Could not acquire "
						+ messagesProperties);
			}
		} catch (Exception e) {
			System.out.println("---> " + e.getMessage());
			throw new IllegalStateException(e);
		}


	}

	/**
	 * Modify application.properties by adding the i18n messages that apply to this report.
	 * 
	 * @param entityName The name of the entity upon which the report is based.
	 */
	private void insertI18nAddMessages(String entityName) {
		String applicationProperties = pathResolver.getIdentifier(
				Path.SRC_MAIN_WEBAPP, "WEB-INF/i18n/application.properties");

		MutableFile mutableApplicationProperties = null;

		String entityNamePlural = getPlural(entityName);

		try {
			if (fileManager.exists(applicationProperties)) {
				mutableApplicationProperties = fileManager
						.updateFile(applicationProperties);
				String originalData = convertStreamToString(mutableApplicationProperties
						.getInputStream());

				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
						mutableApplicationProperties.getOutputStream()));

				out.write(originalData);

				out.write("menu_item_" + entityName.toLowerCase()
						+ "_list_pdf_label=" + entityNamePlural + " (pdf)\n");
				out.write("menu_item_" + entityName.toLowerCase()
						+ "_list_xls_label=" + entityNamePlural + " (xls)\n");

				out.close();

			} else {
				throw new IllegalStateException("Could not acquire "
						+ applicationProperties);
			}
		} catch (Exception e) {
			System.out.println("---> " + e.getMessage());
			throw new IllegalStateException(e);
		}

	}

	/**
	 * The i18n messages that are displayed in reports are stored in a files 
	 * called "jasperoo.properties" in the "WEB-INF/classes" folder. This 
	 * method adds the requires messages for a given report. 
	 * 
	 * @param key The name of the key in question.
	 * @param value The value of that message.
	 */
	private void insertJasperooMessages(String key, String value) {
		String applicationProperties = pathResolver.getIdentifier(
				Path.SRC_MAIN_WEBAPP, "WEB-INF/classes/jasperoo.properties");

		MutableFile mutableApplicationProperties = null;

		try {
			if (fileManager.exists(applicationProperties)) {
				mutableApplicationProperties = fileManager
						.updateFile(applicationProperties);
				String originalData = convertStreamToString(mutableApplicationProperties
						.getInputStream());

				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
						mutableApplicationProperties.getOutputStream()));

				out.write(originalData);
				out.write(key + "=" + value + "\n");

				out.close();

			} else {
				throw new IllegalStateException("Could not acquire "
						+ applicationProperties);
			}
		} catch (Exception e) {
			System.out.println("---> " + e.getMessage());
			throw new IllegalStateException(e);
		}

	}

	/**
	 * A utility method to convert an input stream to a String.
	 * 
	 * @param source The source input stream.
	 * @return The source as a String, or an empty String if the source is null
	 * @throws IOException If unable to read stream.
	 */
	private String convertStreamToString(InputStream source) throws IOException {
		/*
		 * To convert the InputStream to String we use the Reader.read(char[]
		 * buffer) method. We iterate until the Reader return -1 which means
		 * there's no more data to read. We use the StringWriter class to
		 * produce the string.
		 */
		if (source != null) {
			Writer writer = new StringWriter();

			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(new InputStreamReader(source,
						"UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} finally {
				source.close();
			}
			return writer.toString();
		} else {
			return "";
		}
	}

	/**
	 * This method returns the plural of the term specified in English. 
	 * ATTENTION: this method does NOT take @RooPlural into account.
	 * 
	 * @param term
	 *            The term to be pluralized
	 * @return pluralized term
	 */
	private String getPlural(String term) {
		try {
			return Noun.pluralOf(term, Locale.ENGLISH);
		} catch (RuntimeException re) {
			// Inflector failed (see for example ROO-305), so don't pluralize it
			return term;
		}
	}

	/**
	 * Gets the MutableClassOrInterfaceTypeDetails needed to query the fields 
	 * available for the JavaType specified.
	 * 
	 * @param source The JavaType of the entity being queried.
	 * @return MutableClassOrInterfaceTypeDetails of the JavaType specified.
	 */
	private MutableClassOrInterfaceTypeDetails getJavaTypeDetails(JavaType source){
		MutableClassOrInterfaceTypeDetails result = null;
		// Retrieve metadata for the Java source type
		String fqEntityName = physicalTypeMetadataProvider.findIdentifier(source);
		if (fqEntityName == null) {
			throw new IllegalArgumentException("Cannot locate source for '"
					+ source.getFullyQualifiedTypeName() + "'");
		}

		// Obtain the physical type and itd mutable details
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService
				.get(fqEntityName);
				
		Assert.notNull(physicalTypeMetadata,
				"Java source code unavailable for type "
						+ PhysicalTypeIdentifier.getFriendlyName(fqEntityName));

		// Obtain physical type details for the target type
		PhysicalTypeDetails physicalTypeDetails = physicalTypeMetadata.getMemberHoldingTypeDetails();

		Assert.notNull(physicalTypeDetails,
				"Java source code details unavailable for type "
						+ PhysicalTypeIdentifier.getFriendlyName(fqEntityName));
		
		// Test if the type is an MutableClassOrInterfaceTypeDetails
		Assert.isInstanceOf(MutableClassOrInterfaceTypeDetails.class,
				physicalTypeDetails, "Java source code is immutable for type "
						+ PhysicalTypeIdentifier.getFriendlyName(fqEntityName));
		
		result = (MutableClassOrInterfaceTypeDetails) physicalTypeDetails;
		
		return result;
	}
	

}