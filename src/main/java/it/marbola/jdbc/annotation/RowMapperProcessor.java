package it.marbola.jdbc.annotation;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import org.springframework.jdbc.core.RowMapper;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.text.MessageFormat.format;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes("it.marbola.jdbc.annotation.MapRow")
public class RowMapperProcessor extends AbstractProcessor {

	private Messager messager = null;
	private Elements elements = null;
	private Filer filer = null;

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		messager = processingEnv.getMessager();
		elements = processingEnv.getElementUtils();
		filer = processingEnv.getFiler();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {

		for (Element annotatedInterfaes : roundEnvironment.getElementsAnnotatedWith(MapRow.class)) {

			if (annotatedInterfaes.getKind() != ElementKind.INTERFACE) {
				continue;
			}

			// Recupero attributi
			messager.printMessage(Diagnostic.Kind.NOTE,"Recupero attributi");

			String className = annotatedInterfaes.getSimpleName().toString();
			String packageName = elements
					.getPackageOf(annotatedInterfaes)
					.getQualifiedName()
					.toString();

			TypeMirror returnType = getGenericFromInterface(annotatedInterfaes);
			TypeName returnTypeName = TypeName.get(returnType);

			// Creazione metodo
			messager.printMessage(Diagnostic.Kind.NOTE,"Creazione metodo");

			MethodSpec.Builder builderMethod = MethodSpec.methodBuilder("mapRow")
					.addModifiers(Modifier.PUBLIC)
					.returns(returnTypeName)
					.addParameter(ResultSet.class, "resultSet")
					.addParameter(int.class, "row")
					.addStatement("$T model = new $T()", returnTypeName, returnTypeName);

			// Generazione setter dinamico
			messager.printMessage(Diagnostic.Kind.NOTE,"Generazione setter dinamico");

			Optional<? extends Element> modelEntityOptional = roundEnvironment
					.getRootElements()
					.stream()
					.filter((Element x) -> x.toString().equals(returnType.toString()))
					.limit(1)
					.findAny();

			if (modelEntityOptional.isPresent()) {
				Element modelEntity = modelEntityOptional.get();

				builderMethod.beginControlFlow("try");

				for (Element enclosedElement : modelEntity.getEnclosedElements()) {

					if (enclosedElement.getKind() != ElementKind.FIELD) {
						continue;
					}

					messager.printMessage(Diagnostic.Kind.NOTE,"Generazione enclosedElement dinamico");

					TypeName fieldtypeName = TypeName.get(enclosedElement.asType());
					String[] split = fieldtypeName.toString().split("\\.");
					String action = split[split.length - 1];
					String fieldtypeNameString = null;

					switch (action) {
						case "Integer": {
							fieldtypeNameString = "Int";
							break;
						}
						case "Byte":
						case "Time":
						case "Timestamp":
						case "Short":
						case "Long":
						case "Float":
						case "Double":
						case "Boolean":
						case "String":
						case "BigDecimal":
						case "Object": {
							fieldtypeNameString = action;
							break;
						}
					}

					String fieldSimpleName = enclosedElement.toString();
					String setterName = String.valueOf(fieldSimpleName.charAt(0)).toUpperCase() + fieldSimpleName.substring(1);
					String statement = format("model.set{0}(resultSet.get{1}(\"{2}\"))",
							setterName, fieldtypeNameString,
							fieldSimpleName.toUpperCase());

					builderMethod.addStatement(statement);
				}
			}

			builderMethod.nextControlFlow("catch ($T e)", SQLException.class)
					.addStatement("e.printStackTrace()", RuntimeException.class)
					.endControlFlow()
					.addStatement("return model");

			TypeSpec classImplementation = TypeSpec.classBuilder(className + "Impl")
					.addModifiers(Modifier.PUBLIC)
					.addMethod(builderMethod.build())
					.addSuperinterface(ParameterizedTypeName.get(ClassName.get(RowMapper.class), returnTypeName))
					.build();

			JavaFile javaFile = JavaFile
					.builder(packageName + ".impl", classImplementation)
					.build();

			// Build della classe generata
			try {
				javaFile.writeTo(filer);

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		return true;
	}

	private synchronized TypeMirror getGenericFromInterface(Element element) {
		if (element instanceof TypeElement) {

			TypeElement typeElement = (TypeElement) element;

			List<? extends TypeParameterElement> typeParameters = typeElement.getTypeParameters();

			for (TypeMirror typeMirror : typeElement.getInterfaces()) {
				if (typeMirror instanceof DeclaredType) {
					DeclaredType declaredType = (DeclaredType) typeMirror;
					for (TypeMirror argument : declaredType.getTypeArguments()) {
						return argument;
					}
				}
			}
		}
		return null;
	}

}

