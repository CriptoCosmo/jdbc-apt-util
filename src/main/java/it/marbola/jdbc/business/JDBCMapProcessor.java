package it.marbola.jdbc.business;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.*;
import it.marbola.jdbc.annotation.Field;
import it.marbola.jdbc.annotation.JDBCMap;
import org.springframework.jdbc.core.RowMapper;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.function.Predicate;

import static java.text.MessageFormat.format;

public class JDBCMapProcessor {

	private static final String DOT = "\\.";

	protected JDBCMapProcessor() {
		throw new AssertionError("Method not implemented");
	}

	public static void process(Elements elementsUtil, Filer filer, Set<? extends Element> elements) {

		Predicate<? super Element> predicate =
			(Element element) -> element.getKind() == ElementKind.CLASS;

		elements.stream()
			.filter(predicate)
			.forEach((Element element) -> process(elementsUtil, filer, element));

	}

	public static void process(Elements elementsUtil, Filer filer, Element modelEntity) {


		String className = modelEntity.getSimpleName().toString();
		String packageName = elementsUtil
				.getPackageOf(modelEntity)
				.getQualifiedName()
				.toString() + ".rowmappers.impl";

		Field fieldAnnotation = modelEntity.getAnnotation(Field.class);
		JDBCMap JDBCMapAnnotation = modelEntity.getAnnotation(JDBCMap.class);

		if(JDBCMapAnnotation != null && !JDBCMapAnnotation.packageName().equals("")){
			packageName = JDBCMapAnnotation.packageName();
		}

		TypeName typeReturn = TypeName.get(modelEntity.asType());

		//
	 	// CREAZIONE METODO "mapRow"
	 	//

		MethodSpec.Builder builderMethod = MethodSpec.methodBuilder("mapRow")
			.addModifiers(Modifier.PUBLIC)
			.returns(typeReturn)
			.addParameter(ResultSet.class, "resultSet")
			.addParameter(int.class, "row")
			.addStatement("$T model = new $T()", typeReturn, typeReturn)
			;

		//
		// GENERAZIONE STATEMENT SETTER DINAMICO PER OGNI FIELD
		//

		Predicate<? super Element> predicate = (Element element) ->
				element.getKind() == ElementKind.FIELD &&
				!element.getModifiers().contains(Modifier.FINAL);

		modelEntity
			.getEnclosedElements()
			.stream()
			.filter(predicate)
			.forEach((Element element) -> {

				CaseFormat caseFormat = null;
				if(fieldAnnotation != null){
					caseFormat = fieldAnnotation.caseFormat();
					System.out.println(caseFormat);
				}

				String statement = generateSetterForFiled(element, caseFormat);
				builderMethod.addStatement(statement);

			});

		builderMethod
			.addException(SQLException.class)
			.addStatement("return model");

		TypeSpec classImplementation = TypeSpec.classBuilder(className + "RowMapper")
				.addModifiers(Modifier.PUBLIC)
				.addMethod(builderMethod.build())
				.addSuperinterface(ParameterizedTypeName.get(ClassName.get(RowMapper.class), typeReturn))
				.build();

		JavaFile javaFile = JavaFile
				.builder(packageName, classImplementation)
				.build();

		//
		// BUILD DELLA CLASSE GENERATA
		//

		try {
			javaFile.writeTo(filer);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static String generateSetterForFiled(Element element,
												 CaseFormat parentDefinedCase) {

		String fieldClassType = getFieldClassType(element);
		String fieldSimpleName = element.toString();
		String setterName = String.valueOf(fieldSimpleName.charAt(0)).toUpperCase() + fieldSimpleName.substring(1);
		String dbFiledCase = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, element.toString());

		//
		//	OVERRIDE db-filed-case by Field annotation logic defined
		//

		Field annotation = element.getAnnotation(Field.class);

		if(annotation != null){
			System.out.println("find annotation ");
			if (!annotation.name().equals("")){
				dbFiledCase = annotation.name();
			} else {
				System.out.println("find annotation CaseFormat"+annotation.caseFormat());
				dbFiledCase = CaseFormat.LOWER_CAMEL.to(
					annotation.caseFormat(),
					element.toString()
				);
			}

		} else if(parentDefinedCase != null){
			System.out.println("find parent annotation "+ parentDefinedCase);
			dbFiledCase = CaseFormat.LOWER_CAMEL.to(
				parentDefinedCase,
				element.toString()
			);
		}else{
			System.out.println("no annotation found set default");
		}

		return format("model.set{0}(resultSet.get{1}(\"{2}\"))", setterName, fieldClassType, dbFiledCase);
	}

	private static String getFieldClassType(Element element) {

		String simpleName = String.valueOf(element.asType());

		switch (simpleName) {
			case "java.lang.Integer": {
				simpleName = "Int";
				break;
			}
			case "java.lang.Byte":
			case "java.sql.Time":
			case "java.sql.Timestamp":
			case "java.lang.Short":
			case "java.lang.Long":
			case "java.lang.Float":
			case "java.lang.Double":
			case "java.lang.Boolean":
			case "java.lang.String":
			case "java.math.BigDecimal":
			case "java.lang.Object":{
				String[] split = simpleName.split(DOT);
				simpleName = split[split.length - 1];
				break;
			}
			default: {
				throw new AssertionError(format("FieldType \"{0}\" not supported", simpleName));
			}
		}

		return simpleName;
	}

}
