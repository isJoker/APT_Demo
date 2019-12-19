# APT_Demo



## 1 什么是APT
 **APT(Annotation Processing Toll)** ，是一种处理注解的工具，它对源代码文件进行检测找出其中的Annotation，根据注解自动生成代码，如果想要自定义的注解处理器能够正常运行，必须要通过APT工具进行处理。简单来说就是在编译期，通过注解生成 .java 文件。

APT也是 **AOP(Aspect-Oriented Programming)** 的一种实现方式。

使用到的APT的框架：DataBinding、Dagger2、ButterKnife、EventBus3、ARouter等

## 2 什么是Annotation
### 2.1 注解分为三类
* 标准 Annotation

包括 Override, Deprecated, SuppressWarnings，是java自带的几个注解，他们由编译器来识别，不会进行编译， 不影响代码运行

* 元 Annotation

@Retention, @Target, @Inherited, @Documented，它们是用来定义 Annotation 的 Annotation。
* 自定义 Annotation

 使用@interface自定义注解时，自动继承了java.lang.annotation.Annotation接口，由编译程序自动完成其他细节。在定义注解时，不能继承其他的注解或接口。@interface用来声明一个注解，其中的每一个方法实际上是声明了一个配置参数。方法的名称就是参数的名称，返回值类型就是参数类型（只能是基本类型、Class、String、enum）。可以通过default来声明参数的默认值。
 
### 2.2 自定义的注解也分为三类
* @Retention(RetentionPolicy.SOURCE)

源码时注解，一般用来作为编译器标记。如Override, Deprecated, SuppressWarnings。

* @Retention(RetentionPolicy.RUNTIME)

运行时注解，在运行时通过反射去识别的注解。运行时注解的实质是，在代码中通过注解进行标记，运行时通过反射寻找标记进行某种处理。而运行时注解中使用到了反射，对性能会有一定的影响。

* @Retention(RetentionPolicy.CLASS)

编译时注解，在编译时被识别并处理的注解。编译时按照一定策略生成代码，避免编写重复代码，提高开发效率，且不影响性能。

