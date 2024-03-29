/*
 * Copyright © 2011 - DigitalFace.ca. All Rights Reserved
 * Licensed under the GNU GENERAL PUBLIC LICENSE Version 3, 29 June 2007 (http://www.gnu.org/licenses/gpl.txt).
 */
package ca.digitalface.jasperoo;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * This type produces metadata for a new ITD. It uses an {@link ItdTypeDetailsBuilder} provided by 
 * {@link AbstractItdTypeDetailsProvidingMetadataItem} to register a field in the ITD and a new method.
 * 
 * @since 1.1.0
 */
public class JasperooMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	private static final String PROVIDES_TYPE_STRING = JasperooMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

	
	
	public JasperooMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");

		
		// Adding a new sample method definition
		builder.addMethod(getReportableMethod());
		
		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}
	
	/**
	 * Create metadata for a method definition. 
	 *
	 * @return a MethodMetadata object
	 */
	private MethodMetadata getReportableMethod() {
		// Specify the desired method name
		JavaSymbolName methodName = new JavaSymbolName("isReportable");
		
		// Check if a method with the same signature already exists in the target type
		MethodMetadata method = methodExists(methodName, new ArrayList<AnnotatedJavaType>());
		if (method != null) {
			// If it already exists, just return the method and omit its generation via the ITD
			return method;
		}
		
		// Define method annotations (none in this case)
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		
		// Define method throws types (none in this case)
		List<JavaType> throwsTypes = new ArrayList<JavaType>();
		
		// Define method parameter types (none in this case)
		List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
		
		// Define method parameter names (none in this case)
		List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
		
		// Create the method body
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
//		bodyBuilder.appendFormalLine("reportable = true;");
		bodyBuilder.appendFormalLine("return true;");
		
		// Use the MethodMetadataBuilder for easy creation of MethodMetadata
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.BOOLEAN_PRIMITIVE, parameterTypes, parameterNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		methodBuilder.setThrowsTypes(throwsTypes);
		
		return methodBuilder.build(); // Build and return a MethodMetadata instance
	}
		
	private MethodMetadata methodExists(JavaSymbolName methodName, List<AnnotatedJavaType> paramTypes) {
		// We have no access to method parameter information, so we scan by name alone and treat any match as authoritative
		// We do not scan the superclass, as the caller is expected to know we'll only scan the current class
		for (MethodMetadata method : governorTypeDetails.getDeclaredMethods()) {
			if (method.getMethodName().equals(methodName) && method.getParameterTypes().equals(paramTypes)) {
				// Found a method of the expected name; we won't check method parameters though
				return method;
			}
		}
		return null;
	}
	
	// Typically, no changes are required beyond this point
	
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", valid);
		tsc.append("aspectName", aspectName);
		tsc.append("destinationType", destination);
		tsc.append("governor", governorPhysicalTypeMetadata.getId());
		tsc.append("itdTypeDetails", itdTypeDetails);
		return tsc.toString();
	}

	public static final String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}
	
	public static final String createIdentifier(JavaType javaType, Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static final JavaType getJavaType(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static final Path getPath(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}
