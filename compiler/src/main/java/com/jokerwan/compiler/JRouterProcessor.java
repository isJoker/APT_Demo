package com.jokerwan.compiler;

import com.google.auto.service.AutoService;
import com.jokerwan.annotation.JRouter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * Created by JokerWan on 2019-12-19.
 * Function:
 */
// 通过auto-service中的@AutoService可以自动生成AutoService注解处理器
// 用来生成 META-INF/services/javax.annotation.processing.Processor 文件
@AutoService(Processor.class)
// 允许/支持的注解类型，让注解处理器处理（新增annotation module）
@SupportedAnnotationTypes({"com.jokerwan.annotation.JRouter"})
// 指定JDK编译版本
@SupportedSourceVersion(SourceVersion.RELEASE_8)
// 注解处理器接收的参数
@SupportedOptions("content")
public class JRouterProcessor extends AbstractProcessor {

    // 操作Element工具类 (类、函数、属性都是Element)
    private Elements elementUtils;

    // type(类信息)工具类，包含用于操作TypeMirror的工具方法
    private Types typeUtils;

    // Messager用来报告错误，警告和其他提示信息
    private Messager messager;

    // 文件生成器 类/资源，Filter用来创建新的源文件，class文件以及辅助文件
    private Filer filer;

    /**
     * 初始化
     *
     * @param processingEnvironment 通过该方法的参数ProcessingEnvironment可以获取一些有用的工具类
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        elementUtils = processingEnvironment.getElementUtils();
        messager = processingEnvironment.getMessager();
        filer = processingEnvironment.getFiler();
        typeUtils = processingEnvironment.getTypeUtils();
        // 通过ProcessingEnvironment去获取build.gradle传过来的参数
        String content = processingEnvironment.getOptions().get("content");
        // 有坑：Diagnostic.Kind.ERROR，异常会自动结束，这里打印日志用Diagnostic.Kind.NOTE
        messager.printMessage(Diagnostic.Kind.NOTE, content);
    }

    /**
     * 处理具体的注解，生成Java文件
     *
     * @param set              使用了支持处理注解的节点集合（被注解标记的类的集合）
     * @param roundEnvironment 当前或是之前的运行环境,可以通过该对象查找找到的注解
     * @return true 表示已经处理完成，后续处理器不会再处理
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (set.isEmpty()) {
            return false;
        }

        // 获取所有带JRouter注解的 类节点
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(JRouter.class);
        // 遍历所有类节点
        for (Element element : elements) {
            // 通过类节点获取包节点（全路径：com.jokerwan.xxx）
            String packageName = elementUtils.getPackageOf(element).getQualifiedName().toString();
            // 获取简单类名
            String className = element.getSimpleName().toString();
            messager.printMessage(Diagnostic.Kind.NOTE, "被注解的类有：" + className);
            // 最终想生成的类文件名
            String finalClassName = className + "$$ARouter";

            // -------------------- 方式一：手动生成java代码 --------------------------
            // 创建一个新的源文件（Class），并返回一个对象以允许写入它
            /*try {
                JavaFileObject sourceFile = filer.createSourceFile(packageName + "." + finalClassName);
                // 定义Writer对象，开启写入
                Writer writer = sourceFile.openWriter();
                writer.write("package " + packageName + ";\n");
                writer.write("public class " + finalClassName + " {\n");
                writer.write("public static Class<?> findTargetClass(String path) {\n");
                // 获取类之上的@JRouter注解
                JRouter jRouter = element.getAnnotation(JRouter.class);
                writer.write("if (path.equals(\""+ jRouter.path() +"\")) {\n");
                writer.write("return " + className + ".class;\n}\n");
                writer.write("return null;\n");
                writer.write("}\n}");

                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }*/


            // -------------------- 方式二：通过JavaPoet自动生成java代码 --------------------------
            // 获取类之上@ARouter注解的path值
            JRouter jRouter = element.getAnnotation(JRouter.class);
            // 构建方法体
            MethodSpec methodSpec = MethodSpec.methodBuilder("findTargetClass")// 方法名
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)// 修饰符
                    .addParameter(String.class, "path")// 参数
                    .addStatement("return path.equals($S) ? $T.class : null",
                            jRouter.path(), ClassName.get((TypeElement) element))// 代码语句拼接
                    .returns(Class.class)// 返回值类型
                    .build();
            // 构建类
            TypeSpec typeSpec = TypeSpec.classBuilder(finalClassName)
                    .addModifiers(Modifier.PUBLIC)
                    .addMethod(methodSpec)// 添加方法体
                    .build();
            // 在指定的包名下，生成Java类文件
            JavaFile javaFile = JavaFile.builder(packageName, typeSpec).build();
            try {
                javaFile.writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}