## 3 APT相关API
### 3.1 AbstractProcessor
![](https://upload-images.jianshu.io/upload_images/9513946-624fd21c4233ef06.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


其中五个比较常用的方法
#### 3.1.1 init(ProcessingEnvironment): void
该方法主要用于一些初始化的操作，通过该方法的参数`ProcessingEnvironment`可以获取一些有用的工具类，如
* Elements： 操作Element工具类 (类、函数、属性都是Element)
* Types：type(类信息)工具类，包含用于操作TypeMirror的工具方法
* Messager：用来报告错误，警告和其他提示信息
* Filer：文件生成器 类/资源，Filter用来创建新的源文件，class文件以及辅助文件

#### 3.1.2 process(Set< ? extends TypeElement >,RoundEnvironment): boolean
注解处理器的核心方法，处理具体的注解，生成Java文件。
参数`set`：使用了支持处理注解的节点集合（被注解标记的类的集合）。
参数`roundEnvironment`：当前或是之前的运行环境,可以通过该对象查找找到的注解。
返回值：true 表示已经处理完成，后续处理器不会再处理

#### 3.1.3 getSupportedSourceVersion(): SourceVersion
指定JDK编译版本

#### 3.1.4 getSupportedAnnotationTypes(): Set< String >
指定这个注解处理器允许/支持的注解类型

#### 3.1.5 getSupporteOptions(): Set< String >
注解处理器接收的参数

#### 3.1.6 后面的三个方法可以通过注解的形式完成
```
// AutoService则是固定的写法，加个注解即可
// 通过auto-service中的@AutoService可以自动生成AutoService注解处理器，用来注册
// 用来生成 META-INF/services/javax.annotation.processing.Processor 文件
@AutoService(Processor.class)
// 允许/支持的注解类型，让注解处理器处理
@SupportedAnnotationTypes({"com.jokerwan.annotation.JRouter"})
// 指定JDK编译版本
@SupportedSourceVersion(SourceVersion.RELEASE_8)
// 注解处理器接收的参数
@SupportedOptions("content")
public class ARouterProcessor extends AbstractProcessor {

}
```
## 4 APT练手demo实现ARouter生成路由辅助类
### 4.1 新建名为annotation的java library
定义标记类的路由地址的注解`JRouter`
```
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface JRouter {

    /**
     * 详细路由路径（必填），如："/app/MainActivity"
     */
    String path();

    /**
     * 路由组名（选填，如果开发者不填写，可以从path中截取出来）
     */
    String group() default "";
}
```
该注解是作用在类之上，所以Target是`ElementType.TYPE`，我们想要通过该注解在编译时生成一些辅助路由的文件，所以Retention是`RetentionPolicy.CLASS`，定义了注解的两个参数`path`和`group`，前者必填，后者选填，后者可截取一级路径，即第一个"/"和第二个"/"直接的路径名。

### 4.2 模拟APT生成后的代码`XXXActivity$$ARouter`
```
public class XXXActivity$$ARouter {

    public static Class<?> findTargetClass(String path) {
        if (path.equals("/app/MainActivity")) {
            return MainActivity.class;
        }
        return null;
    }
}
```
此类是我们要通过APT生成的代码，类中有一个`findTargetClass(String)`方法，通过传入path获取到对应的Class。

### 4.3 新建名为compiler的java library
#### 4.3.1 修改`build.gradle`文件如下
```
apply plugin: 'java-library'

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])

    compileOnly'com.google.auto.service:auto-service:1.0-rc4'
    annotationProcessor'com.google.auto.service:auto-service:1.0-rc4'

    // 引入annotation，让注解处理器-处理注解
    implementation project(':annotation')
}

// java控制台输出中文乱码
tasks.withType(JavaCompile) {
    options.encoding = "UTF-8"
}

// jdk编译版本1.8
sourceCompatibility = "8"
targetCompatibility = "8"
```
这里引用 auto-service的目的是注册注解，并对其生成META-INF的配置信息，需要注意的是这里引用 auto-service 和 gradle 版本号有坑，编译的时候会出错，目前没有问题的两种版本号配置对应如下
* As-3.2.1 + gradle4.10.1-all + auto-service:1.0-rc2
* As-3.4.1 + gradle5.1.1-all + auto-service:1.0-rc4

#### 4.3.2 实现注解处理器
两种方式
* 手动生成java代码
* 通过JavaPoet自动生成java代码（推荐，不易出错）

#####方式一：手动生成java代码
```
// 通过auto-service中的@AutoService可以自动生成AutoService注解处理器
// 用来生成 META-INF/services/javax.annotation.processing.Processor 文件
@AutoService(Processor.class)
// 允许/支持的注解类型，让注解处理器处理
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

            // 创建一个新的源文件（Class），并返回一个对象以允许写入它
            try {
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
            }

        }
        return true;
    }
}
```
通过手动方式生成java文件需要严格按照java文件中的格式，注意空格、分号、换行，大部分编译不通过都是在写java文件的时候一些小细节出错了。

#####方式二：通过JavaPoet自动生成java代码
在compiler模块的build.gradle中添加依赖JavaPoet依赖
```
implementation "com.squareup:javapoet:1.10.0"
```
注解处理器代码如下
```
// 通过auto-service中的@AutoService可以自动生成AutoService注解处理器
// 用来生成 META-INF/services/javax.annotation.processing.Processor 文件
@AutoService(Processor.class)
// 允许/支持的注解类型，让注解处理器处理
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

            // 获取类之上@Router注解的path值
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

```

#### 4.3.3 app模块分别依赖annotation和compiler模块
修改app模块build.gradle文件如下
```
apply plugin: 'com.android.application'

android {
    compileSdkVersion 29
    buildToolsVersion "29.0.2"
    defaultConfig {
        applicationId "com.jokerwan.apt_demo"
        minSdkVersion 21
        targetSdkVersion 29
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"

        // 在gradle文件中配置选项参数值（用于APT传参接收）
        // 切记：必须写在defaultConfig节点下
        javaCompileOptions {
            annotationProcessorOptions {
                arguments = [content : 'jokerwan apt']
            }
        }
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation 'androidx.appcompat:appcompat:1.1.0'
    implementation 'androidx.constraintlayout:constraintlayout:1.1.3'
    testImplementation 'junit:junit:4.12'
    androidTestImplementation 'androidx.test:runner:1.2.0'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.2.0'

    // 依赖注解
    implementation project(':annotation')
    // 依赖注解处理器
    annotationProcessor project(':compiler')

}

```
注意`javaCompileOptions`这里是给注解处理器`JRouterProcessor`上的注解`@SupportedOptions("content")`传递参数

rebuild一下工程，Build成功之后会打印如下信息
![](https://upload-images.jianshu.io/upload_images/9513946-65b36bed00dba70d.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


可以看到我们通过`messager`打印的信息已经出来了，一个是我们再app模块的build.gradle中传入的`jokerwan apt`，接着打印了被注解的类

看一下APT生成的类文件路径：
```
app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/com/jokerwan/apt_demo/MainActivity$$ARouter.class
```
打开class文件，Android Studio已经帮我们反编译为java文件
![](https://upload-images.jianshu.io/upload_images/9513946-d4eb8f3b446625ad.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


可以看到，跟我们之前模拟的类一样，通过该类调用`findTargetClass(String)`方法传入标记在类上的注解中的path，就可以拿到该类的Class对象，由于是在编译器生成类，不会对性能产生影响。

demo地址如下
[https://github.com/isJoker/APT_Demo](https://github.com/isJoker/APT_Demo)
