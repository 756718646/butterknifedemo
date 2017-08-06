package com.example;

import com.google.auto.service.AutoService;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;

/**
 *
 * 使用AutoService进行注解，这样系统就能够找到这个 Processor ，
 * 并在编译时对注解进行预处理。
 *
 * 在as中,如果要调试,Build-ReBuild Project,这样每次会执行这生成代码
 * 否则他生成一次后,没有改动,就不会重复执行代码生成过程
 * Created by yongzheng on 17-7-31.
 */
@AutoService(Processor.class)
public class ButterKnifeProcess extends AbstractProcessor {

    //用于生成java源文件
    private Filer filer;

    //下一行
    private static final String NEXT_LINE = "\n";

    //tab键
    private static final String TAB = "\t";

    //2个tab
    private static final String TWOTAB = "\t\t";

    /**
     * 初始化时候,获取fileer
     * @param processingEnvironment
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
    }

    /**
     * 点击Rebuild Project可以重新执行改方法,重新生成代码
     * 流程:
     *      1,返回Set集合,我们需要先整理注解
     *        找到把某个类里面的注解元素放在一起
     *      2,cacheMap就是整理完成后的结构,key是类名,value是该类下面的BindView注解
     *      3,遍历cacheMap,根据类名来生成viewbind
     *
     * @param set
     * @param roundEnvironment
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        System.out.println("--------------ButterKnifeProcess is run,yongzheng ok:"+set.size());

        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(BindView.class);
        //分类(Activity的name为key,元素为value)为了得到Activity里面的BindView注解
        Map<String,List<VariableElement>> cacheMap = new HashMap<>();
        for (Element element : elements){
            //这里我们知道elements都是变量的注解
            VariableElement variableElement = (VariableElement) element;
            String tagClass = getTagClassName(variableElement);
            List<VariableElement> arr = cacheMap.get(tagClass);
            if (arr==null){
                arr = new ArrayList<>();
                cacheMap.put(tagClass,arr);
            }
            arr.add(variableElement);
            System.out.println("---------VariableElement :"+variableElement.getSimpleName().toString());
        }

        //遍历每一个class,生成对应的viewbinder
        Iterator<String> iterator = cacheMap.keySet().iterator();
        while (iterator.hasNext()){

            //需要生成viewbinder 的 class
            String tagClassName = iterator.next();

            //该类的所有bindview注解
            List<VariableElement> fieldElements = cacheMap.get(tagClassName);

            //类名
            String simpleTagName = getSimpleTagClassName(fieldElements.get(0));
            //生成viewbinder 的类名
            String newBinder = simpleTagName + "_ViewBinding";
            //包名
            String packageName = getPackageName(fieldElements.get(0));

            Writer writer = null;
            try {
                JavaFileObject javaFileObject = filer.createSourceFile(newBinder);
                //使用这个writer来写一个java文件
                writer=javaFileObject.openWriter();

                //头部
                createHead(tagClassName,packageName,writer,newBinder);
                //构造函数
                createFindById(newBinder,tagClassName,writer,fieldElements);
                //unbind方法签名
                createUnBind(simpleTagName,writer,fieldElements);
                //结束
                createEnd(writer);

                writer.close();
            }catch (Exception e){
                System.out.println(e.toString());
            }
        }
        //配置完成
        return false;
    }

    /**
     * 结束
     * @param writer
     * @throws IOException
     */
    private void createEnd(Writer writer) throws IOException {
        writer.write("}");
        writer.write(NEXT_LINE);
    }

    /**
     * 创建头部(包名,导入包,类名,成员变量)
     * @param tagClassName
     * @param packageName
     * @param writer
     * @param newBinder
     * @throws IOException
     */
    private void createHead(String tagClassName, String packageName, Writer writer,
                            String newBinder) throws IOException {
        //先创建空类,类名,造车头
        //自己一行一行写java文件
        writer.write("package "+packageName+";");
        writer.write(NEXT_LINE);
        //导入包
        writer.write("import android.view.View;");
        writer.write(NEXT_LINE);
        writer.write("import com.example.Unbinder;");
        writer.write(NEXT_LINE);
        writer.write(NEXT_LINE);
        //类
        writer.write("public class "+newBinder+" implements Unbinder{");
        writer.write(NEXT_LINE);
        writer.write(NEXT_LINE);
        //写入target成员变量
        writer.write(TAB);
        writer.write("private "+tagClassName+" target;");
        writer.write(NEXT_LINE);
        writer.write(NEXT_LINE);
    }

