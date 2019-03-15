package it.marbola.jdbc.processor;

import com.google.auto.service.AutoService;
import it.marbola.jdbc.business.JDBCMapProcessor;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.Set;

import static java.text.MessageFormat.format;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes({"it.marbola.jdbc.annotation.JDBCMap"})
public class AnnotationProcessor extends AbstractProcessor {

	private Elements elements = null;
	private Filer filer = null;

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		elements = processingEnv.getElementUtils();
		filer = processingEnv.getFiler();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {

		annotations.iterator().forEachRemaining((TypeElement annotationElement) -> {

			Set<? extends Element> elementsAnnotated =
					roundEnvironment.getElementsAnnotatedWith(annotationElement);

			switch (String.valueOf(annotationElement)) {
				case "it.marbola.jdbc.annotation.JDBCMap": {
					JDBCMapProcessor.process(elements,filer,elementsAnnotated);
					break;
				}
				default:{
					System.out.println(format("No Processor " +
									"found for annotation \"{0}\" ",
							String.valueOf(annotationElement)));
				}
			}
		});

		return true;
	}

}