    /**
     * 写unbind方法
     * @param simpleTagName
     * @param writer
     * @param fieldElements
     */
    private void createUnBind(String simpleTagName, Writer writer,
                              List<VariableElement> fieldElements) throws IOException {
        //方法签名
        writer.write(NEXT_LINE);
        writer.write(TAB);
        writer.write("@Override");
        writer.write(NEXT_LINE);
        writer.write(TAB);
        writer.write("public void unbind() {");
        writer.write(NEXT_LINE);
        //方法内容开始
        //MainActivity target = this.target;
        writer.write(TWOTAB);
        writer.write(simpleTagName+" target = this.target;");
        writer.write(NEXT_LINE);

        writer.write(TWOTAB);
        writer.write("if (target == null) throw new IllegalStateException(\"Bindings already cleared.\");");
        writer.write(NEXT_LINE);
        writer.write(TWOTAB);
        writer.write("this.target = null;");
        writer.write(NEXT_LINE);

        for (VariableElement item : fieldElements){
            //变量的名字
            String fieldName = item.getSimpleName().toString();
            writer.write(TWOTAB);
            writer.write("target."+fieldName+" = null;");
            writer.write(NEXT_LINE);
        }
        //结束
        writer.write(TAB);
        writer.write("}");
    }

    /**
     * 写findViewById
     * @param newBinder
     * @param tagClassName
     * @param writer
     * @param fieldElements
     */
    private void createFindById(String newBinder,String tagClassName, Writer writer,
                                List<VariableElement> fieldElements) throws IOException {
        //方法签名
        writer.write(TAB);
        writer.write("public "+newBinder+"("+tagClassName+" target, View source) {");
        writer.write(NEXT_LINE);

        //内容开始
        writer.write(TWOTAB);
        writer.write("this.target = target;");
        writer.write(NEXT_LINE);
        //arget.text = (android.widget.TextView) source.findViewById(R.id.text);
        for (VariableElement item : fieldElements){
            BindView bindView = item.getAnnotation(BindView.class);
            //获取变量的名字
            String fieldName = item.getSimpleName().toString();
            //获取变量的类型
            TypeMirror type = item.asType();
            //获取资源id
            int resId = bindView.value();
            writer.write(TWOTAB);
            writer.write("target."+fieldName+" = ("+type.toString()+") source.findViewById("+resId+");");
            writer.write(NEXT_LINE);
        }
        writer.write(TAB);
        writer.write("}");
        writer.write(NEXT_LINE);
    }

    /**
     * 获取仅类名(不带包名)
     * @param variableElement
     * @return
     */
    private String getSimpleTagClassName(VariableElement variableElement) {
        Element typeElement = variableElement.getEnclosingElement();
        return typeElement.getSimpleName().toString();
    }

    /**
     * 根据元素找到所在的完整类名(带包名的)
     * @param variableElement
     * @return
     */
    private String getTagClassName(VariableElement variableElement) {
        String packageName = getPackageName(variableElement);
        Element typeElement = variableElement.getEnclosingElement();
        return packageName+"."+typeElement.getSimpleName().toString();
    }

    /**
     * 这里VariableElement除了拥有Element的方法以外还有以下两个方法
     * getConstantValue     变量初始化的值
     * getEnclosingElement  获取相关类信息
     */

    /**
     * 获取包名
     * @param variableElement
     * @return
     */
    private String getPackageName(VariableElement variableElement) {
        //获取类信息
        TypeElement typeElement = (TypeElement) variableElement.getEnclosingElement();
        //获取包名
        String packageName = processingEnv.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();
        return packageName;
    }

    /**
     * 返回支持最新的jdk版本
     * @return
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * 返回需要处理的注解
     * @return
     */
    public Set<String> getSupportedAnnotationTypes(){
        Set<String> types = new LinkedHashSet<>();
        types.add(BindView.class.getCanonicalName());
        return types;
    }

}
